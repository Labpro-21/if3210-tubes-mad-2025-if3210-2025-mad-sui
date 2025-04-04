package com.vibecoder.purrytify.presentation.features.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoder.purrytify.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class LibraryScreenState(
    val songs: List<Song> = emptyList(),
    val currentPlayingSong: Song? = null,
    val isPlaying: Boolean = false,
    val isCurrentSongFavorite: Boolean = false,
    val selectedTab: Int = 0
)

@HiltViewModel
class LibraryViewModel @Inject constructor(

) : ViewModel() {

    private val _state = MutableStateFlow(LibraryScreenState())
    val state: StateFlow<LibraryScreenState> = _state.asStateFlow()


    var selectedTab by mutableIntStateOf(0)
        private set


    val songs: List<Song> get() = _state.value.songs
    val currentPlayingSong: Song? get() = _state.value.currentPlayingSong
    val isPlaying: StateFlow<Boolean> = MutableStateFlow(_state.value.isPlaying)
    val isCurrentSongFavorite: StateFlow<Boolean> = MutableStateFlow(_state.value.isCurrentSongFavorite)


    init {

        loadSongs()

        _state.update { it.copy(currentPlayingSong = getDummyLibrarySongs().firstOrNull()) }
    }

    private fun loadSongs() {
        viewModelScope.launch {

            val fetchedSongs = if (selectedTab == 0) {
                getDummyLibrarySongs()
            } else {
                getDummyLibrarySongs().filter { it.title.contains("a", ignoreCase = true) }
            }
            _state.update { it.copy(songs = fetchedSongs) }
        }
    }

    fun onTabSelected(index: Int) {
        selectedTab = index
        _state.update { it.copy(selectedTab = index) }
        loadSongs()
    }

    fun onPlaySong(song: Song) {
        _state.update {
            it.copy(
                currentPlayingSong = song,
                isPlaying = true,

                isCurrentSongFavorite = song.title.contains("a", ignoreCase = true)
            )
        }
        (isPlaying as MutableStateFlow).value = true
        (isCurrentSongFavorite as MutableStateFlow).value = _state.value.isCurrentSongFavorite
    }

    fun togglePlayPause() {
        val currentlyPlaying = ! _state.value.isPlaying
        _state.update { it.copy(isPlaying = currentlyPlaying) }
        (isPlaying as MutableStateFlow).value = currentlyPlaying
    }

    fun toggleFavorite() {
        _state.value.currentPlayingSong?.let {
            val currentlyFavorite = !_state.value.isCurrentSongFavorite

            _state.update { currentState ->
                currentState.copy(isCurrentSongFavorite = currentlyFavorite)
            }
            (isCurrentSongFavorite as MutableStateFlow).value = currentlyFavorite

            if (selectedTab == 1 && !currentlyFavorite) {
                loadSongs()
            }
        }
    }

    // Dummy
    private fun getDummyLibrarySongs(): List<Song> {
        return listOf(
            Song("lib1", "Stairway to Heaven", "Led Zeppelin", "https://upload.wikimedia.org/wikipedia/en/3/39/The_Weeknd_-_Starboy.png"),
            Song("lib2", "Like a Rolling Stone", "Bob Dylan", "https://example.com/lib2.jpg"),
            Song("lib3", "Hotel California", "Eagles", "https://example.com/lib3.jpg"),
            Song("lib4", "Sweet Child o' Mine", "Guns N' Roses", "https://example.com/lib4.jpg"),
            Song("lib5", "Imagine", "John Lennon", "https://example.com/lib5.jpg")
        )
    }
}
