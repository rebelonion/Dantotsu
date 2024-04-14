package ani.dantotsu.notifications.comment

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ani.dantotsu.MainActivity
import ani.dantotsu.R
import ani.dantotsu.connections.comments.CommentsAPI
import ani.dantotsu.notifications.Task
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class CommentNotificationTask : Task {
    override suspend fun execute(context: Context): Boolean {
        try {
            withContext(Dispatchers.IO) {
                PrefManager.init(context) //make sure prefs are initialized
                val client = OkHttpClient()
                CommentsAPI.fetchAuthToken(client)
                val notificationResponse = CommentsAPI.getNotifications(client)
                var notifications = notificationResponse?.notifications?.toMutableList()
                //if we have at least one reply notification, we need to fetch the media titles
                var names = emptyMap<Int, MediaNameFetch.Companion.ReturnedData>()
                if (notifications?.any { it.type == 1 || it.type == null } == true) {
                    val mediaIds =
                        notifications.filter { it.type == 1 || it.type == null }.map { it.mediaId }
                    names = MediaNameFetch.fetchMediaTitles(mediaIds)
                }

                val recentGlobal = PrefManager.getVal<Int>(
                    PrefName.RecentGlobalNotification
                )

                notifications =
                    notifications?.filter { it.type != 3 || it.notificationId > recentGlobal }
                        ?.toMutableList()

                val newRecentGlobal =
                    notifications?.filter { it.type == 3 }?.maxOfOrNull { it.notificationId }
                if (newRecentGlobal != null) {
                    PrefManager.setVal(PrefName.RecentGlobalNotification, newRecentGlobal)
                }
                if (notifications.isNullOrEmpty()) return@withContext
                PrefManager.setVal(
                    PrefName.UnreadCommentNotifications,
                    PrefManager.getVal<Int>(PrefName.UnreadCommentNotifications) + (notifications.size
                        ?: 0)
                )

                notifications.forEach {
                    val type: CommentNotificationWorker.NotificationType = when (it.type) {
                        1 -> CommentNotificationWorker.NotificationType.COMMENT_REPLY
                        2 -> CommentNotificationWorker.NotificationType.COMMENT_WARNING
                        3 -> CommentNotificationWorker.NotificationType.APP_GLOBAL
                        420 -> CommentNotificationWorker.NotificationType.NO_NOTIFICATION
                        else -> CommentNotificationWorker.NotificationType.UNKNOWN
                    }
                    val notification = when (type) {
                        CommentNotificationWorker.NotificationType.COMMENT_WARNING -> {
                            val title = "You received a warning"
                            val message = it.content ?: "Be more thoughtful with your comments"

                            val commentStore = CommentStore(
                                title,
                                message,
                                it.mediaId,
                                it.commentId
                            )
                            addNotificationToStore(commentStore)

                            createNotification(
                                context,
                                CommentNotificationWorker.NotificationType.COMMENT_WARNING,
                                message,
                                title,
                                it.mediaId,
                                it.commentId,
                                "",
                                ""
                            )
                        }

                        CommentNotificationWorker.NotificationType.COMMENT_REPLY -> {
                            // Only sending notification for reply, mark as read functionality here
                            PrefManager.markAsRead(it.notificationId)
                            val title = "New Comment Reply"
                            val mediaName = names[it.mediaId]?.title ?: "Unknown"
                            val message = "${it.username} replied to your comment in $mediaName"

                            val commentStore = CommentStore(
                                title,
                                message,
                                it.mediaId,
                                it.commentId
                            )
                            addNotificationToStore(commentStore)

                            createNotification(
                                context,
                                CommentNotificationWorker.NotificationType.COMMENT_REPLY,
                                message,
                                title,
                                it.mediaId,
                                it.commentId,
                                names[it.mediaId]?.color ?: "#222222",
                                names[it.mediaId]?.coverImage ?: ""
                            )
                        }

                        CommentNotificationWorker.NotificationType.APP_GLOBAL -> {
                            val title = "Update from Dantotsu"
                            val message = it.content ?: "New feature available"

                            val commentStore = CommentStore(
                                title,
                                message,
                                null,
                                null
                            )
                            addNotificationToStore(commentStore)

                            createNotification(
                                context,
                                CommentNotificationWorker.NotificationType.APP_GLOBAL,
                                message,
                                title,
                                0,
                                0,
                                "",
                                ""
                            )
                        }

                        CommentNotificationWorker.NotificationType.NO_NOTIFICATION -> {
                            PrefManager.removeCustomVal("genre_thumb")
                            PrefManager.removeCustomVal("banner_ANIME_time")
                            PrefManager.removeCustomVal("banner_MANGA_time")
                            PrefManager.setVal(PrefName.ImageUrl, it.content ?: "")
                            null
                        }

                        CommentNotificationWorker.NotificationType.UNKNOWN -> {
                            null
                        }
                    }

                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                                                if (notification != null) {
                            with(NotificationManagerCompat.from(context)) {
                                notify(
                                    (System.currentTimeMillis() / 1000).toInt(),
                                    notification
                                )
                            }
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Logger.log("CommentNotificationTask: ${e.message}")
            Logger.log(e)
            return false
        }
    }

    private fun addNotificationToStore(commentStore: CommentStore) {
        val notificationsList = PrefManager.getVal<String>(PrefName.NotificationStore).toMutableList()
        notificationsList.add(commentStore.toString())
        PrefManager.setVal(PrefName.NotificationStore, notificationsList.joinToString(","))
    }

    private fun createNotification(
        context: Context,
        type: CommentNotificationWorker.NotificationType,
        message: String,
        title: String,
        mediaId: Int,
        commentId: Int,
        color: String,
        imageUrl: String
    ): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("mediaId", mediaId)
            putExtra("commentId", commentId)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val notificationBuilder = NotificationCompat.Builder(context, "CommentChannel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        return when (type) {
            CommentNotificationWorker.NotificationType.COMMENT_WARNING -> {
                notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
            }
            CommentNotificationWorker.NotificationType.COMMENT_REPLY -> {
                notificationBuilder
                    .setColor(Color.parseColor(color))
                    .setLargeIcon(BitmapFactory.decodeFile(imageUrl))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            }
            CommentNotificationWorker.NotificationType.APP_GLOBAL -> {
                notificationBuilder
                    .setColor(ContextCompat.getColor(context, R.color.purple_500))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            }
            else -> notificationBuilder
        }
    }
}
