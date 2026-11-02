package com.iappsan.carlauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iappsan.carlauncher.R
import com.iappsan.carlauncher.data.Shortcut
import com.iappsan.carlauncher.data.ShortcutList
import com.iappsan.carlauncher.data.ShortcutTarget
import com.iappsan.carlauncher.ui.icons.LauncherIcons

@Composable
internal fun ShortcutSettings(homeShortcuts: List<Shortcut>, navShortcuts: List<Shortcut>, actions: SettingsActions) {
    // Qué lista se está editando: la de Home normal o la de la vista de navegación.
    var list by rememberSaveable { mutableStateOf(ShortcutList.Home) }
    val shortcuts = if (list == ShortcutList.Home) homeShortcuts else navShortcuts
    // Restaurar es destructivo: primer toque pide confirmación, el segundo lo hace.
    var confirmReset by remember(list) { mutableStateOf(false) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ChoiceRow(
                options = listOf(
                    ShortcutList.Home to stringResource(R.string.settings_list_home),
                    ShortcutList.Navigation to stringResource(R.string.settings_list_navigation),
                ),
                selected = list,
                onSelect = { list = it },
            )
        }
        item {
            Text(
                stringResource(if (list == ShortcutList.Home) R.string.settings_list_home_desc else R.string.settings_list_navigation_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Text(
                stringResource(R.string.settings_shortcuts_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        itemsIndexed(shortcuts, key = { _, s -> s.id }) { index, shortcut ->
            ShortcutRow(
                list = list,
                shortcut = shortcut,
                canMoveUp = index > 0,
                canMoveDown = index < shortcuts.lastIndex,
                actions = actions,
            )
        }
        item {
            OutlinedButton(
                onClick = {
                    if (confirmReset) actions.onResetShortcuts(list)
                    confirmReset = !confirmReset
                },
                modifier = Modifier.height(56.dp),
            ) {
                Text(
                    stringResource(if (confirmReset) R.string.settings_shortcuts_reset_confirm else R.string.settings_shortcuts_reset),
                    color = if (confirmReset) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ShortcutRow(
    list: ShortcutList,
    shortcut: Shortcut,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    actions: SettingsActions,
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(shortcut.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = when (val target = shortcut.target) {
                        is ShortcutTarget.App -> target.packageName
                        else -> stringResource(R.string.settings_shortcut_system)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            RowButton(LauncherIcons.ArrowUp, stringResource(R.string.settings_move_up), canMoveUp) {
                actions.onMoveShortcut(list, shortcut.id, -1)
            }
            RowButton(LauncherIcons.ArrowDown, stringResource(R.string.settings_move_down), canMoveDown) {
                actions.onMoveShortcut(list, shortcut.id, +1)
            }
            // El botón "Apps" no se puede quitar
            RowButton(
                LauncherIcons.Close,
                stringResource(R.string.remove_from_home),
                enabled = shortcut.target != ShortcutTarget.AppDrawer,
            ) { actions.onRemoveShortcut(list, shortcut.id) }
        }
    }
}

@Composable
private fun RowButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(52.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(28.dp))
    }
}
