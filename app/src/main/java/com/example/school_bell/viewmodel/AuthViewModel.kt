package com.example.school_bell.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.school_bell.data.model.ApiResult
import com.example.school_bell.data.model.LoginResponse
import com.example.school_bell.data.prefs.AppPreferences
import com.example.school_bell.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val schoolName: String) : AuthState()
    data class Error(val message: String) : AuthState()
    object LoggedOut : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application)
    private val deviceRepository = DeviceRepository(application, preferences)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    private val _serverUrl = MutableStateFlow(AppPreferences.DEFAULT_SERVER_URL)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            _deviceId.value = preferences.ensureDeviceId()
            _serverUrl.value = preferences.serverUrl.first()
            val token = preferences.authToken.first()
            _isLoggedIn.value = !token.isNullOrEmpty()
        }
    }

    fun updateServerUrl(url: String) {
        _serverUrl.value = url
        viewModelScope.launch {
            preferences.saveServerUrl(url)
        }
    }

    fun login(username: String, password: String) {
        // TODO: Uncomment when API is ready
        // viewModelScope.launch {
        //     _authState.value = AuthState.Loading
        //     preferences.saveServerUrl(_serverUrl.value)
        //     val result = deviceRepository.login(username, password)
        //     _authState.value = if (result.isSuccess) {
        //         _isLoggedIn.value = true
        //         AuthState.Success(result.data?.schoolName ?: "")
        //     } else {
        //         AuthState.Error(result.error ?: "Login failed")
        //     }
        // }
    }

    fun logout() {
        viewModelScope.launch {
            deviceRepository.logout()
            _isLoggedIn.value = false
            _authState.value = AuthState.LoggedOut
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
