package com.iappsan.carlauncher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iappsan.carlauncher.R
import com.iappsan.carlauncher.data.AppEntry
import com.iappsan.carlauncher.data.LauncherSettings
import com.iappsan.carlauncher.data.bluetooth.BluetoothState
import com.iappsan.carlauncher.ui.driving.PermissionsState

/** Permisos y Bluetooth. */
@Composable
internal fun ConnectionSettings(
    settings: LauncherSettings,
    permissions: PermissionsState,
    bluetooth: BluetoothState,
    bluetoothApps: List<AppEntry>,
    actions: SettingsActions,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingCard(title = stringResource(R.string.settings_permissions)) {
            PermissionRow(
                title = stringResource(R.string.perm_location),
                description = stringResource(R.string.perm_location_desc),
                granted = permissions.location,
                onRequest = actions::onRequestPermissions,
            )
            if (bluetooth.supported) {
                PermissionRow(
                    title = stringResource(R.string.perm_bluetooth),
                    description = stringResource(R.string.perm_bluetooth_desc),
                    granted = permissions.bluetooth,
                    onRequest = actions::onRequestPermissions,
                )
            }
            PermissionRow(
                title = stringResource(R.string.perm_notifications),
                description = stringResource(R.string.perm_notifications_desc),
                granted = permissions.notifications,
                onRequest = actions::onRequestNotificationAccess,
            )
        }
        SectionSpacer()
        SettingCard(
            title = stringResource(R.string.settings_bluetooth),
            description = stringResource(
                when {
                    !bluetooth.supported -> R.string.bt_unsupported
                    bluetooth.deviceName != null -> R.string.bt_status_connected
                    else -> R.string.bt_status_none
                },
                bluetooth.deviceName.orEmpty(),
            ),
        ) {
            Text(
                stringResource(R.string.bt_vendor_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionSpacer()
        SwitchCard(
            title = stringResource(R.string.settings_resume_music),
            description = stringResource(R.string.settings_resume_music_desc),
            checked = settings.resumeMusicOnConnect,
            onCheckedChange = actions::onResumeMusicChange,
        )
        SectionSpacer()
        SettingCard(
            title = stringResource(R.string.settings_bt_app),
            description = stringResource(R.string.settings_bt_app_desc),
        ) {
            AppChoice(stringResource(R.string.settings_bt_app_system), selected = settings.bluetoothApp.isEmpty()) {
                actions.onBluetoothAppChange("")
            }
            bluetoothApps.forEach { app ->
                AppChoice(app.label, selected = settings.bluetoothApp == app.packageName, subtitle = app.packageName) {
                    actions.onBluetoothAppChange(app.packageName)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, description: String, granted: Boolean, onRequest: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) {
            Text(stringResource(R.string.perm_granted), color = MaterialTheme.colorScheme.primary)
        } else {
            FilledTonalButton(onClick = onRequest, modifier = Modifier.height(52.dp)) {
                Text(stringResource(R.string.perm_grant))
            }
        }
    }
}

@Composable
private fun AppChoice(label: String, selected: Boolean, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.size(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
