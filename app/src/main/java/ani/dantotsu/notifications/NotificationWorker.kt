package ani.dantotsu.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import ani.dantotsu.R
import ani.dantotsu.connections.comments.CommentsAPI
import ani.dantotsu.media.MediaDetailsActivity
import eu.kanade.tachiyomi.data.notification.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val notifications = CommentsAPI.getNotifications()
            val mediaIds = notifications?.notifications?.map { it.mediaId }
            val names = MediaNameFetch.fetchMediaTitles(mediaIds ?: emptyList())
            notifications?.notifications?.forEach {
                val title = "New Comment Reply"
                val mediaName = names[it.mediaId] ?: "Unknown"
                val message = "${it.username} replied to your comment in $mediaName"
                val notification = createNotification(
                    NotificationType.COMMENT_REPLY,
                    message,
                    title,
                    it.mediaId,
                    it.commentId
                )

                if (ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    NotificationManagerCompat.from(applicationContext)
                        .notify(
                            NotificationType.COMMENT_REPLY.id,
                            Notifications.ID_COMMENT_REPLY,
                            notification
                        )
                }

            }
        }
        return Result.success()
    }

    private fun createNotification(
        notificationType: NotificationType,
        message: String,
        title: String,
        mediaId: Int,
        commentId: Int
    ): android.app.Notification {
        val notification = when (notificationType) {
            NotificationType.COMMENT_REPLY -> {
                val intent = Intent(applicationContext, MediaDetailsActivity::class.java).apply {
                    putExtra("FRAGMENT_TO_LOAD", "COMMENTS")
                    putExtra("mediaId", mediaId)
                    putExtra("commentId", commentId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val builder = NotificationCompat.Builder(applicationContext, notificationType.id)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(R.drawable.ic_round_comment_24)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                builder.build()
            }
        }
        return notification
    }

    enum class NotificationType(val id: String) {
        COMMENT_REPLY(Notifications.CHANNEL_COMMENTS),
    }

    companion object {
        const val WORK_NAME = "ani.dantotsu.notifications.NotificationWorker"
    }
}