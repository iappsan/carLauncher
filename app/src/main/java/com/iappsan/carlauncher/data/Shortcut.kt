package com.iappsan.carlauncher.data

/**
 * Qué hace un botón de Home al pulsarlo.
 *
 * `sealed interface` ≈ una jerarquía cerrada (como un sealed interface de Java 17):
 * el compilador conoce todas las variantes, así que un `when` sobre ella no necesita `else`.
 */
sealed interface ShortcutTarget {
    /** Abre la actividad principal de una app instalada. */
    data class App(val packageName: String) : ShortcutTarget

    /** Abre los ajustes del sistema. `data object` = singleton con toString/equals bonitos. */
    data object SystemSettings : ShortcutTarget

    /** Abre el cajón de apps del launcher. */
    data object AppDrawer : ShortcutTarget
}

/** Un acceso de la rejilla de Home. `data class` ≈ un record de Java. */
data class Shortcut(
    val id: String,
    val label: String,
    val target: ShortcutTarget,
)
