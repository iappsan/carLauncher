package com.iappsan.carlauncher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Controles reutilizables de Ajustes, todos con ≥52dp de alto para el coche. */

@Composable
internal fun SettingCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    // `ColumnScope.() -> Unit`: el contenido se escribe "dentro" de la Column (puede usar weight, etc.)
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
internal fun SwitchCard(title: String, description: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }, // toda la fila es pulsable, no solo el interruptor
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                description?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

/**
 * Fila de opciones excluyentes (como un grupo de radio buttons, pero con botones grandes).
 * `<T>` = función genérica, igual que en Java.
 */
@Composable
internal fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // `(value, label)` desestructura el Pair en dos variables
        options.forEach { (value, label) ->
            val modifier = Modifier.height(52.dp).widthIn(min = 72.dp)
            if (value == selected) {
                Button(onClick = { onSelect(value) }, modifier = modifier, enabled = enabled) { Text(label) }
            } else {
                OutlinedButton(onClick = { onSelect(value) }, modifier = modifier, enabled = enabled) { Text(label) }
            }
        }
    }
}

/** Selector de hora con botones − / + (más fácil que un reloj en una pantalla del coche). */
@Composable
internal fun HourStepper(label: String, hour: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalButton(onClick = { onChange(hour - 1) }, enabled = enabled, modifier = Modifier.size(56.dp)) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = "%02d:00".format(hour),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(96.dp),
        )
        FilledTonalButton(onClick = { onChange(hour + 1) }, enabled = enabled, modifier = Modifier.size(56.dp)) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
internal fun SectionSpacer() = Spacer(Modifier.height(12.dp))
