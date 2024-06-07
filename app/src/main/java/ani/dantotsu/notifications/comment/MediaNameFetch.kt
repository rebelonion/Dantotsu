package ani.dantotsu.notifications.comment

import ani.dantotsu.client
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaNameFetch {
    companion object {
        private fun queryBuilder(mediaIds: List<Int>): String {
            var query = "{"
            mediaIds.forEachIndexed { index, mediaId ->
                query += """
                media$index: Media(id: $mediaId) {
                    coverImage {
                        medium
                        color
                    }
                    id
                    title {
                        romaji
                    }
                }
            """.trimIndent()
            }
            query += "}"
            return query
        }

        suspend fun fetchMediaTitles(ids: List<Int>): Map<Int, ReturnedData> {
            return try {
                val url = "https://graphql.anilist.co/"
                val data = mapOf(
                    "query" to queryBuilder(ids),
                )
                withContext(Dispatchers.IO) {
                    val response = client.post(
                        url,
                        headers = mapOf(
                            "Content-Type" to "application/json",
                            "Accept" to "application/json"
                        ),
                        data = data
                    )
                    val mediaResponse = parseMediaResponseWithGson(response.text)
                    val mediaMap = mutableMapOf<Int, ReturnedData>()
                    mediaResponse.data.forEach { (_, mediaItem) ->
                        mediaMap[mediaItem.id] = ReturnedData(
                            mediaItem.title.romaji,
                            mediaItem.coverImage.medium,
                            mediaItem.coverImage.color
                        )
                    }
                    mediaMap
                }
            } catch (e: Exception) {
                val errorMap = mutableMapOf<Int, ReturnedData>()
                ids.forEach { errorMap[it] = ReturnedData("Unknown", "", "#222222") }
                errorMap
            }
        }

        private fun parseMediaResponseWithGson(response: String): MediaResponse {
            val gson = Gson()
            val type = object : TypeToken<MediaResponse>() {}.type
            return gson.fromJson(response, type)
        }

        data class ReturnedData(val title: String, val coverImage: String, val color: String)

        data class MediaResponse(val data: Map<String, MediaItem>)
        data class MediaItem(
            val coverImage: MediaCoverImage,
            val id: Int,
            val title: MediaTitle
        )

        data class MediaTitle(val romaji: String)
        data class MediaCoverImage(val medium: String, val color: String)

    }
}