package ani.dantotsu.notifications.anilist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ani.dantotsu.notifications.AlarmManagerScheduler
import ani.dantotsu.notifications.TaskScheduler
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.Logger
import kotlinx.coroutines.runBlocking

class AnilistNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Logger.log("AnilistNotificationReceiver: onReceive")

        if (intent?.action == MARK_AS_READ_ACTION) {
            // Handle the mark as read action
            val notificationId = intent.getIntExtra("notificationId", -1)
            Logger.log("Marking notification with ID $notificationId as read...")
            NotificationManager.markNotificationAsRead(notificationId)
            return
        }

        runBlocking {
            AnilistNotificationTask().execute(context)
        }
        val anilistInterval =
            AnilistNotificationWorker.checkIntervals[PrefManager.getVal(PrefName.AnilistNotificationInterval)]
        AlarmManagerScheduler(context).scheduleRepeatingTask(
            TaskScheduler.TaskType.ANILIST_NOTIFICATION,
            anilistInterval
        )
    }

    companion object {
        const val MARK_AS_READ_ACTION = "ani.dantotsu.notifications.anilist.ACTION_MARK_AS_READ"
    }
}

object NotificationManager {
    private val readNotificationIds = mutableSetOf<Int>()

    fun markNotificationAsRead(notificationId: Int) {
        readNotificationIds.add(notificationId)
    }

    fun isNotificationRead(notificationId: Int): Boolean {
        return readNotificationIds.contains(notificationId)
    }
}
