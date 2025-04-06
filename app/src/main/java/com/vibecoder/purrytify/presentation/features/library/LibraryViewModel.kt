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
import com.vibecoder.purrytify.playback.PlaybackStateManager


data class LibraryScreenState(
    val songs: List<SongEntity> = emptyList(),
    val isBottomSheetVisible: Boolean = false,
    val selectedTab: Int = 0,
    val isLoadingSongs: Boolean = true,
    val libraryError: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playbackStateManager: PlaybackStateManager
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryScreenState())
    val state: StateFlow<LibraryScreenState> = _state.asStateFlow()


    private val _selectedTab = MutableStateFlow(0)

    private val currentSong = playbackStateManager.currentSong

    init {
      // tab changes
        viewModelScope.launch {
            _selectedTab.collect { tabIndex ->
                _state.update { it.copy(selectedTab = tabIndex, isLoadingSongs = true) }
                loadSongsForCurrentTab()
            }
        }

        // song changes (favorite/unfavorite)
        viewModelScope.launch {
            currentSong.collect { song ->

                if (song != null) {
                    refreshSongs()
                }
            }
        }
    }

    private fun loadSongsForCurrentTab() {
        viewModelScope.launch {
            try {
                val songsFlow = when (_selectedTab.value) {
                    0 -> songRepository.getAllSongs()
                    1 -> songRepository.getLikedSongs()
                    else -> flow { emit(emptyList<SongEntity>()) }
                }

                songsFlow.collect { songs ->
                    _state.update {
                        it.copy(
                            songs = songs,
                            isLoadingSongs = false,
                            libraryError = null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("LibraryVM", "Error loading songs", e)
                _state.update {
                    it.copy(
                        isLoadingSongs = false,
                        libraryError = "Failed to load songs: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun onTabSelected(index: Int) {
        if (_selectedTab.value != index) {
            _selectedTab.value = index
        }
    }

    fun onPlaySong(song: SongEntity) {
        playbackStateManager.playSong(song)
    }


    fun refreshSongs() {
        viewModelScope.launch {
            loadSongsForCurrentTab()
        }
    }

    fun showBottomSheet() {
        _state.update { it.copy(isBottomSheetVisible = true) }
    }

    fun hideBottomSheet(refreshList: Boolean = false) {
        _state.update { it.copy(isBottomSheetVisible = false) }
        if (refreshList) {
            refreshSongs()
        }
    }
}