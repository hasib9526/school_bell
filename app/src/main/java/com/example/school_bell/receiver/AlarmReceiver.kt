package com.example.school_bell.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.school_bell.azan.AzanScheduler
import com.example.school_bell.data.db.AppDatabase
import com.example.school_bell.data.db.entities.BellSchedule
import com.example.school_bell.data.prefs.AppPreferences
import com.example.school_bell.service.BellPlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_BELL_ID = "extra_bell_id"
        const val EXTRA_SOUND_FILE = "extra_sound_file"
        const val EXTRA_BELL_LABEL = "extra_bell_label"

        fun scheduleAlarm(context: Context, schedule: BellSchedule) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.school_bell.ACTION_BELL_ALARM"
                putExtra(EXTRA_BELL_ID, schedule.id)
                putExtra(EXTRA_SOUND_FILE, schedule.soundFile)
                putExtra(EXTRA_BELL_LABEL, schedule.label)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                schedule.id.toInt(),
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, schedule.hour)
                set(Calendar.MINUTE, schedule.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                // SCHEDULE_EXACT_ALARM not granted - fall back to inexact
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }

        fun cancelAlarm(context: Context, scheduleId: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.school_bell.ACTION_BELL_ALARM"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                scheduleId.toInt(),
                alarmIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val bellId = intent.getLongExtra(EXTRA_BELL_ID, -1L)
        val soundFile = intent.getStringExtra(EXTRA_SOUND_FILE) ?: "default_bell.mp3"
        val bellLabel = intent.getStringExtra(EXTRA_BELL_LABEL) ?: "School Bell"

        // Detect if this is an Azan alarm
        val isAzan = bellLabel.startsWith("Azan:")

        // Start the player service
        val serviceIntent = BellPlayerService.createIntent(context, soundFile, bellLabel, isAzan)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // If last azan of the day (Isha), schedule tomorrow's azan
        if (isAzan && bellLabel.contains("Isha", ignoreCase = true)) {
            CoroutineScope(Dispatchers.IO).launch {
                val prefs  = AppPreferences(context)
                val lat    = prefs.latitude.first()
                val lon    = prefs.longitude.first()
                val method = prefs.calcMethod.first()
                if (lat != 0.0 || lon != 0.0) {
                    AzanScheduler(context).calculateAndSchedule(lat, lon, method)
                }
            }
        }

        // Reschedule for next occurrence
        if (bellId >= 0) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(context)
                val schedule = db.bellScheduleDao().getById(bellId)
                schedule?.let {
                    if (it.isEnabled) {
                        scheduleAlarm(context, it)
                    }
                }
            }
        }
    }
}
