package com.iappsan.carlauncher.data.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class SunCalculatorTest {

    private val utc: ZoneId = ZoneOffset.UTC

    /** Comprueba que [actual] está a menos de [toleranceMin] minutos de HH:MM UTC. */
    private fun assertNear(expected: String, actual: ZonedDateTime?, toleranceMin: Long = 5) {
        val (h, m) = expected.split(":").map { it.toInt() }
        val exp = actual!!.withZoneSameInstant(utc).withHour(h).withMinute(m).withSecond(0).withNano(0)
        val diff = Duration.between(exp, actual.withZoneSameInstant(utc)).abs().toMinutes()
        assertTrue("esperado ~$expected UTC, obtenido ${actual.withZoneSameInstant(utc)}", diff <= toleranceMin)
    }

    @Test
    fun londres_solsticio_de_verano() {
        // Londres, 21 jun: amanecer 04:43 BST (03:43 UTC), atardecer 21:21 BST (20:21 UTC)
        val t = SunCalculator.sunTimes(LocalDate.of(2026, 6, 21), 51.5074, -0.1278, utc)
        assertNear("03:43", t.sunrise)
        assertNear("20:21", t.sunset)
    }

    @Test
    fun ecuador_equinoccio_dura_unas_12_horas() {
        val t = SunCalculator.sunTimes(LocalDate.of(2026, 3, 20), 0.0, 0.0, utc)
        val length = Duration.between(t.sunrise, t.sunset).toMinutes()
        assertTrue("duración del día $length min", length in 720..735)
        assertNear("06:04", t.sunrise, toleranceMin = 6)
    }

    @Test
    fun ciudad_de_mexico_hora_local() {
        // CDMX (UTC-6 todo el año desde 2022), 21 jun: mediodía solar ~12:38 local y ~13 h 13 min de día
        // → amanecer ~06:00 y atardecer ~19:15 hora local.
        val zone = ZoneId.of("America/Mexico_City")
        val t = SunCalculator.sunTimes(LocalDate.of(2026, 6, 21), 19.4326, -99.1332, zone)
        assertEquals(LocalDate.of(2026, 6, 21), t.sunrise!!.toLocalDate())
        assertEquals(LocalDate.of(2026, 6, 21), t.sunset!!.toLocalDate())
        assertNear("12:00", t.sunrise) // 06:00 local
        assertNear("01:15", t.sunset) // 19:15 local = 01:15 UTC del día siguiente
    }

    @Test
    fun svalbard_sol_de_medianoche_y_noche_polar() {
        val summer = SunCalculator.sunTimes(LocalDate.of(2026, 6, 21), 78.22, 15.65, utc)
        assertTrue(summer.polarDay)
        assertNull(summer.sunrise)
        val winter = SunCalculator.sunTimes(LocalDate.of(2026, 12, 21), 78.22, 15.65, utc)
        assertTrue(winter.polarNight)
    }

    @Test
    fun zona_del_equipo_distinta_a_la_del_lugar() {
        // Equipo en UTC pero en CDMX (caso del emulador): el sol sale ~12:37 UTC y se pone ~00:02 UTC
        // del día siguiente. A las 16:06 UTC es de día, no de noche.
        val t = SunCalculator.sunTimes(LocalDate.of(2026, 11, 2), 19.4326, -99.1332, utc)
        assertTrue(t.sunset!!.isAfter(t.sunrise))
        assertFalse(SunCalculator.isNight(ZonedDateTime.of(2026, 11, 2, 16, 6, 0, 0, utc), 19.4326, -99.1332))
        // 23:50 UTC: aún de día (el sol se pone pasada la medianoche UTC)
        assertFalse(SunCalculator.isNight(ZonedDateTime.of(2026, 11, 2, 23, 50, 0, 0, utc), 19.4326, -99.1332))
        // 00:01 UTC del día 3: sigue siendo el día del 2 (intervalo de "ayer")
        assertFalse(SunCalculator.isNight(ZonedDateTime.of(2026, 11, 3, 0, 1, 0, 0, utc), 19.4326, -99.1332))
        // 03:00 UTC: noche en CDMX (21:00 local)
        assertTrue(SunCalculator.isNight(ZonedDateTime.of(2026, 11, 3, 3, 0, 0, 0, utc), 19.4326, -99.1332))
    }

    @Test
    fun isNight_antes_y_despues_del_sol() {
        val noon = ZonedDateTime.of(2026, 6, 21, 12, 0, 0, 0, utc)
        val midnight = ZonedDateTime.of(2026, 6, 21, 0, 30, 0, 0, utc)
        assertFalse(SunCalculator.isNight(noon, 51.5074, -0.1278))
        assertTrue(SunCalculator.isNight(midnight, 51.5074, -0.1278))
    }
}
