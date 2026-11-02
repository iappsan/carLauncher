package com.iappsan.carlauncher.data.media

import android.service.notification.NotificationListenerService

/**
 * Servicio vacío a propósito: no leemos notificaciones.
 *
 * Android solo deja llamar a `MediaSessionManager.getActiveSessions()` a apps que tengan
 * un NotificationListenerService con permiso concedido por el usuario. Este servicio
 * existe únicamente para poder pedir ese permiso y pasar su ComponentName a esa API.
 */
class MediaListenerService : NotificationListenerService()
