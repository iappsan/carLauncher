package com.iappsan.carlauncher.ui.home

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iappsan.carlauncher.AppContainer
import com.iappsan.carlauncher.data.AppRepository
import com.iappsan.carlauncher.data.LauncherSettingsRepository
import com.iappsan.carlauncher.data.Shortcut
import com.iappsan.carlauncher.data.ShortcutList
import com.iappsan.carlauncher.data.ShortcutRepository
import com.iappsan.carlauncher.data.ShortcutTarget
import com.iappsan.carlauncher.data.SpeedUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Un botón de Home ya resuelto para pintar. */
data class ShortcutItem(
    val shortcut: Shortcut,
    val icon: ImageBitmap?,
    /** `false` si es una app que no está instalada (se pinta atenuado). */
    val available: Boolean,
)

data class HomeUiState(
    val items: List<ShortcutItem> = emptyList(),
    /** Accesos de la vista de navegación (cuando hay una ruta activa). */
    val navItems: List<ShortcutItem> = emptyList(),
    val showClock: Boolean = true,
    val showSpeed: Boolean = true,
    val speedUnit: SpeedUnit = SpeedUnit.Kmh,
)

class HomeViewModel(
    private val shortcutRepository: ShortcutRepository,
    private val appRepository: AppRepository,
    settingsRepository: LauncherSettingsRepository,
) : ViewModel() {

    /**
     * Se recalcula solo cuando cambian los accesos guardados, las apps instaladas o los ajustes.
     * `combine` emite cada vez que cualquiera de los flows emite; `stateIn` lo convierte
     * en un StateFlow que vive mientras viva el ViewModel (viewModelScope).
     */
    val state: StateFlow<HomeUiState> =
        combine(
            shortcutRepository.shortcuts(ShortcutList.Home),
            shortcutRepository.shortcuts(ShortcutList.Navigation),
            appRepository.changes,
            settingsRepository.settings,
        ) { home, nav, _, settings ->
            HomeUiState(
                items = resolve(home),
                navItems = resolve(nav),
                showClock = settings.showClock,
                showSpeed = settings.showSpeed,
                speedUnit = settings.speedUnit,
            )
        }
            .flowOn(Dispatchers.Default) // el trabajo de arriba (iconos, lista de apps) va en segundo plano
            .stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    fun removeShortcut(list: ShortcutList, shortcut: Shortcut) {
        viewModelScope.launch { shortcutRepository.remove(list, shortcut.id) }
    }

    private fun resolve(shortcuts: List<Shortcut>): List<ShortcutItem> {
        val installed = appRepository.installedApps().mapTo(HashSet()) { it.packageName }
        return shortcuts.map { shortcut ->
            when (val target = shortcut.target) {
                is ShortcutTarget.App -> {
                    val available = target.packageName in installed
                    ShortcutItem(
                        shortcut = shortcut,
                        icon = if (available) appRepository.loadIcon(target.packageName) else null,
                        available = available,
                    )
                }
                ShortcutTarget.SystemSettings -> ShortcutItem(shortcut, appRepository.settingsIcon(), available = true)
                ShortcutTarget.AppDrawer -> ShortcutItem(shortcut, icon = null, available = true)
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                HomeViewModel(container.shortcutRepository, container.appRepository, container.launcherSettingsRepository)
            }
        }
    }
}
