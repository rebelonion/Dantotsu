package ani.dantotsu.others

import ani.dantotsu.FileUrl
import ani.dantotsu.client
import ani.dantotsu.media.Media
import ani.dantotsu.media.anime.Episode
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.util.Logger
import com.google.gson.Gson
import com.lagradost.nicehttp.NiceResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

object Kitsu {
    private suspend fun getKitsuData(query: String): KitsuResponse? {
        val headers = mapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json",
        )
        val response = tryWithSuspend {
            val res = client.post(
                "https://kitsu.io/api/graphql",
                headers,
                data = mapOf("query" to query)
            )
            res
        }
        val json = decodeToString(response)
        val gson = Gson()
        return gson.fromJson(json, KitsuResponse::class.java)
    }

    suspend fun getKitsuEpisodesDetails(media: Media): Map<String, Episode>? {
        Logger.log("Kitsu : title=${media.mainName()}")
        val query =
            """
query {
  lookupMapping(externalId: ${media.id}, externalSite: ANILIST_ANIME) {
    __typename
    ... on Anime {
      id
      episodes(first: 2000) {
        nodes {
          number
          titles {
            canonical
          }
          description
          thumbnail {
            original {
              url
            }
          }
        }
      }
    }
  }
}""".trimIndent()


        val result = getKitsuData(query) ?: return null
        //Logger.log("Kitsu : result=$result")
        media.idKitsu = result.data?.lookupMapping?.id
        val a = (result.data?.lookupMapping?.episodes?.nodes ?: return null).mapNotNull { ep ->
            val num = ep?.number?.toString() ?: return@mapNotNull null
            num to Episode(
                number = num,
                title = ep.titles?.canonical,
                desc = ep.description?.en,
                thumb = FileUrl[ep.thumbnail?.original?.url],
            )
        }.toMap()
        //Logger.log("Kitsu : a=$a")
        return a
    }

    private fun decodeToString(res: NiceResponse?): String? {
        return when (res?.headers?.get("Content-Encoding")) {
            "gzip" -> {
                res.body.byteStream().use { inputStream ->
                    GZIPInputStream(inputStream).use { gzipInputStream ->
                        InputStreamReader(gzipInputStream).use { reader ->
                            reader.readText()
                        }
                    }
                }
            }

            else -> {
                res?.body?.string()
            }
        }
    }

    @Serializable
    private data class KitsuResponse(
        @SerialName("data") val data: Data? = null
    ) {
        @Serializable
        data class Data(
            @SerialName("lookupMapping") val lookupMapping: LookupMapping? = null
        )

        @Serializable
        data class LookupMapping(
            @SerialName("id") val id: String? = null,
            @SerialName("episodes") val episodes: Episodes? = null
        )

        @Serializable
        data class Episodes(
            @SerialName("nodes") val nodes: List<Node?>? = null
        )

        @Serializable
        data class Node(
            @SerialName("number") val number: Int? = null,
            @SerialName("titles") val titles: Titles? = null,
            @SerialName("description") val description: Description? = null,
            @SerialName("thumbnail") val thumbnail: Thumbnail? = null
        )

        @Serializable
        data class Description(
            @SerialName("en") val en: String? = null
        )

        @Serializable
        data class Thumbnail(
            @SerialName("original") val original: Original? = null
        )

        @Serializable
        data class Original(
            @SerialName("url") val url: String? = null
        )

        @Serializable
        data class Titles(
            @SerialName("canonical") val canonical: String? = null
        )

    }

}
