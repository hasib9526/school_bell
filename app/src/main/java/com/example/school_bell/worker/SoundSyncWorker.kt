package com.example.school_bell.worker

import android.content.Context
import androidx.work.*
import com.example.school_bell.data.prefs.AppPreferences
import com.example.school_bell.data.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SoundSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val WORK_NAME = "SoundSyncWork"
        private const val DEFAULT_INTERVAL_HOURS = 6L

        fun schedule(context: Context, intervalHours: Long = DEFAULT_INTERVAL_HOURS) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SoundSyncWorker>(
                intervalHours, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // flex interval
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SoundSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        val preferences = AppPreferences(applicationContext)
        val token = preferences.authToken.first() ?: return Result.success() // Not logged in

        val repository = DeviceRepository(applicationContext, preferences)
        val result = repository.checkAndSyncSounds()

        return if (result.isSuccess) {
            Result.success()
        } else {
            // Retry if network error, success if server intentionally returned error
            Result.retry()
        }
    }
}
