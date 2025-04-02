package com.vibecoder.purrytify.data.remote

import com.vibecoder.purrytify.data.remote.dto.*
import retrofit2.http.*

interface PurrytifyApi {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): LoginResponse

    @GET("api/profile")
    suspend fun getProfile(): UserDto

    @GET("api/verify-token")
    suspend fun verifyToken()
}
