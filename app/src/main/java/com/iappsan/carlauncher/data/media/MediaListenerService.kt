package com.iappsan.carlauncher.data.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.iappsan.carlauncher.CarLauncherApp

/**
 * Acceso a notificaciones del launcher. Sirve para dos cosas:
 * 1. Poder llamar a `MediaSessionManager.getActiveSessions()` (widget de música): Android solo lo
 *    permite a apps con un NotificationListenerService concedido.
 * 2. Leer la guía de Maps/Waze (tarjeta de maniobra). Solo se procesan las notificaciones de apps de
 *    navegación conocidas (ver NavigationRepository); el resto se ignora.
 *
 * (Conserva el nombre de la fase 3 para no perder el permiso ya concedido.)
 */
class MediaListenerService : NotificationListenerService() {

    private val navigation get() = (application as CarLauncherApp).container.navigationRepository

    override fun onListenerConnected() {
        val current = try {
            activeNotifications
        } catch (_: SecurityException) {
            null
        }
        navigation.replaceAll(current.orEmpty())
    }

    override fun onListenerDisconnected() = navigation.clear()

    override fun onNotificationPosted(sbn: StatusBarNotification) = navigation.onPosted(sbn)

    override fun onNotificationRemoved(sbn: StatusBarNotification) = navigation.onRemoved(sbn)
}
