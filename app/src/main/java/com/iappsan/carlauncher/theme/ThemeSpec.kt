package com.iappsan.carlauncher.theme

import kotlinx.serialization.Serializable

/**
 * Contenido de `theme.json`, tal cual lo escribe el autor del tema (ver docs/THEMES.md).
 *
 * `@Serializable` hace que el plugin de kotlinx.serialization genere en compilación el
 * código para leer/escribir JSON (sin reflexión, a diferencia de Jackson/Gson).
 * Todos los campos tienen valor por defecto: un tema puede definir solo lo que quiera cambiar.
 */
@Serializable
data class ThemeSpec(
    val name: String = "Sin nombre",
    val author: String = "",
    /** Paleta de noche (o la única, si no hay [dayColors]). */
    val colors: ThemeColors = ThemeColors(),
    /** Paleta para el modo día. `null` = el tema se ve igual de día que de noche. */
    val dayColors: ThemeColors? = null,
    val background: BackgroundSpec = BackgroundSpec(),
    /** Fondo para el modo día. `null` = el mismo [background]. */
    val dayBackground: BackgroundSpec? = null,
    val font: FontSpec = FontSpec(),
    val layout: LayoutSpec = LayoutSpec(),
    /** Clave: package de la app, o `settings` / `apps` para esos botones. Valor: ruta de imagen. */
    val icons: Map<String, String> = emptyMap(),
)

/**
 * Colores en `#RRGGBB` o `#AARRGGBB` (con transparencia). Un campo `null` (ausente en el JSON)
 * toma el valor de [DarkPalette] o [LightPalette] según si el `background` es oscuro o claro.
 */
@Serializable
data class ThemeColors(
    val background: String? = null,
    val onBackground: String? = null,
    val surface: String? = null,
    val onSurface: String? = null,
    val onSurfaceVariant: String? = null,
    val primary: String? = null,
    val onPrimary: String? = null,
    val primaryContainer: String? = null,
    val onPrimaryContainer: String? = null,
    val secondaryContainer: String? = null,
    val onSecondaryContainer: String? = null,
)

/** Valores por defecto para temas oscuros (= tema Default de noche). */
val DarkPalette = ThemeColors(
    background = "#0B0D10",
    onBackground = "#ECEFF4",
    surface = "#1A1E24",
    onSurface = "#ECEFF4",
    onSurfaceVariant = "#A9B1BC",
    primary = "#4FC3F7",
    onPrimary = "#00222F",
    primaryContainer = "#0D4A63",
    onPrimaryContainer = "#CDEBFA",
    secondaryContainer = "#2C333D",
    onSecondaryContainer = "#DCE3EC",
)

/** Valores por defecto para temas claros (= tema Default de día). */
val LightPalette = ThemeColors(
    background = "#EEF1F5",
    onBackground = "#12161C",
    surface = "#FFFFFF",
    onSurface = "#12161C",
    onSurfaceVariant = "#4A5361",
    primary = "#0277BD",
    onPrimary = "#FFFFFF",
    primaryContainer = "#CDE8F7",
    onPrimaryContainer = "#00344F",
    secondaryContainer = "#DEE4EC",
    onSecondaryContainer = "#1E252E",
)

@Serializable
data class BackgroundSpec(
    /** Imagen de fondo relativa a la carpeta del tema (WebP recomendado). `null` = color liso. */
    val image: String? = null,
    /** Oscurecimiento sobre la imagen, de 0.0 (nada) a 1.0 (negro). */
    val dim: Float = 0f,
)

@Serializable
data class FontSpec(
    /** Fichero .ttf/.otf relativo a la carpeta del tema. Tiene prioridad sobre [family]. */
    val file: String? = null,
    /** Fuente del sistema: `sans-serif`, `serif`, `monospace`, `sans-serif-condensed`… */
    val family: String = "sans-serif",
    /** Multiplicador del tamaño de todo el texto (0.8–1.4). */
    val scale: Float = 1f,
)

@Serializable
data class LayoutSpec(
    /** Columnas de la rejilla de accesos (1–6). */
    val columns: Int = 3,
    /** Radio de las esquinas de botones y paneles, en dp (0–48). */
    val cornerRadius: Int = 24,
    /** Separación entre botones y márgenes, en dp (4–32). */
    val spacing: Int = 16,
    /** Fracción del ancho para el widget de música (0 lo oculta; máx. 0.6). */
    val mediaPanelWeight: Float = 0.38f,
    /** Tamaño de los iconos de los accesos, en dp (32–96). */
    val iconSize: Int = 56,
    /** Mostrar el nombre bajo el icono en los accesos de Home. */
    val showLabels: Boolean = true,
)
