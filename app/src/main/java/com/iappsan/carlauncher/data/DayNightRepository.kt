package com.iappsan.carlauncher.data

import com.iappsan.carlauncher.data.location.GeoPoint
import com.iappsan.carlauncher.data.location.LocationRepository
import com.iappsan.carlauncher.data.location.SunCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime

/** Decide si toca la variante de día o de noche del tema. */
class DayNightRepository(
    settingsRepository: LauncherSettingsRepository,
    locationRepository: LocationRepository,
    systemNightMonitor: SystemNightMonitor,
) {
    // Señal para recalcular (la hora avanza aunque nada más cambie).
    private val tick = MutableStateFlow(0)

    /**
     * `true` si ahora toca modo noche. Por sol/horario se evalúa al arrancar y cada vez que se llama
     * a [reevaluate] (al volver a Home), no en tiempo real: así el tema no cambia de golpe mientras se usa.
     * Por luces del carro, en cambio, sigue al modo noche del sistema al instante.
     */
    val isNight: Flow<Boolean> =
        combine(
            settingsRepository.settings,
            locationRepository.lastKnownPoint,
            systemNightMonitor.isNight,
            tick,
        ) { settings, point, systemNight, _ ->
            if (settings.dayNightMode == DayNightMode.CarLights) systemNight
            else isNight(settings, point, ZonedDateTime.now())
        }.distinctUntilChanged()

    fun reevaluate() = tick.update { it + 1 }

    companion object {
        fun isNight(settings: LauncherSettings, point: GeoPoint?, now: ZonedDateTime): Boolean =
            if (settings.dayNightMode == DayNightMode.Sun && point != null) {
                SunCalculator.isNight(now, point.latitude, point.longitude)
            } else {
                // Por horario, fijo, o "por sol" sin ubicación todavía
                settings.isNightBySchedule(now.toLocalTime())
            }
    }
}
