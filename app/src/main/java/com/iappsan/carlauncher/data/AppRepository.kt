package com.iappsan.carlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Acceso a las apps instaladas: comprobar si existen, cargar sus iconos (con caché) y lanzarlas.
 *
 * Las funciones de iconos hacen I/O: llamarlas desde un hilo de fondo (Dispatchers.Default/IO).
 */
class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    /** Tamaño de los iconos en píxeles (64dp según la densidad de la pantalla). */
    private val iconSizePx = (64 * context.resources.displayMetrics.density).toInt()

    /** Caché limitada por memoria (KB) — 1/16 de la memoria de la app como máximo. */
    private val iconCache = object : LruCache<String, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 16).toInt()
    ) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4 / 1024
    }

    fun isInstalled(packageName: String): Boolean =
        pm.getLaunchIntentForPackage(packageName) != null

    fun loadIcon(packageName: String): ImageBitmap? {
        iconCache.get(packageName)?.let { return it }
        return try {
            pm.getApplicationIcon(packageName)
                .toBitmap(iconSizePx, iconSizePx)
                .asImageBitmap()
                .also { iconCache.put(packageName, it) }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    /** Intent para abrir la app, o `null` si no está instalada. */
    fun launchIntent(packageName: String): Intent? =
        pm.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
}
