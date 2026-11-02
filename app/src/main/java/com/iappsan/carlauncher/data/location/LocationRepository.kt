package com.iappsan.carlauncher.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.iappsan.carlauncher.data.settingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * GPS para el velocímetro y para saber dónde amanece. Sin Google Play Services: usa
 * LocationManager directamente (los head units genéricos a menudo no traen Play Services).
 *
 * Solo escucha entre [start] y [stop] (Home visible), una actualización por segundo.
 */
class LocationRepository(private val context: Context) {

    private val locationManager = context.getSystemService(LocationManager::class.java)
    private val dataStore = context.settingsDataStore
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _speedKmh = MutableStateFlow<Float?>(null)

    /** Velocidad actual en km/h; `null` sin señal GPS (o sin permiso). */
    val speedKmh: StateFlow<Float?> = _speedKmh.asStateFlow()

    /**
     * Última posición conocida, guardada en DataStore para poder calcular el amanecer al
     * arrancar el coche aunque el GPS aún no tenga señal.
     */
    val lastKnownPoint: Flow<GeoPoint?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val lat = prefs[KEY_LAT]
            val lon = prefs[KEY_LON]
            if (lat != null && lon != null) GeoPoint(lat, lon) else null
        }
        .distinctUntilChanged()

    private var listening = false
    private var previous: Location? = null
    private var lastSaved: Location? = null

    // Si en 5 s no llega ninguna posición, la velocidad deja de ser fiable.
    private val markStale = Runnable { _speedKmh.value = null }

    // LocationListenerCompat (de androidx) trae implementación por defecto de los métodos
    // que en Android < 11 eran abstractos; con el LocationListener de la plataforma, una lambda
    // haría fallar la app en esos Android al llamar onStatusChanged.
    private val listener = object : LocationListenerCompat {
        override fun onLocationChanged(location: Location) = onLocation(location)
    }

    fun hasPermission(): Boolean =
        granted(Manifest.permission.ACCESS_FINE_LOCATION) || granted(Manifest.permission.ACCESS_COARSE_LOCATION)

    // El permiso se comprueba en hasPermission(); lint no sabe verlo.
    @SuppressLint("MissingPermission")
    fun start() {
        if (listening || !hasPermission()) return
        val provider = when {
            granted(Manifest.permission.ACCESS_FINE_LOCATION) &&
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return
        }
        try {
            locationManager.requestLocationUpdates(provider, 1_000L, 0f, listener, Looper.getMainLooper())
            listening = true
            locationManager.getLastKnownLocation(provider)?.let(::maybeSavePoint)
        } catch (_: SecurityException) {
            // permiso revocado justo ahora
        }
    }

    fun stop() {
        if (listening) locationManager.removeUpdates(listener)
        listening = false
        previous = null
        handler.removeCallbacks(markStale)
        _speedKmh.value = null
    }

    private fun onLocation(location: Location) {
        val prev = previous
        val speed = when {
            location.hasSpeed() -> location.speed * 3.6f
            // Sin velocidad del GPS (proveedor de red): distancia / tiempo entre dos posiciones
            prev != null && location.time > prev.time ->
                prev.distanceTo(location) / ((location.time - prev.time) / 1000f) * 3.6f
            else -> null
        }
        // Por debajo de 2 km/h el GPS "baila" estando parado: mostrar 0
        _speedKmh.value = speed?.let { if (it < 2f) 0f else it }
        previous = location
        handler.removeCallbacks(markStale)
        handler.postDelayed(markStale, 5_000L)
        maybeSavePoint(location)
    }

    /** Guarda la posición solo si es la primera o si nos movimos > 5 km (no escribir cada segundo). */
    private fun maybeSavePoint(location: Location) {
        val saved = lastSaved
        if (saved != null && saved.distanceTo(location) < 5_000f) return
        lastSaved = location
        scope.launch {
            dataStore.edit {
                it[KEY_LAT] = location.latitude
                it[KEY_LON] = location.longitude
            }
        }
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private companion object {
        val KEY_LAT = doublePreferencesKey("last_latitude")
        val KEY_LON = doublePreferencesKey("last_longitude")
    }
}
