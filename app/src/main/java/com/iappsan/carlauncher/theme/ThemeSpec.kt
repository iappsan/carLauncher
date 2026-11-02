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
    val colors: ThemeColors = ThemeColors(),
    val background: BackgroundSpec = BackgroundSpec(),
    val font: FontSpec = FontSpec(),
    val layout: LayoutSpec = LayoutSpec(),
    /** Clave: package de la app, o `settings` / `apps` para esos botones. Valor: ruta de imagen. */
    val icons: Map<String, String> = emptyMap(),
)

/** Colores en `#RRGGBB` o `#AARRGGBB` (con transparencia). */
@Serializable
data class ThemeColors(
    val background: String = "#0B0D10",
    val onBackground: String = "#ECEFF4",
    val surface: String = "#1A1E24",
    val onSurface: String = "#ECEFF4",
    val onSurfaceVariant: String = "#A9B1BC",
    val primary: String = "#4FC3F7",
    val onPrimary: String = "#00222F",
    val primaryContainer: String = "#0D4A63",
    val onPrimaryContainer: String = "#CDEBFA",
    val secondaryContainer: String = "#2C333D",
    val onSecondaryContainer: String = "#DCE3EC",
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
