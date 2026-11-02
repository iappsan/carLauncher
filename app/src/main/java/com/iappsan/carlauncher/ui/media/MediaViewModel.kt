package com.iappsan.carlauncher.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.iappsan.carlauncher.AppContainer
import com.iappsan.carlauncher.data.media.MediaRepository
import com.iappsan.carlauncher.data.media.MediaState
import kotlinx.coroutines.flow.StateFlow

class MediaViewModel(private val mediaRepository: MediaRepository) : ViewModel() {

    val state: StateFlow<MediaState> = mediaRepository.state

    /** Home visible: empezar a escuchar (y volver a comprobar el permiso, por si se acaba de conceder). */
    fun onVisible() = mediaRepository.start()

    fun onHidden() = mediaRepository.stop()

    fun playPause() = mediaRepository.playPause()
    fun next() = mediaRepository.next()
    fun previous() = mediaRepository.previous()

    override fun onCleared() = mediaRepository.stop()

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { MediaViewModel(container.mediaRepository) }
        }
    }
}
