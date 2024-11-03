package ani.dantotsu.connections.discord.serializers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Activity(
    @SerialName("application_id")
    val applicationId: String? = null,
    val name: String? = null,
    val details: String? = null,
    val state: String? = null,
    val type: Int? = null,
    val timestamps: Timestamps? = null,
    val assets: Assets? = null,
    val buttons: List<String>? = null,
    val metadata: Metadata? = null
) {
    @Serializable
    data class Assets(
        @SerialName("large_image")
        val largeImage: String? = null,

        @SerialName("large_text")
        val largeText: String? = null,

        @SerialName("small_image")
        val smallImage: String? = null,

        @SerialName("small_text")
        val smallText: String? = null
    )

    @Serializable
    data class Metadata(
        @SerialName("button_urls")
        val buttonUrls: List<String>
    )

    @Serializable
    data class Timestamps(
        val start: Long? = null,
        @SerialName("end")
        val stop: Long? = null
    )
}