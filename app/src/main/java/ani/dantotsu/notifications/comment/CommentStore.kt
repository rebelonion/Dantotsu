package ani.dantotsu.notifications.comment

import kotlinx.serialization.Serializable


@Serializable
data class CommentStore(
    val title: String,
    val content: String,
    val type: CommentNotificationWorker.NotificationType,
    val mediaId: Int? = null,
    val commentId: Int? = null,
    val time: Long = System.currentTimeMillis(),
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID = 2L
    }
}