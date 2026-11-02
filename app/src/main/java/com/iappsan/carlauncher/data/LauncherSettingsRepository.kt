package com.iappsan.carlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.time.LocalTime

enum class DayNightMode { Auto, AlwaysNight, AlwaysDay }

/** Preferencias del usuario sobre el launcher (lo que no depende del tema). */
data class LauncherSettings(
    val showClock: Boolean = true,
    /** Columnas de la rejilla; 0 = las que diga el tema. */
    val columns: Int = 0,
    val dayNightMode: DayNightMode = DayNightMode.Auto,
    /** Hora (0–23) a la que empieza el modo día en automático. */
    val dayStartHour: Int = 7,
    /** Hora (0–23) a la que empieza el modo noche en automático. */
    val nightStartHour: Int = 19,
) {
    /** ¿Toca modo noche a esta hora? */
    fun isNightAt(time: LocalTime): Boolean = when (dayNightMode) {
        DayNightMode.AlwaysNight -> true
        DayNightMode.AlwaysDay -> false
        DayNightMode.Auto -> {
            val hour = time.hour
            when {
                dayStartHour == nightStartHour -> false
                dayStartHour < nightStartHour -> hour < dayStartHour || hour >= nightStartHour
                else -> hour >= nightStartHour && hour < dayStartHour // p. ej. día 20 h, noche 6 h
            }
        }
    }
}

class LauncherSettingsRepository(context: Context) {

    private val dataStore = context.settingsDataStore

    val settings: Flow<LauncherSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }
        .distinctUntilChanged()

    // Señal para recalcular día/noche (la hora cambia aunque los ajustes no).
    private val timeTick = MutableStateFlow(0)

    /**
     * `true` si ahora toca modo noche. Se evalúa al arrancar y cada vez que se llama a
     * [reevaluateDayNight] (al volver a Home), no en tiempo real: así el tema no cambia de golpe
     * mientras se usa.
     */
    val isNight: Flow<Boolean> = combine(settings, timeTick) { s, _ -> s.isNightAt(LocalTime.now()) }
        .distinctUntilChanged()

    fun reevaluateDayNight() = timeTick.update { it + 1 }

    suspend fun setShowClock(value: Boolean) = dataStore.edit { it[KEY_SHOW_CLOCK] = value }
    suspend fun setColumns(value: Int) = dataStore.edit { it[KEY_COLUMNS] = value.coerceIn(0, 6) }
    suspend fun setDayNightMode(value: DayNightMode) = dataStore.edit { it[KEY_MODE] = value.name }
    suspend fun setDayStartHour(value: Int) = dataStore.edit { it[KEY_DAY_START] = value.mod(24) }
    suspend fun setNightStartHour(value: Int) = dataStore.edit { it[KEY_NIGHT_START] = value.mod(24) }

    private fun Preferences.toSettings(): LauncherSettings {
        val defaults = LauncherSettings()
        return LauncherSettings(
            showClock = this[KEY_SHOW_CLOCK] ?: defaults.showClock,
            columns = this[KEY_COLUMNS] ?: defaults.columns,
            // runCatching: si el valor guardado no es un enum válido, usar el defecto
            dayNightMode = this[KEY_MODE]?.let { runCatching { DayNightMode.valueOf(it) }.getOrNull() }
                ?: defaults.dayNightMode,
            dayStartHour = this[KEY_DAY_START] ?: defaults.dayStartHour,
            nightStartHour = this[KEY_NIGHT_START] ?: defaults.nightStartHour,
        )
    }

    private companion object {
        val KEY_SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val KEY_COLUMNS = intPreferencesKey("columns")
        val KEY_MODE = stringPreferencesKey("day_night_mode")
        val KEY_DAY_START = intPreferencesKey("day_start_hour")
        val KEY_NIGHT_START = intPreferencesKey("night_start_hour")
    }
}
