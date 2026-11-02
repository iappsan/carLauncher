package com.iappsan.carlauncher

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.core.net.toUri
import com.iappsan.carlauncher.data.AppEntry
import com.iappsan.carlauncher.data.DayNightMode
import com.iappsan.carlauncher.data.ShortcutList
import com.iappsan.carlauncher.data.SpeedUnit
import com.iappsan.carlauncher.data.Shortcut
import com.iappsan.carlauncher.data.ShortcutTarget
import com.iappsan.carlauncher.data.media.MediaListenerService
import com.iappsan.carlauncher.ui.drawer.DrawerActions
import com.iappsan.carlauncher.ui.drawer.DrawerScreen
import com.iappsan.carlauncher.ui.drawer.DrawerViewModel
import com.iappsan.carlauncher.ui.driving.BluetoothIndicator
import com.iappsan.carlauncher.ui.driving.DrivingViewModel
import com.iappsan.carlauncher.ui.driving.NavigationCard
import com.iappsan.carlauncher.ui.driving.SpeedReadout
import com.iappsan.carlauncher.ui.home.HomeScreen
import com.iappsan.carlauncher.ui.home.HomeViewModel
import com.iappsan.carlauncher.ui.media.MediaActions
import com.iappsan.carlauncher.ui.media.MediaBar
import com.iappsan.carlauncher.ui.media.MediaViewModel
import com.iappsan.carlauncher.ui.media.MediaWidget
import com.iappsan.carlauncher.ui.settings.SettingsActions
import com.iappsan.carlauncher.ui.settings.SettingsScreen
import com.iappsan.carlauncher.ui.settings.SettingsViewModel
import com.iappsan.carlauncher.ui.settings.ThemeSettingsState
import com.iappsan.carlauncher.ui.theme.CarLauncherTheme
import com.iappsan.carlauncher.ui.theme.ThemeBackground
import com.iappsan.carlauncher.ui.theme.ThemeViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as CarLauncherApp).container }

    // `by viewModels { ... }` = delegado que crea/recupera el ViewModel usando nuestra factory.
    private val homeViewModel: HomeViewModel by viewModels { HomeViewModel.factory(container) }
    private val drawerViewModel: DrawerViewModel by viewModels { DrawerViewModel.factory(container) }
    private val mediaViewModel: MediaViewModel by viewModels { MediaViewModel.factory(container) }
    private val themeViewModel: ThemeViewModel by viewModels { ThemeViewModel.factory(container) }
    private val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.factory(container) }
    private val drivingViewModel: DrivingViewModel by viewModels { DrivingViewModel.factory(container) }

    /**
     * Pide los permisos en tiempo de ejecución (ubicación y Bluetooth) y, al responder el usuario,
     * reactiva GPS/Bluetooth. `registerForActivityResult` debe crearse antes de onCreate (por eso es un campo).
     */
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        drivingViewModel.onVisible()
    }

    // Para distinguir "nunca preguntado" de "denegado para siempre" en esta sesión.
    private var permissionsAskedBefore = false

    /** Pantallas que se pintan encima de Home. */
    private enum class Overlay { None, Drawer, Settings }

    /** Navegación mínima sin librería: qué overlay hay abierto encima de Home. */
    private var overlay by mutableStateOf(Overlay.None)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Un launcher nunca se cierra con "atrás": consumimos el evento sin hacer nada.
        onBackPressedDispatcher.addCallback(this) { /* no-op */ }

        // La primera vez que arranca, pedir ubicación y Bluetooth (una sola vez; luego desde Ajustes).
        lifecycleScope.launch {
            if (!container.launcherSettingsRepository.markPermissionsAsked()) requestRuntimePermissions()
        }

        setContent {
            // collectAsState convierte el StateFlow en estado de Compose: al cambiar, se repinta.
            val homeState by homeViewModel.state.collectAsState()
            // Hasta que carga el tema (decenas de ms) no se pinta nada: se ve el fondo de la ventana
            // en vez de un frame con los colores equivocados. `?: return@setContent` = salir si es null.
            val theme = themeViewModel.theme.collectAsState().value ?: return@setContent

            // Iconos de la barra de estado oscuros sobre fondos claros (modo día) y viceversa.
            val lightBackground = theme.colorScheme.background.luminance() > 0.5f
            LaunchedEffect(lightBackground) {
                val style = if (lightBackground) {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            CarLauncherTheme(theme) {
                // Registrado después que el callback no-op, así que tiene prioridad mientras hay un overlay.
                BackHandler(enabled = overlay != Overlay.None) { closeOverlay() }
                Box {
                    ThemeBackground()
                    val navigation by drivingViewModel.navigation.collectAsState()
                    val speedKmh by drivingViewModel.speedKmh.collectAsState()
                    val bluetooth by drivingViewModel.bluetooth.collectAsState()
                    val permissions by drivingViewModel.permissions.collectAsState()
                    // Copia local: así Kotlin sabe que no cambia y permite usarla como no-null (smart cast).
                    val nav = navigation
                    HomeScreen(
                        state = homeState,
                        onShortcutClick = ::onShortcutClick,
                        onShortcutRemove = homeViewModel::removeShortcut,
                        navigating = nav != null,
                        mediaPanel = {
                            val mediaState by mediaViewModel.state.collectAsState()
                            MediaWidget(state = mediaState, actions = mediaActions)
                        },
                        navigationPanel = {
                            if (nav != null) {
                                NavigationCard(
                                    navigation = nav,
                                    speedKmh = speedKmh,
                                    speedUnit = homeState.speedUnit,
                                    showSpeed = homeState.showSpeed && permissions.location,
                                    onClick = { openApp(nav.packageName) },
                                )
                            }
                        },
                        mediaBar = {
                            val mediaState by mediaViewModel.state.collectAsState()
                            MediaBar(state = mediaState, actions = mediaActions)
                        },
                        headerExtras = {
                            // En navegación la velocidad va grande en la tarjeta
                            if (homeState.showSpeed && permissions.location && nav == null) {
                                SpeedReadout(speedKmh, homeState.speedUnit, large = false)
                                Spacer(Modifier.width(16.dp))
                            }
                            BluetoothIndicator(bluetooth, onClick = { onBluetoothClick(bluetooth.permissionGranted) })
                        },
                    )
                    OverlayVisibility(overlay == Overlay.Drawer) {
                        val drawerState by drawerViewModel.state.collectAsState()
                        DrawerScreen(
                            state = drawerState,
                            query = drawerViewModel.query,
                            iconLoader = container.appRepository,
                            actions = drawerActions,
                        )
                    }
                    OverlayVisibility(overlay == Overlay.Settings) {
                        val settingsState by settingsViewModel.state.collectAsState()
                        val permissionsState by drivingViewModel.permissions.collectAsState()
                        val bluetoothState by drivingViewModel.bluetooth.collectAsState()
                        val entries by themeViewModel.entries.collectAsState()
                        val selectedId by themeViewModel.selectedId.collectAsState()
                        SettingsScreen(
                            state = settingsState,
                            themeState = ThemeSettingsState(
                                entries = entries,
                                selectedId = selectedId,
                                warnings = theme.warnings,
                                themesPath = themeViewModel.userThemesPath,
                                isNight = theme.isNight,
                                hasDayVariant = theme.hasDayVariant,
                            ),
                            permissions = permissionsState,
                            bluetooth = bluetoothState,
                            actions = settingsActions,
                        )
                    }
                }
            }
        }
    }

    /** Animación común de entrada/salida de los overlays. */
    @Composable
    private fun OverlayVisibility(visible: Boolean, content: @Composable () -> Unit) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { it / 10 },
            exit = fadeOut() + slideOutVertically { it / 10 },
        ) { content() }
    }

    // Solo escuchamos las sesiones de música mientras el launcher está visible.
    // Y al volver a Home se recalcula si toca día o noche.
    override fun onStart() {
        super.onStart()
        mediaViewModel.onVisible()
        themeViewModel.onVisible()
        drivingViewModel.onVisible()
    }

    override fun onStop() {
        super.onStop()
        mediaViewModel.onHidden()
        drivingViewModel.onHidden()
    }

    private fun requestRuntimePermissions() {
        val wanted = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        val missing = wanted.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        when {
            missing.isEmpty() -> drivingViewModel.onVisible()
            // Si el usuario lo denegó "para siempre", el diálogo ya no aparece: abrir los ajustes de la app
            permissionsAskedBefore && missing.none { shouldShowRequestPermissionRationale(it) } -> openAppDetails()
            else -> permissionLauncher.launch(missing.toTypedArray())
        }
        permissionsAskedBefore = true
    }

    private fun openAppDetails() = startSafely(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        getString(R.string.app_name),
    )

    /** Indicador de Bluetooth: sin permiso lo pide; con permiso abre la app elegida o los ajustes de BT. */
    private fun onBluetoothClick(permissionGranted: Boolean) {
        if (!permissionGranted) return requestRuntimePermissions()
        lifecycleScope.launch {
            val app = container.launcherSettingsRepository.settings.first().bluetoothApp
            val intent = app.takeIf { it.isNotEmpty() }?.let { container.appRepository.launchIntent(it) }
                ?: Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startSafely(intent, "Bluetooth")
        }
    }

    private fun openApp(packageName: String) {
        container.appRepository.launchIntent(packageName)?.let { startSafely(it, packageName) }
    }

    /** Pulsar Home estando ya en el launcher (p. ej. con el cajón abierto) vuelve a la pantalla principal. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        closeOverlay()
    }

    private fun closeOverlay() {
        overlay = Overlay.None
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
            drawerViewModel.addTo(ShortcutList.Home, app)
            toast(getString(R.string.added_to_home, app.label))
        }

        override fun onAddToNavigation(app: AppEntry) {
            drawerViewModel.addTo(ShortcutList.Navigation, app)
            toast(getString(R.string.added_to_navigation, app.label))
        }

        override fun onAppInfo(app: AppEntry) = startSafely(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${app.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            app.label,
        )

        override fun onOpenSettings() {
            themeViewModel.refresh() // busca temas nuevos copiados desde la última vez
            overlay = Overlay.Settings
        }

        override fun onClose() = closeOverlay()
    }

    private val settingsActions = object : SettingsActions {
        override fun onClose() = closeOverlay()
        override fun onShowClockChange(show: Boolean) = settingsViewModel.setShowClock(show)
        override fun onColumnsChange(columns: Int) = settingsViewModel.setColumns(columns)
        override fun onShowSpeedChange(show: Boolean) = settingsViewModel.setShowSpeed(show)
        override fun onSpeedUnitChange(unit: SpeedUnit) = settingsViewModel.setSpeedUnit(unit)
        override fun onDayNightModeChange(mode: DayNightMode) = settingsViewModel.setDayNightMode(mode)
        override fun onDayStartChange(hour: Int) = settingsViewModel.setDayStartHour(hour)
        override fun onNightStartChange(hour: Int) = settingsViewModel.setNightStartHour(hour)
        override fun onMoveShortcut(list: ShortcutList, id: String, delta: Int) =
            settingsViewModel.moveShortcut(list, id, delta)
        override fun onRemoveShortcut(list: ShortcutList, id: String) = settingsViewModel.removeShortcut(list, id)
        override fun onResetShortcuts(list: ShortcutList) = settingsViewModel.resetShortcuts(list)
        override fun onRequestPermissions() = requestRuntimePermissions()
        override fun onRequestNotificationAccess() = requestNotificationAccess()
        override fun onResumeMusicChange(resume: Boolean) = settingsViewModel.setResumeMusicOnConnect(resume)
        override fun onBluetoothAppChange(packageName: String) = settingsViewModel.setBluetoothApp(packageName)
        override fun onSelectTheme(id: String) = themeViewModel.select(id)
        override fun onReloadThemes() = themeViewModel.refresh()
    }

    private val mediaActions = object : MediaActions {
        override fun onPlayPause() = mediaViewModel.playPause()
        override fun onNext() = mediaViewModel.next()
        override fun onPrevious() = mediaViewModel.previous()

        override fun onOpenPlayer(packageName: String) = openApp(packageName)

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
            ShortcutTarget.AppDrawer -> overlay = Overlay.Drawer
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
