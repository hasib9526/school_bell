package com.example.school_bell.data.model

import com.google.gson.annotations.SerializedName

// ─── Auth ───────────────────────────────────────────────────────────────────

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("device_id") val deviceId: String
)

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("expires_in") val expiresIn: Long = 0,
    @SerializedName("school_name") val schoolName: String = "",
    @SerializedName("message") val message: String = ""
)

// ─── Announcements ──────────────────────────────────────────────────────────

data class AnnouncementResponse(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("message") val message: String = "",
    @SerializedName("audio_url") val audioUrl: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("priority") val priority: Int = 0,
    @SerializedName("is_active") val isActive: Boolean = true
)

// ─── Device / Heartbeat ─────────────────────────────────────────────────────

data class DeviceHeartbeat(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("battery_level") val batteryLevel: Int,
    @SerializedName("battery_charging") val batteryCharging: Boolean = false,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("app_version") val appVersion: String = "1.0",
    @SerializedName("status") val status: String = "online",
    @SerializedName("school_name") val schoolName: String = ""
)

data class HeartbeatResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("message") val message: String = ""
)

// ─── Sound Sync ─────────────────────────────────────────────────────────────

data class SoundVersion(
    @SerializedName("version") val version: Int = 0,
    @SerializedName("files") val files: List<SoundFile> = emptyList()
)

data class SoundFile(
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String,
    @SerializedName("checksum") val checksum: String = "",
    @SerializedName("size_bytes") val sizeBytes: Long = 0
)

// ─── Schedules ──────────────────────────────────────────────────────────────

data class ScheduleResponse(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("label") val label: String,
    @SerializedName("hour") val hour: Int,
    @SerializedName("minute") val minute: Int,
    @SerializedName("days") val days: Int = 0b0111110,
    @SerializedName("sound_file") val soundFile: String = "default_bell.mp3",
    @SerializedName("is_enabled") val isEnabled: Boolean = true,
    @SerializedName("routine_type") val routineType: String = "SCHOOL"
)

// ─── API wrapper ────────────────────────────────────────────────────────────

data class ApiResult<T>(
    val data: T? = null,
    val error: String? = null,
    val isSuccess: Boolean = data != null
)
