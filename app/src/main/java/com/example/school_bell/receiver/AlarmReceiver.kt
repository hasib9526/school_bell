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
        const val EXTRA_BELL_ID    = "extra_bell_id"
        const val EXTRA_SOUND_FILE = "extra_sound_file"
        const val EXTRA_BELL_LABEL = "extra_bell_label"

        /**
         * Schedules the next alarm for [schedule], skipping days not set in the bitmask.
         * Bitmask: bit0=Mon, bit1=Tue, bit2=Wed, bit3=Thu, bit4=Fri, bit5=Sat, bit6=Sun
         */
        fun scheduleAlarm(context: Context, schedule: BellSchedule) {
            if (schedule.days == 0) return  // No days selected — nothing to schedule

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

            val triggerMs = nextTriggerMs(schedule) ?: return

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMs,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                // SCHEDULE_EXACT_ALARM not granted — fall back to inexact
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
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

        /**
         * Returns the next epoch-ms trigger time for a schedule, respecting the days bitmask.
         * Returns null if no valid day is found (days == 0).
         */
        private fun nextTriggerMs(schedule: BellSchedule): Long? {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, schedule.hour)
                set(Calendar.MINUTE, schedule.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If this time slot already passed today, start checking from tomorrow
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Walk forward up to 7 days to find next matching day in bitmask
            for (attempt in 0 until 7) {
                val bitIndex = calDayToBitIndex(cal.get(Calendar.DAY_OF_WEEK))
                if (schedule.days and (1 shl bitIndex) != 0) {
                    return cal.timeInMillis
                }
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return null  // days bitmask is 0 or otherwise unreachable
        }

        /**
         * Converts Calendar.DAY_OF_WEEK to the bitmask index used in BellSchedule.days.
         * Calendar: Sun=1, Mon=2, Tue=3, Wed=4, Thu=5, Fri=6, Sat=7
         * Bitmask:  bit0=Mon, bit1=Tue, bit2=Wed, bit3=Thu, bit4=Fri, bit5=Sat, bit6=Sun
         */
        private fun calDayToBitIndex(dayOfWeek: Int): Int = when (dayOfWeek) {
            Calendar.MONDAY    -> 0
            Calendar.TUESDAY   -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY  -> 3
            Calendar.FRIDAY    -> 4
            Calendar.SATURDAY  -> 5
            Calendar.SUNDAY    -> 6
            else               -> 0
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val bellId    = intent.getLongExtra(EXTRA_BELL_ID, -1L)
        val soundFile = intent.getStringExtra(EXTRA_SOUND_FILE) ?: "default_bell.mp3"
        val bellLabel = intent.getStringExtra(EXTRA_BELL_LABEL) ?: "School Bell"

        val isAzan = bellLabel.startsWith("Azan:")

        // Start the player service
        val serviceIntent = BellPlayerService.createIntent(context, soundFile, bellLabel, isAzan)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // If last azan of the day (Isha), schedule tomorrow from pre-calculated DB
        if (isAzan && bellLabel.contains("Isha", ignoreCase = true)) {
            CoroutineScope(Dispatchers.IO).launch {
                val prefs  = AppPreferences(context)
                val lat    = prefs.latitude.first()
                val lon    = prefs.longitude.first()
                val method = prefs.calcMethod.first()
                AzanScheduler(context).scheduleTomorrow(lat, lon, method)
            }
        }

        // Reschedule for next occurrence (respects day bitmask)
        if (bellId >= 0 && !isAzan) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(context)
                val schedule = db.bellScheduleDao().getById(bellId)
                schedule?.let {
                    if (it.isEnabled) scheduleAlarm(context, it)
                }
            }
        }
    }
}
