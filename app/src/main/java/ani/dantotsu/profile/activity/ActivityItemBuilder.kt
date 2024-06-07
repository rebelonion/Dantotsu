package ani.dantotsu.profile.activity

import ani.dantotsu.connections.anilist.api.Notification
import ani.dantotsu.connections.anilist.api.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityItemBuilder {

    companion object {
        fun getContent(notification: Notification): String {
            val notificationType: NotificationType =
                NotificationType.valueOf(notification.notificationType)
            return when (notificationType) {
                NotificationType.ACTIVITY_MESSAGE -> {
                    "${notification.user?.name} sent you a message"
                }

                NotificationType.ACTIVITY_REPLY -> {
                    "${notification.user?.name} replied to your activity"
                }

                NotificationType.FOLLOWING -> {
                    "${notification.user?.name} followed you"
                }

                NotificationType.ACTIVITY_MENTION -> {
                    "${notification.user?.name} mentioned you in their activity"
                }

                NotificationType.THREAD_COMMENT_MENTION -> {
                    "${notification.user?.name} mentioned you in a forum comment"
                }

                NotificationType.THREAD_SUBSCRIBED -> {
                    "${notification.user?.name} commented in one of your subscribed forum threads"
                }

                NotificationType.THREAD_COMMENT_REPLY -> {
                    "${notification.user?.name} replied to your forum comment"
                }

                NotificationType.AIRING -> {
                    "Episode ${notification.episode} of ${notification.media?.title?.english ?: notification.media?.title?.romaji} has aired"
                }

                NotificationType.ACTIVITY_LIKE -> {
                    "${notification.user?.name} liked your activity"
                }

                NotificationType.ACTIVITY_REPLY_LIKE -> {
                    "${notification.user?.name} liked your reply"
                }

                NotificationType.THREAD_LIKE -> {
                    "${notification.user?.name} liked your forum thread"
                }

                NotificationType.THREAD_COMMENT_LIKE -> {
                    "${notification.user?.name} liked your forum comment"
                }

                NotificationType.ACTIVITY_REPLY_SUBSCRIBED -> {
                    "${notification.user?.name} replied to activity you have also replied to"
                }

                NotificationType.RELATED_MEDIA_ADDITION -> {
                    "${notification.media?.title?.english ?: notification.media?.title?.romaji} has been added to the site"
                }

                NotificationType.MEDIA_DATA_CHANGE -> {
                    "${notification.media?.title?.english ?: notification.media?.title?.romaji} has had a data change: ${notification.reason}"
                }

                NotificationType.MEDIA_MERGE -> {
                    "${notification.deletedMediaTitles?.joinToString(", ")} have been merged into ${notification.media?.title?.english ?: notification.media?.title?.romaji}"
                }

                NotificationType.MEDIA_DELETION -> {
                    "${notification.deletedMediaTitle} has been deleted from the site"
                }

                NotificationType.COMMENT_REPLY -> {
                    notification.context ?: "You should not see this"
                }

                NotificationType.COMMENT_WARNING -> {
                    notification.context ?: "You should not see this"
                }

                NotificationType.DANTOTSU_UPDATE -> {
                    notification.context ?: "You should not see this"
                }

                NotificationType.SUBSCRIPTION -> {
                    notification.context ?: "You should not see this"
                }
            }
        }


        fun getDateTime(timestamp: Int): String {

            val targetDate = Date(timestamp * 1000L)

            if (targetDate < Date(946684800000L)) { // January 1, 2000 (who want dates before that?)
                return ""
            }

            val currentDate = Date()
            val difference = currentDate.time - targetDate.time

            return when (val daysDifference = difference / (1000 * 60 * 60 * 24)) {
                0L -> {
                    val hoursDifference = difference / (1000 * 60 * 60)
                    val minutesDifference = (difference / (1000 * 60)) % 60

                    when {
                        hoursDifference > 0 -> "$hoursDifference hour${if (hoursDifference > 1) "s" else ""} ago"
                        minutesDifference > 0 -> "$minutesDifference minute${if (minutesDifference > 1) "s" else ""} ago"
                        else -> "Just now"
                    }
                }

                1L -> "1 day ago"
                in 2..6 -> "$daysDifference days ago"
                else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(targetDate)
            }
        }
    }
}