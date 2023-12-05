package ani.dantotsu.connections.discord.serializers

import kotlinx.serialization.Serializable

@Serializable
data class Presence(
    val activities: List<Activity> = listOf(),
    val afk: Boolean = true,
    val since: Long? = null,
    val status: String? = null
) {
    @Serializable
    data class Response(
        val op: Long,
        val d: Presence
    )
}


