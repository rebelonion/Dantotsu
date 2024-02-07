package ani.dantotsu.subcriptions

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.subcriptions.Subscription.Companion.defaultTime
import ani.dantotsu.subcriptions.Subscription.Companion.timeMinutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SubscriptionWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {
            Subscription.perform(context)
        }
        return Result.success()
    }

    companion object {

        private const val SUBSCRIPTION_WORK_NAME = "work_subscription"
        fun enqueue(context: Context) {
            val curTime = PrefManager.getVal(PrefName.SubscriptionsTimeS, defaultTime)
            if (timeMinutes[curTime] > 0L) {
                val constraints =
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                val periodicSyncDataWork = PeriodicWorkRequest.Builder(
                    SubscriptionWorker::class.java, 6, TimeUnit.HOURS
                ).apply {
                    addTag(SUBSCRIPTION_WORK_NAME)
                    setConstraints(constraints)
                }.build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    SUBSCRIPTION_WORK_NAME,
                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                    periodicSyncDataWork
                )
            }
        }
    }
}