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

                notifications.forEach {
                    val type: CommentNotificationWorker.NotificationType = when (it.type) {
                        1 -> CommentNotificationWorker.NotificationType.COMMENT_REPLY
                        2 -> CommentNotificationWorker.NotificationType.COMMENT_WARNING
                        3 -> {
                            val newRecentGlobal = it.notificationId
                            PrefManager.setVal(PrefName.RecentGlobalNotification, newRecentGlobal)
                            null
                        }
                        420 -> CommentNotificationWorker.NotificationType.NO_NOTIFICATION
                        else -> CommentNotificationWorker.NotificationType.UNKNOWN
                    }

                    // Increment count only if the notification type is not null and not a global message notification
                    if (type != null && type != CommentNotificationWorker.NotificationType.APP_GLOBAL) {
                        PrefManager.setVal(
                            PrefName.UnreadCommentNotifications,
                            PrefManager.getVal<Int>(PrefName.UnreadCommentNotifications) + 1
                        )
                    }

                    // Create and send the notification for non-global message notification types
                    if (type != CommentNotificationWorker.NotificationType.APP_GLOBAL) {
                        val notification = createNotification(
                            context,
                            type,
                            it.content ?: "",
                            getTitle(type),
                            it.mediaId,
                            it.commentId,
                            getColor(type, names[it.mediaId]?.color ?: ""),
                            names[it.mediaId]?.coverImage ?: ""
                        )
                        if (notification != null) {
                            NotificationManagerCompat.from(context)
                                .notify(type.id, System.currentTimeMillis().toInt(), notification)
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

    private fun getTitle(type: CommentNotificationWorker.NotificationType): String {
        return when (type) {
            CommentNotificationWorker.NotificationType.COMMENT_WARNING -> "You received a warning"
            CommentNotificationWorker.NotificationType.COMMENT_REPLY -> "New Comment Reply"
            CommentNotificationWorker.NotificationType.APP_GLOBAL -> "Update from Dantotsu"
            CommentNotificationWorker.NotificationType.NO_NOTIFICATION -> ""
            CommentNotificationWorker.NotificationType.UNKNOWN -> ""
        }
    }

    private fun getColor(type: CommentNotificationWorker.NotificationType, defaultColor: String): String {
        return when (type) {
            CommentNotificationWorker.NotificationType.COMMENT_REPLY -> defaultColor
            else -> ""
        }
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

            CommentNotificationWorker.NotificationType.APP_GLOBAL -> {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    System                 
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
}
