package ani.dantotsu.connections.anilist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class NotificationType(val value: String) {
    ACTIVITY_MESSAGE("ACTIVITY_MESSAGE"),
    ACTIVITY_REPLY("ACTIVITY_REPLY"),
    FOLLOWING("FOLLOWING"),
    ACTIVITY_MENTION("ACTIVITY_MENTION"),
    THREAD_COMMENT_MENTION("THREAD_COMMENT_MENTION"),
    THREAD_SUBSCRIBED("THREAD_SUBSCRIBED"),
    THREAD_COMMENT_REPLY("THREAD_COMMENT_REPLY"),
    AIRING("AIRING"),
    ACTIVITY_LIKE("ACTIVITY_LIKE"),
    ACTIVITY_REPLY_LIKE("ACTIVITY_REPLY_LIKE"),
    THREAD_LIKE("THREAD_LIKE"),
    THREAD_COMMENT_LIKE("THREAD_COMMENT_LIKE"),
    ACTIVITY_REPLY_SUBSCRIBED("ACTIVITY_REPLY_SUBSCRIBED"),
    RELATED_MEDIA_ADDITION("RELATED_MEDIA_ADDITION"),
    MEDIA_DATA_CHANGE("MEDIA_DATA_CHANGE"),
    MEDIA_MERGE("MEDIA_MERGE"),
    MEDIA_DELETION("MEDIA_DELETION")
}

@Serializable
data class NotificationResponse(
    @SerialName("data")
    val data: Data,
) : java.io.Serializable {
    @Serializable
    data class Data(
        @SerialName("User")
        val user: NotificationUser,
        @SerialName("Page")
        val page: NotificationPage,
    ) : java.io.Serializable
}

@Serializable
data class NotificationUser(
    @SerialName("unreadNotificationCount")
    val unreadNotificationCount: Int,
) : java.io.Serializable

@Serializable
data class NotificationPage(
    @SerialName("notifications")
    val notifications: List<Notification>,
) : java.io.Serializable

@Serializable
data class Notification(
    @SerialName("__typename")
    val typename: String,
    @SerialName("id")
    val id: Int,
    @SerialName("userId")
    val userId: Int?,
    @SerialName("CommentId")
    val commentId: Int?,
    @SerialName("type")
    val notificationType: String,
    @SerialName("activityId")
    val activityId: Int?,
    @SerialName("animeId")
    val mediaId: Int?,
    @SerialName("episode")
    val episode: Int?,
    @SerialName("contexts")
    val contexts: List<String>?,
    @SerialName("context")
    val context: String?,
    @SerialName("reason")
    val reason: String?,
    @SerialName("deletedMediaTitle")
    val deletedMediaTitle: String?,
    @SerialName("deletedMediaTitles")
    val deletedMediaTitles: List<String>?,
    @SerialName("createdAt")
    val createdAt: Int,
    @SerialName("media")
    val media: ani.dantotsu.connections.anilist.api.Media?,
    @SerialName("user")
    val user: ani.dantotsu.connections.anilist.api.User?,
    @SerialName("message")
    val message: MessageActivity?,
    @SerialName("activity")
    val activity: ActivityUnion?,
    @SerialName("Thread")
    val thread: Thread?,
    @SerialName("comment")
    val comment: ThreadComment?,
) : java.io.Serializable

@Serializable
data class MessageActivity(
    @SerialName("id")
    val id: Int?,
) : java.io.Serializable

@Serializable
data class ActivityUnion(
    @SerialName("id")
    val id: Int?,
) : java.io.Serializable

@Serializable
data class Thread(
    @SerialName("id")
    val id: Int?,
) : java.io.Serializable

@Serializable
data class ThreadComment(
    @SerialName("id")
    val id: Int?,
) : java.io.Serializable
