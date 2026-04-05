package com.example.school_bell.data.network

import com.example.school_bell.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/announcements/latest")
    suspend fun getLatestAnnouncement(): Response<AnnouncementResponse>

    @GET("api/announcements")
    suspend fun getAllAnnouncements(
        @Query("limit") limit: Int = 20
    ): Response<List<AnnouncementResponse>>

    @POST("api/device/heartbeat")
    suspend fun sendHeartbeat(@Body heartbeat: DeviceHeartbeat): Response<HeartbeatResponse>

    @GET("api/sounds/version")
    suspend fun getSoundVersion(): Response<SoundVersion>

    @GET("api/schedules")
    suspend fun getSchedules(): Response<List<ScheduleResponse>>
}
