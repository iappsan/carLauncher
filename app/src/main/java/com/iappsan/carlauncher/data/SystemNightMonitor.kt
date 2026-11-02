package com.iappsan.carlauncher.data

import android.app.UiModeManager
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Modo noche **del sistema** (el `uiMode` de Android). Algunos estéreos lo activan al encender
 * las luces del carro (cable ILL); así el launcher puede seguir las luces sin APIs del fabricante.
 *
 * Se escucha por dos vías porque cada firmware lo hace a su manera:
 * - cambios de configuración (`onConfigurationChanged` de la Application);
 * - el ajuste `Settings.Secure.ui_night_mode` (lo que se vio en el log del estéreo Topway).
 */
class SystemNightMonitor(context: Context) {

    private val appContext = context.applicationContext
    private val uiModeManager = appContext.getSystemService(UiModeManager::class.java)

    private val _isNight = MutableStateFlow(read(appContext.resources.configuration))
    val isNight: StateFlow<Boolean> = _isNight.asStateFlow()

    init {
        appContext.registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                _isNight.value = read(newConfig)
            }

            @Deprecated("Deprecated in Java")
            override fun onLowMemory() = Unit
        })

        // `Settings.Secure.UI_NIGHT_MODE` está oculto en el SDK: se usa el nombre del ajuste tal cual.
        appContext.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("ui_night_mode"),
            false,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    _isNight.value = read(appContext.resources.configuration)
                }
            },
        )
    }

    private fun read(config: Configuration): Boolean = when (uiModeManager?.nightMode) {
        // Si el ajuste es explícito, manda; en "auto"/"personalizado", lo que diga la configuración actual.
        UiModeManager.MODE_NIGHT_YES -> true
        UiModeManager.MODE_NIGHT_NO -> false
        else -> config.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
}
