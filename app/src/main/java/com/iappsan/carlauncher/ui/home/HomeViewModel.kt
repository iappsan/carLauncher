package com.iappsan.carlauncher.ui.home

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iappsan.carlauncher.AppContainer
import com.iappsan.carlauncher.data.AppRepository
import com.iappsan.carlauncher.data.Shortcut
import com.iappsan.carlauncher.data.ShortcutRepository
import com.iappsan.carlauncher.data.ShortcutTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Un botón de Home ya resuelto para pintar. */
data class ShortcutItem(
    val shortcut: Shortcut,
    val icon: ImageBitmap?,
    /** `false` si es una app que no está instalada (se pinta atenuado). */
    val available: Boolean,
)

data class HomeUiState(
    val items: List<ShortcutItem> = emptyList(),
    val columns: Int = 3,
)

class HomeViewModel(
    private val shortcutRepository: ShortcutRepository,
    private val appRepository: AppRepository,
) : ViewModel() {

    // Patrón habitual: el MutableStateFlow es privado y se expone como StateFlow de solo lectura.
    private val _state = MutableStateFlow(HomeUiState(columns = shortcutRepository.columns))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Recalcula los accesos (p. ej. al volver a Home, por si se instaló/desinstaló una app). */
    fun refresh() {
        // viewModelScope: coroutines que se cancelan solas cuando el ViewModel se destruye.
        viewModelScope.launch {
            val items = withContext(Dispatchers.Default) {
                shortcutRepository.shortcuts().map { shortcut ->
                    when (val target = shortcut.target) {
                        is ShortcutTarget.App -> ShortcutItem(
                            shortcut = shortcut,
                            icon = appRepository.loadIcon(target.packageName),
                            available = appRepository.isInstalled(target.packageName),
                        )
                        ShortcutTarget.SystemSettings,
                        ShortcutTarget.AppDrawer -> ShortcutItem(shortcut, icon = null, available = true)
                    }
                }
            }
            _state.value = _state.value.copy(items = items)
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { HomeViewModel(container.shortcutRepository, container.appRepository) }
        }
    }
}
