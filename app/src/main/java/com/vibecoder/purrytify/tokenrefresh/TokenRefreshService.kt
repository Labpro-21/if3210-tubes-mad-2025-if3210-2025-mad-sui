package com.vibecoder.purrytify.tokenrefresh
import android.app.Service

import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.vibecoder.purrytify.data.repository.AuthRepository
import com.vibecoder.purrytify.util.Resource
import com.vibecoder.purrytify.util.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "TokenRefreshService"
private val REFRESH_INTERVAL_MS = TimeUnit.MINUTES.toMillis(3)

@AndroidEntryPoint
class TokenRefreshService : Service() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var tokenManager: TokenManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var refreshJob: Job? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Background Service onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Background Service onStartCommand")
        startPeriodicCheck()
        Log.w(TAG, "Running as a standard background service. System may kill this service.")
        return START_STICKY
    }

    private fun startPeriodicCheck() {
        if (isRunning) {
            Log.d(TAG, "Periodic check already running.")
            return
        }
        isRunning = true
        Log.d(TAG, "Starting periodic token check (Background Mode).")
        refreshJob = serviceScope.launch {
            while (isRunning) {
                performTokenCheckAndRefresh()
                delay(REFRESH_INTERVAL_MS)
            }
            Log.d(TAG, "Periodic check loop stopped.")
        }
    }

    private suspend fun performTokenCheckAndRefresh() {
        Log.d(TAG, "Performing token check (Background)...")
        val currentToken = tokenManager.token.firstOrNull()
        if (currentToken.isNullOrEmpty()) {
            Log.w(TAG, "No token found. Stopping service.")
            stopSelf()
            return
        }

        when (val verificationResult = authRepository.verifyToken()) {
            is Resource.Success -> {
                Log.i(TAG, "Token is still valid (Background).")
            }
            is Resource.Error -> {
                Log.w(TAG, "Token verification failed (${verificationResult.message}) (Background). Attempting refresh.")
                attemptTokenRefresh()
            }
            is Resource.Loading -> {
                Log.d(TAG, "Token verification in loading state (unexpected).")
            }
        }
    }

    private suspend fun attemptTokenRefresh() {
        val currentRefreshToken = tokenManager.refreshToken.firstOrNull()
        if (currentRefreshToken.isNullOrEmpty()) {
            Log.e(TAG, "Token expired, no refresh token found (Background). Logging out and stopping service.")
            withContext(Dispatchers.Main) {
                authRepository.logout()
            }
            return
        }

        Log.d(TAG, "Attempting token refresh using refresh token (Background).")
        when (val refreshResult = authRepository.refreshToken()) {
            is Resource.Success -> {
                Log.i(TAG, "Token refresh successful (Background).")
            }
            is Resource.Error -> {
                Log.e(TAG, "Token refresh failed (Background): ${refreshResult.message}. Logging out.")
            }
            is Resource.Loading -> {
                Log.d(TAG, "Token refresh in loading state (Background).")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Background Service onDestroy")
        isRunning = false
        refreshJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {

        return null
    }

}