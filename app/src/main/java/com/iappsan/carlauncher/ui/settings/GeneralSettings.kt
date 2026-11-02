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
import com.iappsan.carlauncher.data.LauncherSettings

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
            title = stringResource(R.string.settings_columns),
            description = stringResource(R.string.settings_columns_desc),
        ) {
            ChoiceRow(
                // 0 = las del tema
                options = listOf(0 to stringResource(R.string.settings_columns_theme)) + (2..5).map { it to "$it" },
                selected = settings.columns,
                onSelect = actions::onColumnsChange,
            )
        }
    }
}

@Composable
internal fun DayNightSettings(
    settings: LauncherSettings,
    isNight: Boolean,
    themeHasDayVariant: Boolean,
    actions: SettingsActions,
) {
    val auto = settings.dayNightMode == DayNightMode.Auto
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingCard(
            title = stringResource(R.string.settings_mode),
            description = stringResource(R.string.settings_day_night_desc),
        ) {
            ChoiceRow(
                options = listOf(
                    DayNightMode.Auto to stringResource(R.string.settings_mode_auto),
                    DayNightMode.AlwaysNight to stringResource(R.string.settings_mode_night),
                    DayNightMode.AlwaysDay to stringResource(R.string.settings_mode_day),
                ),
                selected = settings.dayNightMode,
                onSelect = actions::onDayNightModeChange,
            )
            Text(
                stringResource(if (isNight) R.string.settings_now_night else R.string.settings_now_day),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
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
            HourStepper(stringResource(R.string.settings_day_start), settings.dayStartHour, auto, actions::onDayStartChange)
            HourStepper(stringResource(R.string.settings_night_start), settings.nightStartHour, auto, actions::onNightStartChange)
        }
    }
}
