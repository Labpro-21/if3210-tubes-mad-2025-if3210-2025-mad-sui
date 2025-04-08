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
import kotlin.random.Random
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

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    private val _originalQueue = MutableStateFlow<List<QueueItem>>(emptyList())
    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    private val _shuffleMode = MutableStateFlow(ShuffleMode.OFF)

    // Recently played songs tracking
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

    // Current song index in queue
    private var currentQueueIndex = -1
    private var positionUpdateJob: Job? = null

    init {
        initializeMediaController()
        loadRecentlyPlayed()
    }

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
                            }
                            if (resource.data == null)
                                    Log.w(
                                            TAG,
                                            "Song with ID $currentSongId not found in repository."
                                    )
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

                        val isAtEnd = _currentPositionMs.value >= _totalDurationMs.value * 0.98
                        // precision issues
                        if (isAtEnd && _playbackState.value != Player.STATE_ENDED) {
                            Log.d(
                                    TAG,
                                    "Detected end of track through position (${_currentPositionMs.value}/${_totalDurationMs.value})"
                            )
                            handlePlaybackEnd()
                        }
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

                            handlePlaybackEnd()
                        }
                        Player.STATE_READY -> {
                            _totalDurationMs.value =
                                    mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                            if (mediaController?.isPlaying == true) startPositionUpdates()
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
                        updateCurrentQueueIndex(songId)
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
                createNewQueue(songsList, song.id)
            } else {
                playMediaItem(song)
                if (_queue.value.isEmpty()) {
                    _queue.value = listOf(QueueItem(song.id, 0))
                    _originalQueue.value = _queue.value
                    currentQueueIndex = 0
                }
            }
        }
    }

    fun addToQueue(song: SongEntity) {
        mainScope.launch {
            if (_queue.value.isEmpty()) {
                playSong(song)
                return@launch
            }

            val newItem = QueueItem(song.id, _originalQueue.value.size)
            _originalQueue.update { it + newItem }

            if (_shuffleMode.value == ShuffleMode.ON) {
                val currentIndex = currentQueueIndex
                if (currentIndex >= 0 && currentIndex < _queue.value.size - 1) {
                    val insertIndex = Random.nextInt(currentIndex + 1, _queue.value.size + 1)
                    val newQueue = _queue.value.toMutableList()
                    newQueue.add(insertIndex, newItem)
                    _queue.value = newQueue
                } else {
                    _queue.update { it + newItem }
                }
            } else {
                _queue.update { it + newItem }
            }

            Log.d(TAG, "Added song ${song.title} to queue, position: ${_queue.value.size}")
        }
    }

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

    private fun createNewQueue(songs: List<SongEntity>, startSongId: Long) {
        if (songs.isEmpty()) return

        val queueItems = songs.mapIndexed { index, song -> QueueItem(song.id, index) }

        _originalQueue.value = queueItems

        if (_shuffleMode.value == ShuffleMode.ON) {
            applyShuffleToQueue(startSongId)
        } else {
            _queue.value = queueItems
        }

        val startIndex =
                _queue.value.indexOfFirst { it.songId == startSongId }.takeIf { it >= 0 } ?: 0
        currentQueueIndex = startIndex

        mainScope.launch {
            val songToPlay = songRepository.getSongById(startSongId)
            if (songToPlay is Resource.Success && songToPlay.data != null) {
                playMediaItem(songToPlay.data)
            } else {
                _error.value = "Failed to load the selected song."
            }
        }
    }

    private fun updateCurrentQueueIndex(songId: Long) {
        currentQueueIndex = _queue.value.indexOfFirst { it.songId == songId }
    }

    private fun applyShuffleToQueue(keepSongId: Long = -1L) {
        val currentItems = _originalQueue.value
        if (currentItems.isEmpty()) return

        val songToKeep =
                if (keepSongId > 0) {
                    currentItems.find { it.songId == keepSongId }
                } else null

        val itemsToShuffle = currentItems.toMutableList()
        if (songToKeep != null) {
            itemsToShuffle.removeIf { it.songId == keepSongId }
        }

        // Fisher-Yates shuffle algorithm
        val shuffledItems = itemsToShuffle.toMutableList()
        for (i in shuffledItems.size - 1 downTo 1) {
            val j = Random.nextInt(i + 1)
            val temp = shuffledItems[j]
            shuffledItems[j] = shuffledItems[i]
            shuffledItems[i] = temp
        }

        val newQueue =
                if (songToKeep != null) {
                    listOf(songToKeep) + shuffledItems
                } else {
                    shuffledItems
                }

        _queue.value = newQueue
        Log.d(TAG, "Queue shuffled, new size: ${_queue.value.size}")
    }

    fun toggleShuffleMode() {
        val currentSongId = _currentSong.value?.id ?: -1L

        _shuffleMode.value =
                if (_shuffleMode.value == ShuffleMode.OFF) {
                    applyShuffleToQueue(currentSongId)
                    ShuffleMode.ON
                } else {
                    _queue.value = _originalQueue.value

                    if (currentSongId > 0) {
                        currentQueueIndex = _queue.value.indexOfFirst { it.songId == currentSongId }
                    }

                    ShuffleMode.OFF
                }

        Log.d(
                TAG,
                "Shuffle mode toggled to: ${_shuffleMode.value}, queue size: ${_queue.value.size}"
        )
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
    }

    private fun handlePlaybackEnd() {
        Log.d(
                TAG,
                "Handling playback end with repeat mode: ${_repeatMode.value}, currentQueueIndex: $currentQueueIndex, queue size: ${_queue.value.size}"
        )

        if (_queue.value.isEmpty()) {
            Log.d(TAG, "Queue is empty, nothing to do")
            return
        }

        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                Log.d(TAG, "RepeatMode.ONE: Repeating current song")
                mediaController?.seekTo(0)
                mediaController?.play()
            }
            RepeatMode.ALL -> {
                if (currentQueueIndex >= _queue.value.size - 1) {
                    Log.d(TAG, "RepeatMode.ALL: At end of queue, restarting from first song")
                    currentQueueIndex = -1
                    if (_queue.value.isNotEmpty()) {
                        playSpecificQueueItem(0)
                    }
                } else {
                    Log.d(TAG, "RepeatMode.ALL: Moving to next song in queue")
                    skipToNext()
                }
            }
            RepeatMode.NONE -> {
                if (currentQueueIndex < _queue.value.size - 1) {
                    Log.d(TAG, "RepeatMode.NONE: Not at end, playing next song")
                    skipToNext()
                } else {
                    Log.d(TAG, "RepeatMode.NONE: At end of queue, stopping playback")
                }
            }
        }
    }

    private fun playSpecificQueueItem(index: Int) {
        if (index < 0 || index >= _queue.value.size) {
            Log.e(TAG, "Invalid queue index: $index (queue size: ${_queue.value.size})")
            return
        }

        val queueItem = _queue.value[index]
        Log.d(TAG, "Playing specific queue item at index $index (song ID: ${queueItem.songId})")

        mainScope.launch(Dispatchers.Main) {
            try {
                val songResult = songRepository.getSongById(queueItem.songId)
                if (songResult is Resource.Success && songResult.data != null) {
                    currentQueueIndex = index

                    val song = songResult.data
                    playMediaItem(song)
                    Log.d(TAG, "Successfully loaded and playing song: ${song.title}")
                } else {
                    Log.e(TAG, "Failed to load song at queue index $index")
                    _error.value = "Failed to load song from queue."

                    if (index < _queue.value.size - 1) {
                        playSpecificQueueItem(index + 1)
                    } else if (_repeatMode.value == RepeatMode.ALL && _queue.value.size > 1) {
                        playSpecificQueueItem(0)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing queue item $index", e)
                _error.value = "Error playing song: ${e.message}"
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

    /** Skips to the next song in the queue, respecting repeat mode */
    fun skipToNext() {
        if (_queue.value.isEmpty()) {
            Log.d(TAG, "Skip to next: Queue is empty")
            return
        }

        // Calculate next index
        var nextIndex = currentQueueIndex + 1

        if (nextIndex >= _queue.value.size) {
            if (_repeatMode.value == RepeatMode.ALL) {
                nextIndex = 0
                Log.d(TAG, "Skip to next: Wrapping to beginning (index 0) in REPEAT ALL mode")
            } else {
                Log.d(
                        TAG,
                        "Skip to next: Reached end of queue in ${_repeatMode.value} mode - not wrapping"
                )
                return
            }
        }

        playSpecificQueueItem(nextIndex)
    }

    fun skipToPrevious() {
        if (_queue.value.isEmpty()) {
            Log.d(TAG, "Skip to previous: Queue is empty")
            return
        }

        if (_currentPositionMs.value > 3000) {
            Log.d(TAG, "Skip to previous: More than 3 seconds in, restarting current song")
            mediaController?.seekTo(0)
            return
        }

        var prevIndex = currentQueueIndex - 1

        if (prevIndex < 0) {
            if (_repeatMode.value == RepeatMode.ALL) {
                // Wrap to end in repeat ALL mode
                prevIndex = _queue.value.size - 1
                Log.d(
                        TAG,
                        "Skip to previous: Wrapping to end (index ${prevIndex}) in REPEAT ALL mode"
                )
            } else {
                // In NONE or ONE mode, restart current song
                Log.d(
                        TAG,
                        "Skip to previous: At beginning with ${_repeatMode.value} mode - restarting current song"
                )
                mediaController?.seekTo(0)
                return
            }
        }

        // Play the previous item
        playSpecificQueueItem(prevIndex)
    }

    /** Skips to the previous song in the queue, or replays current song */
    fun releaseController() {
        Log.d(TAG, "Releasing MediaController.")
        stopPositionUpdates()
        mediaController?.removeListener(playerListener)
        mediaControllerFuture?.let { future -> MediaController.releaseFuture(future) }
        mediaController = null
        mediaControllerFuture = null
        resetState()
    }
}
