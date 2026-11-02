package com.iappsan.carlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalTime

/**
 * Cómo se decide entre tema de día y de noche.
 * (`Auto` = por horario; se mantiene ese nombre porque es el que ya está guardado en DataStore.)
 */
enum class DayNightMode { Sun, Auto, CarLights, AlwaysNight, AlwaysDay }

/** Cómo se colocan los accesos en Home: rejilla a la derecha o dock inferior (estilo Mac). */
enum class HomeLayout { Grid, Dock }

enum class SpeedUnit(val label: String, val fromKmh: Float) {
    Kmh("km/h", 1f),
    Mph("mph", 0.621371f),
}

/** Preferencias del usuario sobre el launcher (lo que no depende del tema). */
data class LauncherSettings(
    val showClock: Boolean = true,
    val homeLayout: HomeLayout = HomeLayout.Grid,
    /** Columnas de la rejilla; 0 = las que diga el tema. */
    val columns: Int = 0,
    val dayNightMode: DayNightMode = DayNightMode.Sun,
    /** Hora (0–23) a la que empieza el modo día con el horario. */
    val dayStartHour: Int = 7,
    /** Hora (0–23) a la que empieza el modo noche con el horario. */
    val nightStartHour: Int = 19,
    val showSpeed: Boolean = true,
    val speedUnit: SpeedUnit = SpeedUnit.Kmh,
    /** Al conectar el teléfono por Bluetooth, enviar "play" a la última app de música. */
    val resumeMusicOnConnect: Boolean = true,
    /** App que se abre al tocar el indicador de Bluetooth; vacío = ajustes de Bluetooth del sistema. */
    val bluetoothApp: String = "",
) {
    /**
     * Día/noche según el horario. Para el modo [DayNightMode.Sun] es el respaldo mientras no
     * hay ubicación (ver DayNightRepository).
     */
    fun isNightBySchedule(time: LocalTime): Boolean = when (dayNightMode) {
        DayNightMode.AlwaysNight -> true
        DayNightMode.AlwaysDay -> false
        // CarLights usa el horario solo como respaldo (DayNightRepository decide con el modo noche del sistema).
        DayNightMode.Auto, DayNightMode.Sun, DayNightMode.CarLights -> {
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

    suspend fun setShowClock(value: Boolean) = edit { it[KEY_SHOW_CLOCK] = value }
    suspend fun setHomeLayout(value: HomeLayout) = edit { it[KEY_HOME_LAYOUT] = value.name }
    suspend fun setColumns(value: Int) = edit { it[KEY_COLUMNS] = value.coerceIn(0, 6) }
    suspend fun setDayNightMode(value: DayNightMode) = edit { it[KEY_MODE] = value.name }
    suspend fun setDayStartHour(value: Int) = edit { it[KEY_DAY_START] = value.mod(24) }
    suspend fun setNightStartHour(value: Int) = edit { it[KEY_NIGHT_START] = value.mod(24) }
    suspend fun setShowSpeed(value: Boolean) = edit { it[KEY_SHOW_SPEED] = value }
    suspend fun setSpeedUnit(value: SpeedUnit) = edit { it[KEY_SPEED_UNIT] = value.name }
    suspend fun setResumeMusicOnConnect(value: Boolean) = edit { it[KEY_RESUME_MUSIC] = value }
    suspend fun setBluetoothApp(packageName: String) = edit { it[KEY_BT_APP] = packageName }

    /** Devuelve si ya se pidieron los permisos al arrancar por primera vez, y lo marca. */
    suspend fun markPermissionsAsked(): Boolean {
        val asked = dataStore.data.first()[KEY_PERMISSIONS_ASKED] ?: false
        if (!asked) edit { it[KEY_PERMISSIONS_ASKED] = true }
        return asked
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        dataStore.edit { block(it) }
    }

    private fun Preferences.toSettings(): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            showClock = this[KEY_SHOW_CLOCK] ?: d.showClock,
            homeLayout = enumOrNull<HomeLayout>(this[KEY_HOME_LAYOUT]) ?: d.homeLayout,
            columns = this[KEY_COLUMNS] ?: d.columns,
            // runCatching: si el valor guardado no es un enum válido, usar el defecto
            dayNightMode = enumOrNull<DayNightMode>(this[KEY_MODE]) ?: d.dayNightMode,
            dayStartHour = this[KEY_DAY_START] ?: d.dayStartHour,
            nightStartHour = this[KEY_NIGHT_START] ?: d.nightStartHour,
            showSpeed = this[KEY_SHOW_SPEED] ?: d.showSpeed,
            speedUnit = enumOrNull<SpeedUnit>(this[KEY_SPEED_UNIT]) ?: d.speedUnit,
            resumeMusicOnConnect = this[KEY_RESUME_MUSIC] ?: d.resumeMusicOnConnect,
            bluetoothApp = this[KEY_BT_APP] ?: d.bluetoothApp,
        )
    }

    private companion object {
        // `inline reified`: el tipo genérico T está disponible en tiempo de ejecución (enumValueOf<T>).
        inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
            name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }

        val KEY_SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val KEY_HOME_LAYOUT = stringPreferencesKey("home_layout")
        val KEY_COLUMNS = intPreferencesKey("columns")
        val KEY_MODE = stringPreferencesKey("day_night_mode")
        val KEY_DAY_START = intPreferencesKey("day_start_hour")
        val KEY_NIGHT_START = intPreferencesKey("night_start_hour")
        val KEY_SHOW_SPEED = booleanPreferencesKey("show_speed")
        val KEY_SPEED_UNIT = stringPreferencesKey("speed_unit")
        val KEY_RESUME_MUSIC = booleanPreferencesKey("resume_music_on_connect")
        val KEY_BT_APP = stringPreferencesKey("bluetooth_app")
        val KEY_PERMISSIONS_ASKED = booleanPreferencesKey("permissions_asked")
    }
}
