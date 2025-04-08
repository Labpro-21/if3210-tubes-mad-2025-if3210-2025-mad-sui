package com.vibecoder.purrytify.presentation.features.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.data.repository.SongRepository
import com.vibecoder.purrytify.playback.PlaybackStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LibraryScreenState(
        val songs: List<SongEntity> = emptyList(),
        val isBottomSheetVisible: Boolean = false,
        val selectedTab: Int = 0,
        val isLoadingSongs: Boolean = true,
        val libraryError: String? = null,
        val selectedSong: SongEntity? = null,
        val isContextMenuVisible: Boolean = false,
        val searchQuery: String = "",
        val originalSongs: List<SongEntity> = emptyList()
)

@HiltViewModel
class LibraryViewModel
@Inject
constructor(
        private val songRepository: SongRepository,
        private val playbackStateManager: PlaybackStateManager
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryScreenState())
    val state: StateFlow<LibraryScreenState> = _state.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)

    val currentSong = playbackStateManager.currentSong
    val isPlaying = playbackStateManager.isPlaying

    init {
        // Tab changes
        viewModelScope.launch {
            _selectedTab.collect { tabIndex ->
                _state.update { it.copy(selectedTab = tabIndex, isLoadingSongs = true) }
                loadSongsForCurrentTab()
            }
        }

        // Song changes (favorite/unfavorite)
        viewModelScope.launch {
            currentSong.collect { song ->
                if (song != null) {
                    refreshSongs()
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        // Update search query
        _state.update { it.copy(searchQuery = query) }

        // Filter songs based on the query
        val filteredSongs =
                if (query.isBlank()) {
                    _state.value.originalSongs
                } else {
                    applySearchFilter(_state.value.originalSongs, query)
                }

        _state.update { it.copy(songs = filteredSongs) }
    }

    private fun applySearchFilter(songs: List<SongEntity>, query: String): List<SongEntity> {
        if (query.isBlank()) return songs

        return songs.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true)
        }
    }

    private fun loadSongsForCurrentTab() {
        viewModelScope.launch {
            try {
                val songsFlow =
                        when (_selectedTab.value) {
                            0 -> songRepository.getAllSongs()
                            1 -> songRepository.getLikedSongs()
                            else -> flow { emit(emptyList<SongEntity>()) }
                        }

                songsFlow.collect { allSongs ->
                    val filteredSongs =
                            if (_state.value.searchQuery.isBlank()) {
                                allSongs
                            } else {
                                applySearchFilter(allSongs, _state.value.searchQuery)
                            }

                    _state.update {
                        it.copy(
                                songs = filteredSongs,
                                originalSongs = allSongs,
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
        // Play song with current tab songs as queue
        playbackStateManager.playSong(song, _state.value.originalSongs)
    }

    fun togglePlayPause() {
        playbackStateManager.playPause()
    }

    fun addToQueue(song: SongEntity) {
        playbackStateManager.addToQueue(song)
    }

    fun showContextMenuForSong(song: SongEntity) {
        _state.update { it.copy(selectedSong = song, isContextMenuVisible = true) }
    }

    fun hideContextMenu() {
        _state.update { it.copy(isContextMenuVisible = false) }
    }

    fun toggleFavoriteForSelectedSong() {
        val song = _state.value.selectedSong ?: return

        viewModelScope.launch {
            val newLikedStatus = !song.isLiked
            songRepository.updateLikeStatus(song.id, newLikedStatus)
            refreshSongs()
        }
    }

    fun deleteSong(songId: Long) {
        viewModelScope.launch {
            songRepository.deleteSong(songId)
            refreshSongs()
        }
    }

    fun refreshSongs() {
        viewModelScope.launch { loadSongsForCurrentTab() }
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
