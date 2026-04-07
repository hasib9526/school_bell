package com.example.school_bell.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.school_bell.MainActivity
import com.example.school_bell.azan.AzanScheduler
import com.example.school_bell.data.db.AppDatabase
import com.example.school_bell.data.prefs.AppPreferences
import com.example.school_bell.service.MonitoringService
import com.example.school_bell.worker.SoundSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in listOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_LOCKED_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_POWER_CONNECTED
            )
        ) return

        // On power connected: launch the app to foreground (kiosk auto-start)
        if (action == Intent.ACTION_POWER_CONNECTED) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(launchIntent)
        }

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
        schedules.forEach { schedule ->
            AlarmReceiver.scheduleAlarm(context, schedule)
        }
    }
}
