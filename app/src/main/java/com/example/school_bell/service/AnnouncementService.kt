package com.example.school_bell.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.school_bell.MainActivity
import com.example.school_bell.data.model.AnnouncementResponse
import com.example.school_bell.data.network.RetrofitClient
import com.example.school_bell.data.prefs.AppPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AnnouncementService : Service() {

    companion object {
        const val CHANNEL_ID = "ANNOUNCEMENT_CHANNEL"
        const val NOTIFICATION_ID = 1003
        private const val POLL_INTERVAL_MS = 30_000L
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var player: ExoPlayer? = null
    private var lastAnnouncementId: Long = -1
    private lateinit var preferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Monitoring announcements…"))
        startPolling()
    }

    private fun startPolling() {
        serviceScope.launch {
            while (isActive) {
                try {
                    pollForAnnouncements()
                } catch (e: Exception) {
                    // Silently handle errors, keep polling
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun pollForAnnouncements() {
        val token = preferences.authToken.first() ?: return
        val serverUrl = preferences.serverUrl.first()
        val api = RetrofitClient.getApiService(serverUrl)
        val response = api.getLatestAnnouncement()
        if (response.isSuccessful) {
            val announcement = response.body() ?: return
            if (announcement.isActive && announcement.id != lastAnnouncementId) {
                lastAnnouncementId = announcement.id
                announcement.audioUrl?.let { audioUrl ->
                    withContext(Dispatchers.Main) {
                        playAnnouncementAudio(audioUrl)
                    }
                }
                showAnnouncementNotification(announcement)
                broadcastAnnouncement(announcement)
            }
        }
    }

    private fun playAnnouncementAudio(audioUrl: String) {
        try {
            releasePlayer()
            player = ExoPlayer.Builder(this).build()
            val mediaItem = MediaItem.fromUri(audioUrl)
            player?.apply {
                setMediaItem(mediaItem)
                prepare()
                play()
            }
        } catch (e: Exception) {
            // Audio playback failed gracefully
        }
    }

    private fun showAnnouncementNotification(announcement: AnnouncementResponse) {
        val manager = getSystemService(NotificationManager::class.java)
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(announcement.title)
            .setContentText(announcement.message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID + 100, notification)
    }

    private fun broadcastAnnouncement(announcement: AnnouncementResponse) {
        val intent = Intent("com.example.school_bell.NEW_ANNOUNCEMENT").apply {
            putExtra("announcement_title", announcement.title)
            putExtra("announcement_message", announcement.message)
            putExtra("announcement_id", announcement.id)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun buildNotification(text: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Announcement Monitor")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live announcement notifications"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    override fun onDestroy() {
        serviceScope.cancel()
        releasePlayer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
