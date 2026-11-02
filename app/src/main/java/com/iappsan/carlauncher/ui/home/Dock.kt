package com.iappsan.carlauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.iappsan.carlauncher.data.Shortcut
import com.iappsan.carlauncher.data.ShortcutTarget
import com.iappsan.carlauncher.ui.theme.LauncherTheme

/** Lado de cada botón del dock: cumple el mínimo de 80dp para tocarlo sin mirar. */
private val DockTileSize = 88.dp

/**
 * Fila inferior de accesos al estilo del Dock de la Mac: solo iconos, centrada, y si no caben
 * se desliza de lado (los botones nunca se encogen).
 */
@Composable
fun Dock(
    items: List<ShortcutItem>,
    onShortcutClick: (Shortcut) -> Unit,
    onShortcutRemove: (Shortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = LauncherTheme.layout
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // El Surface se ajusta a su contenido hasta el ancho de pantalla; a partir de ahí,
        // horizontalScroll hace que la fila se pueda deslizar.
        Surface(
            modifier = Modifier.widthIn(max = maxWidth),
            shape = RoundedCornerShape(layout.cornerRadius + 8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items.forEach { item ->
                    DockTile(
                        item = item,
                        onClick = { onShortcutClick(item.shortcut) },
                        onRemove = { onShortcutRemove(item.shortcut) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DockTile(item: ShortcutItem, onClick: () -> Unit, onRemove: () -> Unit) {
    val layout = LauncherTheme.layout
    val isDrawer = item.shortcut.target == ShortcutTarget.AppDrawer
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .size(DockTileSize)
                .clip(RoundedCornerShape(layout.cornerRadius))
                // "Apps" resaltado, igual que en la rejilla
                .then(if (isDrawer) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = if (isDrawer) null else ({ menuOpen = true }),
                )
                // Sin texto visible: el nombre queda para TalkBack.
                .semantics { contentDescription = item.shortcut.label }
                .alpha(if (item.available) 1f else 0.45f),
            contentAlignment = Alignment.Center,
        ) {
            // El icono ocupa casi todo el botón, como en la Mac (el de "Apps" algo menos, por su fondo).
            val iconSize = if (isDrawer) layout.iconSize else maxOf(layout.iconSize, DockTileSize - 20.dp)
            CompositionLocalProvider(
                LocalContentColor provides if (isDrawer) MaterialTheme.colorScheme.onPrimaryContainer else LocalContentColor.current,
            ) {
                TileIcon(item, iconSize)
            }
        }
        RemoveShortcutMenu(expanded = menuOpen, onDismiss = { menuOpen = false }, onRemove = onRemove)
    }
}
