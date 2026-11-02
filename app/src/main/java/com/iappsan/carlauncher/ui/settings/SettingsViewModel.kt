package com.iappsan.carlauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iappsan.carlauncher.AppContainer
import com.iappsan.carlauncher.data.DayNightMode
import com.iappsan.carlauncher.data.LauncherSettings
import com.iappsan.carlauncher.data.LauncherSettingsRepository
import com.iappsan.carlauncher.data.Shortcut
import com.iappsan.carlauncher.data.ShortcutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: LauncherSettings = LauncherSettings(),
    val shortcuts: List<Shortcut> = emptyList(),
)

/** Ajustes del launcher y gestión de accesos. (El tema lo lleva ThemeViewModel.) */
class SettingsViewModel(
    private val settingsRepository: LauncherSettingsRepository,
    private val shortcutRepository: ShortcutRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> =
        combine(settingsRepository.settings, shortcutRepository.shortcuts, ::SettingsUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setShowClock(value: Boolean) = save { settingsRepository.setShowClock(value) }
    fun setColumns(value: Int) = save { settingsRepository.setColumns(value) }
    fun setDayNightMode(value: DayNightMode) = save { settingsRepository.setDayNightMode(value) }
    fun setDayStartHour(value: Int) = save { settingsRepository.setDayStartHour(value) }
    fun setNightStartHour(value: Int) = save { settingsRepository.setNightStartHour(value) }

    fun moveShortcut(id: String, delta: Int) = save { shortcutRepository.move(id, delta) }
    fun removeShortcut(id: String) = save { shortcutRepository.remove(id) }
    fun resetShortcuts() = save { shortcutRepository.resetToDefaults() }

    // Las escrituras en DataStore son suspend: se lanzan en el scope del ViewModel.
    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { SettingsViewModel(container.launcherSettingsRepository, container.shortcutRepository) }
        }
    }
}
