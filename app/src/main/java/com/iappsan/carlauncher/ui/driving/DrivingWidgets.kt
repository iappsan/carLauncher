package com.iappsan.carlauncher.ui.driving

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iappsan.carlauncher.R
import com.iappsan.carlauncher.data.SpeedUnit
import com.iappsan.carlauncher.data.bluetooth.BluetoothState
import com.iappsan.carlauncher.data.navigation.NavigationState
import com.iappsan.carlauncher.ui.icons.LauncherIcons
import com.iappsan.carlauncher.ui.theme.CarLauncherTheme
import com.iappsan.carlauncher.ui.theme.LauncherTheme
import kotlin.math.roundToInt

/** Velocidad: número + unidad. "—" sin señal GPS. */
@Composable
fun SpeedReadout(speedKmh: Float?, unit: SpeedUnit, large: Boolean, modifier: Modifier = Modifier) {
    val value = speedKmh?.let { (it * unit.fromKmh).roundToInt().toString() } ?: "—"
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = if (large) MaterialTheme.typography.displayLarge else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = unit.label,
            style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = if (large) 10.dp else 4.dp),
        )
    }
}

/** Indicador de Bluetooth para la cabecera: teléfono conectado y batería. */
@Composable
fun BluetoothIndicator(state: BluetoothState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (!state.supported) return // equipo sin Bluetooth accesible: no ocupar sitio
    val label = when {
        !state.permissionGranted -> stringResource(R.string.bt_no_permission)
        !state.enabled -> stringResource(R.string.bt_off)
        state.deviceName == null -> stringResource(R.string.bt_no_phone)
        state.batteryPercent != null -> "${state.deviceName} · ${state.batteryPercent} %"
        else -> state.deviceName
    }
    val connected = state.deviceName != null
    Surface(
        modifier = modifier
            .heightIn(min = 52.dp)
            .widthIn(max = 260.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = if (connected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (connected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(LauncherIcons.Bluetooth, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Tarjeta de la próxima maniobra (sustituye al widget de música mientras hay ruta).
 * Tocarla abre la app que está guiando.
 */
@Composable
fun NavigationCard(
    navigation: NavigationState,
    speedKmh: Float?,
    speedUnit: SpeedUnit,
    showSpeed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(LauncherTheme.layout.cornerRadius)
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                navigation.appLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = navigation.maneuverIcon
                if (icon != null) {
                    Image(icon, contentDescription = null, modifier = Modifier.size(96.dp))
                } else {
                    Icon(
                        LauncherIcons.Navigation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(96.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        navigation.title.ifEmpty { stringResource(R.string.nav_in_progress) },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (navigation.text.isNotEmpty()) {
                        Text(
                            navigation.text,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    navigation.subText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (showSpeed) SpeedReadout(speedKmh, speedUnit, large = true)
            }
        }
    }
}

@Preview(widthDp = 380, heightDp = 540)
@Composable
private fun NavigationCardPreview() {
    CarLauncherTheme {
        NavigationCard(
            navigation = NavigationState("pkg", "Maps", "300 m", "Gira a la derecha hacia Av. Reforma", "Llegada 15:42 · 12 min", null),
            speedKmh = 48f,
            speedUnit = SpeedUnit.Kmh,
            showSpeed = true,
            onClick = {},
        )
    }
}
