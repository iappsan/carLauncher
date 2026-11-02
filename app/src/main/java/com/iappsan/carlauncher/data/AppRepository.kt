package com.iappsan.carlauncher.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.text.Collator

/** Una app que aparece en el cajón (una actividad con categoría LAUNCHER). */
data class AppEntry(
    val packageName: String,
    val label: String,
    val component: ComponentName,
)

/** Carga de iconos desacoplada del repositorio, para poder pasarla a la UI (y a los previews). */
interface IconLoader {
    /** Icono si ya está en caché; no bloquea. */
    fun cached(packageName: String): ImageBitmap?

    /** Carga el icono en segundo plano (y lo cachea). */
    suspend fun load(packageName: String): ImageBitmap?
}

/**
 * Acceso a las apps instaladas: lista para el cajón, iconos con caché y lanzamiento.
 *
 * Usa [LauncherApps], la API pensada para launchers: devuelve solo actividades lanzables
 * y avisa cuando se instala, actualiza o desinstala una app.
 */
class AppRepository(private val context: Context) : IconLoader {

    private val pm: PackageManager = context.packageManager
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val user: UserHandle = Process.myUserHandle()

    /** Tamaño de los iconos en píxeles (64dp según la densidad de la pantalla). */
    private val iconSizePx = (64 * context.resources.displayMetrics.density).toInt()

    /** Caché limitada por memoria (KB) — 1/16 de la memoria de la app como máximo. */
    private val iconCache = object : LruCache<String, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 16).toInt()
    ) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4 / 1024
    }

    // @Volatile: se escribe desde el hilo principal (callback) y se lee desde hilos de fondo.
    @Volatile
    private var cachedApps: List<AppEntry>? = null

    private val _changes = MutableStateFlow(0)

    /** Contador que sube cada vez que cambian las apps instaladas; la UI lo observa para recargar. */
    val changes: StateFlow<Int> = _changes.asStateFlow()

    init {
        // `object : Clase() { ... }` = clase anónima, como `new Clase() { ... }` en Java.
        launcherApps.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) = invalidate(packageName)
            override fun onPackageAdded(packageName: String, user: UserHandle) = invalidate(packageName)
            override fun onPackageChanged(packageName: String, user: UserHandle) = invalidate(packageName)
            override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) =
                invalidate(*packageNames)
            override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) =
                invalidate(*packageNames)
        })
    }

    // `vararg` ≈ `String...` de Java; `*array` expande un array como varargs.
    private fun invalidate(vararg packageNames: String) {
        packageNames.forEach { iconCache.remove(it) }
        cachedApps = null
        _changes.update { it + 1 }
    }

    /** Apps lanzables ordenadas alfabéticamente (sin este launcher). Llamar fuera del hilo principal. */
    fun installedApps(): List<AppEntry> = cachedApps ?: loadApps().also { cachedApps = it }

    private fun loadApps(): List<AppEntry> {
        val collator = Collator.getInstance() // orden según el idioma: "Ñ" tras "N", ignora acentos
        return launcherApps.getActivityList(null, user)
            .filter { it.applicationInfo.packageName != context.packageName }
            .map { AppEntry(it.applicationInfo.packageName, it.label.toString(), it.componentName) }
            .sortedWith(compareBy(collator) { it.label })
    }

    override fun cached(packageName: String): ImageBitmap? = iconCache.get(packageName)

    override suspend fun load(packageName: String): ImageBitmap? =
        withContext(Dispatchers.IO) { loadIcon(packageName) }

    /** Versión bloqueante de [load]; para código que ya corre en segundo plano. */
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

    /** Icono de la app de ajustes del sistema (el package varía según el fabricante). */
    fun settingsIcon(): ImageBitmap? {
        val packageName = pm.resolveActivity(Intent(Settings.ACTION_SETTINGS), 0)
            ?.activityInfo?.packageName ?: return null
        return loadIcon(packageName)
    }

    /** Intent para abrir la app, o `null` si no está instalada. */
    fun launchIntent(packageName: String): Intent? =
        pm.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }

    /** Abre una actividad del cajón. Devuelve `false` si no se pudo. */
    fun launch(component: ComponentName): Boolean = try {
        launcherApps.startMainActivity(component, user, null, null)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
