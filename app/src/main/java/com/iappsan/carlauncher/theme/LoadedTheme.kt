package com.iappsan.carlauncher.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Parámetros de layout ya validados y en unidades de Compose. */
@Immutable
data class ThemeLayout(
    val columns: Int,
    val cornerRadius: Dp,
    val spacing: Dp,
    val mediaPanelWeight: Float,
    val iconSize: Dp,
    val showLabels: Boolean,
)

/**
 * Tema listo para pintar: colores parseados, imágenes decodificadas y fuente cargada.
 * `@Immutable` le dice a Compose que no cambia por dentro, así puede saltarse recomposiciones.
 */
@Immutable
data class LoadedTheme(
    val id: String,
    val name: String,
    val colorScheme: ColorScheme,
    val backgroundImage: ImageBitmap?,
    val backgroundDim: Float,
    val fontFamily: FontFamily,
    val fontScale: Float,
    val layout: ThemeLayout,
    /** Iconos personalizados por package (o `settings` / `apps`). */
    val icons: Map<String, ImageBitmap>,
    /** Problemas no fatales encontrados al cargar (colores inválidos, imágenes que faltan…). */
    val warnings: List<String>,
) {
    companion object {
        /** Tema de reserva en código, idéntico a assets/themes/default. Se usa hasta que carga el real. */
        val Builtin: LoadedTheme = resolveBasic(ThemeRepository.DEFAULT_ID, ThemeSpec(name = "Default"), mutableListOf())
    }
}

/** Convierte colores y layout (lo que no requiere leer ficheros). */
internal fun resolveBasic(id: String, spec: ThemeSpec, warnings: MutableList<String>): LoadedTheme =
    LoadedTheme(
        id = id,
        name = spec.name,
        colorScheme = spec.colors.toColorScheme(warnings),
        backgroundImage = null,
        backgroundDim = spec.background.dim.coerceIn(0f, 1f),
        fontFamily = FontFamily.Default,
        fontScale = spec.font.scale.coerceIn(0.8f, 1.4f),
        layout = spec.layout.toThemeLayout(),
        icons = emptyMap(),
        warnings = warnings,
    )

private fun LayoutSpec.toThemeLayout() = ThemeLayout(
    // coerceIn = limitar a un rango (como Math.max(min, Math.min(max, x)))
    columns = columns.coerceIn(1, 6),
    cornerRadius = cornerRadius.coerceIn(0, 48).dp,
    spacing = spacing.coerceIn(4, 32).dp,
    mediaPanelWeight = mediaPanelWeight.coerceIn(0f, 0.6f),
    iconSize = iconSize.coerceIn(32, 96).dp,
    showLabels = showLabels,
)

private fun ThemeColors.toColorScheme(warnings: MutableList<String>): ColorScheme {
    val defaults = ThemeColors()

    // Función local: parsea un color o usa el valor por defecto y anota el aviso.
    fun color(field: String, value: String, fallback: String): Color = try {
        Color(android.graphics.Color.parseColor(value.trim()))
    } catch (_: IllegalArgumentException) {
        warnings += "Color inválido en colors.$field: \"$value\""
        Color(android.graphics.Color.parseColor(fallback))
    }

    val background = color("background", background, defaults.background)
    val surface = color("surface", surface, defaults.surface)
    val onSurface = color("onSurface", onSurface, defaults.onSurface)
    val primary = color("primary", primary, defaults.primary)
    val onPrimary = color("onPrimary", onPrimary, defaults.onPrimary)
    val primaryContainer = color("primaryContainer", primaryContainer, defaults.primaryContainer)
    val onPrimaryContainer = color("onPrimaryContainer", onPrimaryContainer, defaults.onPrimaryContainer)
    val secondaryContainer = color("secondaryContainer", secondaryContainer, defaults.secondaryContainer)
    val onSecondaryContainer = color("onSecondaryContainer", onSecondaryContainer, defaults.onSecondaryContainer)
    val onBackground = color("onBackground", onBackground, defaults.onBackground)
    val onSurfaceVariant = color("onSurfaceVariant", onSurfaceVariant, defaults.onSurfaceVariant)
    // Menús y diálogos deben ser opacos aunque `surface` tenga transparencia.
    val opaqueSurface = surface.copy(alpha = 1f)

    // Base clara u oscura según el fondo, para que los colores que el tema no define encajen.
    val base = if (background.luminance() > 0.5f) lightColorScheme() else darkColorScheme()
    return base.copy(
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainer = opaqueSurface,
        surfaceContainerHigh = opaqueSurface,
        surfaceVariant = secondaryContainer,
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
    )
}
