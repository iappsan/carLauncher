package com.iappsan.carlauncher.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.iappsan.carlauncher.data.Shortcut
import com.iappsan.carlauncher.data.ShortcutTarget
import com.iappsan.carlauncher.ui.theme.CarLauncherTheme

private val MinTileHeight = 80.dp
private val TileSpacing = 16.dp

@Composable
fun HomeScreen(
    state: HomeUiState,
    onShortcutClick: (Shortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Deja espacio para barras del sistema / recortes de pantalla
            .safeDrawingPadding()
            .padding(TileSpacing),
    ) {
        ShortcutGrid(
            items = state.items,
            columns = state.columns,
            onShortcutClick = onShortcutClick,
        )
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
) {
    if (items.isEmpty()) return
    val rows = items.chunked(columns)

    // BoxWithConstraints expone maxWidth/maxHeight del espacio disponible (en dp).
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fittedHeight: Dp = (maxHeight - TileSpacing * (rows.size - 1)) / rows.size
        val tileHeight = max(MinTileHeight, fittedHeight)

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TileSpacing),
        ) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tileHeight),
                    horizontalArrangement = Arrangement.spacedBy(TileSpacing),
                ) {
                    rowItems.forEach { item ->
                        ShortcutTile(
                            item = item,
                            onClick = { onShortcutClick(item.shortcut) },
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
    modifier: Modifier = Modifier,
) {
    val highlighted = item.shortcut.target == ShortcutTarget.AppDrawer
    Surface(
        onClick = onClick,
        modifier = modifier.alpha(if (item.available) 1f else 0.45f),
        shape = RoundedCornerShape(24.dp),
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TileIcon(item)
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

@Composable
private fun TileIcon(item: ShortcutItem) {
    val icon = item.icon
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(56.dp))
    } else {
        // Sin icono (ajustes, cajón o app no instalada): círculo con la inicial
        Box(
            modifier = Modifier
                .size(56.dp)
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
        Shortcut("maps", "Maps", ShortcutTarget.App("x")),
        Shortcut("waze", "Waze", ShortcutTarget.App("y")),
        Shortcut("spotify", "Spotify", ShortcutTarget.App("z")),
        Shortcut("ytmusic", "YouTube Music", ShortcutTarget.App("w")),
        Shortcut("settings", "Ajustes", ShortcutTarget.SystemSettings),
        Shortcut("apps", "Apps", ShortcutTarget.AppDrawer),
    ).map { ShortcutItem(it, icon = null, available = it.id != "waze") }
    CarLauncherTheme {
        HomeScreen(state = HomeUiState(items = items), onShortcutClick = {})
    }
}
