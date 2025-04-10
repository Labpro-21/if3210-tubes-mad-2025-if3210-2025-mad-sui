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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeScreenState(
        val recentlyPlayed: List<SongEntity> = emptyList(),
        val newSongs: List<SongEntity> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null,
        val showEditDialog: Boolean = false,
        val songToEdit: SongEntity? = null
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

        viewModelScope.launch {
            playbackStateManager.recentlyPlayed.collect { recentSongs ->
                _state.update { it.copy(recentlyPlayed = recentSongs) }
            }
        }

        initialize(this)
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
        if (_state.value.recentlyPlayed.contains(song)) {
            playbackStateManager.playSong(song, _state.value.recentlyPlayed)
        } else {
            playbackStateManager.playSong(song, _state.value.newSongs)
        }
    }

    fun toggleLikeStatus(song: SongEntity) {
        viewModelScope.launch {
            val newLikedStatus = !song.isLiked
            when (val result = songRepository.updateLikeStatus(song.id, newLikedStatus)) {
                is Resource.Success -> {
                    if (song.id == currentSong.value?.id) {
                        playbackStateManager.refreshCurrentSongData()
                    }
                    loadHomepageSongs()
                    Log.d("HomeViewModel", "Song like status updated successfully.")
                }
                is Resource.Error -> {
                    Log.e("HomeViewModel", "Error updating song like status: ${result.message}")
                }
                is Resource.Loading -> {
                    Log.d("HomeViewModel", "Updating song like status...")
                }
            }
        }
    }

    fun deleteSong(song: SongEntity) {
        viewModelScope.launch {
            when (val result = songRepository.deleteSong(song.id)) {
                is Resource.Success -> {
                    playbackStateManager.removeFromRecentlyPlayed(song.id)

                    if (song.id == currentSong.value?.id) {
                        if (playbackStateManager.isPlayingFromQueue.value) {
                            Log.d(
                                    "HomeViewModel",
                                    "Deleted currently playing song - skipping to next in queue"
                            )
                            playbackStateManager.skipToNext()
                        } else {
                            Log.d(
                                    "HomeViewModel",
                                    "Deleted currently playing song - stopping playback"
                            )
                            playbackStateManager.stopPlayback()
                        }
                    }

                    loadHomepageSongs()
                    Log.d("HomeViewModel", "Song deleted successfully: ${song.title}")

                    if (song.id == currentSong.value?.id) {
                        delay(300)
                        playbackStateManager.refreshCurrentSongData()
                    }
                }
                is Resource.Error -> {
                    Log.e("HomeViewModel", "Error deleting song: ${result.message}")
                }
                is Resource.Loading -> {
                    Log.d("HomeViewModel", "Deleting song...")
                }
            }
        }
    }

    fun showEditSongDialog(song: SongEntity) {
        _state.update { it.copy(showEditDialog = true, songToEdit = song) }
    }

    fun hideEditSongDialog(refreshList: Boolean = false) {
        val editedSongId = _state.value.songToEdit?.id
        _state.update { it.copy(showEditDialog = false, songToEdit = null) }

        if (refreshList) {
            refreshSongs()

            if (editedSongId != null && editedSongId == currentSong.value?.id) {
                Log.d("HomeViewModel", "Edited currently playing song, refreshing player state")
                playbackStateManager.refreshCurrentSongData()
            }
        }
    }

    fun refreshSongs() {
        loadHomepageSongs()
    }

    companion object {
        private var instance: HomeViewModel? = null

        fun initialize(viewModel: HomeViewModel) {
            instance = viewModel
        }

        fun getInstance(): HomeViewModel {
            return instance ?: throw IllegalStateException("HomeViewModel not initialized")
        }
    }
}
