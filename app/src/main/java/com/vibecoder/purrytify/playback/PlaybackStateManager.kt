package com.vibecoder.purrytify.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.data.repository.SongRepository
import com.vibecoder.purrytify.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private const val TAG = "PlaybackStateManager"

@Singleton
class PlaybackStateManager
@Inject
constructor(
        @ApplicationContext private val context: Context,
        private val songRepository: SongRepository,
        private val gson: Gson
) {

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    private val _isPlaying = MutableStateFlow(false)
    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    private val _currentPositionMs = MutableStateFlow(0L)
    private val _totalDurationMs = MutableStateFlow(0L)
    private val _isConnecting = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    //   Bonus State
    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    private val _shuffleMode = MutableStateFlow(ShuffleMode.OFF)

    // Navigation state
    private val _canSkipNext = MutableStateFlow(false)
    private val _canSkipPrevious = MutableStateFlow(false)

    // Current index in queue
    private val _currentQueueIndex = MutableStateFlow(-1)
    private val _isPlayingFromQueue = MutableStateFlow(false)

    // Regular songs list case empty queue
    private val _playbackList = MutableStateFlow<List<SongEntity>>(emptyList())
    private var currentPlaybackIndex = -1

    private val _recentlyPlayed = MutableStateFlow<List<SongEntity>>(emptyList())

    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()
    val totalDurationMs: StateFlow<Long> = _totalDurationMs.asStateFlow()
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    val shuffleMode: StateFlow<ShuffleMode> = _shuffleMode.asStateFlow()
    val recentlyPlayed: StateFlow<List<SongEntity>> = _recentlyPlayed.asStateFlow()
    val canSkipNext: StateFlow<Boolean> = _canSkipNext.asStateFlow()
    val canSkipPrevious: StateFlow<Boolean> = _canSkipPrevious.asStateFlow()
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()
    val isPlayingFromQueue: StateFlow<Boolean> = _isPlayingFromQueue.asStateFlow()

    private var handlingPlaybackEnd = false
    private var positionUpdateJob: Job? = null

    init {
        initializeMediaController()
        loadRecentlyPlayed()
        updateNavigationState()
    }

    fun removeFromRecentlyPlayed(songId: Long) {
        _recentlyPlayed.update { currentList -> currentList.filterNot { it.id == songId }.toList() }
        saveRecentlyPlayed()
        Log.d(TAG, "Song $songId removed from recently played list")
    }

    fun refreshRecentlyPlayed(delayMs: Long = 0) {
        if (_recentlyPlayed.value.isEmpty()) return

        mainScope.launch {
            // Apply delay if requested
            if (delayMs > 0) {
                delay(delayMs)
            }

            val currentIds = _recentlyPlayed.value.map { it.id }
            val refreshedSongs = mutableListOf<SongEntity>()

            for (id in currentIds) {
                when (val result = songRepository.getSongById(id)) {
                    is Resource.Success -> {
                        result.data?.let { refreshedSongs.add(it) }
                    }
                    else -> {}
                }
            }

            _recentlyPlayed.value = refreshedSongs
            saveRecentlyPlayed()
            Log.d(TAG, "Recently played list refreshed with ${refreshedSongs.size} songs")
        }
    }

    // Prev and next update state button
    private fun updateNavigationState() {
        // Using queue
        if (_queue.value.isNotEmpty()) {
            val currentIndex = _currentQueueIndex.value

            val hasNext =
                    currentIndex < _queue.value.size - 1 || _repeatMode.value == RepeatMode.ALL

            val hasPrevious = currentIndex > 0 || _repeatMode.value == RepeatMode.ALL

            _canSkipNext.value = hasNext
            _canSkipPrevious.value = hasPrevious
        }
        // Using regular playback list
        else {
            val hasNext =
                    currentPlaybackIndex < _playbackList.value.size - 1 ||
                            _repeatMode.value == RepeatMode.ALL

            val hasPrevious = currentPlaybackIndex > 0 || _repeatMode.value == RepeatMode.ALL

            _canSkipNext.value = hasNext
            _canSkipPrevious.value = hasPrevious
        }
    }

    // Load recently played songs from SharedPreferences
    private fun loadRecentlyPlayed() {
        mainScope.launch {
            try {
                // Get stored IDs from SharedPreferences
                val recentlyPlayedJson =
                        context.getSharedPreferences("purrytify_prefs", Context.MODE_PRIVATE)
                                .getString("recently_played", null)

                if (recentlyPlayedJson != null) {
                    val songIds =
                            gson.fromJson<List<Long>>(
                                    recentlyPlayedJson,
                                    object : TypeToken<List<Long>>() {}.type
                            )

                    // Load songs from database by IDs (in order)
                    val loadedSongs = mutableListOf<SongEntity>()
                    for (id in songIds) {
                        val result = songRepository.getSongById(id)
                        if (result is Resource.Success && result.data != null) {
                            loadedSongs.add(result.data)
                        }
                    }

                    _recentlyPlayed.value = loadedSongs
                } else {
                    _recentlyPlayed.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recently played songs", e)
                _recentlyPlayed.value = emptyList()
            }
        }
    }

    // Save recently played songs to persistent storage
    private fun saveRecentlyPlayed() {
        mainScope.launch(Dispatchers.IO) {
            try {
                val recentlyPlayedJson = gson.toJson(_recentlyPlayed.value.map { it.id })
                context.getSharedPreferences("purrytify_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("recently_played", recentlyPlayedJson)
                        .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving recently played songs", e)
            }
        }
    }

    private fun addToRecentlyPlayed(song: SongEntity) {
        _recentlyPlayed.update { currentList ->
            val newList = currentList.toMutableList()
            newList.removeIf { it.id == song.id }
            newList.add(0, song)
            // at most 20 songs
            newList.take(20).toList()
        }

        saveRecentlyPlayed()
    }

    private fun initializeMediaController() {
        if (mediaController != null || mediaControllerFuture?.isDone == true) {
            Log.d(TAG, "Controller already initialized or future done.")
            return
        }
        Log.d(TAG, "Initializing MediaController...")
        _isConnecting.value = true
        _error.value = null

        val sessionToken = SessionToken(context, ComponentName(context, PlayerService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        mediaControllerFuture?.addListener(
                {
                    try {
                        val controller = mediaControllerFuture?.get()
                        if (controller != null) {
                            mediaController = controller
                            controller.addListener(playerListener)

                            updateStateFromController(controller)
                            Log.i(TAG, "MediaController connected successfully.")
                            _isConnecting.value = false
                            startPositionUpdates()
                        } else {
                            handleConnectionError("MediaController future returned null.")
                        }
                    } catch (e: Exception) {
                        handleConnectionError("Failed to connect MediaController: ${e.message}")
                        Log.e(TAG, "Error getting MediaController from future", e)
                    }
                },
                MoreExecutors.directExecutor()
        )
    }

    private fun handleConnectionError(message: String) {
        _isConnecting.value = false
        _error.value = message
        resetState()
    }

    fun refreshCurrentSongData() {
        val songId = _currentSong.value?.id ?: return

        Log.d(TAG, "Refreshing data for current song ID: $songId")
        mainScope.launch {
            when (val resource = songRepository.getSongById(songId)) {
                is Resource.Success -> {
                    if (_currentSong.value?.id == songId) {
                        _currentSong.value = resource.data
                        if (resource.data == null)
                                Log.w(TAG, "Current song $songId seems to have been deleted.")
                    } else {
                        Log.d(TAG, "Song changed during refresh, ignoring result for $songId.")
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "Error refreshing song $songId from repo: ${resource.message}")
                }
                else -> {}
            }
        }
    }

    private fun updateStateFromController(controller: MediaController?) {
        if (controller == null) {
            resetState()
            return
        }
        _isPlaying.value = controller.isPlaying
        _playbackState.value = controller.playbackState
        _currentPositionMs.value = controller.currentPosition.coerceAtLeast(0L)
        _totalDurationMs.value = controller.duration.coerceAtLeast(0L)

        val currentMediaItem = controller.currentMediaItem
        val currentMediaId = currentMediaItem?.mediaId
        val currentSongId = currentMediaId?.toLongOrNull()

        if (currentSongId != _currentSong.value?.id) {
            if (currentSongId != null) {
                mainScope.launch {
                    when (val resource = songRepository.getSongById(currentSongId)) {
                        is Resource.Success -> {
                            resource.data?.let { song ->
                                _currentSong.value = song
                                addToRecentlyPlayed(song)

                                // Update queue index if playing from queue
                                if (_isPlayingFromQueue.value) {
                                    for (i in _queue.value.indices) {
                                        if (_queue.value[i].songId == song.id) {
                                            _currentQueueIndex.value = i
                                            break
                                        }
                                    }
                                }
                            }
                            if (resource.data == null) {
                                Log.w(TAG, "Song with ID $currentSongId not found in repository.")
                            }
                        }
                        is Resource.Error -> {
                            Log.e(
                                    TAG,
                                    "Error fetching song $currentSongId from repo: ${resource.message}"
                            )
                            _currentSong.value = null
                            _error.value = "Error loading current song details."
                        }
                        else -> {}
                    }

                    updateNavigationState()
                }
            } else {
                _currentSong.value = null
            }
        }
    }

    private fun resetState() {
        _currentSong.value = null
        _isPlaying.value = false
        _playbackState.value = Player.STATE_IDLE
        _currentPositionMs.value = 0L
        _totalDurationMs.value = 0L
        _currentQueueIndex.value = -1
        _isPlayingFromQueue.value = false
        _canSkipNext.value = false
        _canSkipPrevious.value = false
    }

    private val playerListener =
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        startPositionUpdates()
                    } else {
                        stopPositionUpdates()
                        _currentPositionMs.value =
                                mediaController?.currentPosition?.coerceAtLeast(0L) ?: 0L
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _playbackState.value = playbackState
                    Log.d(TAG, "Playback state changed to: $playbackState")

                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            Log.d(TAG, "Playback STATE_ENDED detected")
                            stopPositionUpdates()
                            _isPlaying.value = false
                            _currentPositionMs.value = _totalDurationMs.value

                            // This fucking problem to solve the undeterministic behaviour of double
                            // jump
                            if (!handlingPlaybackEnd) {
                                handlePlaybackEnd()
                            }
                        }
                        Player.STATE_READY -> {
                            _totalDurationMs.value =
                                    mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                            if (mediaController?.isPlaying == true) startPositionUpdates()

                            handlingPlaybackEnd = false

                            updateNavigationState()
                        }
                        Player.STATE_IDLE -> {
                            resetState()
                            stopPositionUpdates()
                        }
                        else -> {}
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateStateFromController(mediaController)
                    _totalDurationMs.value = mediaController?.duration?.coerceAtLeast(0L) ?: 0L

                    mediaItem?.mediaId?.toLongOrNull()?.let { songId ->
                        if (!_isPlayingFromQueue.value) {
                            updateCurrentPlaybackIndex(songId)
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Player error: ${error.message}", error)
                    _error.value = "Playback error: ${error.message}"
                    stopPositionUpdates()
                    resetState()
                }

                override fun onTimelineChanged(
                        timeline: androidx.media3.common.Timeline,
                        reason: Int
                ) {
                    if (reason == Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE) {
                        _totalDurationMs.value = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                    }
                }
            }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        if (_isPlaying.value && mediaController != null) {
            positionUpdateJob =
                    mainScope.launch {
                        while (isActive && _isPlaying.value) {
                            val currentPos =
                                    mediaController?.currentPosition?.coerceAtLeast(0L) ?: 0L
                            if (_currentPositionMs.value != currentPos) {
                                _currentPositionMs.value = currentPos
                            }
                            delay(500)
                        }
                    }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    // --- Playback Control Methods ---
    fun playPause() {
        if (mediaController?.isPlaying == true) {
            mediaController?.pause()
        } else {
            if (mediaController?.playbackState != Player.STATE_IDLE &&
                            mediaController?.playbackState != Player.STATE_ENDED
            ) {
                mediaController?.play()
            } else {
                Log.w(TAG, "Play called but player state is ${_playbackState.value}")
                mediaController?.prepare()
                mediaController?.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = _totalDurationMs.value
        if (duration <= 0) {
            mediaController?.seekTo(positionMs)
            return
        }

        val seekPosition = positionMs.coerceIn(0, duration)

        val isSeekingToEnd = seekPosition >= duration - 500

        if (isSeekingToEnd) {
            Log.d(TAG, "Seeking to end of track (${seekPosition}ms of ${duration}ms)")

            _currentPositionMs.value = duration

            mediaController?.pause()

            mediaController?.seekTo(duration)

            handlePlaybackEnd()
        } else {
            mediaController?.seekTo(seekPosition)
            _currentPositionMs.value = seekPosition
        }
    }

    fun getRecentlyPlayed(): List<SongEntity> {
        return _recentlyPlayed.value
    }

    // Play a single song (not from queue)
    fun playSong(song: SongEntity, songsList: List<SongEntity> = emptyList()) {
        if (mediaController == null) {
            Log.w(TAG, "playSong called but mediaController is null. Attempting to initialize.")
            initializeMediaController()
            _error.value = "Player not ready. Please wait and try again."
            return
        }
        _error.value = null

        mainScope.launch {
            if (songsList.isNotEmpty()) {
                _playbackList.value = songsList

                val index = songsList.indexOfFirst { it.id == song.id }
                if (index >= 0) {
                    currentPlaybackIndex = index
                }
            }

            _isPlayingFromQueue.value = false

            playMediaItem(song)

            updateNavigationState()
        }
    }

    // Add song to manual queue
    fun addToQueue(song: SongEntity) {
        mainScope.launch {
            // Duplicate check
            if (_queue.value.any { it.songId == song.id }) {
                Log.d(TAG, "Song ${song.title} is already in queue")
                return@launch
            }

            val newItem = QueueItem(song.id, _queue.value.size)

            _queue.update { it + newItem }

            Log.d(TAG, "Added song ${song.title} to queue, queue size: ${_queue.value.size}")

            updateNavigationState()
        }
    }

    // Remove song from manual queue
    fun removeFromQueue(songId: Long) {
        mainScope.launch {
            if (!_queue.value.any { it.songId == songId }) {
                Log.d(TAG, "Song ID $songId not found in queue")
                return@launch
            }

            _queue.update { it.filterNot { item -> item.songId == songId } }
            Log.d(TAG, "Removed song ID $songId from queue, new size: ${_queue.value.size}")

            updateNavigationState()
        }
    }

    // Get songs in queue for display
    fun getQueueSongs(callback: (List<SongEntity>) -> Unit) {
        mainScope.launch {
            val songsList = mutableListOf<SongEntity>()

            for (queueItem in _queue.value) {
                val songResult = songRepository.getSongById(queueItem.songId)
                if (songResult is Resource.Success && songResult.data != null) {
                    songsList.add(songResult.data)
                }
            }

            callback(songsList)
        }
    }

    // Check if a song is in the queue
    fun isInQueue(songId: Long): Boolean {
        return _queue.value.any { it.songId == songId }
    }

    // Play a song from the queue at a specific index
    fun playQueueItem(index: Int) {
        if (index < 0 || index >= _queue.value.size) {
            Log.e(TAG, "Invalid queue index: $index (queue size: ${_queue.value.size})")
            return
        }

        val queueItem = _queue.value[index]
        Log.d(TAG, "Playing queue item at index $index (song ID: ${queueItem.songId})")

        mainScope.launch {
            val songResult = songRepository.getSongById(queueItem.songId)
            if (songResult is Resource.Success && songResult.data != null) {
                _isPlayingFromQueue.value = true
                _currentQueueIndex.value = index

                val song = songResult.data
                playMediaItem(song)

                Log.d(TAG, "Successfully loaded and playing song from queue: ${song.title}")

                updateNavigationState()
            } else {
                Log.e(TAG, "Failed to load song from queue index $index")
                _error.value = "Failed to load song from queue."
            }
        }
    }

    private fun updateCurrentPlaybackIndex(songId: Long) {
        val newIndex = _playbackList.value.indexOfFirst { it.id == songId }
        if (newIndex >= 0) {
            currentPlaybackIndex = newIndex
            Log.d(
                    TAG,
                    "Updated current playback index to $currentPlaybackIndex for song ID $songId"
            )
        }
    }

    fun toggleShuffleMode() {
        _shuffleMode.value =
                if (_shuffleMode.value == ShuffleMode.OFF) {
                    ShuffleMode.ON
                } else {
                    ShuffleMode.OFF
                }

        Log.d(TAG, "Shuffle mode toggled to: ${_shuffleMode.value}")
    }

    /** Cycles through repeat modes: NONE -> ALL -> ONE -> NONE */
    fun cycleRepeatMode() {
        val newMode =
                when (_repeatMode.value) {
                    RepeatMode.NONE -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.NONE
                }

        _repeatMode.value = newMode

        val controller = mediaController ?: return
        when (newMode) {
            RepeatMode.NONE -> {
                controller.repeatMode = Player.REPEAT_MODE_OFF
            }
            RepeatMode.ALL -> {
                controller.repeatMode = Player.REPEAT_MODE_ALL
            }
            RepeatMode.ONE -> {
                controller.repeatMode = Player.REPEAT_MODE_ONE
            }
        }

        Log.d(TAG, "Repeat mode changed to: $newMode")

        updateNavigationState()
    }

    private fun handlePlaybackEnd() {
        // Set flag to prevent multiple handlers from running simultaneously
        if (handlingPlaybackEnd) {
            Log.d(TAG, "Already handling playback end, ignoring additional call")
            return
        }
        handlingPlaybackEnd = true

        Log.d(TAG, "Handling playback end with repeat mode: ${_repeatMode.value}")

        if (_repeatMode.value == RepeatMode.ONE) {
            Log.d(TAG, "RepeatMode.ONE: Repeating current song")
            mediaController?.seekTo(0)
            mediaController?.play()
            handlingPlaybackEnd = false
            return
        }

        if (_queue.value.isNotEmpty()) {
            if (_isPlayingFromQueue.value) {
                val nextQueueIndex = _currentQueueIndex.value + 1

                if (nextQueueIndex < _queue.value.size) {
                    playQueueItem(nextQueueIndex)
                } else if (_repeatMode.value == RepeatMode.ALL && _queue.value.isNotEmpty()) {
                    playQueueItem(0)
                } else {
                    handlingPlaybackEnd = false
                }
            } else {
                playQueueItem(0)
            }
        } else {
            if (currentPlaybackIndex < _playbackList.value.size - 1) {
                currentPlaybackIndex++
                playMediaItem(_playbackList.value[currentPlaybackIndex])
            } else if (_repeatMode.value == RepeatMode.ALL && _playbackList.value.isNotEmpty()) {
                currentPlaybackIndex = 0
                if (_playbackList.value.isNotEmpty()) {
                    playMediaItem(_playbackList.value[0])
                }
            } else {
                handlingPlaybackEnd = false
            }
        }
    }

    /** Play the media item for the given song */
    private fun playMediaItem(song: SongEntity) {
        val mediaItem =
                MediaItem.Builder()
                        .setMediaId(song.id.toString())
                        .setUri(song.filePathUri)
                        .setMediaMetadata(
                                MediaMetadata.Builder()
                                        .setTitle(song.title)
                                        .setArtist(song.artist)
                                        .setArtworkUri(
                                                song.coverArtUri?.let { android.net.Uri.parse(it) }
                                        )
                                        .build()
                        )
                        .build()

        mediaController?.setMediaItem(mediaItem)
        mediaController?.prepare()
        mediaController?.play()
        Log.d(TAG, "Set media item and playing: ${song.title}")
    }

    fun skipToNext() {
        if (!_canSkipNext.value) {
            Log.d(TAG, "Skip to next: Operation not allowed")
            return
        }

        // Case from queue
        if (_isPlayingFromQueue.value) {
            val nextIndex = _currentQueueIndex.value + 1

            if (nextIndex < _queue.value.size) {
                playQueueItem(nextIndex)
            } else if (_repeatMode.value == RepeatMode.ALL && _queue.value.isNotEmpty()) {
                playQueueItem(0)
            } else {
                Log.d(TAG, "Skip to next: At end of queue and no repeat")
            }
        } else {
            // case from regular playlist
            if (_queue.value.isNotEmpty()) {
                playQueueItem(0)
                return
            }

            if (_playbackList.value.isEmpty()) {
                Log.d(TAG, "Skip to next: Playback list is empty")
                return
            }

            var nextIndex = currentPlaybackIndex + 1

            if (nextIndex >= _playbackList.value.size) {
                if (_repeatMode.value == RepeatMode.ALL) {
                    nextIndex = 0
                    Log.d(TAG, "Skip to next: Wrapping to beginning (index 0) in REPEAT ALL mode")
                } else {
                    Log.d(TAG, "Skip to next: Reached end of playback list - not wrapping")
                    return
                }
            }

            // Play the next song
            val nextSong = _playbackList.value[nextIndex]
            currentPlaybackIndex = nextIndex
            playMediaItem(nextSong)
        }
    }

    fun skipToPrevious() {
        // Tolerance for skipping to previous song
        if (_currentPositionMs.value > 3000) {
            Log.d(TAG, "Skip to previous: More than 3 seconds in, restarting current song")
            mediaController?.seekTo(0)
            return
        }

        if (!_canSkipPrevious.value) {
            Log.d(TAG, "Skip to previous: Operation not allowed")
            mediaController?.seekTo(0)
            return
        }

        // Case from queue
        if (_isPlayingFromQueue.value) {
            val prevIndex = _currentQueueIndex.value - 1

            if (prevIndex >= 0) {
                playQueueItem(prevIndex)
            } else if (_repeatMode.value == RepeatMode.ALL && _queue.value.isNotEmpty()) {
                playQueueItem(_queue.value.size - 1)
            } else {
                mediaController?.seekTo(0)
            }
        } else {
            // Case regular playback list
            if (_playbackList.value.isEmpty()) {
                Log.d(TAG, "Skip to previous: Playback list is empty")
                return
            }

            var prevIndex = currentPlaybackIndex - 1

            if (prevIndex < 0) {
                if (_repeatMode.value == RepeatMode.ALL) {
                    // Wrap to end in repeat ALL mode
                    prevIndex = _playbackList.value.size - 1
                    Log.d(
                            TAG,
                            "Skip to previous: Wrapping to end (index ${prevIndex}) in REPEAT ALL mode"
                    )
                } else {
                    // In NONE mode, restart current song
                    Log.d(
                            TAG,
                            "Skip to previous: At beginning and not in repeat ALL mode - restarting current song"
                    )
                    mediaController?.seekTo(0)
                    return
                }
            }

            val prevSong = _playbackList.value[prevIndex]
            currentPlaybackIndex = prevIndex
            playMediaItem(prevSong)
        }
    }

    /** Stop playback and clear the current song */
    fun stopPlayback() {
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _currentSong.value = null
        _isPlaying.value = false
        _playbackState.value = Player.STATE_IDLE
        _currentPositionMs.value = 0L
        _totalDurationMs.value = 0L
        _currentQueueIndex.value = -1
        _isPlayingFromQueue.value = false
        _canSkipNext.value = false
        _canSkipPrevious.value = false
    }

    /** Releases the media controller */
    fun releaseController() {
        stopPositionUpdates()
        mediaController?.removeListener(playerListener)
        mediaControllerFuture?.let { future -> MediaController.releaseFuture(future) }
        mediaController = null
        mediaControllerFuture = null
        resetState()
    }
}
