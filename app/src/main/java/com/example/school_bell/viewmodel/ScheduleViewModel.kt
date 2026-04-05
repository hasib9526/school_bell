package com.example.school_bell.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.school_bell.data.db.AppDatabase
import com.example.school_bell.data.db.entities.BellSchedule
import com.example.school_bell.data.db.entities.RoutineType
import com.example.school_bell.data.prefs.AppPreferences
import com.example.school_bell.data.repository.ScheduleRepository
import com.example.school_bell.receiver.AlarmReceiver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val schedules: List<BellSchedule> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application)
    private val db = AppDatabase.getInstance(application)
    private val repository = ScheduleRepository(db.bellScheduleDao(), preferences)

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    // Available sound files in the sounds directory
    val availableSounds = listOf(
        "default_bell.mp3",
        "school_bell.mp3",
        "azan.mp3",
        "chime.mp3",
        "alarm.mp3",
        "tone1.mp3",
        "tone2.mp3"
    )

    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    init {
        observeSchedules()
    }

    private fun observeSchedules() {
        viewModelScope.launch {
            repository.getAllSchedules().collect { schedules ->
                _uiState.update { it.copy(schedules = schedules, isLoading = false) }
            }
        }
    }

    fun addSchedule(
        label: String,
        hour: Int,
        minute: Int,
        days: Int,
        soundFile: String,
        routineType: RoutineType = RoutineType.SCHOOL
    ) {
        viewModelScope.launch {
            val schedule = BellSchedule(
                label = label,
                hour = hour,
                minute = minute,
                days = days,
                soundFile = soundFile,
                isEnabled = true,
                routineType = routineType
            )
            val id = repository.insert(schedule)
            AlarmReceiver.scheduleAlarm(getApplication(), schedule.copy(id = id))
        }
    }

    fun updateSchedule(schedule: BellSchedule) {
        viewModelScope.launch {
            repository.update(schedule)
            if (schedule.isEnabled) {
                AlarmReceiver.scheduleAlarm(getApplication(), schedule)
            } else {
                AlarmReceiver.cancelAlarm(getApplication(), schedule.id)
            }
        }
    }

    fun deleteSchedule(schedule: BellSchedule) {
        viewModelScope.launch {
            AlarmReceiver.cancelAlarm(getApplication(), schedule.id)
            repository.delete(schedule)
        }
    }

    fun toggleScheduleEnabled(schedule: BellSchedule) {
        viewModelScope.launch {
            val updated = schedule.copy(isEnabled = !schedule.isEnabled)
            repository.update(updated)
            if (updated.isEnabled) {
                AlarmReceiver.scheduleAlarm(getApplication(), updated)
            } else {
                AlarmReceiver.cancelAlarm(getApplication(), updated.id)
            }
        }
    }

    fun refreshFromServer() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.refreshFromServer()
            _uiState.update { it.copy(isLoading = false, error = result.error) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** Convert days bitmask to list of selected day indices (0=Mon..6=Sun) */
    fun getDayIndices(days: Int): List<Int> =
        (0..6).filter { days and (1 shl it) != 0 }

    /** Convert list of day indices to bitmask */
    fun dayIndicesToMask(indices: List<Int>): Int =
        indices.fold(0) { acc, i -> acc or (1 shl i) }
}
