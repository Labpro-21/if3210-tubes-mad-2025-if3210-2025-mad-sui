package com.vibecoder.purrytify.presentation.features.network

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoder.purrytify.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkStateViewModel @Inject constructor(
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        observeNetworkStatus()
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus().collect { connected ->
                _isConnected.value = connected
            }
        }
    }

    fun refreshNetworkStatus() {
        _isConnected.value = networkMonitor.isConnected()
    }
}
