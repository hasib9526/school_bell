package com.example.school_bell.data.db.dao

import androidx.room.*
import com.example.school_bell.data.db.entities.AzanTime
import kotlinx.coroutines.flow.Flow

@Dao
interface AzanTimeDao {

    @Query("SELECT * FROM azan_times WHERE date = :date ORDER BY hour ASC, minute ASC")
    fun getAzanTimesForDate(date: String): Flow<List<AzanTime>>

    @Query("SELECT * FROM azan_times ORDER BY date ASC, hour ASC, minute ASC")
    fun getAllAzanTimes(): Flow<List<AzanTime>>

    @Query("SELECT * FROM azan_times WHERE isEnabled = 1 ORDER BY date ASC, hour ASC, minute ASC")
    fun getEnabledAzanTimes(): Flow<List<AzanTime>>

    @Query("""
        SELECT * FROM azan_times
        WHERE isEnabled = 1
        AND date = :date
        AND (hour > :currentHour OR (hour = :currentHour AND minute > :currentMinute))
        ORDER BY hour ASC, minute ASC
        LIMIT 1
    """)
    suspend fun getNextAzanToday(date: String, currentHour: Int, currentMinute: Int): AzanTime?

    /** Non-flow version — used for offline cache checks. */
    @Query("SELECT * FROM azan_times WHERE date = :date ORDER BY hour ASC, minute ASC")
    suspend fun getTimesForDateOnce(date: String): List<AzanTime>

    /** Count of stored prayer times for a specific date — used to verify 30-day cache. */
    @Query("SELECT COUNT(*) FROM azan_times WHERE date = :date")
    suspend fun getCountForDate(date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(azanTime: AzanTime): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(azanTimes: List<AzanTime>)

    @Update
    suspend fun update(azanTime: AzanTime)

    @Delete
    suspend fun delete(azanTime: AzanTime)

    @Query("DELETE FROM azan_times WHERE date = :date")
    suspend fun deleteForDate(date: String)

    /** Remove prayer time records older than cutoffDate (yyyy-MM-dd). */
    @Query("DELETE FROM azan_times WHERE date < :cutoffDate")
    suspend fun deleteOldDates(cutoffDate: String)

    @Query("UPDATE azan_times SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM azan_times")
    suspend fun deleteAll()
}
