package com.iappsan.carlauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iappsan.carlauncher.R
import com.iappsan.carlauncher.theme.DarkPalette
import com.iappsan.carlauncher.theme.ThemeEntry
import com.iappsan.carlauncher.theme.ThemeSpec

/** Lo que necesita la sección de temas. */
data class ThemeSettingsState(
    val entries: List<ThemeEntry> = emptyList(),
    val selectedId: String = "",
    val warnings: List<String> = emptyList(),
    val themesPath: String? = null,
    val isNight: Boolean = true,
    val hasDayVariant: Boolean = false,
)

@Composable
internal fun ThemeSettings(state: ThemeSettingsState, actions: SettingsActions) {
    // Lista de temas a la izquierda, información a la derecha.
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LazyColumn(Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.entries, key = { it.id }) { entry ->
                ThemeRow(entry, selected = entry.id == state.selectedId, onClick = { actions.onSelectTheme(entry.id) })
            }
        }
        ThemeInfoPanel(
            themesPath = state.themesPath,
            warnings = state.warnings,
            onReload = actions::onReloadThemes,
            modifier = Modifier.weight(0.4f).fillMaxHeight(),
        )
    }
}

@Composable
private fun ThemeRow(entry: ThemeEntry, selected: Boolean, onClick: () -> Unit) {
    val spec = entry.spec
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            // Un tema roto no se puede elegir
            .clickable(enabled = spec != null, onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null, enabled = spec != null)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = spec?.name ?: entry.id,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val subtitle = entry.error
                    ?: listOf(spec?.author.orEmpty(), entry.id).filter { it.isNotEmpty() }.joinToString(" · ")
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (entry.error != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (spec != null) Swatches(spec)
        }
    }
}

/** Muestras de los colores principales del tema (paleta de noche). */
@Composable
private fun Swatches(spec: ThemeSpec) {
    val c = spec.colors
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(
            c.background ?: DarkPalette.background,
            c.surface ?: DarkPalette.surface,
            c.primaryContainer ?: DarkPalette.primaryContainer,
            c.primary ?: DarkPalette.primary,
        ).forEach { hex ->
            Box(
                Modifier
                    .size(28.dp)
                    .background(parseOrGray(hex), CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
            )
        }
    }
}

private fun parseOrGray(hex: String?): Color = try {
    Color(android.graphics.Color.parseColor(hex.orEmpty().trim()))
} catch (_: IllegalArgumentException) {
    Color.Gray
}

@Composable
private fun ThemeInfoPanel(
    themesPath: String?,
    warnings: List<String>,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.settings_themes_folder), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = themesPath ?: stringResource(R.string.settings_storage_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_themes_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onReload, modifier = Modifier.height(56.dp)) {
                Text(stringResource(R.string.settings_reload), style = MaterialTheme.typography.titleMedium)
            }
            if (warnings.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.settings_theme_warnings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                warnings.forEach { warning ->
                    Text("• $warning", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
