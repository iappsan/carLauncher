package com.iappsan.carlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Las dos listas de accesos: la de Home normal y la que se ve mientras hay una ruta activa. */
enum class ShortcutList(internal val key: String) {
    Home("home_shortcuts"),
    Navigation("nav_shortcuts"),
}

/**
 * Accesos de Home, persistidos en DataStore (una lista por [ShortcutList]).
 *
 * Formato guardado (una preferencia de texto, un acceso por línea, campos separados por TAB):
 * `app<TAB>com.waze<TAB>Waze`, `settings` o `apps`. Simple y sin librerías de JSON.
 */
class ShortcutRepository(context: Context) {

    private val dataStore = context.settingsDataStore

    /** Lista actual de accesos; emite de nuevo cada vez que cambia. */
    fun shortcuts(list: ShortcutList): Flow<List<Shortcut>> = dataStore.data
        // Si el fichero está corrupto o no se puede leer, seguimos con los valores por defecto.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs.read(list) }
        .distinctUntilChanged()

    /** Agrega una app justo antes del botón "Apps". No hace nada si ya está. */
    suspend fun addApp(list: ShortcutList, packageName: String, label: String) = update(list) { current ->
        val shortcut = Shortcut.app(packageName, label)
        if (current.any { it.id == shortcut.id }) return@update current
        val drawerIndex = current.indexOfFirst { it.target == ShortcutTarget.AppDrawer }
        current.toMutableList().apply { add(if (drawerIndex >= 0) drawerIndex else size, shortcut) }
    }

    /** Quita un acceso. El botón "Apps" no se puede quitar (sería imposible volver al cajón). */
    suspend fun remove(list: ShortcutList, id: String) = update(list) { current ->
        current.filterNot { it.id == id && it.target != ShortcutTarget.AppDrawer }
    }

    /** Mueve un acceso [delta] posiciones (-1 = antes, +1 = después). */
    suspend fun move(list: ShortcutList, id: String, delta: Int) = update(list) { current ->
        val from = current.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in current.indices) return@update current
        current.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** Vuelve a la lista inicial de accesos. */
    suspend fun resetToDefaults(list: ShortcutList) = update(list) { defaultsFor(list) }

    private suspend fun update(list: ShortcutList, transform: (List<Shortcut>) -> List<Shortcut>) {
        // edit{} es transaccional: lee, transforma y escribe de forma atómica.
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(list.key)] = encode(transform(prefs.read(list)))
        }
    }

    private fun Preferences.read(list: ShortcutList): List<Shortcut> =
        this[stringPreferencesKey(list.key)]?.let(::decode) ?: defaultsFor(list)

    private companion object {
        val DEFAULT_SHORTCUTS = listOf(
            Shortcut.app("com.google.android.apps.maps", "Maps"),
            Shortcut.app("com.waze", "Waze"),
            Shortcut.app("com.spotify.music", "Spotify"),
            Shortcut.app("com.google.android.apps.youtube.music", "YouTube Music"),
            Shortcut.Settings,
            Shortcut.Drawer,
        )

        /** En ruta: pocos botones y grandes (navegación y música). */
        val DEFAULT_NAV_SHORTCUTS = listOf(
            Shortcut.app("com.google.android.apps.maps", "Maps"),
            Shortcut.app("com.waze", "Waze"),
            Shortcut.app("com.spotify.music", "Spotify"),
            Shortcut.app("com.google.android.apps.youtube.music", "YouTube Music"),
            Shortcut.Drawer,
        )

        fun defaultsFor(list: ShortcutList) = when (list) {
            ShortcutList.Home -> DEFAULT_SHORTCUTS
            ShortcutList.Navigation -> DEFAULT_NAV_SHORTCUTS
        }

        private val CONTROL_CHARS = Regex("[\\t\\r\\n]")

        fun encode(shortcuts: List<Shortcut>): String = shortcuts.joinToString("\n") { s ->
            when (val t = s.target) {
                is ShortcutTarget.App -> "app\t${t.packageName}\t${s.label.replace(CONTROL_CHARS, " ")}"
                ShortcutTarget.SystemSettings -> "settings"
                ShortcutTarget.AppDrawer -> "apps"
            }
        }

        fun decode(raw: String): List<Shortcut> {
            val list = raw.lineSequence().mapNotNull { line ->
                val parts = line.split('\t')
                when (parts[0]) {
                    "app" -> if (parts.size >= 3) Shortcut.app(parts[1], parts[2]) else null
                    "settings" -> Shortcut.Settings
                    "apps" -> Shortcut.Drawer
                    else -> null
                }
            }.toList()
            // Red de seguridad: el botón "Apps" siempre debe existir.
            return if (list.any { it.target == ShortcutTarget.AppDrawer }) list else list + Shortcut.Drawer
        }
    }
}
