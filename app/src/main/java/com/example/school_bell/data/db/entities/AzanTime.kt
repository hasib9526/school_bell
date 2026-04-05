package com.example.school_bell.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "azan_times")
data class AzanTime(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val prayerName: String,
    val hour: Int,
    val minute: Int,
    val date: String, // yyyy-MM-dd
    val isEnabled: Boolean = true
)
