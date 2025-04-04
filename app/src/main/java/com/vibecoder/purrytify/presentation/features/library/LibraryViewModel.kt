package com.vibecoder.purrytify.presentation.features.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoder.purrytify.data.repository.SongRepository
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log



data class LibraryScreenState(
    val songs: List<SongEntity> = emptyList(),
    val currentPlayingSong: SongEntity? = null,
    val isPlaying: Boolean = false,

    val isBottomSheetVisible: Boolean = false,
    val selectedTab: Int = 0,
    val isLoadingSongs: Boolean = true,
    val libraryError: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryScreenState())
    val state: StateFlow<LibraryScreenState> = _state.asStateFlow()


    val isCurrentSongFavorite: StateFlow<Boolean> = _state.map { it.currentPlayingSong?.isLiked ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // stateflow to observe the current playing song
    val isPlaying: StateFlow<Boolean> = _state.map { it.isPlaying }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isBottomSheetVisible: StateFlow<Boolean> = _state.map { it.isBottomSheetVisible }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val selectedTab: StateFlow<Int> = _state.map { it.selectedTab }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val isLoadingSongs: StateFlow<Boolean> = _state.map { it.isLoadingSongs }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val libraryError: StateFlow<String?> = _state.map { it.libraryError }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    init {
        observeSongs()
    }

    private fun observeSongs() {
        viewModelScope.launch {
            _state.flatMapLatest { currentState ->
                when (currentState.selectedTab) {
                    0 -> songRepository.getAllSongs()
                    1 -> songRepository.getLikedSongs()
                    else -> flowOf(emptyList())
                }
            }.catch { e ->
                Log.e("LibraryVM", "Error observing songs", e)
                _state.update { it.copy(libraryError = "Failed to load songs.", isLoadingSongs = false) }
            }.collect { songsList ->
                _state.update { currentState ->

                    val updatedCurrentSong = currentState.currentPlayingSong?.let { current ->
                        songsList.find { s -> s.id == current.id }
                    } ?: currentState.currentPlayingSong

                    currentState.copy(
                        songs = songsList,
                        isLoadingSongs = false,
                        libraryError = null,
                        currentPlayingSong = updatedCurrentSong

                    )
                }
            }
        }
    }

    fun onTabSelected(index: Int) {
        if (_state.value.selectedTab != index) {
            _state.update { it.copy(selectedTab = index, isLoadingSongs = true, libraryError = null) }
        }
    }


    fun onPlaySong(song: SongEntity) {
        _state.update {
            it.copy(
                currentPlayingSong = song,
                isPlaying = true

            )
        }
    }

    fun togglePlayPause() {

        val currentlyPlaying = !_state.value.isPlaying
        _state.update { it.copy(isPlaying = currentlyPlaying) }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            _state.value.currentPlayingSong?.let { song ->
                val newLikedStatus = !song.isLiked

                when (val result = songRepository.updateLikeStatus(song.id, newLikedStatus)) {
                    is Resource.Success -> {
                        // The state update will happen via the observeSongs flow catching the DB changes
                        Log.d("LibraryVM", "Like status updated for ${song.id}")
                    }
                    is Resource.Error -> {
                        Log.e("LibraryVM", "Failed to update like status for ${song.id}: ${result.message}")
                    }
                    else -> {}
                }
            } ?: Log.w("LibraryVM", "Toggle Favorite called but no song is playing.")
        }
    }

    fun showBottomSheet() {
        _state.update { it.copy(isBottomSheetVisible = true) }
    }

    fun hideBottomSheet() {
        _state.update { it.copy(isBottomSheetVisible = false) }
    }
}