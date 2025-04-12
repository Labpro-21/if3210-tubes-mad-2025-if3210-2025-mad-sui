package com.vibecoder.purrytify.presentation.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.data.repository.SongRepository
import com.vibecoder.purrytify.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.vibecoder.purrytify.data.repository.AuthRepository
import com.vibecoder.purrytify.playback.PlaybackStateManager

sealed class NavigationEvent {
    object NavigateToLogin : NavigationEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val songRepository: SongRepository,
    private val playbackStateManager: PlaybackStateManager
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileScreenState())
    val state: StateFlow<ProfileScreenState> = _state.asStateFlow()
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()
    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.getProfile()) {
                is Resource.Success -> {
                    result.data.let { userDto ->
                        _state.update { currentState ->
                            currentState.copy(

                                userProfile = UserProfile(
                                    id = userDto.id.toString(),
                                    username = userDto.username,
                                    location = userDto.location,
                                    profileImageUrl = constructProfilePhotoUrl(userDto.profilePhoto),

                                )
                            )
                        }
                    }
                    loadUserStats()
                }
                is Resource.Error -> {
                    Log.e("ProfileViewModel", "Error loading user profile: ${result.message}")
                    loadPlaceholderProfile()
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }
    private fun constructProfilePhotoUrl(path: String?): String {
        val baseUrl = "http://34.101.226.132:3000/uploads/profile-picture"
        Log.d("ProfileViewModel", "Constructing profile photo URL with path: $path")
        Log.d("ProfileViewModel", "full path : $baseUrl/$path")
        return "$baseUrl/$path"
    }

    private fun loadPlaceholderProfile() {
        _state.update {
            it.copy(
                userProfile = UserProfile(
                    id = "placeholder_id",
                    username = "Placeholder Nama",
                    location = "Negeri ajaib",
                    profileImageUrl = ""
                ),
                userStats = UserStats(
                    songCount = 0,
                    likedCount = 0,
                    listenedCount = 0
                )
            )
        }
        loadPlaceholderStats()
    }

    private fun loadPlaceholderStats() {
        _state.update {
            it.copy(
                userStats = UserStats(
                    songCount = 33,
                    likedCount = 66,
                    listenedCount = 99
                )
            )
        }
    }

    private fun loadUserStats() {
        viewModelScope.launch {
            combine(
                songRepository.getSongCount(),
                songRepository.getLikedSongCount(),
                songRepository.getListenedSongCount()
            ) { songCount, likedCount, listenedCount ->
                UserStats(
                    songCount = songCount,
                    likedCount = likedCount,
                    listenedCount = listenedCount
                )
            }.catch { e ->
                Log.e("ProfileViewModel", "Error loading user stats for user ", e)
                _state.update { it.copy(userStats = UserStats(), error = "Failed to load stats.") }
            }.collect { stats ->
                _state.update { it.copy(userStats = stats, isLoading = false) }
            }
        }
    }

    fun navigateToEditProfile() {
        Log.d("ProfileViewModel", "Navigate to edit profile")
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            playbackStateManager.clearRecentlyPlayed()
            playbackStateManager.stopPlayback()
            playbackStateManager.releaseController()

            _navigationEvent.emit(NavigationEvent.NavigateToLogin)
        }
    }
}

data class ProfileScreenState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userProfile: UserProfile = UserProfile(),
    val userStats: UserStats = UserStats(),
    val currentSong: SongEntity? = null,
    val isPlaying: Boolean = false
)

data class UserProfile(
    val id: String = "",
    val username: String = "",
    val location: String = "",
    val profileImageUrl: String = ""
)

data class UserStats(
    val songCount: Int = 0,
    val likedCount: Int = 0,
    val listenedCount: Int = 0
)
