package ani.dantotsu.notifications.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionStore(
    val title: String,
    val content: String,
    val mediaId: Int,
    val type: String = "SUBSCRIPTION",
    val time: Long = System.currentTimeMillis(),
    val image: String? = "",
    val banner: String? = "",
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}