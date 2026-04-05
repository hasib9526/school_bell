package com.example.school_bell.data.repository

import com.example.school_bell.data.db.dao.BellScheduleDao
import com.example.school_bell.data.db.entities.BellSchedule
import com.example.school_bell.data.db.entities.RoutineType
import com.example.school_bell.data.model.ApiResult
import com.example.school_bell.data.model.ScheduleResponse
import com.example.school_bell.data.network.RetrofitClient
import com.example.school_bell.data.prefs.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ScheduleRepository(
    private val bellScheduleDao: BellScheduleDao,
    private val preferences: AppPreferences
) {

    fun getAllSchedules(): Flow<List<BellSchedule>> =
        bellScheduleDao.getAllSchedules()

    fun getEnabledSchedules(): Flow<List<BellSchedule>> =
        bellScheduleDao.getEnabledSchedules()

    suspend fun getNextUpcoming(hour: Int, minute: Int): BellSchedule? =
        bellScheduleDao.getNextUpcoming(hour, minute)

    suspend fun insert(schedule: BellSchedule): Long =
        bellScheduleDao.insert(schedule)

    suspend fun update(schedule: BellSchedule) =
        bellScheduleDao.update(schedule)

    suspend fun delete(schedule: BellSchedule) =
        bellScheduleDao.delete(schedule)

    suspend fun deleteById(id: Long) =
        bellScheduleDao.deleteById(id)

    suspend fun setEnabled(id: Long, enabled: Boolean) =
        bellScheduleDao.setEnabled(id, enabled)

    suspend fun refreshFromServer(): ApiResult<List<ScheduleResponse>> {
        return try {
            val serverUrl = preferences.serverUrl.first()
            val api = RetrofitClient.getApiService(serverUrl)
            val response = api.getSchedules()
            if (response.isSuccessful) {
                val schedules = response.body() ?: emptyList()
                // Merge server schedules into local DB
                val entities = schedules.map { it.toEntity() }
                bellScheduleDao.insertAll(entities)
                ApiResult(data = schedules)
            } else {
                ApiResult(error = "Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult(error = e.message ?: "Network error")
        }
    }

    private fun ScheduleResponse.toEntity(): BellSchedule = BellSchedule(
        id = id,
        label = label,
        hour = hour,
        minute = minute,
        days = days,
        soundFile = soundFile,
        isEnabled = isEnabled,
        routineType = try { RoutineType.valueOf(routineType) } catch (e: Exception) { RoutineType.SCHOOL }
    )
}
