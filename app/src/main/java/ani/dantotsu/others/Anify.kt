package ani.dantotsu.others

import ani.dantotsu.FileUrl
import ani.dantotsu.Mapper
import ani.dantotsu.client
import ani.dantotsu.media.anime.Episode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

object Anify {
    suspend fun fetchAndParseMetadata(id :Int): Map<String, Episode>? {
        val response = client.get("https://api.anify.tv/content-metadata/$id")
            .parsed<JsonArray>().map {
                Mapper.json.decodeFromJsonElement<ContentMetadata>(it)
            }
        return response.first().data.associate {
            it.number.toString() to Episode(
                number = it.number.toString(),
                title = it.title,
                desc = it.description,
                thumb = FileUrl[it.img],
                filler = it.isFiller,
            )
        }
    }
    @Serializable
    data class ContentMetadata(
        @SerialName("providerId") val providerId: String,
        @SerialName("data") val data: List<ProviderData>
    )

    @Serializable
    data class ProviderData(
        @SerialName("id") val id: String,
        @SerialName("description") val description: String,
        @SerialName("hasDub") val hasDub: Boolean,
        @SerialName("img") val img: String,
        @SerialName("isFiller") val isFiller: Boolean,
        @SerialName("number") val number: Int,
        @SerialName("title") val title: String,
        @SerialName("updatedAt") val updatedAt: Long,
        @SerialName("rating") val rating: Double? = null
    )
}