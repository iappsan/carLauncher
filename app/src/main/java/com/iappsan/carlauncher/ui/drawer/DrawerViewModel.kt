package com.iappsan.carlauncher.ui.drawer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iappsan.carlauncher.AppContainer
import com.iappsan.carlauncher.data.AppEntry
import com.iappsan.carlauncher.data.AppRepository
import com.iappsan.carlauncher.data.HiddenAppsRepository
import com.iappsan.carlauncher.data.ShortcutList
import com.iappsan.carlauncher.data.ShortcutRepository
import com.iappsan.carlauncher.data.ShortcutTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer

data class DrawerUiState(
    val apps: List<AppEntry> = emptyList(),
    /** Packages que ya tienen acceso en Home. */
    val homePackages: Set<String> = emptySet(),
    /** Packages que ya tienen acceso en la vista de navegación. */
    val navPackages: Set<String> = emptySet(),
    /** Se está viendo el "cajón de ocultas" en vez del normal. */
    val showingHidden: Boolean = false,
    val hiddenCount: Int = 0,
    val loading: Boolean = true,
)

class DrawerViewModel(
    private val appRepository: AppRepository,
    private val shortcutRepository: ShortcutRepository,
    private val hiddenAppsRepository: HiddenAppsRepository,
) : ViewModel() {

    /**
     * Texto de búsqueda como estado de Compose (no StateFlow): el TextField lo lee y escribe
     * de forma síncrona, así el cursor no "salta" al teclear rápido.
     * `by` delega el getter/setter en el MutableState; `private set` = solo se cambia desde aquí.
     */
    var query by mutableStateOf("")
        private set

    /** `true` = se muestran solo las apps ocultas. */
    private var showHidden by mutableStateOf(false)

    private val allApps = appRepository.changes.map { appRepository.installedApps() }

    private fun packagesIn(list: ShortcutList) = shortcutRepository.shortcuts(list).map { shortcuts ->
        // `as?` = cast seguro: devuelve null si no es de ese tipo (mapNotNull los descarta).
        shortcuts.mapNotNullTo(HashSet()) { (it.target as? ShortcutTarget.App)?.packageName }
    }

    val state: StateFlow<DrawerUiState> =
        // snapshotFlow convierte un estado de Compose en un Flow que emite al cambiar.
        combine(
            allApps,
            snapshotFlow { query },
            packagesIn(ShortcutList.Home),
            packagesIn(ShortcutList.Navigation),
            hiddenAppsRepository.hidden,
            snapshotFlow { showHidden },
        ) { values ->
            // combine con más de 5 flows recibe un Array<Any?>: se recupera cada valor con un cast.
            @Suppress("UNCHECKED_CAST")
            val apps = values[0] as List<AppEntry>
            val q = values[1] as String
            @Suppress("UNCHECKED_CAST")
            val hidden = values[4] as Set<String>
            val inHiddenView = values[5] as Boolean
            // `partition` separa una lista en dos según la condición: (las que cumplen, las que no).
            val (hiddenApps, visibleApps) = apps.partition { it.component.flattenToString() in hidden }
            @Suppress("UNCHECKED_CAST")
            DrawerUiState(
                apps = filter(if (inHiddenView) hiddenApps else visibleApps, q),
                homePackages = values[2] as Set<String>,
                navPackages = values[3] as Set<String>,
                showingHidden = inHiddenView,
                hiddenCount = hiddenApps.size,
                loading = false,
            )
        }
            .flowOn(Dispatchers.Default)
            // WhileSubscribed: deja de trabajar 5 s después de cerrar el cajón.
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrawerUiState())

    fun onQueryChange(value: String) {
        query = value
    }

    fun toggleHiddenView() {
        showHidden = !showHidden
    }

    /** Al cerrar el cajón se vuelve a la vista normal. */
    fun resetView() {
        showHidden = false
        query = ""
    }

    fun setHidden(app: AppEntry, hidden: Boolean) {
        viewModelScope.launch { hiddenAppsRepository.setHidden(app.component, hidden) }
    }

    fun addTo(list: ShortcutList, app: AppEntry) {
        viewModelScope.launch { shortcutRepository.addApp(list, app.packageName, app.label) }
    }

    private fun filter(apps: List<AppEntry>, query: String): List<AppEntry> {
        val q = query.trim().normalized()
        if (q.isEmpty()) return apps
        return apps.filter { it.label.normalized().contains(q) || it.packageName.contains(q) }
    }

    companion object {
        private val DIACRITICS = Regex("\\p{Mn}+")

        /** Minúsculas y sin acentos: "Música" coincide con "musica". Función de extensión sobre String. */
        private fun String.normalized(): String =
            Normalizer.normalize(this, Normalizer.Form.NFD).replace(DIACRITICS, "").lowercase()

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { DrawerViewModel(container.appRepository, container.shortcutRepository, container.hiddenAppsRepository) }
        }
    }
}
