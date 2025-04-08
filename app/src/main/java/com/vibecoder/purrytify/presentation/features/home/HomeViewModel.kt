package com.vibecoder.purrytify.presentation.features.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.data.repository.SongRepository
import com.vibecoder.purrytify.playback.PlaybackStateManager
import com.vibecoder.purrytify.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeScreenState(
        val recentlyPlayed: List<SongEntity> = emptyList(),
        val newSongs: List<SongEntity> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null
)

@HiltViewModel
class HomeViewModel
@Inject
constructor(
        private val songRepository: SongRepository,
        private val playbackStateManager: PlaybackStateManager
) : ViewModel() {

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    val currentSong = playbackStateManager.currentSong
    val isPlaying = playbackStateManager.isPlaying

    init {
        loadHomepageSongs()

        // Subscribe to recently played changes
        viewModelScope.launch {
            playbackStateManager.recentlyPlayed.collect { recentSongs ->
                _state.update { it.copy(recentlyPlayed = recentSongs) }
            }
        }
    }

    private fun loadHomepageSongs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            songRepository
                    .getAllSongs()
                    .catch { e ->
                        Log.e("HomeViewModel", "Error loading songs", e)
                        _state.update {
                            it.copy(isLoading = false, error = "Failed to load songs.")
                        }
                    }
                    .collect { songs ->
                        _state.update {
                            it.copy(
                                    isLoading = false,
                                    newSongs = songs.sortedByDescending { it.createdAt },
                            )
                        }
                    }
        }
    }

    fun togglePlayPause() {
        playbackStateManager.playPause()
    }

    fun selectSong(song: SongEntity) {
        // Play song with current section songs as queue
        if (_state.value.recentlyPlayed.contains(song)) {
            playbackStateManager.playSong(song, _state.value.recentlyPlayed)
        } else {
            playbackStateManager.playSong(song, _state.value.newSongs)
        }
    }

    fun addToQueue(song: SongEntity) {
        playbackStateManager.addToQueue(song)
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val songToToggle = playbackStateManager.currentSong.value
            if (songToToggle != null) {
                val newLikedStatus = !songToToggle.isLiked
                when (val result = songRepository.updateLikeStatus(songToToggle.id, newLikedStatus)
                ) {
                    is Resource.Success -> {
                        playbackStateManager.refreshCurrentSongData()
                        Log.d("HomeViewModel", "Song favorite status updated successfully.")
                    }
                    is Resource.Error -> {
                        Log.e(
                                "HomeViewModel",
                                "Error updating song favorite status: ${result.message}"
                        )
                    }
                    is Resource.Loading -> {
                        Log.d("HomeViewModel", "Updating song favorite status...")
                    }
                }
            }
        }
    }
}
