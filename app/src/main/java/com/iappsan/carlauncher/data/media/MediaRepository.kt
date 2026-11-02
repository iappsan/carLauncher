package com.iappsan.carlauncher.data.media

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.scale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lo que está sonando ahora en alguna app de música. */
data class NowPlaying(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val artist: String,
    val art: ImageBitmap?,
    val isPlaying: Boolean,
)

data class MediaState(
    /** Acceso a notificaciones concedido (necesario para ver las sesiones de otras apps). */
    val hasPermission: Boolean = false,
    /** `null` = no hay ninguna sesión de música con contenido. */
    val nowPlaying: NowPlaying? = null,
)

/**
 * Observa las sesiones multimedia activas (Spotify, YouTube Music, radio, Bluetooth…)
 * y expone la "principal" como [state]. También envía los controles de reproducción.
 *
 * Todo ocurre en el hilo principal: los callbacks del sistema llegan por [mainHandler].
 * Solo escucha entre [start] y [stop] (mientras Home está visible) para no gastar recursos.
 */
class MediaRepository(private val context: Context) {

    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Las carátulas pueden venir a 1000px o más; las reducimos para ahorrar memoria. */
    private val artMaxPx = (256 * context.resources.displayMetrics.density).toInt()

    private val _state = MutableStateFlow(MediaState())
    val state: StateFlow<MediaState> = _state.asStateFlow()

    private var listening = false
    private var controllers: List<MediaController> = emptyList()
    private var current: MediaController? = null

    // Caché de la última carátula: cada lectura de metadata por IPC trae un Bitmap nuevo,
    // así que comparamos por una clave de contenido en vez de por referencia.
    private var lastArtKey: String? = null
    private var lastArt: ImageBitmap? = null
    private val labelCache = HashMap<String, String>()

    // Lambda como implementación de una interfaz Java de un solo método (SAM conversion).
    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        setControllers(list.orEmpty())
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() = refreshSessions()
    }

    fun hasPermission(): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    /** Empieza a escuchar. Seguro de llamar varias veces (p. ej. en cada onStart). */
    fun start() {
        if (!hasPermission()) {
            stop()
            _state.value = MediaState(hasPermission = false)
            return
        }
        if (!listening) {
            try {
                sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, mainHandler)
                listening = true
            } catch (_: SecurityException) {
                _state.value = MediaState(hasPermission = false)
                return
            }
        }
        refreshSessions()
    }

    /** Deja de escuchar. Conserva el último estado para que no parpadee al volver. */
    fun stop() {
        if (listening) {
            sessionManager.removeOnActiveSessionsChangedListener(sessionsListener)
            listening = false
        }
        controllers.forEach { it.unregisterCallback(controllerCallback) }
        controllers = emptyList()
        current = null
    }

    fun playPause() {
        val controller = current ?: return
        if (controller.playbackState.isPlaying) controller.transportControls.pause()
        else controller.transportControls.play()
    }

    fun next() {
        current?.transportControls?.skipToNext()
    }

    fun previous() {
        current?.transportControls?.skipToPrevious()
    }

    private fun refreshSessions() {
        if (!listening) return
        val list = try {
            sessionManager.getActiveSessions(listenerComponent)
        } catch (_: SecurityException) {
            emptyList()
        }
        setControllers(list)
    }

    private fun setControllers(list: List<MediaController>) {
        controllers.forEach { it.unregisterCallback(controllerCallback) }
        controllers = list
        list.forEach { it.registerCallback(controllerCallback, mainHandler) }
        publish()
    }

    /** Elige la sesión principal (la que suena; si ninguna, la primera con contenido) y emite. */
    private fun publish() {
        val controller = controllers.firstOrNull { it.playbackState.isPlaying }
            ?: controllers.firstOrNull { it.metadata != null }
        current = controller
        _state.value = MediaState(hasPermission = true, nowPlaying = controller?.toNowPlaying())
    }

    // Función de extensión: se llama como si fuera un método de MediaController.
    private fun MediaController.toNowPlaying(): NowPlaying {
        val md = metadata
        val title = md.firstText(MediaMetadata.METADATA_KEY_TITLE, MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val artist = md.firstText(
            MediaMetadata.METADATA_KEY_ARTIST,
            MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
            MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
        )
        return NowPlaying(
            packageName = packageName,
            appLabel = appLabel(packageName),
            title = title,
            artist = artist,
            art = artFor(md, "$packageName|$title|$artist"),
            isPlaying = playbackState.isPlaying,
        )
    }

    private fun artFor(md: MediaMetadata?, key: String): ImageBitmap? {
        val source: Bitmap = md?.let {
            it.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: it.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: it.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        } ?: return null

        val fullKey = "$key|${source.width}x${source.height}"
        if (fullKey == lastArtKey) return lastArt

        val image = try {
            val longest = maxOf(source.width, source.height)
            val bitmap = if (longest > artMaxPx) {
                val factor = artMaxPx.toFloat() / longest
                source.scale((source.width * factor).toInt(), (source.height * factor).toInt())
            } else {
                source
            }
            bitmap.asImageBitmap()
        } catch (_: RuntimeException) {
            null // bitmap en un formato que no se puede escalar: mejor sin carátula que un cierre
        }
        lastArtKey = fullKey
        lastArt = image
        return image
    }

    private fun appLabel(packageName: String): String = labelCache.getOrPut(packageName) {
        try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private companion object {
        val PlaybackState?.isPlaying: Boolean
            get() = this?.state == PlaybackState.STATE_PLAYING || this?.state == PlaybackState.STATE_BUFFERING

        /** Primer texto no vacío entre varias claves de metadata. */
        fun MediaMetadata?.firstText(vararg keys: String): String =
            keys.firstNotNullOfOrNull { key -> this?.getString(key)?.takeIf { it.isNotBlank() } }.orEmpty()
    }
}
