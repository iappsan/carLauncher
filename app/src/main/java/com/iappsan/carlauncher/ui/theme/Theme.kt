package com.iappsan.carlauncher.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.iappsan.carlauncher.theme.LoadedTheme
import com.iappsan.carlauncher.theme.ThemeLayout

/**
 * CompositionLocal = valor "ambiental" que cualquier composable descendiente puede leer sin
 * pasarlo por parámetros (así funciona MaterialTheme por dentro). "static" porque cambia muy poco.
 */
private val LocalLauncherTheme = staticCompositionLocalOf { LoadedTheme.Builtin }

/** Acceso a lo propio del launcher que no cabe en MaterialTheme: `LauncherTheme.layout.columns`. */
object LauncherTheme {
    val current: LoadedTheme
        @Composable @ReadOnlyComposable get() = LocalLauncherTheme.current

    val layout: ThemeLayout
        @Composable @ReadOnlyComposable get() = LocalLauncherTheme.current.layout

    val icons: Map<String, ImageBitmap>
        @Composable @ReadOnlyComposable get() = LocalLauncherTheme.current.icons
}

@Composable
fun CarLauncherTheme(
    theme: LoadedTheme = LoadedTheme.Builtin,
    content: @Composable () -> Unit,
) {
    // remember(clave): solo se recalcula si cambia la fuente o la escala.
    val typography = remember(theme.fontFamily, theme.fontScale) {
        Typography.themed(theme.fontFamily, theme.fontScale)
    }
    CompositionLocalProvider(LocalLauncherTheme provides theme) {
        MaterialTheme(
            colorScheme = theme.colorScheme,
            typography = typography,
            content = content,
        )
    }
}

/** Fondo del tema: color + imagen opcional + oscurecimiento. Va detrás de todo lo demás. */
@Composable
fun ThemeBackground(modifier: Modifier = Modifier) {
    val theme = LauncherTheme.current
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        theme.backgroundImage?.let { image ->
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (theme.backgroundDim > 0f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = theme.backgroundDim)))
            }
        }
    }
}

/** Aplica la fuente y la escala del tema a todos los estilos de la tipografía base. */
private fun Typography.themed(family: FontFamily, scale: Float): Typography {
    fun TextStyle.t() = copy(fontFamily = family, fontSize = fontSize * scale, lineHeight = lineHeight * scale)
    return Typography(
        displayLarge = displayLarge.t(), displayMedium = displayMedium.t(), displaySmall = displaySmall.t(),
        headlineLarge = headlineLarge.t(), headlineMedium = headlineMedium.t(), headlineSmall = headlineSmall.t(),
        titleLarge = titleLarge.t(), titleMedium = titleMedium.t(), titleSmall = titleSmall.t(),
        bodyLarge = bodyLarge.t(), bodyMedium = bodyMedium.t(), bodySmall = bodySmall.t(),
        labelLarge = labelLarge.t(), labelMedium = labelMedium.t(), labelSmall = labelSmall.t(),
    )
}
