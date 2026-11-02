package com.iappsan.carlauncher.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Iconos vectoriales propios (trazados estilo Material Icons, 24×24), para no añadir
 * la librería de iconos completa solo por unos pocos.
 */
object LauncherIcons {
    val Play: ImageVector = icon("Play") {
        moveTo(8f, 5f); verticalLineToRelative(14f); lineToRelative(11f, -7f); close()
    }

    val Pause: ImageVector = icon("Pause") {
        moveTo(6f, 19f); horizontalLineToRelative(4f); verticalLineTo(5f); horizontalLineTo(6f); close()
        moveTo(14f, 5f); verticalLineToRelative(14f); horizontalLineToRelative(4f); verticalLineTo(5f); close()
    }

    val Next: ImageVector = icon("Next") {
        moveTo(6f, 18f); lineToRelative(8.5f, -6f); lineTo(6f, 6f); close()
        moveTo(16f, 6f); verticalLineToRelative(12f); horizontalLineToRelative(2f); verticalLineTo(6f); close()
    }

    val Previous: ImageVector = icon("Previous") {
        moveTo(6f, 6f); horizontalLineToRelative(2f); verticalLineToRelative(12f); horizontalLineTo(6f); close()
        moveTo(9.5f, 12f); lineToRelative(8.5f, 6f); verticalLineTo(6f); close()
    }

    /** Rejilla de 3×3 cuadrados: botón "Apps". */
    val Apps: ImageVector = icon("Apps") {
        for (row in 0..2) for (col in 0..2) {
            val x = 4f + col * 6f
            val y = 4f + row * 6f
            moveTo(x, y); horizontalLineToRelative(4f); verticalLineToRelative(4f); horizontalLineToRelative(-4f); close()
        }
    }

    val ArrowUp: ImageVector = icon("ArrowUp") {
        moveTo(7.41f, 15.41f); lineTo(12f, 10.83f); lineToRelative(4.59f, 4.58f)
        lineTo(18f, 14f); lineToRelative(-6f, -6f); lineToRelative(-6f, 6f); close()
    }

    val ArrowDown: ImageVector = icon("ArrowDown") {
        moveTo(7.41f, 8.59f); lineTo(12f, 13.17f); lineToRelative(4.59f, -4.58f)
        lineTo(18f, 10f); lineToRelative(-6f, 6f); lineToRelative(-6f, -6f); close()
    }

    val Close: ImageVector = icon("Close") {
        moveTo(19f, 6.41f); lineTo(17.59f, 5f); lineTo(12f, 10.59f); lineTo(6.41f, 5f)
        lineTo(5f, 6.41f); lineTo(10.59f, 12f); lineTo(5f, 17.59f); lineTo(6.41f, 19f)
        lineTo(12f, 13.41f); lineTo(17.59f, 19f); lineTo(19f, 17.59f); lineTo(13.41f, 12f); close()
    }

    /** Flecha de navegación (cuando la notificación no trae la flecha de la maniobra). */
    val Navigation: ImageVector = icon("Navigation") {
        moveTo(12f, 2f); lineTo(4.5f, 20.29f); lineToRelative(0.71f, 0.71f)
        lineTo(12f, 18f); lineToRelative(6.79f, 3f); lineToRelative(0.71f, -0.71f); close()
    }

    val Bluetooth: ImageVector = icon("Bluetooth") {
        moveTo(17.71f, 7.71f); lineTo(12f, 2f); horizontalLineToRelative(-1f); verticalLineToRelative(7.59f)
        lineTo(6.41f, 5f); lineTo(5f, 6.41f); lineTo(10.59f, 12f); lineTo(5f, 17.59f); lineTo(6.41f, 19f)
        lineTo(11f, 14.41f); verticalLineTo(22f); horizontalLineToRelative(1f); lineToRelative(5.71f, -5.71f)
        lineToRelative(-4.3f, -4.29f); close()
        moveTo(13f, 5.83f); lineToRelative(1.88f, 1.88f); lineTo(13f, 9.59f); close()
        moveTo(14.88f, 16.29f); lineTo(13f, 18.17f); verticalLineToRelative(-3.76f); close()
    }

    // `PathBuilder.() -> Unit` = lambda "con receptor": dentro, `this` es el PathBuilder,
    // por eso se puede escribir moveTo(...) directamente (así funcionan los DSL de Kotlin).
    private fun icon(name: String, pathData: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            // El color real lo pone Icon() con su tint; aquí da igual cuál sea.
            .path(fill = SolidColor(Color.Black), pathBuilder = pathData)
            .build()
}
