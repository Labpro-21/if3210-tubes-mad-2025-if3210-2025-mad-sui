package com.vibecoder.purrytify.presentation.features.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.data.repository.SongRepository
import com.vibecoder.purrytify.playback.PlaybackStateManager
import com.vibecoder.purrytify.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackStateManager: PlaybackStateManager,
    private val songRepository: SongRepository
) : ViewModel() {



    val currentSong: StateFlow<SongEntity?> = playbackStateManager.currentSong
    val isPlaying: StateFlow<Boolean> = playbackStateManager.isPlaying
    val playbackState: StateFlow<Int> = playbackStateManager.playbackState
    val currentPositionMs: StateFlow<Long> = playbackStateManager.currentPositionMs
    val totalDurationMs: StateFlow<Long> = playbackStateManager.totalDurationMs
    val playerError: StateFlow<String?> = playbackStateManager.error

    val isFavorite: StateFlow<Boolean> = currentSong
        .map { song -> song?.isLiked ?: false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = false
        )


    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object NavigateToFullScreenPlayer : UiEvent()
    }



    fun togglePlayPause() {
        playbackStateManager.playPause()
    }

    fun seekTo(positionMs: Long) {
        playbackStateManager.seekTo(positionMs)
    }

    fun skipToNext() {
        playbackStateManager.skipToNext()
    }

    fun skipToPrevious() {
        playbackStateManager.skipToPrevious()
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val songToToggle = currentSong.value
            if (songToToggle != null) {
                val newLikedStatus = !songToToggle.isLiked
                Log.d("PlayerViewModel", "Toggling favorite for song ${songToToggle.id} to $newLikedStatus")
                when (val result = songRepository.updateLikeStatus(songToToggle.id, newLikedStatus)) {
                    is Resource.Success -> {
                        Log.d("PlayerViewModel", "DB update successful. Refreshing player state.")
                        playbackStateManager.refreshCurrentSongData()
                    }
                    is Resource.Error -> {
                        Log.e("PlayerViewModel", "Failed to update favorite status in DB: ${result.message}")
                        _uiEvents.emit(UiEvent.ShowSnackbar("Failed to update favorite status."))
                    }
                    is Resource.Loading -> {  }
                }
            } else {
                Log.w("PlayerViewModel", "toggleFavorite called but currentSong is null.")
                viewModelScope.launch { _uiEvents.emit(UiEvent.ShowSnackbar("No song playing to like.")) }
            }
        }
    }


    fun onPlayerClicked() {
        Log.d("PlayerViewModel", "Minimized Player clicked - Requesting navigation.")
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.NavigateToFullScreenPlayer)
        }
    }
}