package ani.dantotsu.connections.anilist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedResponse(
    @SerialName("data")
    val data: Data
) {
    @Serializable
    data class Data(
        @SerialName("Page")
        val page: ActivityPage
    )
}

@Serializable
data class ActivityPage(
    @SerialName("activities")
    val activities: List<Activity>
)

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
    val replyCount: Int,
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
    val isLocked: Boolean,
    @SerialName("isSubscribed")
    val isSubscribed: Boolean,
    @SerialName("likeCount")
    val likeCount: Int?,
    @SerialName("isLiked")
    val isLiked: Boolean?,
    @SerialName("isPinned")
    val isPinned: Boolean?,
    @SerialName("isPrivate")
    val isPrivate: Boolean?,
    @SerialName("createdAt")
    val createdAt: Int,
    @SerialName("user")
    val user: User?,
    @SerialName("media")
    val media: Media?,
    @SerialName("replies")
    val replies: List<Reply>?,
    @SerialName("likes")
    val likes: List<User>?,
)

@Serializable
data class Reply(
    @SerialName("id")
    val id: Int,
    @SerialName("userId")
    val userId: Int,
    @SerialName("text")
    val text: String,
    @SerialName("likeCount")
    val likeCount: Int,
    @SerialName("isLiked")
    val isLiked: Boolean,
    @SerialName("createdAt")
    val createdAt: Int,
    @SerialName("user")
    val user: User,
    @SerialName("likes")
    val likes: List<User>?,
)