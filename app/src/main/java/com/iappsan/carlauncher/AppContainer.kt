package com.iappsan.carlauncher

import android.app.Application
import android.content.Context
import com.iappsan.carlauncher.data.AppRepository
import com.iappsan.carlauncher.data.ShortcutRepository

/** Inyección de dependencias manual: un único lugar donde se crean los singletons de la app. */
class AppContainer(context: Context) {
    val appRepository = AppRepository(context)
    val shortcutRepository = ShortcutRepository()
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
