package com.example.school_bell.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.school_bell.MainActivity
import com.example.school_bell.data.prefs.AppPreferences
import com.example.school_bell.data.repository.DeviceRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class MonitoringService : Service() {

    companion object {
        const val CHANNEL_ID = "MONITORING_CHANNEL"
        const val NOTIFICATION_ID = 1002
        private const val HEARTBEAT_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var preferences: AppPreferences
    private lateinit var deviceRepository: DeviceRepository
    private var wakeLock: PowerManager.WakeLock? = null

    private var batteryLevel: Int = 100
    private var isCharging: Boolean = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                batteryLevel = it.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
        deviceRepository = DeviceRepository(this, preferences)
        createNotificationChannel()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, buildNotification())
        startHeartbeatLoop()
    }

    private fun startHeartbeatLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val token = preferences.authToken.first()
                    if (token != null) {
                        deviceRepository.sendHeartbeat(batteryLevel, isCharging)
                    }
                } catch (e: Exception) {
                    // Heartbeat failed, will retry
                }
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun buildNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("School Bell Monitor")
            .setContentText("Device monitoring active • Battery: $batteryLevel%")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Device Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background monitoring service"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SchoolBell::MonitoringWakeLock"
        ).apply { acquire() }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        try { unregisterReceiver(batteryReceiver) } catch (e: Exception) { }
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
