package com.iappsan.carlauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iappsan.carlauncher.AppContainer
import com.iappsan.carlauncher.data.AppEntry
import com.iappsan.carlauncher.data.AppRepository
import com.iappsan.carlauncher.data.DayNightMode
import com.iappsan.carlauncher.data.HomeLayout
import com.iappsan.carlauncher.data.LauncherSettings
import com.iappsan.carlauncher.data.LauncherSettingsRepository
import com.iappsan.carlauncher.data.Shortcut
import com.iappsan.carlauncher.data.ShortcutList
import com.iappsan.carlauncher.data.ShortcutRepository
import com.iappsan.carlauncher.data.SpeedUnit
import com.iappsan.carlauncher.data.location.LocationRepository
import com.iappsan.carlauncher.data.location.SunCalculator
import com.iappsan.carlauncher.data.location.SunTimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class SettingsUiState(
    val settings: LauncherSettings = LauncherSettings(),
    val homeShortcuts: List<Shortcut> = emptyList(),
    val navShortcuts: List<Shortcut> = emptyList(),
    /** Amanecer/atardecer de hoy en la última ubicación; `null` si aún no hay ubicación. */
    val sunTimes: SunTimes? = null,
    /** Apps que parecen de Bluetooth/teléfono, candidatas a abrirse desde el indicador. */
    val bluetoothApps: List<AppEntry> = emptyList(),
)

/** Ajustes del launcher y gestión de accesos. (El tema lo lleva ThemeViewModel.) */
class SettingsViewModel(
    private val settingsRepository: LauncherSettingsRepository,
    private val shortcutRepository: ShortcutRepository,
    locationRepository: LocationRepository,
    appRepository: AppRepository,
) : ViewModel() {

    private val bluetoothApps = appRepository.changes.map {
        appRepository.installedApps().filter(::looksLikeBluetoothApp)
    }

    val state: StateFlow<SettingsUiState> =
        combine(
            settingsRepository.settings,
            shortcutRepository.shortcuts(ShortcutList.Home),
            shortcutRepository.shortcuts(ShortcutList.Navigation),
            locationRepository.lastKnownPoint,
            bluetoothApps,
        ) { settings, home, nav, point, btApps ->
            SettingsUiState(
                settings = settings,
                homeShortcuts = home,
                navShortcuts = nav,
                sunTimes = point?.let {
                    SunCalculator.sunTimes(LocalDate.now(), it.latitude, it.longitude, ZoneId.systemDefault())
                },
                bluetoothApps = btApps,
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setShowClock(value: Boolean) = save { settingsRepository.setShowClock(value) }
    fun setHomeLayout(value: HomeLayout) = save { settingsRepository.setHomeLayout(value) }
    fun setColumns(value: Int) = save { settingsRepository.setColumns(value) }
    fun setDayNightMode(value: DayNightMode) = save { settingsRepository.setDayNightMode(value) }
    fun setDayStartHour(value: Int) = save { settingsRepository.setDayStartHour(value) }
    fun setNightStartHour(value: Int) = save { settingsRepository.setNightStartHour(value) }
    fun setShowSpeed(value: Boolean) = save { settingsRepository.setShowSpeed(value) }
    fun setSpeedUnit(value: SpeedUnit) = save { settingsRepository.setSpeedUnit(value) }
    fun setResumeMusicOnConnect(value: Boolean) = save { settingsRepository.setResumeMusicOnConnect(value) }
    fun setBluetoothApp(packageName: String) = save { settingsRepository.setBluetoothApp(packageName) }

    fun moveShortcut(list: ShortcutList, id: String, delta: Int) = save { shortcutRepository.move(list, id, delta) }
    fun removeShortcut(list: ShortcutList, id: String) = save { shortcutRepository.remove(list, id) }
    fun resetShortcuts(list: ShortcutList) = save { shortcutRepository.resetToDefaults(list) }

    // Las escrituras en DataStore son suspend: se lanzan en el scope del ViewModel.
    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        private val BT_WORDS = listOf("bluetooth", "phone", "dial", "call", "tel", "llamada", "teléfono", "telefono")

        /** Heurística: nombre o package que suenen a Bluetooth/teléfono (las apps "BT" de los head units). */
        private fun looksLikeBluetoothApp(app: AppEntry): Boolean {
            val label = app.label.lowercase()
            val segments = app.packageName.lowercase().split('.')
            return BT_WORDS.any { it in label || segments.any { s -> s.contains(it) } } ||
                segments.any { it == "bt" || it.startsWith("bt") || it.endsWith("bt") }
        }

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                SettingsViewModel(
                    container.launcherSettingsRepository,
                    container.shortcutRepository,
                    container.locationRepository,
                    container.appRepository,
                )
            }
        }
    }
}
