package com.iappsan.carlauncher.data.navigation

import android.app.Notification
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Guía activa de Maps/Waze, sacada de su notificación. */
data class NavigationState(
    val packageName: String,
    val appLabel: String,
    /** Normalmente la distancia o la maniobra ("300 m", "Gira a la derecha"). */
    val title: String,
    /** Normalmente la calle o el detalle de la maniobra. */
    val text: String,
    /** Normalmente la hora de llegada / tiempo restante. */
    val subText: String,
    /** Flecha de la maniobra (el "large icon" de la notificación). */
    val maneuverIcon: ImageBitmap?,
)

/**
 * Detecta si Maps/Waze está guiando y qué maniobra viene, a partir de sus notificaciones.
 *
 * Solo se procesan notificaciones de apps de navegación conocidas; el resto se ignora sin leerse.
 * Es "best effort": depende de cómo cada app escriba su notificación y puede cambiar con
 * sus actualizaciones.
 */
class NavigationRepository(private val context: Context) {

    private val _state = MutableStateFlow<NavigationState?>(null)

    /** `null` = no hay ninguna ruta activa. */
    val state: StateFlow<NavigationState?> = _state.asStateFlow()

    // Notificaciones de navegación activas por clave (puede haber más de una app a la vez).
    private val active = LinkedHashMap<String, NavigationState>()
    private val iconSizePx = (96 * context.resources.displayMetrics.density).toInt()
    private val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    // Todos los métodos se llaman desde el hilo principal (callbacks del NotificationListenerService).

    fun onPosted(sbn: StatusBarNotification) {
        if (isNavigation(sbn)) {
            active[sbn.key] = parse(sbn)
            publish()
        } else if (active.remove(sbn.key) != null) {
            publish() // la misma notificación dejó de ser de navegación (p. ej. ruta terminada)
        }
    }

    fun onRemoved(sbn: StatusBarNotification) {
        if (active.remove(sbn.key) != null) publish()
    }

    fun replaceAll(notifications: Array<out StatusBarNotification>) {
        active.clear()
        notifications.filter(::isNavigation).forEach { active[it.key] = parse(it) }
        publish()
    }

    fun clear() {
        active.clear()
        publish()
    }

    private fun publish() {
        _state.value = active.values.lastOrNull()
    }

    private fun isNavigation(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        // En builds de depuración se aceptan notificaciones de `adb shell cmd notification post`
        // para poder probar la tarjeta sin conducir (ver docs/DEVELOPMENT.md).
        if (isDebuggable && sbn.packageName == SHELL_PACKAGE && sbn.tag?.startsWith("nav") == true) return true
        if (sbn.packageName !in NAVIGATION_PACKAGES) return false
        // Maps/Waze marcan la guía con la categoría "navigation". Algunas versiones no lo hacen:
        // entonces se acepta una notificación persistente con icono grande (la flecha).
        return n.category == Notification.CATEGORY_NAVIGATION ||
            (sbn.isOngoing && n.getLargeIcon() != null)
    }

    private fun parse(sbn: StatusBarNotification): NavigationState {
        val n = sbn.notification
        val extras = n.extras
        fun text(key: String) = extras.getCharSequence(key)?.toString()?.trim().orEmpty()

        return NavigationState(
            packageName = sbn.packageName,
            appLabel = appLabel(sbn.packageName),
            title = text(Notification.EXTRA_TITLE),
            text = text(Notification.EXTRA_TEXT).ifEmpty { text(Notification.EXTRA_BIG_TEXT) },
            subText = text(Notification.EXTRA_SUB_TEXT),
            maneuverIcon = try {
                n.getLargeIcon()?.loadDrawable(context)?.toBitmap(iconSizePx, iconSizePx)?.asImageBitmap()
            } catch (_: RuntimeException) {
                null
            },
        )
    }

    private fun appLabel(packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }

    private companion object {
        const val SHELL_PACKAGE = "com.android.shell"
        val NAVIGATION_PACKAGES = setOf(
            "com.google.android.apps.maps",
            "com.waze",
            "com.google.android.apps.mapslite",
            "com.sygic.aura",
            "com.here.app.maps",
            "net.osmand",
            "net.osmand.plus",
        )
    }
}
