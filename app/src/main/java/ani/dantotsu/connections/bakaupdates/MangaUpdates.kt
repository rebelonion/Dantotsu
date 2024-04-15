package ani.dantotsu.connections

import android.content.Context
import ani.dantotsu.R
import ani.dantotsu.client
import ani.dantotsu.connections.anilist.AnilistQueries
import ani.dantotsu.connections.anilist.api.FuzzyDate
import ani.dantotsu.connections.anilist.api.Media
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.ByteString.Companion.encode
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.Charset

class MangaUpdates {

    private val anilistQueries = AnilistQueries()

    private val Int?.dateFormat get() = String.format("%02d", this)

    private val apiUrl = "https://api.mangaupdates.com/v1/releases/search"

    suspend fun search(title: String, startDate: FuzzyDate?): MangaUpdatesResponse.Results? {
        return tryWithSuspend {
            val media = anilistQueries.searchMedia(title)
            if (media != null) {
                when (media.type) {
                    "MANGA" -> fetchMangaDetails(media.id)
                    "NOVEL" -> fetchNovelDetails(media.id)
                    else -> null
                }
            } else {
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
                coroutineScope {
                    res.results?.map {
                        async(Dispatchers.IO) {
                            Logger.log(it.toString())
                        }
                    }
                }?.awaitAll()
                res.results?.first {
                    it.metadata.series.lastUpdated?.timestamp != null
                            && it.metadata.series.latestChapter != null
                            || it.record.volume.isNullOrBlank() && it.record.chapter != null
                }
            }
        }
    }

    private suspend fun fetchMangaDetails(mediaId: Int): MangaUpdatesResponse.Results? {
        val manga = anilistQueries.getMedia(mediaId)
        return manga?.let { media ->
            MangaUpdatesResponse.Results(
                record = MangaUpdatesResponse.Record(
                    id = media.id,
                    title = media.title?.userPreferred ?: "",
                    volume = null,
                    chapter = null,
                    releaseDate = ""
                ),
                metadata = MangaUpdatesResponse.MetaData(
                    series = MangaUpdatesResponse.MetaData.Series(
                        seriesId = media.id.toLong(),
                        title = media.title?.userPreferred,
                        latestChapter = null,
                        lastUpdated = null
                    )
                )
            )
        }
    }

    private suspend fun fetchNovelDetails(mediaId: Int): MangaUpdatesResponse.Results? {
        val novel = anilistQueries.getMedia(mediaId)
        return novel?.let { media ->
            MangaUpdatesResponse.Results(
                record = MangaUpdatesResponse.Record(
                    id = media.id,
                    title = media.title?.userPreferred ?: "",
                    volume = null,
                    chapter = null,
                    releaseDate = ""
                ),
                metadata = MangaUpdatesResponse.MetaData(
                    series = MangaUpdatesResponse.MetaData.Series(
                        seriesId = media.id.toLong(),
                        title = media.title?.userPreferred,
                        latestChapter = null,
                        lastUpdated = null
                    )
                )
            )
        }
    }

    companion object {
        fun getLatestChapter(context: Context, results: MangaUpdatesResponse.Results): String {
            return context.getString(R.string.unknown_chapter)
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
                val id: Int,
                val title: String,
                val volume: String?,
                val chapter: String?,
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
}
