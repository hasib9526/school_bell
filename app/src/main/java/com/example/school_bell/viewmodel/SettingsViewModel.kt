package com.example.school_bell.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.school_bell.data.prefs.AppPreferences
import com.example.school_bell.data.repository.DeviceRepository
import com.example.school_bell.kiosk.KioskManager
import com.example.school_bell.worker.SoundSyncWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String = AppPreferences.DEFAULT_SERVER_URL,
    val schoolName: String = AppPreferences.DEFAULT_SCHOOL_NAME,
    val deviceId: String = "",
    val isKioskEnabled: Boolean = false,
    val azanEnabled: Boolean = true,
    val syncIntervalHours: Int = AppPreferences.DEFAULT_SYNC_INTERVAL,
    val isAdminActive: Boolean = false,
    val isDeviceOwner: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application)
    private val deviceRepository = DeviceRepository(application, preferences)
    val kioskManager = KioskManager(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Editable fields before saving
    private val _editServerUrl = MutableStateFlow(AppPreferences.DEFAULT_SERVER_URL)
    val editServerUrl: StateFlow<String> = _editServerUrl.asStateFlow()

    private val _editSchoolName = MutableStateFlow(AppPreferences.DEFAULT_SCHOOL_NAME)
    val editSchoolName: StateFlow<String> = _editSchoolName.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            preferences.serverUrl.collect { url ->
                _uiState.update { it.copy(serverUrl = url) }
                _editServerUrl.value = url
            }
        }
        viewModelScope.launch {
            preferences.schoolName.collect { name ->
                _uiState.update { it.copy(schoolName = name) }
                _editSchoolName.value = name
            }
        }
        viewModelScope.launch {
            preferences.isKioskEnabled.collect { enabled ->
                _uiState.update { it.copy(isKioskEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            preferences.azanEnabled.collect { enabled ->
                _uiState.update { it.copy(azanEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAdminActive = kioskManager.isAdminActive,
                    isDeviceOwner = kioskManager.isDeviceOwner
                )
            }
        }
        viewModelScope.launch {
            preferences.syncIntervalHours.collect { hours ->
                _uiState.update { it.copy(syncIntervalHours = hours) }
            }
        }
        viewModelScope.launch {
            val id = preferences.ensureDeviceId()
            _uiState.update { it.copy(deviceId = id) }
        }
    }

    fun updateEditServerUrl(url: String) {
        _editServerUrl.value = url
    }

    fun updateEditSchoolName(name: String) {
        _editSchoolName.value = name
    }

    fun saveSettings() {
        viewModelScope.launch {
            preferences.saveServerUrl(_editServerUrl.value)
            preferences.saveSchoolName(_editSchoolName.value)
            _uiState.update { it.copy(isSaved = true, error = null) }
        }
    }

    fun clearSaved() {
        _uiState.update { it.copy(isSaved = false) }
    }

    fun toggleAzan(enabled: Boolean) {
        viewModelScope.launch {
            preferences.saveAzanEnabled(enabled)
            _uiState.update { it.copy(azanEnabled = enabled) }
        }
    }

    fun setSyncInterval(hours: Int) {
        viewModelScope.launch {
            preferences.saveSyncIntervalHours(hours)
            _uiState.update { it.copy(syncIntervalHours = hours) }
            SoundSyncWorker.cancel(getApplication())
            SoundSyncWorker.schedule(getApplication(), hours.toLong())
        }
    }

    fun toggleKiosk(enabled: Boolean, activity: Activity) {
        viewModelScope.launch {
            preferences.saveKioskEnabled(enabled)
            _uiState.update { it.copy(isKioskEnabled = enabled) }
            if (enabled) {
                if (!kioskManager.isAdminActive) {
                    kioskManager.requestAdminPermission(activity, REQUEST_ADMIN_CODE)
                } else {
                    kioskManager.startKioskMode(activity)
                }
            } else {
                kioskManager.stopKioskMode(activity)
            }
        }
    }

    fun requestDeviceAdmin(activity: Activity) {
        kioskManager.requestAdminPermission(activity, REQUEST_ADMIN_CODE)
    }

    fun refreshAdminState() {
        _uiState.update {
            it.copy(
                isAdminActive = kioskManager.isAdminActive,
                isDeviceOwner = kioskManager.isDeviceOwner
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            deviceRepository.logout()
        }
    }

    fun triggerSoundSync() {
        SoundSyncWorker.scheduleImmediate(getApplication())
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        const val REQUEST_ADMIN_CODE = 1001
    }
}
