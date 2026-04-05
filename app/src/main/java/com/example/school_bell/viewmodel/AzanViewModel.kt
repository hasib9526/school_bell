package com.example.school_bell.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.school_bell.azan.AzanScheduler
import com.example.school_bell.data.db.AppDatabase
import com.example.school_bell.data.db.entities.AzanTime
import com.example.school_bell.data.prefs.AppPreferences
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AzanUiState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val prayerTimes: List<AzanTime> = emptyList(),
    val prayerTimesDisplay: Map<String, String> = emptyMap(),
    val calcMethod: String = "MUSLIM_WORLD_LEAGUE",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLocationPermissionGranted: Boolean = false
)

val CALCULATION_METHODS = listOf(
    "MUSLIM_WORLD_LEAGUE" to "Muslim World League",
    "ISNA" to "ISNA (North America)",
    "EGYPT" to "Egyptian",
    "KARACHI" to "University of Islamic Sciences, Karachi",
    "UMM_AL_QURA" to "Umm al-Qura (Mecca)",
    "GULF" to "Gulf / Dubai",
    "QATAR" to "Qatar",
    "KUWAIT" to "Kuwait",
    "SINGAPORE" to "Singapore",
    "TURKEY" to "Turkey",
    "TEHRAN" to "Institute of Geophysics, Tehran"
)

class AzanViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application)
    private val db = AppDatabase.getInstance(application)
    private val azanScheduler = AzanScheduler(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _uiState = MutableStateFlow(AzanUiState())
    val uiState: StateFlow<AzanUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
        observePrayerTimes()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            combine(
                preferences.latitude,
                preferences.longitude,
                preferences.calcMethod
            ) { lat, lon, method -> Triple(lat, lon, method) }
                .collect { (lat, lon, method) ->
                    _uiState.update { it.copy(latitude = lat, longitude = lon, calcMethod = method) }
                    if (lat != 0.0 || lon != 0.0) {
                        refreshDisplayTimes(lat, lon, method)
                    }
                }
        }
    }

    private fun observePrayerTimes() {
        viewModelScope.launch {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Calendar.getInstance().time)
            db.azanTimeDao().getAzanTimesForDate(today).collect { times ->
                _uiState.update { it.copy(prayerTimes = times) }
            }
        }
    }

    private fun refreshDisplayTimes(lat: Double, lon: Double, method: String) {
        val times = azanScheduler.getPrayerTimesDisplay(lat, lon, method)
        _uiState.update { it.copy(prayerTimesDisplay = times) }
    }

    fun recalculate() {
        val state = _uiState.value
        if (state.latitude == 0.0 && state.longitude == 0.0) {
            fetchLocation()
            return
        }
        scheduleAzan(state.latitude, state.longitude, state.calcMethod)
    }

    fun fetchLocation() {
        viewModelScope.launch {
            val hasPermission = ContextCompat.checkSelfPermission(
                getApplication(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                _uiState.update { it.copy(isLocationPermissionGranted = false) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, isLocationPermissionGranted = true) }
            try {
                val cts = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).await()

                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    preferences.saveLocation(lat, lon)
                    _uiState.update { it.copy(latitude = lat, longitude = lon) }
                    scheduleAzan(lat, lon, _uiState.value.calcMethod)
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Could not get location") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Location error") }
            }
        }
    }

    fun scheduleAzan(latitude: Double, longitude: Double, method: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val times = azanScheduler.calculateAndSchedule(latitude, longitude, method)
                refreshDisplayTimes(latitude, longitude, method)
                _uiState.update { it.copy(isLoading = false, prayerTimes = times) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Calculation error") }
            }
        }
    }

    fun setCalcMethod(method: String) {
        viewModelScope.launch {
            preferences.saveCalcMethod(method)
            _uiState.update { it.copy(calcMethod = method) }
            val state = _uiState.value
            if (state.latitude != 0.0 || state.longitude != 0.0) {
                scheduleAzan(state.latitude, state.longitude, method)
            }
        }
    }

    fun togglePrayerEnabled(azanTime: AzanTime) {
        viewModelScope.launch {
            db.azanTimeDao().setEnabled(azanTime.id, !azanTime.isEnabled)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
