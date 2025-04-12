package com.vibecoder.purrytify.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.vibecoder.purrytify.data.remote.PurrytifyApi
import com.vibecoder.purrytify.data.remote.dto.LoginRequest
import com.vibecoder.purrytify.data.remote.dto.RefreshTokenRequest
import com.vibecoder.purrytify.data.remote.dto.UserDto
import com.vibecoder.purrytify.playback.PlaybackStateManager
import com.vibecoder.purrytify.tokenrefresh.TokenRefreshService
import com.vibecoder.purrytify.util.Resource
import com.vibecoder.purrytify.util.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

@Singleton
class AuthRepository
@Inject
constructor(
        @ApplicationContext private val context: Context,
        private val api: PurrytifyApi,
        private val tokenManager: TokenManager,
        private val playbackStateManagerProvider: Provider<PlaybackStateManager>
) {
    private var currentUser: UserDto? = null

    suspend fun login(email: String, password: String): Resource<Unit> {
        return try {
            val response = api.login(LoginRequest(email, password))
            tokenManager.saveToken(response.accessToken)
            tokenManager.saveRefreshToken(response.refreshToken)
            startTokenRefreshService()
            val profileResult = getProfileInternal()
            if (profileResult is Resource.Success) {
                currentUser = profileResult.data
            } else {
                return Resource.Error("Failed to fetch user profile after login.")
            }
            Resource.Success(Unit)
        } catch (e: HttpException) {
            Resource.Error(message = "Invalid credentials or server error.")
        } catch (e: IOException) {
            Resource.Error(message = "Couldn't reach server. Check connection.")
        } catch (e: Exception) {
            Resource.Error(message = "An unexpected error occurred during login.")
        }
    }

    suspend fun logout() {
        try {
            val playbackStateManager = playbackStateManagerProvider.get()

            playbackStateManager.stopPlayback()
            playbackStateManager.clearRecentlyPlayed()

            currentUser = null

            // Stop token refresh service
            stopTokenRefreshService()

            // Clear tokens
            tokenManager.deleteToken()
            tokenManager.deleteRefreshToken()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error during logout playback cleanup", e)
        }

        // Continue with logout process
        stopTokenRefreshService()
        tokenManager.deleteToken()
        tokenManager.deleteRefreshToken()
        currentUser = null

        Log.d("AuthRepository", "User logged out successfully")
    }

    suspend fun refreshToken(): Resource<Unit> {
        return try {
            val refreshToken = tokenManager.refreshToken.first()
            if (!refreshToken.isNullOrEmpty()) {
                val response = api.refreshToken(RefreshTokenRequest(refreshToken))
                tokenManager.saveToken(response.accessToken)
                getProfileInternal()
                Resource.Success(Unit)
            } else {
                logout()
                Resource.Error("No refresh token found. Logged out.")
            }
        } catch (e: HttpException) {
            logout()
            Resource.Error("Failed to refresh token: ${e.message()}. Logged out.")
        } catch (e: IOException) {
            Resource.Error("Network error during token refresh.")
        } catch (e: Exception) {
            logout()
            Resource.Error("Unexpected error during token refresh. Logged out.")
        }
    }

    suspend fun verifyToken(): Resource<Unit> {
        return try {
            api.verifyToken()
            Resource.Success(Unit)
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                Resource.Error("Token invalid or expired")
            } else {
                Resource.Error("Token verification failed: Server error (${e.code()}).")
            }
        } catch (e: IOException) {
            Resource.Error("Failed to verify token: Network error.")
        } catch (e: Exception) {
            Resource.Error("Failed to verify token: Unexpected error.")
        }
    }

    private fun startTokenRefreshService() {
        val intent = Intent(context, TokenRefreshService::class.java)
        context.startService(intent)
    }

    private fun stopTokenRefreshService() {
        val intent = Intent(context, TokenRefreshService::class.java)
        context.stopService(intent)
    }

    fun getCurrentUserEmail(): String? {
        return currentUser?.email
    }

    suspend fun getProfile(): Resource<UserDto> {
        currentUser?.let {
            return Resource.Success(it)
        }
        return getProfileInternal()
    }

    private suspend fun getProfileInternal(): Resource<UserDto> {
        return try {
            val responseDto = api.getProfile()
            currentUser = responseDto
            Resource.Success(responseDto)
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                Resource.Error("Authentication required. Please login again.")
            } else {
                Resource.Error("Failed to fetch profile: Server error (${e.code()}).")
            }
        } catch (e: IOException) {
            Resource.Error("Failed to fetch profile: Network error.")
        } catch (e: Exception) {
            Resource.Error("Failed to fetch profile: Unexpected error.")
        }
    }
}
