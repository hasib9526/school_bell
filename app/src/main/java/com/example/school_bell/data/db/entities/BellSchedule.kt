package com.example.school_bell.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RoutineType {
    SCHOOL, AZAN, CUSTOM
}

@Entity(tableName = "bell_schedules")
data class BellSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val hour: Int,
    val minute: Int,
    /** Bitmask: bit0=Mon, bit1=Tue, bit2=Wed, bit3=Thu, bit4=Fri, bit5=Sat, bit6=Sun */
    val days: Int = 0b0111110, // Mon-Fri by default
    val soundFile: String = "default_bell.mp3",
    val isEnabled: Boolean = true,
    val routineType: RoutineType = RoutineType.SCHOOL
)
