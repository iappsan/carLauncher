package com.iappsan.carlauncher.ui.drawer

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iappsan.carlauncher.R
import com.iappsan.carlauncher.data.AppEntry
import com.iappsan.carlauncher.data.IconLoader
import com.iappsan.carlauncher.ui.theme.CarLauncherTheme

private val IconSize = 56.dp

/** Callbacks del cajón agrupados para no pasar media docena de lambdas sueltas. */
interface DrawerActions {
    fun onQueryChange(query: String)
    fun onLaunch(app: AppEntry)
    fun onAddToHome(app: AppEntry)
    fun onAppInfo(app: AppEntry)
    fun onClose()
}

@Composable
fun DrawerScreen(
    state: DrawerUiState,
    query: String,
    iconLoader: IconLoader,
    actions: DrawerActions,
    modifier: Modifier = Modifier,
) {
    // Surface (y no solo .background) para que el texto herede onBackground como color de contenido.
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            SearchBar(
                query = query,
                onQueryChange = actions::onQueryChange,
                // "Buscar" en el teclado abre el primer resultado
                onSearch = { state.apps.firstOrNull()?.let(actions::onLaunch) },
                onClose = actions::onClose,
            )
            Spacer(Modifier.height(12.dp))

            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                state.apps.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(stringResource(R.string.drawer_empty), style = MaterialTheme.typography.titleLarge)
                }
                else -> LazyVerticalGrid(
                    // Adaptive: tantas columnas de ≥120dp como quepan (≈8 en 1024×600 a 160dpi)
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // key estable: Compose reutiliza las celdas al filtrar en vez de recrearlas
                    items(state.apps, key = { it.component.flattenToString() }) { app ->
                        AppCell(
                            app = app,
                            onHome = app.packageName in state.homePackages,
                            iconLoader = iconLoader,
                            actions = actions,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClose: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.drawer_search_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                onSearch()
            }),
        )
        Spacer(Modifier.size(12.dp))
        FilledTonalButton(onClick = onClose, modifier = Modifier.height(56.dp)) {
            Text(stringResource(R.string.drawer_close), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun AppCell(
    app: AppEntry,
    onHome: Boolean,
    iconLoader: IconLoader,
    actions: DrawerActions,
) {
    // `remember` conserva el valor entre recomposiciones de esta celda.
    var menuOpen by remember { mutableStateOf(false) }

    // produceState: arranca con el icono en caché (si lo hay) y lo carga en segundo plano si no.
    val icon by produceState(initialValue = iconLoader.cached(app.packageName), app.packageName) {
        if (value == null) value = iconLoader.load(app.packageName)
    }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = { actions.onLaunch(app) },
                    onLongClick = { menuOpen = true },
                )
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(icon, app.label)
            Spacer(Modifier.height(8.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(48.dp), // altura fija: filas alineadas aunque el nombre ocupe 1 o 2 líneas
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(if (onHome) R.string.already_on_home else R.string.add_to_home))
                },
                enabled = !onHome,
                onClick = {
                    menuOpen = false
                    actions.onAddToHome(app)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.app_info)) },
                onClick = {
                    menuOpen = false
                    actions.onAppInfo(app)
                },
            )
        }
    }
}

@Composable
private fun AppIcon(icon: ImageBitmap?, label: String) {
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(IconSize))
    } else {
        // Placeholder mientras carga
        Box(
            modifier = Modifier
                .size(IconSize)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.take(1),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Preview(widthDp = 1024, heightDp = 600)
@Composable
private fun DrawerScreenPreview() {
    val apps = listOf("Calculadora", "Cámara", "Chrome", "Contactos", "Maps", "Música", "Teléfono", "YouTube Music")
        .map { AppEntry("pkg.$it", it, ComponentName("pkg.$it", "Main")) }
    val noIcons = object : IconLoader {
        override fun cached(packageName: String): ImageBitmap? = null
        override suspend fun load(packageName: String): ImageBitmap? = null
    }
    val noActions = object : DrawerActions {
        override fun onQueryChange(query: String) {}
        override fun onLaunch(app: AppEntry) {}
        override fun onAddToHome(app: AppEntry) {}
        override fun onAppInfo(app: AppEntry) {}
        override fun onClose() {}
    }
    CarLauncherTheme {
        DrawerScreen(
            state = DrawerUiState(apps = apps, homePackages = setOf("pkg.Maps"), loading = false),
            query = "",
            iconLoader = noIcons,
            actions = noActions,
        )
    }
}
