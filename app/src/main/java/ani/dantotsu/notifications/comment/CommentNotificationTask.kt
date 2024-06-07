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
import ani.dantotsu.settings.saving.PrefName
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
                CommentsAPI.fetchAuthToken(context, client)
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
                    notifications?.filter { !it.type.isGlobal() || it.notificationId > recentGlobal }
                        ?.toMutableList()

                val newRecentGlobal =
                    notifications?.filter { it.type.isGlobal() }?.maxOfOrNull { it.notificationId }
                if (newRecentGlobal != null) {
                    PrefManager.setVal(PrefName.RecentGlobalNotification, newRecentGlobal)
                }
                if (notifications.isNullOrEmpty()) return@withContext
                PrefManager.setVal(
                    PrefName.UnreadCommentNotifications,
                    PrefManager.getVal<Int>(PrefName.UnreadCommentNotifications) + (notifications.size)
                )

                notifications.forEach {
                    val type: CommentNotificationWorker.NotificationType = when (it.type) {
                        1 -> CommentNotificationWorker.NotificationType.COMMENT_REPLY
                        2 -> CommentNotificationWorker.NotificationType.COMMENT_WARNING
                        3 -> CommentNotificationWorker.NotificationType.DANTOTSU_UPDATE
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
                                CommentNotificationWorker.NotificationType.COMMENT_WARNING,
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
                            val title = "New Comment Reply"
                            val mediaName = names[it.mediaId]?.title ?: "Unknown"
                            val message = "${it.username} replied to your comment in $mediaName"

                            val commentStore = CommentStore(
                                title,
                                message,
                                CommentNotificationWorker.NotificationType.COMMENT_REPLY,
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

                        CommentNotificationWorker.NotificationType.DANTOTSU_UPDATE -> {
                            val title = "Update from Dantotsu"
                            val message = it.content ?: "New feature available"

                            val commentStore = CommentStore(
                                title,
                                message,
                                CommentNotificationWorker.NotificationType.DANTOTSU_UPDATE,
                                null,
                                null
                            )
                            addNotificationToStore(commentStore)

                            createNotification(
                                context,
                                CommentNotificationWorker.NotificationType.DANTOTSU_UPDATE,
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
                            NotificationManagerCompat.from(context)
                                .notify(
                                    type.id,
                                    System.currentTimeMillis().toInt(),
                                    notification
                                )
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

    private fun addNotificationToStore(notification: CommentStore) {
        val notificationStore = PrefManager.getNullableVal<List<CommentStore>>(
            PrefName.CommentNotificationStore,
            null
        ) ?: listOf()
        val newStore = notificationStore.toMutableList()
        if (newStore.size > 30) {
            newStore.remove(newStore.minByOrNull { it.time })
        }
        if (newStore.any { it.content == notification.content }) {
            return
        }
        newStore.add(notification)
        PrefManager.setVal(PrefName.CommentNotificationStore, newStore)
    }

    private fun createNotification(
        context: Context,
        notificationType: CommentNotificationWorker.NotificationType,
        message: String,
        title: String,
        mediaId: Int,
        commentId: Int,
        color: String,
        imageUrl: String
    ): android.app.Notification? {
        Logger.log(
            "Creating notification of type $notificationType" +
                    ", message: $message, title: $title, mediaId: $mediaId, commentId: $commentId"
        )
        val notification = when (notificationType) {
            CommentNotificationWorker.NotificationType.COMMENT_WARNING -> {
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("FRAGMENT_TO_LOAD", "COMMENTS")
                    putExtra("mediaId", mediaId)
                    putExtra("commentId", commentId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    commentId,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val builder = NotificationCompat.Builder(context, notificationType.id)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                builder.build()
            }

            CommentNotificationWorker.NotificationType.COMMENT_REPLY -> {
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("FRAGMENT_TO_LOAD", "COMMENTS")
                    putExtra("mediaId", mediaId)
                    putExtra("commentId", commentId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    commentId,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val builder = NotificationCompat.Builder(context, notificationType.id)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                if (imageUrl.isNotEmpty()) {
                    val bitmap = getBitmapFromUrl(imageUrl)
                    if (bitmap != null) {
                        builder.setLargeIcon(bitmap)
                    }
                }
                if (color.isNotEmpty()) {
                    builder.color = Color.parseColor(color)
                }
                builder.build()
            }

            CommentNotificationWorker.NotificationType.DANTOTSU_UPDATE -> {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val builder = NotificationCompat.Builder(context, notificationType.id)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                builder.build()
            }

            else -> {
                null
            }
        }
        return notification
    }

    @Suppress("unused")
    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun getBitmapFromUrl(url: String): Bitmap? {
        return try {
            val inputStream = java.net.URL(url).openStream()
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    private fun Int?.isGlobal() = this == 3 || this == 420
}