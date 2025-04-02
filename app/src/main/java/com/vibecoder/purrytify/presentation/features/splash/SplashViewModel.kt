package com.vibecoder.purrytify.presentation.features.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoder.purrytify.data.repository.AuthRepository
import com.vibecoder.purrytify.util.Resource
import com.vibecoder.purrytify.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isAuthenticated = MutableStateFlow<Boolean?>(null)

    val isAuthenticated: StateFlow<Boolean?> = _isAuthenticated.asStateFlow()


    init {
        checkAuthentication()
    }

    private fun checkAuthentication() {
        viewModelScope.launch {
            val token = tokenManager.token.firstOrNull()
            if (token.isNullOrEmpty()) {

                _isAuthenticated.value = false
            } else {
                when (val verifyResult = authRepository.verifyToken()) {
                    is Resource.Success -> {

                        _isAuthenticated.value = true
                    }
                    is Resource.Error -> {
                        handleTokenRefresh()
                    }
                    is Resource.Loading -> {

                    }
                }
            }
        }
    }

    private suspend fun handleTokenRefresh() {
        val refreshToken = tokenManager.refreshToken.firstOrNull()

        if (!refreshToken.isNullOrEmpty()) {

            when (val refreshResult = authRepository.refreshToken()) {
                is Resource.Success -> {
                    _isAuthenticated.value = true
                }
                is Resource.Error -> {
                    tokenManager.deleteToken()
                    tokenManager.deleteRefreshToken()
                    _isAuthenticated.value = false
                }
                is Resource.Loading -> {

                }
            }
        } else {

            tokenManager.deleteToken()
            tokenManager.deleteRefreshToken()
            _isAuthenticated.value = false
        }
    }
}