package io.github.meko123456.kharji.data.fx

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.meko123456.kharji.data.FxRate
import io.github.meko123456.kharji.data.KharjiDatabase
import io.github.meko123456.kharji.domain.KCurrency
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Refreshes the cached FX matrix. One request per base currency; failures
 * keep the previous cache (offline fallback) and retry via WorkManager.
 */
class FxRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = KharjiDatabase.get(applicationContext).dao()
        val client = FxClient()
        val today = LocalDate.now().toEpochDay()
        var anyFailure = false

        for (base in KCurrency.entries) {
            client.latest(base).fold(
                onSuccess = { rates ->
                    rates.forEach { (target, rate) ->
                        if (target != base) {
                            dao.upsert(FxRate(base.code, target.code, rate, today))
                        }
                    }
                },
                onFailure = { anyFailure = true },
            )
        }
        return if (anyFailure && runAttemptCount < 3) Result.retry() else Result.success()
    }

    companion object {
        private const val PERIODIC = "fx_refresh"
        private const val INITIAL = "fx_refresh_now"

        fun schedule(context: Context) {
            val network = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<FxRefreshWorker>(24, TimeUnit.HOURS)
                    .setConstraints(network)
                    .build(),
            )
            // Also fetch soon after launch so a fresh install has rates.
            WorkManager.getInstance(context).enqueueUniqueWork(
                INITIAL,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<FxRefreshWorker>()
                    .setConstraints(network)
                    .build(),
            )
        }
    }
}
