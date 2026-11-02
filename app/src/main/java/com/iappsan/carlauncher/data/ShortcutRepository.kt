package com.iappsan.carlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Accesos de Home, persistidos en DataStore.
 *
 * Formato guardado (una preferencia de texto, un acceso por línea, campos separados por TAB):
 * `app<TAB>com.waze<TAB>Waze`, `settings` o `apps`. Simple y sin librerías de JSON.
 */
class ShortcutRepository(context: Context) {

    private val dataStore = context.settingsDataStore

    /** Lista actual de accesos; emite de nuevo cada vez que cambia. */
    val shortcuts: Flow<List<Shortcut>> = dataStore.data
        // Si el fichero está corrupto o no se puede leer, seguimos con los valores por defecto.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[KEY_SHORTCUTS]?.let(::decode) ?: DEFAULT_SHORTCUTS }
        .distinctUntilChanged()

    /** Agrega una app justo antes del botón "Apps". No hace nada si ya está. */
    suspend fun addApp(packageName: String, label: String) = update { current ->
        val shortcut = Shortcut.app(packageName, label)
        if (current.any { it.id == shortcut.id }) return@update current
        val drawerIndex = current.indexOfFirst { it.target == ShortcutTarget.AppDrawer }
        current.toMutableList().apply { add(if (drawerIndex >= 0) drawerIndex else size, shortcut) }
    }

    /** Quita un acceso. El botón "Apps" no se puede quitar (sería imposible volver al cajón). */
    suspend fun remove(id: String) = update { current ->
        current.filterNot { it.id == id && it.target != ShortcutTarget.AppDrawer }
    }

    private suspend fun update(transform: (List<Shortcut>) -> List<Shortcut>) {
        // edit{} es transaccional: lee, transforma y escribe de forma atómica.
        dataStore.edit { prefs ->
            val current = prefs[KEY_SHORTCUTS]?.let(::decode) ?: DEFAULT_SHORTCUTS
            prefs[KEY_SHORTCUTS] = encode(transform(current))
        }
    }

    private companion object {
        val KEY_SHORTCUTS = stringPreferencesKey("home_shortcuts")

        val DEFAULT_SHORTCUTS = listOf(
            Shortcut.app("com.google.android.apps.maps", "Maps"),
            Shortcut.app("com.waze", "Waze"),
            Shortcut.app("com.spotify.music", "Spotify"),
            Shortcut.app("com.google.android.apps.youtube.music", "YouTube Music"),
            Shortcut.Settings,
            Shortcut.Drawer,
        )

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
