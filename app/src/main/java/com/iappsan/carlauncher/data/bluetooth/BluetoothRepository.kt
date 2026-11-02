package com.iappsan.carlauncher.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.iappsan.carlauncher.data.LauncherSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BluetoothState(
    /** El equipo tiene Bluetooth accesible con las APIs estándar de Android. */
    val supported: Boolean = false,
    val permissionGranted: Boolean = false,
    val enabled: Boolean = false,
    /** Nombre del teléfono conectado; `null` si no hay ninguno. */
    val deviceName: String? = null,
    /** Batería del teléfono (0–100) si la informa; `null` si no se sabe. */
    val batteryPercent: Int? = null,
)

/**
 * Estado del teléfono conectado por Bluetooth, su batería, y "reanudar música al conectar".
 *
 * Importante: muchos head units genéricos gestionan el Bluetooth del teléfono con un módulo propio
 * del fabricante que no pasa por estas APIs; en esos equipos [BluetoothState.deviceName] será `null`.
 * El receptor de eventos vive mientras viva el proceso (no solo con Home visible), para poder
 * reanudar la música aunque haya otra app delante.
 */
class BluetoothRepository(
    private val context: Context,
    settingsRepository: LauncherSettingsRepository,
) {
    private val adapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(BluetoothState(supported = adapter != null))
    val state: StateFlow<BluetoothState> = _state.asStateFlow()

    // Teléfonos conectados (por dirección MAC) y su nombre.
    private val connected = LinkedHashMap<String, String>()
    private var battery: Int? = null
    private var started = false

    @Volatile
    private var resumeMusicOnConnect = true

    init {
        scope.launch { settingsRepository.settings.collect { resumeMusicOnConnect = it.resumeMusicOnConnect } }
        start()
    }

    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

    /** Empieza a escuchar. Se puede llamar varias veces (p. ej. tras conceder el permiso). */
    fun start() {
        if (started || adapter == null) {
            publish()
            return
        }
        if (!hasPermission()) {
            publish()
            return
        }
        started = true
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
            addAction(ACTION_HFP_CLIENT_AG_EVENT)
        }
        // Son difusiones del sistema: hace falta EXPORTED para recibirlas en Android 13+.
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        queryConnectedDevices()
        publish()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                        connected.clear()
                        battery = null
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> device?.takeIf(::isPhone)?.let { onPhoneConnected(it) }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> device?.let {
                    connected.remove(it.address)
                    if (connected.isEmpty()) battery = null
                }
                // Teléfono/auricular que informa su batería (0–100)
                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                    if (level in 0..100) battery = level
                }
                // El estéreo actuando de "manos libres" (HFP client) recibe la batería del teléfono (0–5)
                ACTION_HFP_CLIENT_AG_EVENT -> {
                    val level = intent.getIntExtra(EXTRA_HFP_BATTERY_LEVEL, -1)
                    if (level in 0..5) battery = level * 20
                }
            }
            publish()
        }
    }

    private fun onPhoneConnected(device: BluetoothDevice) {
        val isNew = connected.put(device.address, nameOf(device)) == null
        if (isNew && resumeMusicOnConnect) {
            // Esperar a que el audio Bluetooth esté listo antes de pedir "play"
            handler.postDelayed(::sendPlay, RESUME_DELAY_MS)
        }
    }

    /** "Play" a la última sesión multimedia (la que el sistema recuerda para los botones del volante). */
    private fun sendPlay() {
        val now = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
    }

    /**
     * Estado inicial: dispositivos ya conectados antes de arrancar el launcher. Se pregunta a varios
     * perfiles, incluidos los del lado "coche" (A2DP sink = 11, HFP client = 16), que no son públicos
     * pero se pueden pedir por su número.
     */
    private fun queryConnectedDevices() {
        val adapter = adapter ?: return
        for (profile in listOf(BluetoothProfile.HEADSET, BluetoothProfile.A2DP, PROFILE_A2DP_SINK, PROFILE_HFP_CLIENT)) {
            try {
                adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        try {
                            proxy.connectedDevices.filter(::isPhone).forEach { connected[it.address] = nameOf(it) }
                        } catch (_: SecurityException) {
                        }
                        adapter.closeProfileProxy(profile, proxy)
                        publish()
                    }

                    override fun onServiceDisconnected(profile: Int) {}
                }, profile)
            } catch (_: SecurityException) {
            }
        }
    }

    private fun publish() {
        val enabled = try {
            adapter?.isEnabled == true
        } catch (_: SecurityException) {
            false
        }
        _state.value = BluetoothState(
            supported = adapter != null,
            permissionGranted = hasPermission(),
            enabled = enabled,
            deviceName = connected.values.lastOrNull(),
            batteryPercent = battery.takeIf { connected.isNotEmpty() },
        )
    }

    // Con permiso comprobado en start(); lint no puede verlo.
    @SuppressLint("MissingPermission")
    private fun nameOf(device: BluetoothDevice): String = try {
        device.name ?: device.address
    } catch (_: SecurityException) {
        device.address
    }

    @SuppressLint("MissingPermission")
    private fun isPhone(device: BluetoothDevice): Boolean = try {
        val major = device.bluetoothClass?.majorDeviceClass
        // Sin clase conocida se acepta (algunos teléfonos no la anuncian)
        major == null || major == BluetoothClass.Device.Major.PHONE || major == BluetoothClass.Device.Major.UNCATEGORIZED
    } catch (_: SecurityException) {
        true
    }

    private companion object {
        const val RESUME_DELAY_MS = 4_000L
        const val PROFILE_A2DP_SINK = 11
        const val PROFILE_HFP_CLIENT = 16

        // Constantes ocultas del SDK (existen en el sistema, pero no son públicas)
        const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
        const val ACTION_HFP_CLIENT_AG_EVENT = "android.bluetooth.headsetclient.profile.action.AG_EVENT"
        const val EXTRA_HFP_BATTERY_LEVEL = "android.bluetooth.headsetclient.extra.BATTERY_LEVEL"
    }
}
