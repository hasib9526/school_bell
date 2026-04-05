package com.example.school_bell

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import com.example.school_bell.service.AnnouncementService
import com.example.school_bell.service.BellPlayerService
import com.example.school_bell.service.MonitoringService
import com.example.school_bell.worker.SoundSyncWorker

class SchoolBellApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleSoundSync()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val bellChannel = NotificationChannel(
                BellPlayerService.CHANNEL_ID,
                "School Bell Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for school bell events"
                setSound(null, null)
            }

            val monitoringChannel = NotificationChannel(
                MonitoringService.CHANNEL_ID,
                "Device Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background monitoring service"
                setShowBadge(false)
            }

            val announcementChannel = NotificationChannel(
                AnnouncementService.CHANNEL_ID,
                "Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live announcement notifications"
            }

            manager.createNotificationChannels(
                listOf(bellChannel, monitoringChannel, announcementChannel)
            )
        }
    }

    private fun scheduleSoundSync() {
        SoundSyncWorker.schedule(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
