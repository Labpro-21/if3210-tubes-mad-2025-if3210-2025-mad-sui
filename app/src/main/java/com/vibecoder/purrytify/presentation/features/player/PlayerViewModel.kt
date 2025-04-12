package com.vibecoder.purrytify.presentation.features.player

import android.media.session.PlaybackState
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.data.repository.SongRepository
import com.vibecoder.purrytify.playback.PlaybackStateManager
import com.vibecoder.purrytify.playback.RepeatMode
import com.vibecoder.purrytify.playback.ShuffleMode
import com.vibecoder.purrytify.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel
@Inject
constructor(
        private val playbackStateManager: PlaybackStateManager,
        private val songRepository: SongRepository
) : ViewModel() {

    val currentSong: StateFlow<SongEntity?> = playbackStateManager.currentSong
    val isPlaying: StateFlow<Boolean> = playbackStateManager.isPlaying
    val playbackState: StateFlow<Int> = playbackStateManager.playbackState
    val currentPositionMs: StateFlow<Long> = playbackStateManager.currentPositionMs
    val totalDurationMs: StateFlow<Long> = playbackStateManager.totalDurationMs
    val playerError: StateFlow<String?> = playbackStateManager.error
    val queue = playbackStateManager.queue
    val repeatMode = playbackStateManager.repeatMode
    val shuffleMode = playbackStateManager.shuffleMode
    val canSkipNext = playbackStateManager.canSkipNext
    val canSkipPrevious = playbackStateManager.canSkipPrevious
    val currentQueueIndex = playbackStateManager.currentQueueIndex
    val isPlayingFromQueue = playbackStateManager.isPlayingFromQueue

    private val _songToEditFlow = MutableSharedFlow<SongEntity>()
    val songToEditFlow: SharedFlow<SongEntity> = _songToEditFlow.asSharedFlow()

    // Queue songs for display
    private val _queueSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val queueSongs: StateFlow<List<SongEntity>> = _queueSongs.asStateFlow()

    // Flag to track if player is visible
    private val _isPlayerVisible = MutableStateFlow(false)
    val isPlayerVisible: StateFlow<Boolean> = _isPlayerVisible.asStateFlow()

    val isShuffleOn =
            shuffleMode
                    .map { it == ShuffleMode.ON }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val repeatModeIcon =
            repeatMode
                    .map { mode ->
                        when (mode) {
                            RepeatMode.NONE -> RepeatModeIcon.NONE
                            RepeatMode.ALL -> RepeatModeIcon.ALL
                            RepeatMode.ONE -> RepeatModeIcon.ONE
                        }
                    }
                    .stateIn(
                            viewModelScope,
                            SharingStarted.WhileSubscribed(5000),
                            RepeatModeIcon.NONE
                    )

    val isFavorite: StateFlow<Boolean> =
            currentSong
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
        data class ShowEditDialog(val song: SongEntity) : UiEvent()
        object HideMinimizedPlayer : UiEvent() // To hide the minimized player
        object CurrentSongDeleted : UiEvent() // For current song deletion
        data class EditRejected(val message: String) : UiEvent() // New event for rejected edits
    }

    enum class RepeatModeIcon {
        NONE,
        ALL,
        ONE
    }

    enum class SongStatus {
        CURRENTLY_PLAYING,
        IN_QUEUE,
        NOT_IN_QUEUE
    }

    init {
        refreshQueueSongs()
        viewModelScope.launch { queue.collect { refreshQueueSongs() } }
        playBackObserver()
    }

    fun setPlayerVisibility(isVisible: Boolean) {
        _isPlayerVisible.value = isVisible
    }   

    fun playBackObserver(){
        viewModelScope.launch {
            playbackState.collect { state ->
                if (state == PlaybackState.STATE_PLAYING) {
                    currentSong.value?.let { song ->

                        viewModelScope.launch {
                            songRepository.markAsListened(song.id)
                        }
                    }
                }
            }
        }
    }

    fun refreshPlayerState() {
        viewModelScope.launch {
            // Refresh current song data if available
            currentSong.value?.let { song ->
                when (val result = songRepository.getSongById(song.id)) {
                    is Resource.Success -> {
                        if (result.data != null) {
                            // Update current song in PlaybackStateManager
                            playbackStateManager.refreshCurrentSongData()

                            // Update queue if playing from queue
                            refreshQueueSongs()

                            Log.d("PlayerViewModel", "Player state refreshed after edit/delete")

                            _uiEvents.emit(UiEvent.ShowSnackbar("Song updated"))
                        } else {
                            if (isPlayingFromQueue.value) {
                                skipToNext()
                            } else {
                                _uiEvents.emit(UiEvent.HideMinimizedPlayer)
                                _uiEvents.emit(UiEvent.CurrentSongDeleted)

                                Log.d(
                                        "PlayerViewModel",
                                        "Current song no longer exists, hiding player"
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        Log.e(
                                "PlayerViewModel",
                                "Failed to refresh player state: ${result.message}"
                        )
                        // The song might have been deleted
                        _uiEvents.emit(UiEvent.ShowSnackbar("Unable to access current song data"))
                        _uiEvents.emit(UiEvent.HideMinimizedPlayer)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun refreshQueueSongs() {
        playbackStateManager.getQueueSongs { songs -> _queueSongs.value = songs }
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

    fun playQueueItemAt(index: Int) {
        if (index >= 0 && index < _queueSongs.value.size) {
            playbackStateManager.playQueueItem(index)
        }
    }

    fun toggleShuffle() {
        playbackStateManager.toggleShuffleMode()
    }

    fun cycleRepeatMode() {
        playbackStateManager.cycleRepeatMode()
    }

    // Add a song to the manual queue
    fun addToQueue(song: SongEntity) {
        if (isInQueue(song.id)) {
            viewModelScope.launch { _uiEvents.emit(UiEvent.ShowSnackbar("Song already in queue")) }
            return
        }

        playbackStateManager.addToQueue(song)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowSnackbar("Added to queue: ${song.title}"))
        }
    }

    fun removeFromQueue(song: SongEntity) {
        playbackStateManager.removeFromQueue(song.id)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowSnackbar("Removed from queue: ${song.title}"))
        }
    }

    fun isInQueue(songId: Long): Boolean {
        return playbackStateManager.isInQueue(songId)
    }

    fun checkSongStatus(song: SongEntity): SongStatus {
        val currentSongId = currentSong.value?.id

        if (song.id == currentSongId) {
            return SongStatus.CURRENTLY_PLAYING
        }

        return if (isInQueue(song.id)) SongStatus.IN_QUEUE else SongStatus.NOT_IN_QUEUE
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val songToToggle = currentSong.value
            if (songToToggle != null) {
                val newLikedStatus = !songToToggle.isLiked
                when (val result = songRepository.updateLikeStatus(songToToggle.id, newLikedStatus)
                ) {
                    is Resource.Success -> {
                        playbackStateManager.refreshCurrentSongData()
                    }
                    is Resource.Error -> {
                        _uiEvents.emit(UiEvent.ShowSnackbar("Failed to update favorite status."))
                    }
                    is Resource.Loading -> {}
                }
            } else {
                _uiEvents.emit(UiEvent.ShowSnackbar("No song playing to like."))
            }
        }
    }

    fun onPlayerClicked() {
        viewModelScope.launch { _uiEvents.emit(UiEvent.NavigateToFullScreenPlayer) }
    }

    // Toggle a song's queue status
    fun toggleQueueStatus(song: SongEntity) {
        val status = checkSongStatus(song)
        when (status) {
            SongStatus.IN_QUEUE -> removeFromQueue(song)
            SongStatus.NOT_IN_QUEUE -> addToQueue(song)
            SongStatus.CURRENTLY_PLAYING -> {
                addToQueue(song)
            }
        }
    }

    /**
     * Requests to edit a song, with safeguards to prevent editing currently playing songs
     *
     * @param song The song to edit
     * @param collapsePlayer Whether to collapse the player when editing
     * @param forceEdit If true, will allow editing even if song is currently playing
     * @return True if edit was attempted, false if prevented
     */
    fun requestEditSong(
            song: SongEntity,
            collapsePlayer: Boolean = false,
            forceEdit: Boolean = false
    ): Boolean {
        val isCurrentlyPlaying = currentSong.value?.id == song.id && isPlaying.value

        // Prevent editing if song is currently playing and force edit is not enabled
        if (isCurrentlyPlaying && !forceEdit) {
            viewModelScope.launch {
                _uiEvents.emit(
                        UiEvent.EditRejected(
                                "Cannot edit currently playing song. Pause playback first."
                        )
                )
                _uiEvents.emit(
                        UiEvent.ShowSnackbar(
                                "Cannot edit currently playing song. Pause playback first."
                        )
                )
            }
            return false
        }

        viewModelScope.launch {
            if (collapsePlayer) {
                _songToEditFlow.emit(song)
            } else {
                _uiEvents.emit(UiEvent.ShowEditDialog(song))
            }
        }
        return true
    }

    fun deleteSong(songId: Long, forceDelete: Boolean = false): Boolean {
        val isCurrentlyPlaying = currentSong.value?.id == songId && isPlaying.value

        // Prevent deletion if song is currently playing and force delete is not enabled
        if (isCurrentlyPlaying && !forceDelete) {
            viewModelScope.launch {
                _uiEvents.emit(
                        UiEvent.ShowSnackbar(
                                "Cannot delete currently playing song. Pause playback first."
                        )
                )
            }
            return false
        }

        viewModelScope.launch {
            when (val result = songRepository.deleteSong(songId)) {
                is Resource.Success -> {
                    // Remove from queue if in queue
                    if (isInQueue(songId)) {
                        removeFromQueue(
                                SongEntity(
                                        id = songId,
                                        title = "",
                                        artist = "",
                                        filePathUri = "",
                                        coverArtUri = null,
                                        duration = 0,
                                        userEmail = ""
                                )
                        )
                    }

                    // Remove from recently played
                    playbackStateManager.removeFromRecentlyPlayed(songId)

                    // Handle current song deletion
                    if (currentSong.value?.id == songId) {
                        playbackStateManager.stopPlayback()
                        _uiEvents.emit(UiEvent.CurrentSongDeleted)
                        _uiEvents.emit(UiEvent.HideMinimizedPlayer)
                    }

                    // Refresh queue display
                    refreshQueueSongs()
                    refreshPlayerState()

                    _uiEvents.emit(UiEvent.ShowSnackbar("Song deleted successfully"))
                }
                is Resource.Error -> {
                    _uiEvents.emit(UiEvent.ShowSnackbar("Failed to delete song: ${result.message}"))
                }
                else -> {}
            }
        }
        return true
    }

    /** Stop current playback and release resources */
    fun stopPlayback() {
        playbackStateManager.stopPlayback()
    }
}
