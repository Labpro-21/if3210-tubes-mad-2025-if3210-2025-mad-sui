package com.vibecoder.purrytify.data.repository

import com.vibecoder.purrytify.data.remote.PurrytifyApi
import com.vibecoder.purrytify.data.remote.dto.LoginRequest
import com.vibecoder.purrytify.data.remote.dto.RefreshTokenRequest
import com.vibecoder.purrytify.domain.model.User
import com.vibecoder.purrytify.util.Resource
import com.vibecoder.purrytify.util.TokenManager
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: PurrytifyApi,
    private val tokenManager: TokenManager
)  {

    suspend fun login(email: String, password: String): Resource<Unit> {
        return try {
            val response = api.login(LoginRequest(email, password))
            tokenManager.saveToken(response.token)
            tokenManager.saveRefreshToken(response.refreshToken)
            Resource.Success(Unit)
        } catch (e: HttpException) {
            Resource.Error(message = "Invalid credentials")
        } catch (e: IOException) {
            Resource.Error(message = "Couldn't reach server")
        }
    }

    suspend fun logout() {
        tokenManager.deleteToken()
        tokenManager.deleteRefreshToken()
    }

    suspend fun refreshToken(): Resource<Unit> {
        return try {
            val refreshToken = tokenManager.refreshToken.first()
            if (refreshToken != null) {
                val response = api.refreshToken(RefreshTokenRequest(refreshToken))
                tokenManager.saveToken(response.token)
                tokenManager.saveRefreshToken(response.refreshToken)
                Resource.Success(Unit)
            } else {
                Resource.Error("No refresh token found")
            }
        } catch (e: Exception) {
            Resource.Error("Failed to refresh token")
        }
    }

    suspend fun getProfile(): Resource<User> {
        return try {
            val response = api.getProfile()
            Resource.Success(
                User(
                    id = response.id,
                    username = response.username,
                    email = response.email,
                    profilePhoto = response.profilePhoto,
                    location = response.location,
                    createdAt = response.createdAt,
                    updatedAt = response.updatedAt
                )
            )
        } catch (e: Exception) {
            Resource.Error("Failed to fetch profile")
        }
    }

    suspend fun verifyToken(): Resource<Unit> {
        return try {
            api.verifyToken()
            Resource.Success(Unit)
        } catch (e: HttpException) {
            if (e.code() == 403) {
                Resource.Error("Token expired")
            } else {
                Resource.Error("Verification failed")
            }
        } catch (e: Exception) {
            Resource.Error("Failed to verify token")
        }
    }
}
