package com.example.school_bell.azan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.school_bell.data.db.AppDatabase
import com.example.school_bell.data.db.entities.AzanTime
import com.example.school_bell.receiver.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class AzanScheduler(private val context: Context) {

    companion object {
        private const val AZAN_REQUEST_CODE_BASE = 2000
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val PRAYER_NAMES = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

        // Triple(fajrAngle, ishaAngle, ishaMinutesAfterMaghrib)
        private val METHODS = mapOf(
            "MUSLIM_WORLD_LEAGUE" to Triple(18.0, 17.0, 0),
            "MWL"                 to Triple(18.0, 17.0, 0),
            "ISNA"                to Triple(15.0, 15.0, 0),
            "EGYPT"               to Triple(19.5, 17.5, 0),
            "KARACHI"             to Triple(18.0, 18.0, 0),
            "UMM_AL_QURA"         to Triple(18.5,  0.0, 90),
            "GULF"                to Triple(19.5,  0.0, 90),
            "KUWAIT"              to Triple(18.0, 17.5, 0),
            "QATAR"               to Triple(18.0,  0.0, 90),
            "SINGAPORE"           to Triple(20.0, 18.0, 0),
            "TURKEY"              to Triple(18.0, 17.0, 0),
            "TEHRAN"              to Triple(17.7, 14.0, 0)
        )
    }

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun calculateAndSchedule(
        latitude: Double,
        longitude: Double,
        methodName: String
    ): List<AzanTime> {
        if (latitude == 0.0 && longitude == 0.0) return emptyList()

        val db = AppDatabase.getInstance(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val today = Calendar.getInstance()
        val todayStr = DATE_FORMAT.format(today.time)

        db.azanTimeDao().deleteForDate(todayStr)

        val timesMs = computePrayerTimes(latitude, longitude, today, methodName)
        val result = mutableListOf<AzanTime>()

        PRAYER_NAMES.forEachIndexed { index, name ->
            val ms = timesMs[index]
            val c = Calendar.getInstance().apply { timeInMillis = ms }
            val entity = AzanTime(
                prayerName = name,
                hour = c.get(Calendar.HOUR_OF_DAY),
                minute = c.get(Calendar.MINUTE),
                date = todayStr,
                isEnabled = true
            )
            val id = db.azanTimeDao().insert(entity)
            result.add(entity.copy(id = id))
            scheduleAzanAlarm(id, name, ms, alarmManager)
        }
        return result
    }

    fun getPrayerTimesDisplay(
        latitude: Double,
        longitude: Double,
        methodName: String
    ): Map<String, String> {
        if (latitude == 0.0 && longitude == 0.0) return emptyMap()
        return try {
            val timesMs = computePrayerTimes(latitude, longitude, Calendar.getInstance(), methodName)
            val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
            PRAYER_NAMES.zip(timesMs).associate { (name, ms) -> name to fmt.format(Date(ms)) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun cancelAllAzanAlarms() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in AZAN_REQUEST_CODE_BASE until AZAN_REQUEST_CODE_BASE + 20) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.school_bell.ACTION_BELL_ALARM"
            }
            val pi = PendingIntent.getBroadcast(
                context, i, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let { alarmManager.cancel(it) }
        }
    }

    // ── Astronomical computation ──────────────────────────────────────────────

    private fun computePrayerTimes(
        lat: Double,
        lon: Double,
        cal: Calendar,
        methodName: String
    ): List<Long> {
        val (fajrAngle, ishaAngle, ishaInterval) =
            METHODS[methodName] ?: METHODS["MUSLIM_WORLD_LEAGUE"]!!

        val jd = julianDay(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )

        val (sunDec, eqTime) = sunPosition(jd)
        val tzHours = cal.timeZone.getOffset(cal.timeInMillis) / 3_600_000.0

        // Solar noon in local decimal hours
        val transit = 12.0 - lon / 15.0 - eqTime + tzHours

        val fajr    = transit - hourAngle(lat, sunDec, -fajrAngle)
        val dhuhr   = transit + 0.0167   // ~1 min past solar noon
        val asr     = transit + asrHourAngle(lat, sunDec)
        val maghrib = transit + hourAngle(lat, sunDec, -0.8333)
        val isha    = if (ishaInterval > 0) {
            maghrib + ishaInterval / 60.0
        } else {
            transit + hourAngle(lat, sunDec, -ishaAngle)
        }

        return listOf(fajr, dhuhr, asr, maghrib, isha)
            .map { localHoursToEpochMs(it, cal) }
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year; var m = month
        if (m <= 2) { y--; m += 12 }
        val a = y / 100
        val b = 2 - a + a / 4
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2_451_545.0
        val g = Math.toRadians((357.529 + 0.98560028 * d).mod(360.0))
        val q = (280.459 + 0.98564736 * d).mod(360.0)
        val L = Math.toRadians((q + 1.915 * sin(g) + 0.020 * sin(2 * g)).mod(360.0))
        val e = Math.toRadians(23.439 - 0.00000036 * d)
        val dec = Math.toDegrees(asin(sin(e) * sin(L)))
        val ra  = Math.toDegrees(atan2(cos(e) * sin(L), cos(L))) / 15.0
        val eqTime = q / 15.0 - (ra + 360.0).mod(24.0)
        return Pair(dec, eqTime)
    }

    private fun hourAngle(lat: Double, dec: Double, altitudeDeg: Double): Double {
        val cosHa = (sin(Math.toRadians(altitudeDeg))
                - sin(Math.toRadians(lat)) * sin(Math.toRadians(dec))) /
                (cos(Math.toRadians(lat)) * cos(Math.toRadians(dec)))
        return if (abs(cosHa) > 1.0) 0.0
        else Math.toDegrees(acos(cosHa)) / 15.0
    }

    /** Shafi madhab (shadow factor = 1). For Hanafi use factor = 2. */
    private fun asrHourAngle(lat: Double, dec: Double, shadowFactor: Double = 1.0): Double {
        val angle = Math.toDegrees(atan(1.0 / (shadowFactor + tan(Math.toRadians(abs(lat - dec))))))
        return hourAngle(lat, dec, angle)
    }

    private fun localHoursToEpochMs(localHours: Double, cal: Calendar): Long {
        val h = localHours.toInt().coerceIn(0, 23)
        val minsFrac = (localHours - localHours.toInt()) * 60.0
        val m = minsFrac.toInt().coerceIn(0, 59)
        val s = ((minsFrac - m) * 60).toInt().coerceIn(0, 59)
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, h)
        c.set(Calendar.MINUTE, m)
        c.set(Calendar.SECOND, s)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    // ── Alarm scheduling ──────────────────────────────────────────────────────

    private fun scheduleAzanAlarm(id: Long, name: String, triggerMs: Long, am: AlarmManager) {
        if (triggerMs <= System.currentTimeMillis()) return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.school_bell.ACTION_BELL_ALARM"
            putExtra(AlarmReceiver.EXTRA_BELL_ID, id)
            putExtra(AlarmReceiver.EXTRA_SOUND_FILE, "azan.mp3")
            putExtra(AlarmReceiver.EXTRA_BELL_LABEL, "Azan: $name")
        }
        val pi = PendingIntent.getBroadcast(
            context,
            AZAN_REQUEST_CODE_BASE + id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        } catch (e: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }
    }
}
