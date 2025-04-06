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
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.data.repository.SongRepository
import com.vibecoder.purrytify.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PlaybackStateManager"

@Singleton
class PlaybackStateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepository: SongRepository,
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


    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()
    val totalDurationMs: StateFlow<Long> = _totalDurationMs.asStateFlow()
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()


    private var positionUpdateJob: Job? = null

    init {
        initializeMediaController()
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

        mediaControllerFuture?.addListener({
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
        }, MoreExecutors.directExecutor())
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
            when(val resource = songRepository.getSongById(songId)) {
                is Resource.Success -> {
                    if (_currentSong.value?.id == songId) {
                        _currentSong.value = resource.data
                        if (resource.data == null) Log.w(TAG, "Current song $songId seems to have been deleted.")
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
                    when(val resource = songRepository.getSongById(currentSongId)) {
                        is Resource.Success -> {
                            _currentSong.value = resource.data
                            if (resource.data == null) Log.w(TAG, "Song with ID $currentSongId not found in repository.")
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "Error fetching song $currentSongId from repo: ${resource.message}")
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

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startPositionUpdates()
            } else {
                stopPositionUpdates()

                _currentPositionMs.value = mediaController?.currentPosition?.coerceAtLeast(0L) ?: 0L
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.value = playbackState
            when (playbackState) {
                Player.STATE_ENDED -> {
                    stopPositionUpdates()
                    _isPlaying.value = false
                    _currentPositionMs.value = _totalDurationMs.value
                }
                Player.STATE_READY -> {
                    _totalDurationMs.value = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                    if (mediaController?.isPlaying == true) startPositionUpdates()
                }
                Player.STATE_IDLE -> {
                    resetState()
                    stopPositionUpdates()
                }
                else -> {

                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {

            updateStateFromController(mediaController)
            _totalDurationMs.value = mediaController?.duration?.coerceAtLeast(0L) ?: 0L

        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Player error: ${error.message}", error)
            _error.value = "Playback error: ${error.message}"
            stopPositionUpdates()
            resetState()
        }


        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE) {
                _totalDurationMs.value = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
            }
        }
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        if (_isPlaying.value && mediaController != null) {
            positionUpdateJob = mainScope.launch {
                while (isActive && _isPlaying.value) {
                    val currentPos = mediaController?.currentPosition?.coerceAtLeast(0L) ?: 0L
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
                mediaController?.playbackState != Player.STATE_ENDED) {
                mediaController?.play()
            } else {
                Log.w(TAG, "Play called but player state is ${_playbackState.value}")
                mediaController?.prepare()
                mediaController?.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPositionMs.value = positionMs.coerceIn(0, _totalDurationMs.value)
    }

    fun playSong(song: SongEntity) {
        if (mediaController == null) {
            Log.w(TAG, "playSong called but mediaController is null. Attempting to initialize.")
            initializeMediaController()
            _error.value = "Player not ready. Please wait and try again."
            return
        }
        _error.value = null

        mainScope.launch {
            val mediaItem = MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.filePathUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(song.coverArtUri?.let { android.net.Uri.parse(it) })
                        .build()
                )
                .build()

            mediaController?.setMediaItem(mediaItem)
            mediaController?.prepare()
            mediaController?.play()
            Log.d(TAG, "Set media item and playing: ${song.title}")
        }
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }


    fun releaseController() {
        Log.d(TAG, "Releasing MediaController.")
        stopPositionUpdates()
        mediaController?.removeListener(playerListener)
        mediaControllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }
        mediaController = null
        mediaControllerFuture = null
        resetState()

    }
}