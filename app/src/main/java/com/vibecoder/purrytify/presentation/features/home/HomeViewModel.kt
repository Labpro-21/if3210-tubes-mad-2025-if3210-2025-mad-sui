package com.vibecoder.purrytify.presentation.features.home

import androidx.lifecycle.ViewModel
import com.vibecoder.purrytify.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeScreenState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    val recentlyPlayed: List<Song> = emptyList(),
    val newSongs: List<Song> = emptyList()
)

//@HiltViewModel
class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    init {
        _state.value = HomeScreenState(
            recentlyPlayed = getDummyRecentlyPlayed(),
            newSongs = getDummyRecentlyPlayed(),
            currentSong = getDummyRecentlyPlayed().first()
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

    fun selectSong(song: Song) {
        _state.value = _state.value.copy(
            currentSong = song,
            isPlaying = true
        )
    }
}
