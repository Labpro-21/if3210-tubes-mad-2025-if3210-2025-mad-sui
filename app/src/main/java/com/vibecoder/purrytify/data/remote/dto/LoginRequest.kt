package com.vibecoder.purrytify.data.remote.dto

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class UserDto(
    val id: Int,
    val username: String,
    val email: String,
    val profilePhoto: String,
    val location: String,
    val createdAt: String,
    val updatedAt: String
)
