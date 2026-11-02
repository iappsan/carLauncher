package com.iappsan.carlauncher.data.location

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** Amanecer y atardecer de un día. `null` en días polares (el sol no sale o no se pone). */
data class SunTimes(
    val sunrise: ZonedDateTime?,
    val sunset: ZonedDateTime?,
    /** Sol de medianoche: es de día todo el día. */
    val polarDay: Boolean = false,
    /** Noche polar: es de noche todo el día. */
    val polarNight: Boolean = false,
)

/**
 * Cálculo de amanecer/atardecer sin internet (algoritmo del "Almanac for Computers", 1990,
 * el mismo que usan muchas calculadoras solares). Precisión de ±1–2 minutos, de sobra para
 * cambiar el tema del launcher.
 */
object SunCalculator {

    /** Altura del sol en el amanecer "oficial": 90° 50' (incluye refracción y radio del disco). */
    private const val ZENITH = 90.833

    fun sunTimes(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId): SunTimes {
        val rise = utcHours(date, latitude, longitude, rising = true)
        val set = utcHours(date, latitude, longitude, rising = false)
        // Si el sol no sale/no se pone, se distingue día polar de noche polar por el signo.
        if (rise is Event.Never || set is Event.Never) {
            val polarDay = (rise as? Event.Never)?.alwaysUp ?: (set as Event.Never).alwaysUp
            return SunTimes(null, null, polarDay = polarDay, polarNight = !polarDay)
        }
        val sunrise = toZoned(date, (rise as Event.At).utcHours, zone)
        var sunset = toZoned(date, (set as Event.At).utcHours, zone)
        // Si la zona horaria del equipo no es la del lugar (o muy al oeste de su huso), el atardecer
        // puede caer pasada la medianoche: pertenece al día siguiente.
        if (!sunset.isAfter(sunrise)) sunset = sunset.plusDays(1)
        return SunTimes(sunrise = sunrise, sunset = sunset)
    }

    /** ¿Es de noche en [now] en esta posición? */
    fun isNight(now: ZonedDateTime, latitude: Double, longitude: Double): Boolean {
        val today = sunTimes(now.toLocalDate(), latitude, longitude, now.zone)
        if (today.polarDay) return false
        if (today.polarNight) return true
        // Es de día si estamos dentro del intervalo amanecer–atardecer de hoy, o del de ayer
        // (cuando el atardecer de ayer cae después de medianoche).
        val yesterday = sunTimes(now.toLocalDate().minusDays(1), latitude, longitude, now.zone)
        val isDay = listOf(today, yesterday).any { t ->
            t.sunrise != null && t.sunset != null && !now.isBefore(t.sunrise) && now.isBefore(t.sunset)
        }
        return !isDay
    }

    // Resultado de un evento solar: a cierta hora UTC, o nunca (con el sol siempre arriba o abajo).
    private sealed interface Event {
        data class At(val utcHours: Double) : Event
        data class Never(val alwaysUp: Boolean) : Event
    }

    private fun utcHours(date: LocalDate, latitude: Double, longitude: Double, rising: Boolean): Event {
        val n = date.dayOfYear
        val lngHour = longitude / 15.0
        val t = n + ((if (rising) 6.0 else 18.0) - lngHour) / 24.0

        // Anomalía media y longitud verdadera del sol
        val m = 0.9856 * t - 3.289
        val l = normalize(m + 1.916 * sinD(m) + 0.020 * sinD(2 * m) + 282.634, 360.0)

        // Ascensión recta, en el mismo cuadrante que L, en horas
        var ra = normalize(atanD(0.91764 * tanD(l)), 360.0)
        ra += floor(l / 90.0) * 90.0 - floor(ra / 90.0) * 90.0
        ra /= 15.0

        // Declinación
        val sinDec = 0.39782 * sinD(l)
        val cosDec = cos(asin(sinDec))

        // Ángulo horario local
        val cosH = (cosD(ZENITH) - sinDec * sinD(latitude)) / (cosDec * cosD(latitude))
        if (cosH > 1) return Event.Never(alwaysUp = false) // nunca sale
        if (cosH < -1) return Event.Never(alwaysUp = true) // nunca se pone

        val h = (if (rising) 360.0 - acosD(cosH) else acosD(cosH)) / 15.0
        val localMeanTime = h + ra - 0.06571 * t - 6.622
        return Event.At(normalize(localMeanTime - lngHour, 24.0))
    }

    private fun toZoned(date: LocalDate, utcHours: Double, zone: ZoneId): ZonedDateTime {
        val utc = date.atStartOfDay(ZoneOffset.UTC).plusSeconds((utcHours * 3600).toLong())
        var local = utc.withZoneSameInstant(zone)
        // En longitudes extremas el evento puede caer en el día UTC anterior/siguiente.
        if (local.toLocalDate().isBefore(date)) local = local.plusDays(1)
        if (local.toLocalDate().isAfter(date)) local = local.minusDays(1)
        return local
    }

    private fun normalize(value: Double, range: Double) = ((value % range) + range) % range
    private fun sinD(deg: Double) = sin(Math.toRadians(deg))
    private fun cosD(deg: Double) = cos(Math.toRadians(deg))
    private fun tanD(deg: Double) = tan(Math.toRadians(deg))
    private fun atanD(x: Double) = Math.toDegrees(atan(x))
    private fun acosD(x: Double) = Math.toDegrees(acos(x))
}
