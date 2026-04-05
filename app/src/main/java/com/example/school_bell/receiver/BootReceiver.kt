package com.example.school_bell.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.school_bell.azan.AzanScheduler
import com.example.school_bell.data.db.AppDatabase
import com.example.school_bell.data.prefs.AppPreferences
import com.example.school_bell.service.MonitoringService
import com.example.school_bell.worker.SoundSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in listOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_LOCKED_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED
            )
        ) return

        // Start monitoring service
        val monitoringIntent = Intent(context, MonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(monitoringIntent)
        } else {
            context.startService(monitoringIntent)
        }

        // Reschedule all active alarms
        CoroutineScope(Dispatchers.IO).launch {
            rescheduleAlarms(context)
            rescheduleAzan(context)
            SoundSyncWorker.schedule(context)
        }
    }

    private suspend fun rescheduleAzan(context: Context) {
        val prefs = AppPreferences(context)
        val lat    = prefs.latitude.first()
        val lon    = prefs.longitude.first()
        val method = prefs.calcMethod.first()
        if (lat == 0.0 && lon == 0.0) return

        // Recalculate & reschedule today's azan
        AzanScheduler(context).calculateAndSchedule(lat, lon, method)
    }

    private suspend fun rescheduleAlarms(context: Context) {
        val db = AppDatabase.getInstance(context)
        val schedules = db.bellScheduleDao().getEnabledSchedules().first()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        schedules.forEach { schedule ->
            val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.school_bell.ACTION_BELL_ALARM"
                putExtra(AlarmReceiver.EXTRA_BELL_ID, schedule.id)
                putExtra(AlarmReceiver.EXTRA_SOUND_FILE, schedule.soundFile)
                putExtra(AlarmReceiver.EXTRA_BELL_LABEL, schedule.label)
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
                // SCHEDULE_EXACT_ALARM not granted
            }
        }
    }
}
