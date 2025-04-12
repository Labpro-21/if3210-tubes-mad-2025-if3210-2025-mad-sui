package com.vibecoder.purrytify.presentation.features.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoder.purrytify.data.repository.AuthRepository
import com.vibecoder.purrytify.playback.PlaybackStateManager
import com.vibecoder.purrytify.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginState(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val isSuccess: Boolean = false,
        val emailError: String? = null,
        val passwordError: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val authRepository: AuthRepository, private val playbackStateManager: PlaybackStateManager) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onEmailChange(email: String) {
        _state.value = _state.value.copy(email = email.trim(), error = null, emailError = null)
    }

    fun onPasswordChange(password: String) {
        _state.value = _state.value.copy(password = password, error = null, passwordError = null)
    }

    fun login() {
        //  OWASP M4
        if (!validateInputs()) {
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result =
                    authRepository.login(
                            email = _state.value.email,
                            password = _state.value.password
                    )

            _state.value =
                    when (result) {
                        is Resource.Success -> {
                            _state.value.copy(isLoading = false, isSuccess = true, error = null)

                        }
                        is Resource.Error -> {
                            _state.value.copy(
                                    isLoading = false,
                                    error =
                                            sanitizeErrorMessage(
                                                    result.message ?: "An unexpected error occurred"
                                            )
                            )
                        }
                        is Resource.Loading -> {
                            _state.value.copy(isLoading = true)
                        }
                    }
        }
    }

    // OWASP M4:
    private fun validateInputs(): Boolean {
        var isValid = true

        if (_state.value.email.isBlank()) {
            _state.value = _state.value.copy(emailError = "Email cannot be empty")
            isValid = false
        } else if (!isValidEmail(_state.value.email)) {
            _state.value = _state.value.copy(emailError = "Please enter a valid email address")
            isValid = false
        } else if (_state.value.email.length > MAX_EMAIL_LENGTH) {
            _state.value = _state.value.copy(emailError = "Email is too long")
            isValid = false
        }

        // Password validation
        if (_state.value.password.isBlank()) {
            _state.value = _state.value.copy(passwordError = "Password cannot be empty")
            isValid = false
        } else if (_state.value.password.length < MIN_PASSWORD_LENGTH) {
            _state.value =
                    _state.value.copy(
                            passwordError =
                                    "Password must be at least $MIN_PASSWORD_LENGTH characters"
                    )
            isValid = false
        } else if (_state.value.password.length > MAX_PASSWORD_LENGTH) {
            _state.value = _state.value.copy(passwordError = "Password is too long")
            isValid = false
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // OWASP M4: Sanitize error messages
    private fun sanitizeErrorMessage(error: String): String {
        return when {
            error.contains("401", ignoreCase = true) -> "Invalid email or password"
            error.contains("connection", ignoreCase = true) ->
                    "Network error. Please check your connection."
            error.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
            else -> "Authentication failed. Please try again."
        }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
        private const val MAX_PASSWORD_LENGTH = 100
        private const val MAX_EMAIL_LENGTH = 100
    }
}
