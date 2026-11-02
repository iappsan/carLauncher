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
    /** Variante cargada: noche (`colors`) o día (`dayColors`). */
    val isNight: Boolean,
    /** El tema define algo distinto para el día (`dayColors` o `dayBackground`). */
    val hasDayVariant: Boolean,
) {
    companion object {
        /** Tema de reserva en código, idéntico a assets/themes/default. Se usa hasta que carga el real. */
        val Builtin: LoadedTheme = resolveBasic(ThemeRepository.DEFAULT_ID, ThemeSpec(name = "Default"), isNight = true, mutableListOf())
    }
}

/** Fondo que corresponde a la variante (el día usa `dayBackground` si existe). */
internal fun ThemeSpec.backgroundFor(isNight: Boolean): BackgroundSpec =
    if (isNight) background else dayBackground ?: background

/** Convierte colores y layout (lo que no requiere leer ficheros). */
internal fun resolveBasic(id: String, spec: ThemeSpec, isNight: Boolean, warnings: MutableList<String>): LoadedTheme {
    val dayColors = spec.dayColors
    val colorScheme = if (!isNight && dayColors != null) dayColors.toColorScheme(warnings, "dayColors")
    else spec.colors.toColorScheme(warnings, "colors")
    return LoadedTheme(
        id = id,
        name = spec.name,
        colorScheme = colorScheme,
        backgroundImage = null,
        backgroundDim = spec.backgroundFor(isNight).dim.coerceIn(0f, 1f),
        fontFamily = FontFamily.Default,
        fontScale = spec.font.scale.coerceIn(0.8f, 1.4f),
        layout = spec.layout.toThemeLayout(),
        icons = emptyMap(),
        warnings = warnings,
        isNight = isNight,
        hasDayVariant = spec.dayColors != null || spec.dayBackground != null,
    )
}

private fun LayoutSpec.toThemeLayout() = ThemeLayout(
    // coerceIn = limitar a un rango (como Math.max(min, Math.min(max, x)))
    columns = columns.coerceIn(1, 6),
    cornerRadius = cornerRadius.coerceIn(0, 48).dp,
    spacing = spacing.coerceIn(4, 32).dp,
    mediaPanelWeight = mediaPanelWeight.coerceIn(0f, 0.6f),
    iconSize = iconSize.coerceIn(32, 96).dp,
    showLabels = showLabels,
)

private fun ThemeColors.toColorScheme(warnings: MutableList<String>, section: String): ColorScheme {
    // Función local: parsea un color o devuelve null y anota el aviso.
    fun parse(field: String, value: String?): Color? = value?.let {
        try {
            Color(android.graphics.Color.parseColor(it.trim()))
        } catch (_: IllegalArgumentException) {
            warnings += "Color inválido en $section.$field: \"$it\""
            null
        }
    }

    // La paleta de relleno (para campos ausentes) depende de si el fondo es claro u oscuro.
    val explicitBackground = parse("background", background)
    val isLight = (explicitBackground?.luminance() ?: 0f) > 0.5f
    val fallback = if (isLight) LightPalette else DarkPalette

    // `!!` = "sé que no es null" (las paletas por defecto tienen todos los campos).
    fun color(field: String, value: String?, default: String?): Color =
        parse(field, value) ?: Color(android.graphics.Color.parseColor(default!!))

    val surface = color("surface", surface, fallback.surface)
    // Menús y diálogos deben ser opacos aunque `surface` tenga transparencia.
    val opaqueSurface = surface.copy(alpha = 1f)
    val secondaryContainer = color("secondaryContainer", secondaryContainer, fallback.secondaryContainer)

    // Base clara u oscura para que los colores que el tema no define encajen.
    val base = if (isLight) lightColorScheme() else darkColorScheme()
    return base.copy(
        background = explicitBackground ?: color("background", null, fallback.background),
        onBackground = color("onBackground", onBackground, fallback.onBackground),
        surface = surface,
        onSurface = color("onSurface", onSurface, fallback.onSurface),
        onSurfaceVariant = color("onSurfaceVariant", onSurfaceVariant, fallback.onSurfaceVariant),
        surfaceContainer = opaqueSurface,
        surfaceContainerHigh = opaqueSurface,
        surfaceVariant = secondaryContainer,
        primary = color("primary", primary, fallback.primary),
        onPrimary = color("onPrimary", onPrimary, fallback.onPrimary),
        primaryContainer = color("primaryContainer", primaryContainer, fallback.primaryContainer),
        onPrimaryContainer = color("onPrimaryContainer", onPrimaryContainer, fallback.onPrimaryContainer),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = color("onSecondaryContainer", onSecondaryContainer, fallback.onSecondaryContainer),
    )
}
