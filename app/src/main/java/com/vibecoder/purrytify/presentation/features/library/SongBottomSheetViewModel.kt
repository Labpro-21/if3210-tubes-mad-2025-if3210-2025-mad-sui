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
import com.vibecoder.purrytify.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject



data class SongBottomSheetState(
    val title: String = "",
    val artist: String = "",
    val selectedAudioUri: Uri? = null,
    val selectedCoverUri: Uri? = null,
    val audioFileName: String? = null,
    val coverFileName: String? = null,
    val durationMs: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null
    // TODO: Add fields for edit mode later
)


sealed class SheetEvent {
    object SaveSuccess : SheetEvent()
    object Dismiss : SheetEvent()
}

@HiltViewModel
class SongBottomSheetViewModel @Inject constructor(
    application: Application,
    private val songRepository: SongRepository
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
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            Log.e("SongBottomSheetVM", "Failed to take persistable URI permission", e)
            _state.update { it.copy(error = "Permission issue with audio file.") }
        }

        _state.update { it.copy(selectedAudioUri = uri, isLoading = true, error = null) }
        extractMetadata(uri)
    }

    fun setCoverImageUri(uri: Uri?) {
        if (uri == null) return
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            Log.e("SongBottomSheetVM", "Failed to take persistable URI permission for cover", e)
        }

        _state.update { it.copy(
            selectedCoverUri = uri,
            coverFileName = getFileName(uri)
        )}
    }

    private fun extractMetadata(uri: Uri) {
        viewModelScope.launch {
            var title: String? = null
            var artist: String? = null
            var duration: Long? = null
            val fileName = getFileName(uri)

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(getApplication<Application>(), uri)

                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                duration = durationStr?.toLongOrNull()

                retriever.release()

                _state.update { currentState ->
                    currentState.copy(
                        // Only update if metadata found and current field is empty
                        title = if (!title.isNullOrBlank() && currentState.title.isBlank()) title else currentState.title,
                        artist = if (!artist.isNullOrBlank() && currentState.artist.isBlank()) artist else currentState.artist,
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
            getApplication<Application>().contentResolver
                .query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
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
        if (currentState.selectedAudioUri == null) {
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
        if (currentState.durationMs == null || currentState.durationMs <= 0) {
            _state.update { it.copy(error = "Invalid or missing song duration.") }
            if(currentState.selectedAudioUri != null && currentState.durationMs == null) {
                extractMetadata(currentState.selectedAudioUri) // Re-extract metadata to get duration
            }
            return
        }

        // --- Prepare Song Data ---
        _state.update { it.copy(isLoading = true, error = null) }

        val newSong = SongEntity(
            title = currentState.title.trim(),
            artist = currentState.artist.trim(),
            filePathUri = currentState.selectedAudioUri.toString(),
            coverArtUri = currentState.selectedCoverUri?.toString() ?: "",
            duration = currentState.durationMs,
            isLiked = false
        )

        // --- Call Repository ---
        viewModelScope.launch {
            when (val result = songRepository.addSong(newSong)) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _eventFlow.emit(SheetEvent.SaveSuccess)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }

                }
                is Resource.Loading -> { /* Should not happen here unless repo has loading state */ }
            }
        }
    }

    fun dismiss() {
        viewModelScope.launch {
             _state.value = SongBottomSheetState()
            _eventFlow.emit(SheetEvent.Dismiss)
        }
    }
}