package ani.dantotsu.notifications.comment

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ani.dantotsu.util.Logger

class CommentNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Logger.log("CommentNotificationWorker: doWork")
        return if (CommentNotificationTask().execute(applicationContext)) {
            Result.success()
        } else {
            Logger.log("CommentNotificationWorker: doWork failed")
            Result.retry()
        }
    }

    companion object {
        val checkIntervals = arrayOf(0L, 480, 720, 1440)
        const val WORK_NAME = "ani.dantotsu.notifications.comment.CommentNotificationWorker"
    }
}
