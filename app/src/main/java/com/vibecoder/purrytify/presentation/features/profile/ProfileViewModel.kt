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

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileScreenState())
    val state: StateFlow<ProfileScreenState> = _state.asStateFlow()

    val isCurrentSongFavorite: StateFlow<Boolean> = _state.map { it.currentSong?.isLiked ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.getProfile()) {
                is Resource.Success -> {
                    result.data?.let { userDto ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                userProfile = UserProfile(
                                    id = userDto.id.toString(),
                                    username = userDto.username,
                                    location = userDto.location,
                                    profileImageUrl = userDto.profilePhoto
                                ),
                                userStats = UserStats(
                                    songCount = 0,
                                    likedCount = 0,
                                    listenedCount = 0
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
            try {
                val likedCount = 33
                val songCount = 66
                val listenedCount = 99
                // TODO : Take real data

                _state.update {
                    it.copy(
                        userStats = UserStats(
                            songCount = songCount,
                            likedCount = likedCount,
                            listenedCount = listenedCount
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading user stats", e)
            }
        }
    }
    fun navigateToEditProfile() {
        Log.d("ProfileViewModel", "Navigate to edit profile")
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