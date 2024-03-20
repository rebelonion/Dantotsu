package ani.dantotsu.notifications.anilist

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ani.dantotsu.util.Logger

class AnilistNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Logger.log("AnilistNotificationWorker: doWork")
        return if (AnilistNotificationTask().execute(applicationContext)) {
            Result.success()
        } else {
            Logger.log("AnilistNotificationWorker: doWork failed")
            Result.retry()
        }
    }

    companion object {
        val checkIntervals = arrayOf(0L, 30, 60, 120, 240, 360, 720, 1440)
        const val WORK_NAME = "ani.dantotsu.notifications.anilist.AnilistNotificationWorker"
    }
}