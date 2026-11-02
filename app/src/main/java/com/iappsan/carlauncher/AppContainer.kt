package com.iappsan.carlauncher

import android.app.Application
import android.content.Context
import com.iappsan.carlauncher.data.AppRepository
import com.iappsan.carlauncher.data.DayNightRepository
import com.iappsan.carlauncher.data.LauncherSettingsRepository
import com.iappsan.carlauncher.data.ShortcutRepository
import com.iappsan.carlauncher.data.bluetooth.BluetoothRepository
import com.iappsan.carlauncher.data.location.LocationRepository
import com.iappsan.carlauncher.data.media.MediaRepository
import com.iappsan.carlauncher.data.navigation.NavigationRepository
import com.iappsan.carlauncher.theme.ThemeRepository

/** Inyección de dependencias manual: un único lugar donde se crean los singletons de la app. */
class AppContainer(context: Context) {
    val appRepository = AppRepository(context)
    val shortcutRepository = ShortcutRepository(context)
    val mediaRepository = MediaRepository(context)
    val launcherSettingsRepository = LauncherSettingsRepository(context)
    val locationRepository = LocationRepository(context)
    val dayNightRepository = DayNightRepository(launcherSettingsRepository, locationRepository)
    val themeRepository = ThemeRepository(context, dayNightRepository.isNight)
    val navigationRepository = NavigationRepository(context)
    val bluetoothRepository = BluetoothRepository(context, launcherSettingsRepository)
}

class CarLauncherApp : Application() {
    // `lateinit` = se asigna después de construir el objeto (en onCreate), no en el constructor.
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
