package com.vibecoder.purrytify.presentation.features.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeScreenState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    val recentlyPlayed: List<Track> = emptyList(),
    val newSongs: List<Track> = emptyList()
)

class HomeScreenViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    init {

        _state.value = HomeScreenState(
            recentlyPlayed = getDummyRecentlyPlayed(),
            newSongs = getDummyRecentlyPlayed(),
            currentTrack = getDummyRecentlyPlayed().first()
        )
    }

    fun togglePlayPause() {
        _state.value = _state.value.copy(
            isPlaying = !_state.value.isPlaying
        )
    }

    fun toggleFavorite() {
        _state.value = _state.value.copy(
            isFavorite = !_state.value.isFavorite
        )
    }

    fun selectTrack(track: Track) {
        _state.value = _state.value.copy(
            currentTrack = track,
            isPlaying = true
        )
    }
}
