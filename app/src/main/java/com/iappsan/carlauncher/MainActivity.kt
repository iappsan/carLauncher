package com.iappsan.carlauncher

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.iappsan.carlauncher.data.AppEntry
import com.iappsan.carlauncher.data.Shortcut
import com.iappsan.carlauncher.data.ShortcutTarget
import com.iappsan.carlauncher.data.media.MediaListenerService
import com.iappsan.carlauncher.ui.drawer.DrawerActions
import com.iappsan.carlauncher.ui.drawer.DrawerScreen
import com.iappsan.carlauncher.ui.drawer.DrawerViewModel
import com.iappsan.carlauncher.ui.home.HomeScreen
import com.iappsan.carlauncher.ui.home.HomeViewModel
import com.iappsan.carlauncher.ui.media.MediaActions
import com.iappsan.carlauncher.ui.media.MediaViewModel
import com.iappsan.carlauncher.ui.media.MediaWidget
import com.iappsan.carlauncher.ui.theme.CarLauncherTheme

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as CarLauncherApp).container }

    // `by viewModels { ... }` = delegado que crea/recupera el ViewModel usando nuestra factory.
    private val homeViewModel: HomeViewModel by viewModels { HomeViewModel.factory(container) }
    private val drawerViewModel: DrawerViewModel by viewModels { DrawerViewModel.factory(container) }
    private val mediaViewModel: MediaViewModel by viewModels { MediaViewModel.factory(container) }

    /** Navegación mínima sin librería: el cajón se pinta encima de Home cuando es `true`. */
    private var showDrawer by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Un launcher nunca se cierra con "atrás": consumimos el evento sin hacer nada.
        onBackPressedDispatcher.addCallback(this) { /* no-op */ }

        setContent {
            // collectAsState convierte el StateFlow en estado de Compose: al cambiar, se repinta.
            val homeState by homeViewModel.state.collectAsState()
            CarLauncherTheme {
                Box {
                    HomeScreen(
                        state = homeState,
                        onShortcutClick = ::onShortcutClick,
                        onShortcutRemove = homeViewModel::removeShortcut,
                        mediaPanel = {
                            val mediaState by mediaViewModel.state.collectAsState()
                            MediaWidget(state = mediaState, actions = mediaActions)
                        },
                    )
                    AnimatedVisibility(
                        visible = showDrawer,
                        enter = fadeIn() + slideInVertically { it / 10 },
                        exit = fadeOut() + slideOutVertically { it / 10 },
                    ) {
                        // Registrado después que el callback no-op, así que tiene prioridad mientras el cajón está abierto.
                        BackHandler { closeDrawer() }
                        val drawerState by drawerViewModel.state.collectAsState()
                        DrawerScreen(
                            state = drawerState,
                            query = drawerViewModel.query,
                            iconLoader = container.appRepository,
                            actions = drawerActions,
                        )
                    }
                }
            }
        }
    }

    // Solo escuchamos las sesiones de música mientras el launcher está visible.
    override fun onStart() {
        super.onStart()
        mediaViewModel.onVisible()
    }

    override fun onStop() {
        super.onStop()
        mediaViewModel.onHidden()
    }

    /** Pulsar Home estando ya en el launcher (p. ej. con el cajón abierto) vuelve a la pantalla principal. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        closeDrawer()
    }

    private fun closeDrawer() {
        showDrawer = false
        drawerViewModel.onQueryChange("")
    }

    private val drawerActions = object : DrawerActions {
        override fun onQueryChange(query: String) = drawerViewModel.onQueryChange(query)

        override fun onLaunch(app: AppEntry) {
            if (!container.appRepository.launch(app.component)) {
                toast(getString(R.string.cannot_open, app.label))
            }
        }

        override fun onAddToHome(app: AppEntry) {
            drawerViewModel.addToHome(app)
            toast(getString(R.string.added_to_home, app.label))
        }

        override fun onAppInfo(app: AppEntry) = startSafely(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${app.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            app.label,
        )

        override fun onClose() = closeDrawer()
    }

    private val mediaActions = object : MediaActions {
        override fun onPlayPause() = mediaViewModel.playPause()
        override fun onNext() = mediaViewModel.next()
        override fun onPrevious() = mediaViewModel.previous()

        override fun onOpenPlayer(packageName: String) {
            container.appRepository.launchIntent(packageName)?.let { startSafely(it, packageName) }
        }

        override fun onRequestPermission() = requestNotificationAccess()
    }

    /**
     * Abre la pantalla del sistema para conceder acceso a notificaciones. Al volver, onStart
     * vuelve a comprobar el permiso. Algunos head units no traen esta pantalla: ver docs/DEVELOPMENT.md.
     */
    private fun requestNotificationAccess() {
        val component = ComponentName(this, MediaListenerService::class.java)
        val candidates = buildList {
            // Android 11+: directamente a la página de este servicio
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                        .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
                )
            }
            add(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        for (intent in candidates) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // probar la siguiente
            }
        }
        toast(getString(R.string.media_permission_unavailable))
    }

    private fun onShortcutClick(shortcut: Shortcut) {
        when (val target = shortcut.target) {
            is ShortcutTarget.App -> {
                val intent = container.appRepository.launchIntent(target.packageName)
                if (intent == null) {
                    toast(getString(R.string.app_not_installed, shortcut.label))
                } else {
                    startSafely(intent, shortcut.label)
                }
            }
            ShortcutTarget.SystemSettings -> startSafely(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                shortcut.label,
            )
            ShortcutTarget.AppDrawer -> showDrawer = true
        }
    }

    private fun startSafely(intent: Intent, label: String) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            toast(getString(R.string.cannot_open, label))
        } catch (_: SecurityException) {
            toast(getString(R.string.cannot_open, label))
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
