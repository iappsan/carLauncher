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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iappsan.carlauncher.R
import com.iappsan.carlauncher.theme.ThemeEntry
import com.iappsan.carlauncher.theme.ThemeSpec
import com.iappsan.carlauncher.ui.theme.CarLauncherTheme

interface SettingsActions {
    fun onSelectTheme(id: String)
    fun onReloadThemes()
    fun onClose()
}

/**
 * Ajustes del launcher. Por ahora solo el selector de tema; en la fase 5 se añaden
 * columnas, accesos, día/noche…
 */
@Composable
fun SettingsScreen(
    entries: List<ThemeEntry>,
    selectedId: String,
    currentWarnings: List<String>,
    themesPath: String?,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(onClick = actions::onClose, modifier = Modifier.height(56.dp)) {
                    Text(stringResource(R.string.drawer_close), style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(12.dp))

            // Horizontal: lista de temas a la izquierda, información a la derecha.
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(0.6f)) {
                    Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(entries, key = { it.id }) { entry ->
                            ThemeRow(entry, selected = entry.id == selectedId, onClick = { actions.onSelectTheme(entry.id) })
                        }
                    }
                }
                ThemeInfoPanel(
                    themesPath = themesPath,
                    warnings = currentWarnings,
                    onReload = actions::onReloadThemes,
                    modifier = Modifier.weight(0.4f).fillMaxHeight(),
                )
            }
        }
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
                val subtitle = entry.error ?: listOf(spec?.author.orEmpty(), entry.id).filter { it.isNotEmpty() }.joinToString(" · ")
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

/** Muestras de los colores principales del tema. */
@Composable
private fun Swatches(spec: ThemeSpec) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(spec.colors.background, spec.colors.surface, spec.colors.primaryContainer, spec.colors.primary)
            .forEach { hex ->
                Box(
                    Modifier
                        .size(28.dp)
                        .background(parseOrGray(hex), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                )
            }
    }
}

private fun parseOrGray(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex.trim()))
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

@Preview(widthDp = 1024, heightDp = 600)
@Composable
private fun SettingsScreenPreview() {
    CarLauncherTheme {
        SettingsScreen(
            entries = listOf(
                ThemeEntry("builtin:default", ThemeSpec(name = "Default", author = "CarLauncher")),
                ThemeEntry("file:roto", spec = null, error = "theme.json mal formado"),
            ),
            selectedId = "builtin:default",
            currentWarnings = listOf("Color inválido en colors.primary: \"azul\""),
            themesPath = "/storage/emulated/0/Android/data/com.iappsan.carlauncher/files/themes",
            actions = object : SettingsActions {
                override fun onSelectTheme(id: String) {}
                override fun onReloadThemes() {}
                override fun onClose() {}
            },
        )
    }
}
