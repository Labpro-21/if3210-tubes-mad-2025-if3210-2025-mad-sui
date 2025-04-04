package com.vibecoder.purrytify.presentation.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.data.repository.SongRepository
import com.vibecoder.purrytify.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log


data class HomeScreenState(
    val currentSong: SongEntity? = null,
    val isPlaying: Boolean = false,
    val recentlyPlayed: List<SongEntity> = emptyList(),
    val newSongs: List<SongEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()


    val isCurrentSongFavorite: StateFlow<Boolean> = _state.map { it.currentSong?.isLiked ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)


    init {
        loadSongs()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            songRepository.getAllSongs()
                .catch { e ->
                    Log.e("HomeViewModel", "Error loading songs", e)
                    _state.update { it.copy(isLoading = false, error = "Failed to load songs.") }
                }
                .collect { songs ->
                    val currentPlaying = _state.value.currentSong
                    _state.update {
                        it.copy(
                            isLoading = false,
                            newSongs = songs,
                            recentlyPlayed = songs, // TODO : Change this to recent
                            currentSong = currentPlaying?.let { current -> songs.find { s -> s.id == current.id } }
                                ?: songs.firstOrNull() // TODO : change this
                                ?: null,

                            isPlaying = (currentPlaying?.let { current -> songs.find { s -> s.id == current.id } }
                                ?: songs.firstOrNull()) != null && _state.value.isPlaying
                        )
                    }
                }
        }
    }


    fun togglePlayPause() {
        if (_state.value.currentSong != null) {
            val currentlyPlaying = !_state.value.isPlaying
            _state.update { it.copy(isPlaying = currentlyPlaying) }
            // TODO: Integrate with PlaybackStateManager later
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            _state.value.currentSong?.let { song ->
                val newLikedStatus = !song.isLiked

                when (val result = songRepository.updateLikeStatus(song.id, newLikedStatus)) {
                    is Resource.Success -> {

                        _state.update { currentState ->
                            currentState.copy(
                                currentSong = currentState.currentSong?.copy(isLiked = newLikedStatus)
                            )
                        }
                        Log.d("HomeViewModel", "Like status updated for ${song.id}")
                    }
                    is Resource.Error -> {
                        Log.e("HomeViewModel", "Failed to update like status for ${song.id}: ${result.message}")

                    }
                    else -> {}
                }
            } ?: Log.w("HomeViewModel", "Toggle Favorite called but no song is playing.")
        }
    }


    fun selectSong(song: SongEntity) {
        _state.update {
            it.copy(
                currentSong = song,
                    isPlaying = true
                )
            }
        // TODO: Integrate with PlaybackStateManager later
    }
}