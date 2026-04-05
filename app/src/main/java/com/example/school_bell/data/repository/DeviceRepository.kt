package com.example.school_bell.data.repository

import android.content.Context
import android.os.Environment
import com.example.school_bell.data.model.*
import com.example.school_bell.data.network.RetrofitClient
import com.example.school_bell.data.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DeviceRepository(
    private val context: Context,
    private val preferences: AppPreferences
) {

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun login(username: String, password: String): ApiResult<LoginResponse> {
        return try {
            val serverUrl = preferences.serverUrl.first()
            val deviceId = preferences.ensureDeviceId()
            val api = RetrofitClient.getApiService(serverUrl)
            val response = api.login(LoginRequest(username, password, deviceId))
            if (response.isSuccessful) {
                val body = response.body()!!
                preferences.saveAuthToken(body.token)
                preferences.saveUsername(username)
                if (body.schoolName.isNotEmpty()) {
                    preferences.saveSchoolName(body.schoolName)
                }
                RetrofitClient.setAuthToken(body.token)
                ApiResult(data = body)
            } else {
                ApiResult(error = "Login failed: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            ApiResult(error = e.message ?: "Network error")
        }
    }

    suspend fun sendHeartbeat(batteryLevel: Int, isCharging: Boolean): ApiResult<HeartbeatResponse> {
        return try {
            val serverUrl = preferences.serverUrl.first()
            val deviceId = preferences.ensureDeviceId()
            val schoolName = preferences.schoolName.first()
            val api = RetrofitClient.getApiService(serverUrl)
            val heartbeat = DeviceHeartbeat(
                deviceId = deviceId,
                batteryLevel = batteryLevel,
                batteryCharging = isCharging,
                timestamp = System.currentTimeMillis(),
                schoolName = schoolName
            )
            val response = api.sendHeartbeat(heartbeat)
            if (response.isSuccessful) {
                ApiResult(data = response.body())
            } else {
                ApiResult(error = "Heartbeat failed: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult(error = e.message ?: "Network error")
        }
    }

    suspend fun checkAndSyncSounds(): ApiResult<SoundVersion> {
        return try {
            val serverUrl = preferences.serverUrl.first()
            val api = RetrofitClient.getApiService(serverUrl)
            val response = api.getSoundVersion()
            if (response.isSuccessful) {
                val serverVersion = response.body()!!
                val localVersion = preferences.soundVersion.first()
                if (serverVersion.version > localVersion) {
                    // Download new/updated files
                    downloadSoundFiles(serverVersion.files, serverUrl)
                    preferences.saveSoundVersion(serverVersion.version)
                    preferences.saveLastSyncTime(System.currentTimeMillis())
                }
                ApiResult(data = serverVersion)
            } else {
                ApiResult(error = "Version check failed: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult(error = e.message ?: "Network error")
        }
    }

    private suspend fun downloadSoundFiles(files: List<SoundFile>, baseUrl: String) {
        withContext(Dispatchers.IO) {
            val soundsDir = File(context.filesDir, "sounds").apply { mkdirs() }
            files.forEach { soundFile ->
                try {
                    val url = if (soundFile.url.startsWith("http")) soundFile.url
                    else "$baseUrl/${soundFile.url.trimStart('/')}"
                    val request = Request.Builder().url(url).build()
                    val response = downloadClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val destFile = File(soundsDir, soundFile.name)
                        response.body?.byteStream()?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Continue with other files even if one fails
                }
            }
        }
    }

    suspend fun getLatestAnnouncement(): ApiResult<AnnouncementResponse> {
        return try {
            val serverUrl = preferences.serverUrl.first()
            val api = RetrofitClient.getApiService(serverUrl)
            val response = api.getLatestAnnouncement()
            if (response.isSuccessful) {
                ApiResult(data = response.body())
            } else {
                ApiResult(error = "Failed: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult(error = e.message ?: "Network error")
        }
    }

    suspend fun logout() {
        preferences.clearAuthToken()
        RetrofitClient.setAuthToken(null)
        RetrofitClient.reset()
    }

    fun getSoundFile(fileName: String): File {
        val soundsDir = File(context.filesDir, "sounds")
        return File(soundsDir, fileName)
    }
}
