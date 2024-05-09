package ani.dantotsu.notifications.comment

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.data.notification.Notifications


class CommentNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Logger.log("CommentNotificationWorker: doWork")
        if (System.currentTimeMillis() - lastCheck < 60000) {
            Logger.log("CommentNotificationWorker: doWork skipped")
            return Result.success()
        }
        lastCheck = System.currentTimeMillis()
        return if (CommentNotificationTask().execute(applicationContext)) {
            Result.success()
        } else {
            Logger.log("CommentNotificationWorker: doWork failed")
            Result.retry()
        }
    }

    enum class NotificationType(val id: String) {
        COMMENT_REPLY(Notifications.CHANNEL_COMMENTS),
        COMMENT_WARNING(Notifications.CHANNEL_COMMENT_WARING),
        DANTOTSU_UPDATE(Notifications.CHANNEL_APP_GLOBAL),
        NO_NOTIFICATION("no_notification"),
        UNKNOWN("unknown")
    }

    companion object {
        val checkIntervals = arrayOf(0L, 480, 720, 1440)
        const val WORK_NAME = "ani.dantotsu.notifications.comment.CommentNotificationWorker"
        private var lastCheck = 0L
    }
}