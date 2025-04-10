package com.vibecoder.purrytify.presentation.features.library

import android.app.Application
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.data.repository.SongRepository
import com.vibecoder.purrytify.playback.PlaybackStateManager
import com.vibecoder.purrytify.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SongBottomSheetState(
        val id: Long = 0,
        val title: String = "",
        val artist: String = "",
        val selectedAudioUri: Uri? = null,
        val selectedCoverUri: Uri? = null,
        val audioFileName: String? = null,
        val coverFileName: String? = null,
        val durationMs: Long? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val isEditMode: Boolean = false,
        val originalFilePathUri: String? = null,
        val originalCoverArtUri: String? = null
)

sealed class SheetEvent {
    object SaveSuccess : SheetEvent()
    object Dismiss : SheetEvent()
}

@HiltViewModel
class SongBottomSheetViewModel
@Inject
constructor(
        application: Application,
        private val songRepository: SongRepository,
        private val playbackStateManager: PlaybackStateManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SongBottomSheetState())
    val state: StateFlow<SongBottomSheetState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SheetEvent>()
    val eventFlow: SharedFlow<SheetEvent> = _eventFlow.asSharedFlow()

    fun updateTitle(title: String) {
        _state.update { it.copy(title = title, error = null) }
    }

    fun updateArtist(artist: String) {
        _state.update { it.copy(artist = artist, error = null) }
    }

    fun setAudioFileUri(uri: Uri?) {
        if (uri == null) return

        try {
            val contentResolver = getApplication<Application>().contentResolver
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            Log.d(
                    "SongBottomSheetVM",
                    "Successfully took persistable URI permission for audio: $uri"
            )
        } catch (e: SecurityException) {
            Log.e("SongBottomSheetVM", "Failed to take persistable URI permission", e)
            _state.update { it.copy(error = "Permission issue with audio file. Please try again.") }
            return
        }

        _state.update { it.copy(selectedAudioUri = uri, isLoading = true, error = null) }
        extractMetadata(uri)
    }

    fun setCoverImageUri(uri: Uri?) {
        if (uri == null) return

        try {
            val contentResolver = getApplication<Application>().contentResolver
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            Log.d(
                    "SongBottomSheetVM",
                    "Successfully took persistable URI permission for cover: $uri"
            )
        } catch (e: SecurityException) {
            Log.e("SongBottomSheetVM", "Failed to take persistable URI permission for cover", e)
        }

        _state.update { it.copy(selectedCoverUri = uri, coverFileName = getFileName(uri)) }
    }

    private fun extractMetadata(uri: Uri) {
        viewModelScope.launch {
            val title: String?
            val artist: String?
            val duration: Long?
            val fileName = getFileName(uri)

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(getApplication<Application>(), uri)

                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                artist =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                                ?: retriever.extractMetadata(
                                        MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST
                                )
                val durationStr =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                duration = durationStr?.toLongOrNull()

                retriever.release()

                // Only update fields if in new song mode, or if they're empty
                _state.update { currentState ->
                    currentState.copy(
                            title =
                                    if (!currentState.isEditMode &&
                                                    !title.isNullOrBlank() &&
                                                    currentState.title.isBlank()
                                    )
                                            title
                                    else currentState.title,
                            artist =
                                    if (!currentState.isEditMode &&
                                                    !artist.isNullOrBlank() &&
                                                    currentState.artist.isBlank()
                                    )
                                            artist
                                    else currentState.artist,
                            durationMs = duration,
                            audioFileName = fileName,
                            isLoading = false,
                            error = null
                    )
                }
            } catch (e: Exception) {
                Log.e("SongBottomSheetVM", "Error extracting metadata", e)
                _state.update { currentState ->
                    currentState.copy(
                            isLoading = false,
                            audioFileName = fileName,
                            error = "Could not read metadata from audio file."
                    )
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        return try {
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use {
                    cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex =
                            cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) cursor.getString(nameIndex) else "Selected File"
                } else {
                    uri.lastPathSegment ?: "Selected File"
                }
            }
        } catch (e: Exception) {
            Log.w("SongBottomSheetVM", "Error getting file name", e)
            uri.lastPathSegment ?: "Selected File"
        }
    }

    fun saveSong() {
        val currentState = _state.value

        // Validation
        if (!currentState.isEditMode && currentState.selectedAudioUri == null) {
            _state.update { it.copy(error = "Please select an audio file.") }
            return
        }
        if (currentState.title.isBlank()) {
            _state.update { it.copy(error = "Please enter a title.") }
            return
        }
        if (currentState.artist.isBlank()) {
            _state.update { it.copy(error = "Please enter an artist.") }
            return
        }
        if (!currentState.isEditMode &&
                        (currentState.durationMs == null || currentState.durationMs <= 0)
        ) {
            _state.update { it.copy(error = "Invalid or missing song duration.") }
            if (currentState.durationMs == null && currentState.selectedAudioUri != null) {
                extractMetadata(currentState.selectedAudioUri)
            }
            return
        }

        // --- Prepare Song Data ---
        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            if (currentState.isEditMode) {
                // Update existing song
                val updatedSong =
                        SongEntity(
                                id = currentState.id,
                                title = currentState.title.trim(),
                                artist = currentState.artist.trim(),
                                filePathUri = currentState.selectedAudioUri?.toString()
                                                ?: currentState.originalFilePathUri ?: "",
                                coverArtUri = currentState.selectedCoverUri?.toString()
                                                ?: currentState.originalCoverArtUri ?: "",
                                duration = currentState.durationMs ?: 0L,
                                isLiked = false
                        )

                when (val result = songRepository.updateSong(updatedSong)) {
                    is Resource.Success -> {
                        _state.update { it.copy(isLoading = false) }

                        playbackStateManager.refreshRecentlyPlayed(delayMs = 300)

                        _eventFlow.emit(SheetEvent.SaveSuccess)
                    }
                    is Resource.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                    }
                    is Resource.Loading -> {}
                }
            } else {
                // Add new song
                val newSong =
                        SongEntity(
                                title = currentState.title.trim(),
                                artist = currentState.artist.trim(),
                                filePathUri = currentState.selectedAudioUri.toString(),
                                coverArtUri = currentState.selectedCoverUri?.toString() ?: "",
                                duration = currentState.durationMs ?: 0L,
                                isLiked = false
                        )

                when (val result = songRepository.addSong(newSong)) {
                    is Resource.Success -> {
                        _state.update { it.copy(isLoading = false) }
                        _eventFlow.emit(SheetEvent.SaveSuccess)
                    }
                    is Resource.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun setEditMode(song: SongEntity) {
        try {
            val audioUri = if (song.filePathUri.isNotEmpty()) Uri.parse(song.filePathUri) else null
            val coverUri =
                    if (!song.coverArtUri.isNullOrEmpty()) Uri.parse(song.coverArtUri) else null

            _state.update {
                it.copy(
                        id = song.id,
                        title = song.title,
                        artist = song.artist,
                        durationMs = song.duration,
                        originalFilePathUri = song.filePathUri,
                        originalCoverArtUri = song.coverArtUri,
                        audioFileName = audioUri?.lastPathSegment,
                        coverFileName = coverUri?.lastPathSegment,
                        isEditMode = true
                )
            }

            Log.d("SongBottomSheetVM", "Edit mode set for song: ${song.title}")
        } catch (e: Exception) {
            Log.e("SongBottomSheetVM", "Error setting edit mode", e)
            _state.update { it.copy(error = "Error loading song data: ${e.localizedMessage}") }
        }
    }

    fun dismiss() {
        viewModelScope.launch {
            _state.value = SongBottomSheetState()
            _eventFlow.emit(SheetEvent.Dismiss)
        }
    }
}
