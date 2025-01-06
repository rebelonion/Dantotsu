package ani.dantotsu.connections.anilist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedResponse(
    @SerialName("data")
    val data: Data
) : java.io.Serializable {
    @Serializable
    data class Data(
        @SerialName("Page")
        val page: ActivityPage
    ) : java.io.Serializable
}

@Serializable
data class ActivityPage(
    @SerialName("activities")
    val activities: List<Activity>
) : java.io.Serializable

@Serializable
data class Activity(
    @SerialName("__typename")
    val typename: String,
    @SerialName("id")
    val id: Int,
    @SerialName("recipientId")
    val recipientId: Int?,
    @SerialName("messengerId")
    val messengerId: Int?,
    @SerialName("userId")
    val userId: Int?,
    @SerialName("type")
    val type: String,
    @SerialName("replyCount")
    val replyCount: Int = 0,
    @SerialName("status")
    val status: String?,
    @SerialName("progress")
    val progress: String?,
    @SerialName("text")
    val text: String?,
    @SerialName("message")
    val message: String?,
    @SerialName("siteUrl")
    val siteUrl: String?,
    @SerialName("isLocked")
    val isLocked: Boolean?,
    @SerialName("isSubscribed")
    val isSubscribed: Boolean?,
    @SerialName("likeCount")
    var likeCount: Int?,
    @SerialName("isLiked")
    var isLiked: Boolean?,
    @SerialName("isPinned")
    val isPinned: Boolean?,
    @SerialName("isPrivate")
    val isPrivate: Boolean?,
    @SerialName("createdAt")
    val createdAt: Int,
    @SerialName("user")
    val user: User?,
    @SerialName("recipient")
    val recipient: User?,
    @SerialName("messenger")
    val messenger: User?,
    @SerialName("media")
    val media: Media?,
    @SerialName("replies")
    val replies: List<ActivityReply>?,
    @SerialName("likes")
    val likes: List<User>?,
) : java.io.Serializable

@Serializable
data class ReplyResponse(
    @SerialName("data")
    val data: Data
) : java.io.Serializable {
    @Serializable
    data class Data(
        @SerialName("Page")
        val page: ReplyPage
    ) : java.io.Serializable
}

@Serializable
data class ReplyPage(
    @SerialName("activityReplies")
    val activityReplies: List<ActivityReply>
) : java.io.Serializable

@Serializable
data class ActivityReply(
    @SerialName("id")
    val id: Int,
    @SerialName("userId")
    val userId: Int,
    @SerialName("text")
    val text: String,
    @SerialName("likeCount")
    var likeCount: Int,
    @SerialName("isLiked")
    var isLiked: Boolean,
    @SerialName("createdAt")
    val createdAt: Int,
    @SerialName("user")
    val user: User,
    @SerialName("likes")
    val likes: List<User>?,
) : java.io.Serializable

@Serializable
data class ToggleLike(
    @SerialName("data")
    val data: Data
) : java.io.Serializable {
    @Serializable
    data class Data(
        @SerialName("ToggleLikeV2")
        val toggleLike: LikeData
    ) : java.io.Serializable
}

@Serializable
data class LikeData(
    @SerialName("__typename")
    val typename: String
) : java.io.Serializable