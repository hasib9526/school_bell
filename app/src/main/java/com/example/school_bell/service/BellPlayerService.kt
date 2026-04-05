package com.example.school_bell.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.school_bell.MainActivity
import java.io.File

class BellPlayerService : Service() {

    companion object {
        const val CHANNEL_ID       = "BELL_CHANNEL"
        const val NOTIFICATION_ID  = 1001
        const val EXTRA_SOUND_FILE = "extra_sound_file"
        const val EXTRA_BELL_LABEL = "extra_bell_label"
        const val EXTRA_IS_AZAN   = "extra_is_azan"
        const val ACTION_STOP      = "com.example.school_bell.ACTION_STOP_BELL"
        const val MAX_DURATION_MS  = 60_000L  // hard stop after 1 minute

        fun createIntent(
            context: Context,
            soundFile: String,
            label: String,
            isAzan: Boolean = false
        ): Intent = Intent(context, BellPlayerService::class.java).apply {
            putExtra(EXTRA_SOUND_FILE, soundFile)
            putExtra(EXTRA_BELL_LABEL, label)
            putExtra(EXTRA_IS_AZAN, isAzan)
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val stopHandler  = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stopSelf() }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Stop action from notification button
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val soundFile = intent?.getStringExtra(EXTRA_SOUND_FILE) ?: "default_bell.mp3"
        val label     = intent?.getStringExtra(EXTRA_BELL_LABEL) ?: "School Bell"
        val isAzan    = intent?.getBooleanExtra(EXTRA_IS_AZAN, false) ?: false

        startForeground(NOTIFICATION_ID, buildNotification(label, isAzan))
        playSound(soundFile, isAzan)

        return START_NOT_STICKY
    }

    // ── Sound logic ───────────────────────────────────────────────────────────

    private fun playSound(soundFileName: String, isAzan: Boolean) {
        releasePlayer()

        try {
            val customFile = File(File(filesDir, "sounds"), soundFileName)

            when {
                // Priority 1: user-provided file in filesDir/sounds/
                customFile.exists() -> playFromFile(customFile)

                // Priority 2: bundled raw resource (res/raw/azan.mp3 or res/raw/bell.mp3)
                isAzan -> {
                    val rawId = resources.getIdentifier("azan", "raw", packageName)
                    if (rawId != 0) playFromRaw(rawId)
                    else playFromUri(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                }

                else -> {
                    val rawId = resources.getIdentifier("bell", "raw", packageName)
                    if (rawId != 0) playFromRaw(rawId)
                    else playFromUri(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    )
                }
            }

            // Bell: hard stop after 1 minute. Azan: play fully (no forced stop)
            stopHandler.removeCallbacks(stopRunnable)
            if (!isAzan) {
                stopHandler.postDelayed(stopRunnable, MAX_DURATION_MS)
            }

        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun playFromFile(file: File) {
        mediaPlayer = buildPlayer().apply {
            setDataSource(applicationContext, android.net.Uri.fromFile(file))
            isLooping = false
            setOnPreparedListener { it.start() }
            setOnCompletionListener { stopSelf() }
            setOnErrorListener { _, _, _ -> stopSelf(); true }
            prepareAsync()
        }
    }

    private fun playFromUri(uri: android.net.Uri) {
        mediaPlayer = buildPlayer().apply {
            setDataSource(applicationContext, uri)
            isLooping = false
            setOnPreparedListener { it.start() }
            setOnCompletionListener { stopSelf() }
            setOnErrorListener { _, _, _ -> stopSelf(); true }
            prepareAsync()
        }
    }

    private fun playFromRaw(rawResId: Int) {
        mediaPlayer = MediaPlayer.create(this, rawResId)?.apply {
            isLooping = false
            setOnCompletionListener { stopSelf() }
            setOnErrorListener { _, _, _ -> stopSelf(); true }
            start()
        } ?: run { stopSelf(); return }
    }

    private fun buildPlayer(): MediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setLegacyStreamType(AudioManager.STREAM_ALARM)
                .build()
        )
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun buildNotification(label: String, isAzan: Boolean): Notification {
        val openPi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop action pending intent
        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, BellPlayerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isAzan) "🕌 Azan Time" else "🔔 School Bell")
            .setContentText(label)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentIntent(openPi)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPi
            )
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "School Bell Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { setSound(null, null) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "SchoolBell::BellWakeLock"
        ).apply { acquire(MAX_DURATION_MS + 5_000L) }
    }

    private fun releasePlayer() {
        try { mediaPlayer?.stop() } catch (_: Exception) {}
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        stopHandler.removeCallbacks(stopRunnable)
        releasePlayer()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
