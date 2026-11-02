package com.iappsan.carlauncher.ui.driving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iappsan.carlauncher.AppContainer
import com.iappsan.carlauncher.data.bluetooth.BluetoothRepository
import com.iappsan.carlauncher.data.bluetooth.BluetoothState
import com.iappsan.carlauncher.data.location.LocationRepository
import com.iappsan.carlauncher.data.media.MediaRepository
import com.iappsan.carlauncher.data.navigation.NavigationRepository
import com.iappsan.carlauncher.data.navigation.NavigationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Permisos en tiempo de ejecución que usa el launcher. */
data class PermissionsState(
    val location: Boolean = false,
    val bluetooth: Boolean = false,
    val notifications: Boolean = false,
)

/** Todo lo "de conducción": velocidad, ruta activa, Bluetooth y estado de permisos. */
class DrivingViewModel(
    private val locationRepository: LocationRepository,
    navigationRepository: NavigationRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    val speedKmh: StateFlow<Float?> = locationRepository.speedKmh
    val navigation: StateFlow<NavigationState?> = navigationRepository.state
    val bluetooth: StateFlow<BluetoothState> = bluetoothRepository.state

    private val _permissions = MutableStateFlow(PermissionsState())
    val permissions: StateFlow<PermissionsState> = _permissions.asStateFlow()

    /** Home visible (o permisos recién concedidos): GPS encendido y estado al día. */
    fun onVisible() {
        refreshPermissions()
        locationRepository.start()
        bluetoothRepository.start()
    }

    /** Home oculto: GPS apagado para no gastar batería/CPU. */
    fun onHidden() = locationRepository.stop()

    fun refreshPermissions() {
        _permissions.value = PermissionsState(
            location = locationRepository.hasPermission(),
            bluetooth = bluetoothRepository.hasPermission(),
            notifications = mediaRepository.hasPermission(),
        )
    }

    override fun onCleared() = locationRepository.stop()

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                DrivingViewModel(
                    container.locationRepository,
                    container.navigationRepository,
                    container.bluetoothRepository,
                    container.mediaRepository,
                )
            }
        }
    }
}
