package com.example.school_bell.data.db.dao

import androidx.room.*
import com.example.school_bell.data.db.entities.BellSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface BellScheduleDao {

    @Query("SELECT * FROM bell_schedules ORDER BY hour ASC, minute ASC")
    fun getAllSchedules(): Flow<List<BellSchedule>>

    @Query("SELECT * FROM bell_schedules WHERE isEnabled = 1 ORDER BY hour ASC, minute ASC")
    fun getEnabledSchedules(): Flow<List<BellSchedule>>

    @Query("SELECT * FROM bell_schedules WHERE id = :id")
    suspend fun getById(id: Long): BellSchedule?

    @Query("""
        SELECT * FROM bell_schedules
        WHERE isEnabled = 1
        AND (hour > :currentHour OR (hour = :currentHour AND minute > :currentMinute))
        ORDER BY hour ASC, minute ASC
        LIMIT 1
    """)
    suspend fun getNextUpcoming(currentHour: Int, currentMinute: Int): BellSchedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: BellSchedule): Long

    @Update
    suspend fun update(schedule: BellSchedule)

    @Delete
    suspend fun delete(schedule: BellSchedule)

    @Query("DELETE FROM bell_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE bell_schedules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM bell_schedules")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<BellSchedule>)
}
