package ani.dantotsu.notifications.anilist

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ani.dantotsu.MainActivity
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.notifications.Task
import ani.dantotsu.profile.activity.ActivityItemBuilder
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.data.notification.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnilistNotificationTask : Task {
    override suspend fun execute(context: Context): Boolean {
        try {
            withContext(Dispatchers.IO) {
                PrefManager.init(context) //make sure prefs are initialized
                val userId = PrefManager.getVal<String>(PrefName.AnilistUserId)
                if (userId.isNotEmpty()) {
                    Anilist.getSavedToken()
                    val res = Anilist.query.getNotifications(
                        userId.toInt(),
                        resetNotification = false
                    )
                    val unreadNotificationCount = res?.data?.user?.unreadNotificationCount ?: 0
                    if (unreadNotificationCount > 0) {
                        val unreadNotifications =
                            res?.data?.page?.notifications?.sortedBy { it.id }
                                ?.takeLast(unreadNotificationCount)
                        val lastId = PrefManager.getVal<Int>(PrefName.LastAnilistNotificationId)
                        val newNotifications = unreadNotifications?.filter { it.id > lastId }
                        val filteredTypes =
                            PrefManager.getVal<Set<String>>(PrefName.AnilistFilteredTypes)
                        newNotifications?.forEach {
                            if (!filteredTypes.contains(it.notificationType)) {
                                val content = ActivityItemBuilder.getContent(it)
                                val notification = createNotification(context, content, it.id)
                                if (ActivityCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    NotificationManagerCompat.from(context)
                                        .notify(
                                            Notifications.CHANNEL_ANILIST,
                                            System.currentTimeMillis().toInt(),
                                            notification
                                        )
                                }
                            }
                        }
                        if (newNotifications?.isNotEmpty() == true) {
                            PrefManager.setVal(
                                PrefName.LastAnilistNotificationId,
                                newNotifications.last().id
                            )
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Logger.log("AnilistNotificationTask: ${e.message}")
            Logger.log(e)
            return false
        }
    }

    private fun createNotification(
        context: Context,
        content: String,
        notificationId: Int? = null
    ): android.app.Notification {
        val title = "New Anilist Notification"
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("FRAGMENT_TO_LOAD", "NOTIFICATIONS")
            if (notificationId != null) {
                Logger.log("notificationId: $notificationId")
                putExtra("activityId", notificationId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId ?: 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, Notifications.CHANNEL_ANILIST)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

}