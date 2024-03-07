package ani.dantotsu.profile.activity

import ani.dantotsu.connections.anilist.api.Notification
import ani.dantotsu.connections.anilist.api.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
* ACTIVITY_MESSAGE

A user has sent you message
ACTIVITY_REPLY

A user has replied to your activity
FOLLOWING

A user has followed you
ACTIVITY_MENTION

A user has mentioned you in their activity
THREAD_COMMENT_MENTION

A user has mentioned you in a forum comment
THREAD_SUBSCRIBED

A user has commented in one of your subscribed forum threads
THREAD_COMMENT_REPLY

A user has replied to your forum comment
AIRING

An anime you are currently watching has aired
ACTIVITY_LIKE

A user has liked your activity
ACTIVITY_REPLY_LIKE

A user has liked your activity reply
THREAD_LIKE

A user has liked your forum thread
THREAD_COMMENT_LIKE

A user has liked your forum comment
ACTIVITY_REPLY_SUBSCRIBED

A user has replied to activity you have also replied to
RELATED_MEDIA_ADDITION

A new anime or manga has been added to the site where its related media is on the user's list
MEDIA_DATA_CHANGE

An anime or manga has had a data change that affects how a user may track it in their lists
MEDIA_MERGE

Anime or manga entries on the user's list have been merged into a single entry
MEDIA_DELETION

An anime or manga on the user's list has been deleted from the site

* */

interface NotificationItemBuilder {

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
            }
        }

        fun getDateTime(time: Int): String {
            val date = Date(time * 1000L)
            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
            return sdf.format(date)
        }

    }
}