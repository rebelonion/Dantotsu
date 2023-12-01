package ani.dantotsu.subcriptions

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import ani.dantotsu.FileUrl
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.UrlMedia
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("MemberVisibilityCanBePrivate", "unused")
class Notifications {
    enum class Group(val title: String, val icon: Int) {
        ANIME_GROUP("New Episodes", R.drawable.ic_round_movie_filter_24),
        MANGA_GROUP("New Chapters", R.drawable.ic_round_menu_book_24)
    }

    companion object {

        fun openSettings(context: Context, channelId: String?): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(
                    if (channelId != null) Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                    else Settings.ACTION_APP_NOTIFICATION_SETTINGS
                ).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                }
                context.startActivity(intent)
                true
            } else false
        }

        fun getIntent(context: Context, mediaId: Int): PendingIntent {
            val notifyIntent = Intent(context, UrlMedia::class.java)
                .putExtra("media", mediaId)
                .setAction(mediaId.toString())
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            return PendingIntent.getActivity(
                context, 0, notifyIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
                } else {
                    PendingIntent.FLAG_ONE_SHOT
                }
            )
        }

        fun createChannel(
            context: Context,
            group: Group?,
            id: String,
            name: String,
            silent: Boolean = false
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance =
                    if (!silent) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_LOW
                val mChannel = NotificationChannel(id, name, importance)

                val notificationManager =
                    context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                if (group != null) {
                    notificationManager.createNotificationChannelGroup(
                        NotificationChannelGroup(
                            group.name,
                            group.title
                        )
                    )
                    mChannel.group = group.name
                }

                notificationManager.createNotificationChannel(mChannel)
            }
        }

        fun deleteChannel(context: Context, id: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.deleteNotificationChannel(id)
            }
        }

        fun getNotification(
            context: Context,
            group: Group?,
            channelId: String,
            title: String,
            text: String?,
            silent: Boolean = false
        ): NotificationCompat.Builder {
            createChannel(context, group, channelId, title, silent)
            return NotificationCompat.Builder(context, channelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(group?.icon ?: R.drawable.monochrome)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
        }

        suspend fun getNotification(
            context: Context,
            group: Group?,
            channelId: String,
            title: String,
            text: String,
            img: FileUrl?,
            silent: Boolean = false,
            largeImg: FileUrl?
        ): NotificationCompat.Builder {
            val builder = getNotification(context, group, channelId, title, text, silent)
            return if (img != null) {
                val bitmap = withContext(Dispatchers.IO) {
                    Glide.with(context)
                        .asBitmap()
                        .load(GlideUrl(img.url) { img.headers })
                        .submit()
                        .get()
                }

                @Suppress("BlockingMethodInNonBlockingContext")
                val largeBitmap = if (largeImg != null) Glide.with(context)
                    .asBitmap()
                    .load(GlideUrl(largeImg.url) { largeImg.headers })
                    .submit()
                    .get()
                else null

                if (largeBitmap != null) builder.setStyle(
                    NotificationCompat
                        .BigPictureStyle()
                        .bigPicture(largeBitmap)
                        .bigLargeIcon(bitmap)
                )

                builder.setLargeIcon(bitmap)
            } else builder
        }

        suspend fun getNotification(
            context: Context,
            group: Group?,
            channelId: String,
            title: String,
            text: String,
            img: String? = null,
            silent: Boolean = false,
            largeImg: FileUrl? = null
        ): NotificationCompat.Builder {
            return getNotification(
                context,
                group,
                channelId,
                title,
                text,
                if (img != null) FileUrl(img) else null,
                silent,
                largeImg
            )
        }
    }
}