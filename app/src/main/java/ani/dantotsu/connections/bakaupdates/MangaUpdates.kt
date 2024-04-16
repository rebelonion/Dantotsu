package ani.dantotsu.connections

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.*
import java.io.IOException

class AniListMangaUpdates(private val client: OkHttpClient) {

    private val aniListBaseUrl = "https://graphql.anilist.co"
    private val mangaUpdatesBaseUrl = "https://api.mangaupdates.com/v1/series/"
    private val json = Json

    suspend fun getMediaId(mediaType: MediaType, mediaName: String): Int? {
        return withContext(Dispatchers.IO) {
            try {
                val query = """
                    query {
                        Media(search: "$mediaName", type: ${mediaType.typeName}) {
                            id
                        }
                    }
                """.trimIndent()

                val requestBody = query.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(aniListBaseUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val jsonData = response.body?.string()
                parseMediaId(jsonData)
            } catch (e: IOException) {
                throw AniListMangaUpdateException("Error fetching media ID: ${e.message}", e)
            }
        }
    }

    suspend fun getMangaId(mangaName: String): Int? {
        return getMediaId(MediaType.Manga, mangaName)
    }

    suspend fun getLightNovelId(lightNovelName: String): Int? {
        return getMediaId(MediaType.Novel, lightNovelName)
    }

    suspend fun getOneShotId(oneShotName: String): Int? {
        return getMediaId(MediaType.OneShot, oneShotName)
    }

    suspend fun getLatestChapterSinceTime(mediaId: Int, time: Long): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$mangaUpdatesBaseUrl$mediaId")
                    .build()

                val response = client.newCall(request).execute()
                val jsonData = response.body?.string()
                parseLatestChapterSinceTime(jsonData, time)
            } catch (e: IOException) {
                throw AniListMangaUpdateException("Error fetching latest chapter: ${e.message}", e)
            }
        }
    }

    private fun parseMediaId(jsonData: String?): Int? {
        return try {
            val jsonObject = json.decodeFromString<JsonObject>(jsonData!!)
            val media = jsonObject["data"]?.jsonObject?.get("Media")?.jsonObject
            media?.get("id")?.jsonPrimitive?.int
        } catch (e: Exception) {
            throw AniListMangaUpdateException("Error parsing media ID: ${e.message}", e)
        }
    }

    private fun parseLatestChapterSinceTime(jsonData: String?, time: Long): String? {
        return try {
            val response = json.decodeFromString<MangaUpdatesResponse>(jsonData!!)
            val latestChapter = response.results?.firstOrNull { result ->
                result.record.releaseDate.toLongOrNull() ?: 0 > time
            }
            latestChapter?.let { chapter ->
                "${chapter.record.title}: ${chapter.record.chapter ?: "Unknown Chapter"}"
            }
        } catch (e: Exception) {
            throw AniListMangaUpdateException("Error parsing latest chapter: ${e.message}", e)
        }
    }
}

sealed class MediaType(val typeName: String) {
    object Manga : MediaType("MANGA")
    object Novel : MediaType("NOVEL")
    object OneShot : MediaType("ONE_SHOT")
}

@Serializable
data class MangaUpdatesResponse(
    @SerialName("total_hits")
    val totalHits: Int?,
    @SerialName("page")
    val page: Int?,
    @SerialName("per_page")
    val perPage: Int?,
    val results: List<Results>? = null
) {
    @Serializable
    data class Results(
        val record: Record,
        val metadata: MetaData
    ) {
        @Serializable
        data class Record(
            val id: Int,
            val title: String,
            val volume: String?,
            val chapter: String?,
            @SerialName("release_date")
            val releaseDate: String
        )

        @Serializable
        data class MetaData(
            val series: Series
        ) {
            @Serializable
            data class Series(
                @SerialName("series_id")
                val seriesId: Long?,
                val title: String?,
                @SerialName("latest_chapter")
                val latestChapter: Int?,
                @SerialName("last_updated")
                val lastUpdated: LastUpdated?
            ) {
                @Serializable
                data class LastUpdated(
                    @SerialName("timestamp")
                    val timestamp: Long,
                    @SerialName("as_rfc3339")
                    val asRfc3339: String,
                    @SerialName("as_string")
                    val asString: String
                )
            }
        }
    }
}

class AniListMangaUpdateException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
