package com.example.school_bell.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.school_bell.data.db.AppDatabase
import com.example.school_bell.data.db.entities.BellSchedule
import com.example.school_bell.data.model.AnnouncementResponse
import com.example.school_bell.data.prefs.AppPreferences
import com.example.school_bell.data.repository.DeviceRepository
import com.example.school_bell.kiosk.KioskManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardUiState(
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val schoolName: String = "Smart School",
    val nextBell: BellSchedule? = null,
    val nextBellMinutesAway: Long = -1,
    val nextAzanName: String = "",
    val nextAzanMinutesAway: Long = -1,
    val batteryLevel: Int = 100,
    val isBatteryCharging: Boolean = false,
    val isKioskEnabled: Boolean = false,
    val liveAnnouncement: AnnouncementResponse? = null,
    val isServerConnected: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application)
    private val db = AppDatabase.getInstance(application)
    private val deviceRepository = DeviceRepository(application, preferences)
    val kioskManager = KioskManager(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var batteryReceiver: BroadcastReceiver? = null

    init {
        startClockTick()
        observePreferences()
        observeNextBell()
        observeNextAzan()
        registerBatteryReceiver()
        pollAnnouncement()
    }

    private fun startClockTick() {
        viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(currentTimeMillis = System.currentTimeMillis()) }
                updateNextBellCountdown()
                updateNextAzanCountdown()
                delay(1000L)
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferences.schoolName.collect { name ->
                _uiState.update { it.copy(schoolName = name) }
            }
        }
        viewModelScope.launch {
            preferences.isKioskEnabled.collect { enabled ->
                _uiState.update { it.copy(isKioskEnabled = enabled) }
            }
        }
    }

    private fun observeNextBell() {
        viewModelScope.launch {
            db.bellScheduleDao().getEnabledSchedules().collect { schedules ->
                updateNextBellFromList(schedules)
            }
        }
    }

    private fun observeNextAzan() {
        viewModelScope.launch {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(Calendar.getInstance().time)
            db.azanTimeDao().getAzanTimesForDate(today).collect { azanTimes ->
                val now = Calendar.getInstance()
                val nextAzan = azanTimes
                    .filter { it.isEnabled }
                    .firstOrNull { azan ->
                        azan.hour > now.get(Calendar.HOUR_OF_DAY) ||
                                (azan.hour == now.get(Calendar.HOUR_OF_DAY) &&
                                        azan.minute > now.get(Calendar.MINUTE))
                    }
                if (nextAzan != null) {
                    val minutesAway = ((nextAzan.hour - now.get(Calendar.HOUR_OF_DAY)) * 60 +
                            (nextAzan.minute - now.get(Calendar.MINUTE))).toLong()
                    _uiState.update {
                        it.copy(
                            nextAzanName = nextAzan.prayerName,
                            nextAzanMinutesAway = minutesAway
                        )
                    }
                }
            }
        }
    }

    private fun updateNextBellFromList(schedules: List<BellSchedule>) {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentDayBit = (1 shl ((now.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7))

        val nextBell = schedules
            .filter { it.isEnabled && (it.days and currentDayBit != 0) }
            .filter { it.hour > currentHour || (it.hour == currentHour && it.minute > currentMinute) }
            .minByOrNull { it.hour * 60 + it.minute }

        if (nextBell != null) {
            val minutesAway = ((nextBell.hour - currentHour) * 60 + (nextBell.minute - currentMinute)).toLong()
            _uiState.update { it.copy(nextBell = nextBell, nextBellMinutesAway = minutesAway) }
        } else {
            _uiState.update { it.copy(nextBell = null, nextBellMinutesAway = -1) }
        }
    }

    private fun updateNextBellCountdown() {
        val bell = _uiState.value.nextBell ?: return
        val now = Calendar.getInstance()
        val minutesAway = ((bell.hour - now.get(Calendar.HOUR_OF_DAY)) * 60 +
                (bell.minute - now.get(Calendar.MINUTE))).toLong()
        if (minutesAway < 0) {
            // Bell passed, clear it
            _uiState.update { it.copy(nextBell = null, nextBellMinutesAway = -1) }
        } else {
            _uiState.update { it.copy(nextBellMinutesAway = minutesAway) }
        }
    }

    private fun updateNextAzanCountdown() {
        val minutesAway = _uiState.value.nextAzanMinutesAway
        if (minutesAway > 0) {
            _uiState.update { it.copy(nextAzanMinutesAway = minutesAway - 1) }
        }
    }

    private fun registerBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) ?: 100
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                _uiState.update { it.copy(batteryLevel = level, isBatteryCharging = charging) }
            }
        }
        getApplication<Application>().registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    private fun pollAnnouncement() {
        viewModelScope.launch {
            while (true) {
                try {
                    val token = preferences.authToken.first()
                    if (token != null) {
                        val result = deviceRepository.getLatestAnnouncement()
                        if (result.isSuccess) {
                            _uiState.update {
                                it.copy(
                                    liveAnnouncement = result.data,
                                    isServerConnected = true
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isServerConnected = false) }
                }
                delay(30_000L)
            }
        }
    }

    fun toggleKiosk(enabled: Boolean) {
        viewModelScope.launch {
            preferences.saveKioskEnabled(enabled)
            _uiState.update { it.copy(isKioskEnabled = enabled) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        batteryReceiver?.let {
            try { getApplication<Application>().unregisterReceiver(it) } catch (e: Exception) { }
        }
    }
}
