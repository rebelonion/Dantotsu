package ani.dantotsu.notifications.subscription

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ani.dantotsu.notifications.anilist.AnilistNotificationTask
import ani.dantotsu.util.Logger

class SubscriptionNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Logger.log("SubscriptionNotificationWorker: doWork")
        return if (AnilistNotificationTask().execute(applicationContext)) {
            Result.success()
        } else {
            Logger.log("SubscriptionNotificationWorker: doWork failed")
            Result.retry()
        }
    }

    companion object {
        val checkIntervals = arrayOf(0L, 480, 720, 1440)
        const val WORK_NAME =
            "ani.dantotsu.notifications.subscription.SubscriptionNotificationWorker"
    }
}