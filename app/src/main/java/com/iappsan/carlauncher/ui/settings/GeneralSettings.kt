package com.iappsan.carlauncher.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.iappsan.carlauncher.R
import com.iappsan.carlauncher.data.DayNightMode
import com.iappsan.carlauncher.data.HomeLayout
import com.iappsan.carlauncher.data.LauncherSettings
import com.iappsan.carlauncher.data.SpeedUnit
import com.iappsan.carlauncher.data.location.SunTimes
import java.time.format.DateTimeFormatter

@Composable
internal fun DisplaySettings(settings: LauncherSettings, actions: SettingsActions) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SwitchCard(
            title = stringResource(R.string.settings_clock),
            description = stringResource(R.string.settings_clock_desc),
            checked = settings.showClock,
            onCheckedChange = actions::onShowClockChange,
        )
        SectionSpacer()
        SettingCard(
            title = stringResource(R.string.settings_home_layout),
            description = stringResource(R.string.settings_home_layout_desc),
        ) {
            ChoiceRow(
                options = listOf(
                    HomeLayout.Grid to stringResource(R.string.settings_home_layout_grid),
                    HomeLayout.Dock to stringResource(R.string.settings_home_layout_dock),
                ),
                selected = settings.homeLayout,
                onSelect = actions::onHomeLayoutChange,
            )
        }
        SectionSpacer()
        SettingCard(
            title = stringResource(R.string.settings_columns),
            description = stringResource(R.string.settings_columns_desc),
        ) {
            ChoiceRow(
                // 0 = las del tema
                options = listOf(0 to stringResource(R.string.settings_columns_theme)) + (2..5).map { it to "$it" },
                selected = settings.columns,
                onSelect = actions::onColumnsChange,
                // En el dock los accesos van en una sola fila: las columnas no aplican.
                enabled = settings.homeLayout == HomeLayout.Grid,
            )
        }
        SectionSpacer()
        SwitchCard(
            title = stringResource(R.string.settings_speed),
            description = stringResource(R.string.settings_speed_desc),
            checked = settings.showSpeed,
            onCheckedChange = actions::onShowSpeedChange,
        )
        SectionSpacer()
        SettingCard(title = stringResource(R.string.settings_speed_unit)) {
            ChoiceRow(
                options = SpeedUnit.entries.map { it to it.label },
                selected = settings.speedUnit,
                onSelect = actions::onSpeedUnitChange,
                enabled = settings.showSpeed,
            )
        }
    }
}

@Composable
internal fun DayNightSettings(
    settings: LauncherSettings,
    sunTimes: SunTimes?,
    systemNight: Boolean,
    isNight: Boolean,
    themeHasDayVariant: Boolean,
    actions: SettingsActions,
) {
    val mode = settings.dayNightMode
    // El horario se usa en modo "horario" y como respaldo de "por sol" mientras no hay ubicación.
    val scheduleInUse = mode == DayNightMode.Auto || (mode == DayNightMode.Sun && sunTimes == null)
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingCard(
            title = stringResource(R.string.settings_mode),
            description = stringResource(R.string.settings_day_night_desc),
        ) {
            ChoiceRow(
                options = listOf(
                    DayNightMode.Sun to stringResource(R.string.settings_mode_sun),
                    DayNightMode.Auto to stringResource(R.string.settings_mode_auto),
                    DayNightMode.CarLights to stringResource(R.string.settings_mode_lights),
                    DayNightMode.AlwaysNight to stringResource(R.string.settings_mode_night),
                    DayNightMode.AlwaysDay to stringResource(R.string.settings_mode_day),
                ),
                selected = mode,
                onSelect = actions::onDayNightModeChange,
            )
            Text(
                stringResource(if (isNight) R.string.settings_now_night else R.string.settings_now_day),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (mode == DayNightMode.Sun) {
                val sunText = when {
                    sunTimes == null -> stringResource(R.string.settings_sun_no_location)
                    sunTimes.polarDay -> stringResource(R.string.settings_sun_polar_day)
                    sunTimes.polarNight -> stringResource(R.string.settings_sun_polar_night)
                    else -> stringResource(
                        R.string.settings_sun_times,
                        sunTimes.sunrise!!.format(timeFormat),
                        sunTimes.sunset!!.format(timeFormat),
                    )
                }
                Text(sunText, style = MaterialTheme.typography.bodyLarge)
            }
            // Indicador en vivo para comprobar si el estéreo activa el modo noche con las luces.
            Text(
                stringResource(if (systemNight) R.string.settings_system_night_on else R.string.settings_system_night_off),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (mode == DayNightMode.CarLights) {
                Text(
                    stringResource(R.string.settings_lights_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!themeHasDayVariant) {
                Text(
                    stringResource(R.string.settings_no_day_variant),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SectionSpacer()
        SettingCard(title = stringResource(R.string.settings_schedule)) {
            HourStepper(stringResource(R.string.settings_day_start), settings.dayStartHour, scheduleInUse, actions::onDayStartChange)
            HourStepper(stringResource(R.string.settings_night_start), settings.nightStartHour, scheduleInUse, actions::onNightStartChange)
        }
    }
}
