package com.proofapp.android.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.proofapp.android.ProofApplication
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Background sync: refresh recipes, push favourites changed offline, pull favourites changed on
 * other devices. WorkManager retries with backoff and waits for a connection, so a favourite
 * tapped in airplane mode reaches the server as soon as the phone is back online.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as ProofApplication).container.recipeRepository
        return try {
            repository.refresh().getOrThrow()
            repository.syncFavorites()
            Result.success()
        } catch (e: IOException) {
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        } catch (e: retrofit2.HttpException) {
            if (e.code() >= 500 && runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }
}

class SyncScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        // REPLACE coalesces bursts of taps into one sync.
        workManager.enqueueUniqueWork(ONE_OFF, ExistingWorkPolicy.REPLACE, request)
    }

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        const val ONE_OFF = "proof-sync"
        const val PERIODIC = "proof-sync-periodic"
    }
}
