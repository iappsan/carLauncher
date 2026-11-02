package com.iappsan.carlauncher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iappsan.carlauncher.R
import com.iappsan.carlauncher.data.DayNightMode
import com.iappsan.carlauncher.data.HomeLayout
import com.iappsan.carlauncher.data.Shortcut
import com.iappsan.carlauncher.data.ShortcutList
import com.iappsan.carlauncher.data.SpeedUnit
import com.iappsan.carlauncher.data.bluetooth.BluetoothState
import com.iappsan.carlauncher.theme.ThemeEntry
import com.iappsan.carlauncher.theme.ThemeSpec
import com.iappsan.carlauncher.ui.driving.PermissionsState
import com.iappsan.carlauncher.ui.theme.CarLauncherTheme

interface SettingsActions {
    fun onClose()

    // Pantalla
    fun onShowClockChange(show: Boolean)
    fun onHomeLayoutChange(layout: HomeLayout)
    fun onColumnsChange(columns: Int)
    fun onShowSpeedChange(show: Boolean)
    fun onSpeedUnitChange(unit: SpeedUnit)

    // Día y noche
    fun onDayNightModeChange(mode: DayNightMode)
    fun onDayStartChange(hour: Int)
    fun onNightStartChange(hour: Int)

    // Accesos
    fun onMoveShortcut(list: ShortcutList, id: String, delta: Int)
    fun onRemoveShortcut(list: ShortcutList, id: String)
    fun onResetShortcuts(list: ShortcutList)

    // Conexiones
    fun onRequestPermissions()
    fun onRequestNotificationAccess()
    fun onResumeMusicChange(resume: Boolean)
    fun onBluetoothAppChange(packageName: String)

    // Tema
    fun onSelectTheme(id: String)
    fun onReloadThemes()
}

private enum class Section(val titleRes: Int) {
    Display(R.string.settings_section_display),
    DayNight(R.string.settings_section_day_night),
    Shortcuts(R.string.settings_section_shortcuts),
    Connections(R.string.settings_section_connections),
    Theme(R.string.settings_theme),
}

/** Ajustes del launcher: menú de secciones a la izquierda, contenido a la derecha. */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    themeState: ThemeSettingsState,
    permissions: PermissionsState,
    bluetooth: BluetoothState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    // rememberSaveable: como remember, pero sobrevive a la recreación de la Activity.
    var section by rememberSaveable { mutableStateOf(Section.Display) }

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

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.width(200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Section.entries.forEach { item ->
                        SectionItem(stringResource(item.titleRes), selected = item == section) { section = item }
                    }
                }
                Box(Modifier.weight(1f)) {
                    when (section) {
                        Section.Display -> DisplaySettings(state.settings, actions)
                        Section.DayNight -> DayNightSettings(
                            settings = state.settings,
                            sunTimes = state.sunTimes,
                            isNight = themeState.isNight,
                            themeHasDayVariant = themeState.hasDayVariant,
                            actions = actions,
                        )
                        Section.Shortcuts -> ShortcutSettings(state.homeShortcuts, state.navShortcuts, actions)
                        Section.Connections -> ConnectionSettings(
                            settings = state.settings,
                            permissions = permissions,
                            bluetooth = bluetooth,
                            bluetoothApps = state.bluetoothApps,
                            actions = actions,
                        )
                        Section.Theme -> ThemeSettings(themeState, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionItem(title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), contentAlignment = Alignment.CenterStart) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(widthDp = 1024, heightDp = 600)
@Composable
private fun SettingsScreenPreview() {
    CarLauncherTheme {
        SettingsScreen(
            state = SettingsUiState(homeShortcuts = listOf(Shortcut.app("com.google.android.apps.maps", "Maps"), Shortcut.Drawer)),
            themeState = ThemeSettingsState(
                entries = listOf(ThemeEntry("builtin:default", ThemeSpec(name = "Default", author = "CarLauncher"))),
                selectedId = "builtin:default",
            ),
            permissions = PermissionsState(),
            bluetooth = BluetoothState(supported = true),
            actions = object : SettingsActions {
                override fun onClose() {}
                override fun onShowClockChange(show: Boolean) {}
                override fun onHomeLayoutChange(layout: HomeLayout) {}
                override fun onColumnsChange(columns: Int) {}
                override fun onShowSpeedChange(show: Boolean) {}
                override fun onSpeedUnitChange(unit: SpeedUnit) {}
                override fun onDayNightModeChange(mode: DayNightMode) {}
                override fun onDayStartChange(hour: Int) {}
                override fun onNightStartChange(hour: Int) {}
                override fun onMoveShortcut(list: ShortcutList, id: String, delta: Int) {}
                override fun onRemoveShortcut(list: ShortcutList, id: String) {}
                override fun onResetShortcuts(list: ShortcutList) {}
                override fun onRequestPermissions() {}
                override fun onRequestNotificationAccess() {}
                override fun onResumeMusicChange(resume: Boolean) {}
                override fun onBluetoothAppChange(packageName: String) {}
                override fun onSelectTheme(id: String) {}
                override fun onReloadThemes() {}
            },
        )
    }
}
