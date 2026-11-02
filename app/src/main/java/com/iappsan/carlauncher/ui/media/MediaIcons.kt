package com.iappsan.carlauncher.ui.media

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Los 4 iconos de reproducción dibujados como vectores (trazados de Material Icons, 24×24),
 * para no añadir la librería de iconos completa solo por esto.
 */
object MediaIcons {
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

    // `PathBuilder.() -> Unit` = lambda "con receptor": dentro, `this` es el PathBuilder,
    // por eso se puede escribir moveTo(...) directamente (así funcionan los DSL de Kotlin).
    private fun icon(name: String, pathData: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            // El color real lo pone Icon() con su tint; aquí da igual cuál sea.
            .path(fill = SolidColor(Color.Black), pathBuilder = pathData)
            .build()
}
