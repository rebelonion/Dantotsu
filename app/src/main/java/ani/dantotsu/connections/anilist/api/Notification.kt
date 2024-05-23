package ani.dantotsu.connections.anilist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

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
    MEDIA_DELETION("MEDIA_DELETION"),

    //custom
    COMMENT_REPLY("COMMENT_REPLY"),
    COMMENT_WARNING("COMMENT_WARNING"),
    DANTOTSU_UPDATE("DANTOTSU_UPDATE"),
    SUBSCRIPTION("SUBSCRIPTION");

    fun toFormattedString(): String {
        return this.value.replace("_", " ").lowercase(Locale.ROOT)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    companion object {
        fun String.fromFormattedString(): String {
            return this.replace(" ", "_").uppercase(Locale.ROOT)
        }
    }
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
    var unreadNotificationCount: Int,
) : java.io.Serializable

@Serializable
data class NotificationPage(
    @SerialName("pageInfo")
    val pageInfo: PageInfo,
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
    val userId: Int? = null,
    @SerialName("CommentId")
    val commentId: Int?,
    @SerialName("type")
    val notificationType: String,
    @SerialName("activityId")
    val activityId: Int? = null,
    @SerialName("animeId")
    val mediaId: Int? = null,
    @SerialName("episode")
    val episode: Int? = null,
    @SerialName("contexts")
    val contexts: List<String>? = null,
    @SerialName("context")
    val context: String? = null,
    @SerialName("reason")
    val reason: String? = null,
    @SerialName("deletedMediaTitle")
    val deletedMediaTitle: String? = null,
    @SerialName("deletedMediaTitles")
    val deletedMediaTitles: List<String>? = null,
    @SerialName("createdAt")
    val createdAt: Int,
    @SerialName("media")
    val media: Media? = null,
    @SerialName("user")
    val user: User? = null,
    @SerialName("message")
    val message: MessageActivity? = null,
    @SerialName("activity")
    val activity: ActivityUnion? = null,
    @SerialName("Thread")
    val thread: Thread? = null,
    @SerialName("comment")
    val comment: ThreadComment? = null,
    val image: String? = null,
    val banner: String? = null,
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
