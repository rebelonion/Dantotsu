package ani.dantotsu.notifications.subscription

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ani.dantotsu.App
import ani.dantotsu.FileUrl
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.UrlMedia
import ani.dantotsu.hasNotificationPermission
import ani.dantotsu.notifications.Task
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.parsers.Episode
import ani.dantotsu.parsers.MangaChapter
import ani.dantotsu.parsers.MangaSources
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.data.notification.Notifications.CHANNEL_SUBSCRIPTION_CHECK
import eu.kanade.tachiyomi.data.notification.Notifications.CHANNEL_SUBSCRIPTION_CHECK_PROGRESS
import eu.kanade.tachiyomi.data.notification.Notifications.ID_SUBSCRIPTION_CHECK_PROGRESS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SubscriptionNotificationTask : Task {
    private var currentlyPerforming = false

    @SuppressLint("MissingPermission")
    override suspend fun execute(context: Context): Boolean {
        if (!currentlyPerforming) {
            try {
                withContext(Dispatchers.IO) {
                    PrefManager.init(context)
                    currentlyPerforming = true
                    App.context = context
                    Logger.log("SubscriptionNotificationTask: execute")
                    var timeout = 15_000L
                    do {
                        delay(1000)
                        timeout -= 1000
                    } while (timeout > 0 && !AnimeSources.isInitialized && !MangaSources.isInitialized)
                    Logger.log("SubscriptionNotificationTask: timeout: $timeout")
                    if (timeout <= 0) {
                        currentlyPerforming = false
                        return@withContext
                    }
                    val subscriptions = SubscriptionHelper.getSubscriptions()
                    var i = 0
                    val index = subscriptions.map { i++; it.key to i }.toMap()
                    val notificationManager = NotificationManagerCompat.from(context)

                    val progressEnabled: Boolean =
                        PrefManager.getVal(PrefName.SubscriptionCheckingNotifications)
                    val progressNotification = if (progressEnabled) getProgressNotification(
                        context,
                        subscriptions.size
                    ) else null
                    if (progressNotification != null && hasNotificationPermission(context)) {
                        notificationManager.notify(
                            ID_SUBSCRIPTION_CHECK_PROGRESS,
                            progressNotification.build()
                        )
                        //Seems like if the parent coroutine scope gets cancelled, the notification stays
                        //So adding this as a safeguard? dk if this will be useful
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(5 * subscriptions.size * 1000L)
                            notificationManager.cancel(ID_SUBSCRIPTION_CHECK_PROGRESS)
                        }
                    }

                    fun progress(progress: Int, parser: String, media: String) {
                        if (progressNotification != null && hasNotificationPermission(context))
                            notificationManager.notify(
                                ID_SUBSCRIPTION_CHECK_PROGRESS,
                                progressNotification
                                    .setProgress(subscriptions.size, progress, false)
                                    .setContentText("$media on $parser")
                                    .build()
                            )
                    }

                    subscriptions.toList().map {
                        val media = it.second
                        val text = if (media.isAnime) {
                            val parser =
                                SubscriptionHelper.getAnimeParser(media.id)
                            progress(index[it.first]!!, parser.name, media.name)
                            val ep: Episode? =
                                SubscriptionHelper.getEpisode(
                                    parser,
                                    media
                                )
                            if (ep != null) context.getString(R.string.episode) + "${ep.number}${
                                if (ep.title != null) " : ${ep.title}" else ""
                            }${
                                if (ep.isFiller) " [Filler]" else ""
                            } " + context.getString(R.string.just_released) to ep.thumbnail
                            else null
                        } else {
                            val parser =
                                SubscriptionHelper.getMangaParser(media.id)
                            progress(index[it.first]!!, parser.name, media.name)
                            val ep: MangaChapter? =
                                SubscriptionHelper.getChapter(
                                    parser,
                                    media
                                )
                            if (ep != null) ep.number + " " + context.getString(R.string.just_released) to null
                            else null
                        } ?: return@map
                        addSubscriptionToStore(
                            SubscriptionStore(
                                media.name,
                                text.first,
                                media.id,
                                image = media.image,
                                banner = media.banner
                            )
                        )
                        PrefManager.setVal(
                            PrefName.UnreadCommentNotifications,
                            PrefManager.getVal<Int>(PrefName.UnreadCommentNotifications) + 1
                        )
                        val notification = createNotification(
                            context.applicationContext,
                            media,
                            text.first,
                            text.second
                        )
                        if (hasNotificationPermission(context)) {
                            NotificationManagerCompat.from(context)
                                .notify(
                                    CHANNEL_SUBSCRIPTION_CHECK,
                                    System.currentTimeMillis().toInt(),
                                    notification
                                )
                        }
                    }

                    if (progressNotification != null) notificationManager.cancel(
                        ID_SUBSCRIPTION_CHECK_PROGRESS
                    )
                    currentlyPerforming = false
                }
                return true
            } catch (e: Exception) {
                Logger.log("SubscriptionNotificationTask: ${e.message}")
                Logger.log(e)
                return false
            }
        } else {
            return false
        }
    }

    @SuppressLint("MissingPermission")
    private fun createNotification(
        context: Context,
        media: SubscriptionHelper.Companion.SubscribeMedia,
        text: String,
        thumbnail: FileUrl?
    ): android.app.Notification {
        val pendingIntent = getIntent(context, media.id)
        val icon =
            if (media.isAnime) R.drawable.ic_round_movie_filter_24 else R.drawable.ic_round_menu_book_24

        val builder = NotificationCompat.Builder(context, CHANNEL_SUBSCRIPTION_CHECK)
            .setSmallIcon(icon)
            .setContentTitle(media.name)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (thumbnail != null) {
            val bitmap = getBitmapFromUrl(thumbnail.url)
            if (bitmap != null) {
                builder.setLargeIcon(bitmap)
            }
        }

        return builder.build()

    }

    private fun getProgressNotification(
        context: Context,
        size: Int
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_SUBSCRIPTION_CHECK_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(context.getString(R.string.checking_subscriptions_title))
            .setProgress(size, 0, false)
            .setOngoing(true)
            .setAutoCancel(false)
    }

    private fun getBitmapFromUrl(url: String): Bitmap? {
        return try {
            val inputStream = java.net.URL(url).openStream()
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }


    private fun getIntent(context: Context, mediaId: Int): PendingIntent {
        val notifyIntent = Intent(context, UrlMedia::class.java)
            .putExtra("media", mediaId)
            .setAction(mediaId.toString())
            .apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        return PendingIntent.getActivity(
            context, mediaId, notifyIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            } else {
                PendingIntent.FLAG_ONE_SHOT
            }
        )
    }

    private fun addSubscriptionToStore(notification: SubscriptionStore) {
        val notificationStore = PrefManager.getNullableVal<List<SubscriptionStore>>(
            PrefName.SubscriptionNotificationStore,
            null
        ) ?: listOf()
        val newStore = notificationStore.toMutableList()
        if (newStore.size >= 100) {
            newStore.remove(newStore.minByOrNull { it.time })
        }
        if (newStore.any { it.title == notification.title && it.content == notification.content }) {
            return
        }

        newStore.add(notification)
        PrefManager.setVal(PrefName.SubscriptionNotificationStore, newStore)
    }
}