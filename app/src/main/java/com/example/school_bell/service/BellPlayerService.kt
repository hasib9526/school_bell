package com.example.school_bell.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.school_bell.MainActivity
import com.example.school_bell.R
import java.io.File

class BellPlayerService : Service() {

    companion object {
        const val CHANNEL_ID = "BELL_CHANNEL"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_SOUND_FILE = "extra_sound_file"
        const val EXTRA_BELL_LABEL = "extra_bell_label"

        fun createIntent(context: Context, soundFile: String, label: String): Intent =
            Intent(context, BellPlayerService::class.java).apply {
                putExtra(EXTRA_SOUND_FILE, soundFile)
                putExtra(EXTRA_BELL_LABEL, label)
            }
    }

    private var player: ExoPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val soundFile = intent?.getStringExtra(EXTRA_SOUND_FILE) ?: "default_bell.mp3"
        val label = intent?.getStringExtra(EXTRA_BELL_LABEL) ?: "School Bell"

        startForeground(NOTIFICATION_ID, buildNotification(label))
        playSound(soundFile)

        return START_NOT_STICKY
    }

    private fun playSound(soundFileName: String) {
        try {
            releasePlayer()
            player = ExoPlayer.Builder(this).build()

            val soundsDir = File(filesDir, "sounds")
            val soundFile = File(soundsDir, soundFileName)

            val mediaItem = if (soundFile.exists()) {
                MediaItem.fromUri(android.net.Uri.fromFile(soundFile))
            } else {
                // Fallback: use a raw resource if available, otherwise stop
                stopSelf()
                return
            }

            player?.apply {
                setMediaItem(mediaItem)
                prepare()
                play()
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == androidx.media3.common.Player.STATE_ENDED ||
                            state == androidx.media3.common.Player.STATE_IDLE) {
                            stopSelf()
                        }
                    }
                })
            }
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun buildNotification(label: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("School Bell")
            .setContentText(label)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "School Bell Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for school bell events"
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SchoolBell::BellPlayerWakeLock"
        ).apply {
            acquire(60 * 1000L) // max 1 minute
        }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    override fun onDestroy() {
        releasePlayer()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
