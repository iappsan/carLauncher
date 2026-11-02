package com.iappsan.carlauncher.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.iappsan.carlauncher.R
import com.iappsan.carlauncher.data.Shortcut
import com.iappsan.carlauncher.data.ShortcutTarget
import com.iappsan.carlauncher.ui.icons.LauncherIcons
import com.iappsan.carlauncher.ui.theme.CarLauncherTheme
import com.iappsan.carlauncher.ui.theme.LauncherTheme

private val MinTileHeight = 80.dp

@Composable
fun HomeScreen(
    state: HomeUiState,
    onShortcutClick: (Shortcut) -> Unit,
    onShortcutRemove: (Shortcut) -> Unit,
    modifier: Modifier = Modifier,
    // "Slot": quien llama decide qué se pinta a la izquierda (el widget de música).
    // `@Composable () -> Unit` = una función composable pasada como parámetro.
    mediaPanel: @Composable () -> Unit = {},
) {
    val layout = LauncherTheme.layout
    // Sin fondo propio: lo pinta ThemeBackground detrás (color o imagen del tema).
    Row(
        modifier = modifier
            .fillMaxSize()
            // Deja espacio para barras del sistema / recortes de pantalla
            .safeDrawingPadding()
            .padding(layout.spacing),
        horizontalArrangement = Arrangement.spacedBy(layout.spacing),
    ) {
        // Un tema puede ocultar el widget con mediaPanelWeight = 0
        if (layout.mediaPanelWeight > 0f) {
            Box(Modifier.weight(layout.mediaPanelWeight).fillMaxHeight()) { mediaPanel() }
        }
        Column(
            modifier = Modifier.weight(1f - layout.mediaPanelWeight),
            verticalArrangement = Arrangement.spacedBy(layout.spacing / 2),
        ) {
            if (state.showClock) ClockHeader()
            ShortcutGrid(
                items = state.items,
                columns = layout.columns,
                onShortcutClick = onShortcutClick,
                onShortcutRemove = onShortcutRemove,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Rejilla que reparte la altura disponible entre las filas (botones lo más grandes posible)
 * y, si no caben con [MinTileHeight], hace scroll en vez de encoger más.
 */
@Composable
private fun ShortcutGrid(
    items: List<ShortcutItem>,
    columns: Int,
    onShortcutClick: (Shortcut) -> Unit,
    onShortcutRemove: (Shortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LauncherTheme.layout.spacing
    val rows = items.chunked(columns)

    // BoxWithConstraints expone maxWidth/maxHeight del espacio disponible (en dp).
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (rows.isEmpty()) return@BoxWithConstraints
        val fittedHeight: Dp = (maxHeight - spacing * (rows.size - 1)) / rows.size
        val tileHeight = max(MinTileHeight, fittedHeight)

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tileHeight),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    rowItems.forEach { item ->
                        ShortcutTile(
                            item = item,
                            onClick = { onShortcutClick(item.shortcut) },
                            onRemove = { onShortcutRemove(item.shortcut) },
                            // weight(1f): cada celda ocupa la misma fracción del ancho de la fila
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                    // Rellena la última fila incompleta para que las celdas mantengan su ancho
                    repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ShortcutTile(
    item: ShortcutItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = LauncherTheme.layout
    val highlighted = item.shortcut.target == ShortcutTarget.AppDrawer
    var menuOpen by remember { mutableStateOf(false) }

    Box(modifier) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (item.available) 1f else 0.45f),
            shape = RoundedCornerShape(layout.cornerRadius),
            color = if (highlighted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
            contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = onClick,
                        // El botón "Apps" no se puede quitar: sin pulsación larga.
                        onLongClick = if (highlighted) null else ({ menuOpen = true }),
                    )
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                TileIcon(item, layout.iconSize)
                if (layout.showLabels) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = item.shortcut.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_from_home)) },
                onClick = {
                    menuOpen = false
                    onRemove()
                },
            )
        }
    }
}

@Composable
private fun TileIcon(item: ShortcutItem, size: Dp) {
    // Prioridad: icono del tema > icono de la app > vector propio ("Apps") > inicial
    val icon = LauncherTheme.icons[item.shortcut.themeIconKey] ?: item.icon
    when {
        icon != null -> Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(size))
        item.shortcut.target == ShortcutTarget.AppDrawer ->
            Icon(LauncherIcons.Apps, contentDescription = null, modifier = Modifier.size(size))
        else -> Box(
            modifier = Modifier
                .size(size)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.shortcut.label.take(1),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Preview(widthDp = 1024, heightDp = 600)
@Composable
private fun HomeScreenPreview() {
    val items = listOf(
        Shortcut.app("x", "Maps"),
        Shortcut.app("y", "Waze"),
        Shortcut.app("z", "Spotify"),
        Shortcut.app("w", "YouTube Music"),
        Shortcut.Settings,
        Shortcut.Drawer,
    ).map { ShortcutItem(it, icon = null, available = it.label != "Waze") }
    CarLauncherTheme {
        HomeScreen(state = HomeUiState(items = items), onShortcutClick = {}, onShortcutRemove = {})
    }
}
