package com.iappsan.carlauncher.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iappsan.carlauncher.AppContainer
import com.iappsan.carlauncher.data.LauncherSettingsRepository
import com.iappsan.carlauncher.theme.LoadedTheme
import com.iappsan.carlauncher.theme.ThemeEntry
import com.iappsan.carlauncher.theme.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val themeRepository: ThemeRepository,
    private val settingsRepository: LauncherSettingsRepository,
) : ViewModel() {

    /**
     * Tema efectivo: el cargado + los ajustes del usuario que lo sobreescriben (columnas).
     * `null` hasta que carga el primero: así no se pinta un frame con el tema equivocado al arrancar.
     */
    val theme: StateFlow<LoadedTheme?> =
        combine(themeRepository.currentTheme, settingsRepository.settings) { theme, settings ->
            if (settings.columns > 0) theme.copy(layout = theme.layout.copy(columns = settings.columns)) else theme
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val selectedId: StateFlow<String> =
        themeRepository.selectedThemeId.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeRepository.DEFAULT_ID)

    private val _entries = MutableStateFlow<List<ThemeEntry>>(emptyList())
    val entries: StateFlow<List<ThemeEntry>> = _entries.asStateFlow()

    val userThemesPath: String? get() = themeRepository.userThemesDir?.absolutePath

    /** Home visible: recalcular si toca día o noche. */
    fun onVisible() = settingsRepository.reevaluateDayNight()

    /** Vuelve a buscar temas en disco y recarga el actual (por si se editó). */
    fun refresh() {
        viewModelScope.launch { _entries.value = themeRepository.listThemes() }
        themeRepository.reload()
    }

    fun select(id: String) {
        viewModelScope.launch { themeRepository.select(id) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ThemeViewModel(container.themeRepository, container.launcherSettingsRepository) }
        }
    }
}
