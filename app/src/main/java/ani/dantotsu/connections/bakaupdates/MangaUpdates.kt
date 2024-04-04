package ani.dantotsu.connections.bakaupdates

import ani.dantotsu.client
import ani.dantotsu.connections.anilist.api.FuzzyDate
import ani.dantotsu.tryWithSuspend
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.ByteString.Companion.encode
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.Charset


class MangaUpdates {

    private val Int?.dateFormat get() = String.format("%02d", this)

    private val apiUrl = "https://api.mangaupdates.com/v1/releases/search"

    suspend fun search(title: String, startDate: FuzzyDate?) : MangaUpdatesResponse.Results? {
        return tryWithSuspend {
            val query = JSONObject().apply {
                try {
                    put("search", title.encode(Charset.forName("UTF-8")))
                    startDate?.let {
                        put(
                            "start_date",
                            "${it.year}-${it.month.dateFormat}-${it.day.dateFormat}"
                        )
                    }
                    put("include_metadata", true)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            val res = client.post(apiUrl, json = query).parsed<MangaUpdatesResponse>()
            res.results?.forEach{ println("MangaUpdates: $it") }
            res.results?.first { it.metadata.series.lastUpdated?.timestamp != null }
        }
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
                @SerialName("id")
                val id: Int,
                @SerialName("title")
                val title: String,
                @SerialName("volume")
                val volume: String?,
                @SerialName("chapter")
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
                    @SerialName("title")
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
}
