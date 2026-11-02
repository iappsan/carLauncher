package com.iappsan.carlauncher.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iappsan.carlauncher.ui.theme.LauncherTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SPANISH: Locale = Locale.forLanguageTag("es")

/**
 * Reloj grande + fecha. Se actualiza justo al cambiar el minuto (un único "despertar" por minuto,
 * nada de refrescos por segundo).
 */
@Composable
fun ClockHeader(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val locale: Locale = LocalConfiguration.current.locale

    // produceState lanza una coroutine ligada a este composable; se cancela sola al salir.
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(60_000L - System.currentTimeMillis() % 60_000L) // hasta el próximo minuto exacto
        }
    }

    val is24h = DateFormat.is24HourFormat(context)
    val timeFormat = remember(is24h) { DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm") }
    val amPmFormat = remember(locale) { DateTimeFormatter.ofPattern("a", locale) }
    // Fecha fija tipo "02/NOVIEMBRE/2026". El mes va en español como el resto de la interfaz,
    // aunque el estéreo esté en otro idioma.
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd/MMMM/yyyy", SPANISH) }

    // Con imagen de fondo, una sombra suave hace legible el texto sobre cualquier zona de la foto.
    val shadow = if (LauncherTheme.current.backgroundImage != null) {
        Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 8f)
    } else {
        null
    }
    val color = MaterialTheme.colorScheme.onBackground

    Row(modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = now.format(timeFormat),
            style = TextStyle(
                fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
                fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.1f,
                fontWeight = FontWeight.Medium,
                lineHeight = 64.sp,
                shadow = shadow,
            ),
            color = color,
        )
        Spacer(Modifier.width(16.dp))
        Column {
            if (!is24h) {
                Text(now.format(amPmFormat), style = MaterialTheme.typography.titleLarge.copy(shadow = shadow), color = color)
            }
            Text(
                text = now.format(dateFormat).uppercase(SPANISH),
                style = MaterialTheme.typography.bodyMedium.copy(shadow = shadow),
                color = color.copy(alpha = 0.85f),
            )
        }
    }
}
