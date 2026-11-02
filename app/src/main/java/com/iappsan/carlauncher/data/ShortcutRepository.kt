package com.iappsan.carlauncher.data

/**
 * Fuente de los accesos de Home y del número de columnas.
 *
 * Fase 1: valores por defecto en memoria. En fases siguientes se persistirá en DataStore
 * (agregar desde el cajón, reordenar, columnas del tema).
 */
class ShortcutRepository {

    val columns: Int = 3

    fun shortcuts(): List<Shortcut> = DEFAULT_SHORTCUTS

    private companion object {
        // `companion object` ≈ miembros static de Java.
        val DEFAULT_SHORTCUTS = listOf(
            Shortcut("maps", "Maps", ShortcutTarget.App("com.google.android.apps.maps")),
            Shortcut("waze", "Waze", ShortcutTarget.App("com.waze")),
            Shortcut("spotify", "Spotify", ShortcutTarget.App("com.spotify.music")),
            Shortcut("ytmusic", "YouTube Music", ShortcutTarget.App("com.google.android.apps.youtube.music")),
            Shortcut("settings", "Ajustes", ShortcutTarget.SystemSettings),
            Shortcut("apps", "Apps", ShortcutTarget.AppDrawer),
        )
    }
}
