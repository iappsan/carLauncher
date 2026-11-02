package com.iappsan.carlauncher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.iappsan.carlauncher.data.Shortcut
import com.iappsan.carlauncher.data.ShortcutTarget
import com.iappsan.carlauncher.ui.home.HomeScreen
import com.iappsan.carlauncher.ui.home.HomeViewModel
import com.iappsan.carlauncher.ui.theme.CarLauncherTheme

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as CarLauncherApp).container }

    // `by viewModels { ... }` = delegado que crea/recupera el ViewModel usando nuestra factory.
    private val homeViewModel: HomeViewModel by viewModels { HomeViewModel.factory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Un launcher nunca se cierra con "atrás": consumimos el evento sin hacer nada.
        onBackPressedDispatcher.addCallback(this) { /* no-op */ }

        setContent {
            // collectAsState convierte el StateFlow en estado de Compose: al cambiar, se repinta.
            val state by homeViewModel.state.collectAsState()
            CarLauncherTheme {
                HomeScreen(state = state, onShortcutClick = ::onShortcutClick)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.refresh()
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
            ShortcutTarget.AppDrawer -> toast(getString(R.string.drawer_coming_soon))
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
