package ani.dantotsu.connections.bakaupdates

import android.content.Context
import ani.dantotsu.R
import ani.dantotsu.client
import ani.dantotsu.media.Media
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.utf8
import ani.dantotsu.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class MangaUpdates {

    private val Int?.dateFormat get() = String.format("%02d", this)

    suspend fun findLatestRelease(media: Media) : ReleaseResponse.Results? {
        return tryWithSuspend {
            val query = JSONObject().apply {
                try {
                    put("search", media.mangaName().utf8)
                    media.startDate?.let {
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
            val res = client.post(
                "https://api.mangaupdates.com/v1/releases/search",
                json = query
            ).parsed<ReleaseResponse>()
            coroutineScope {
                res.results?.map {
                    async(Dispatchers.IO) {
                        Logger.log(it.toString())
                    }
                }
            }?.awaitAll()
            res.results?.first {
                it.metadata.series.lastUpdated?.timestamp != null
                        && (it.metadata.series.latestChapter != null
                        || (it.record.volume.isNullOrBlank() && it.record.chapter != null))
            }
        }
    }

    suspend fun getSeries(results: ReleaseResponse.Results) : SeriesResponse? {
        return results.metadata.series.seriesId?.let {
            tryWithSuspend {
                val res = client.get(
                    "https://api.mangaupdates.com/v1/series/$it"
                ).parsed<SeriesResponse>()
                Logger.log(res.toString())
                res.latestChapter?.let { res }
            }
        }
    }

    fun getLatestChapter(context: Context, results: ReleaseResponse.Results): String {
        return results.metadata.series.latestChapter?.let {
            context.getString(R.string.chapter_number, it)
        } ?: results.record.chapter!!.substringAfterLast("-").trim().let { chapter ->
            chapter.takeIf {
                it.toIntOrNull() == null
            } ?: context.getString(R.string.chapter_number, chapter.toInt())
        }
    }

    private suspend fun findReleaseDates(media: Media) : List<String> {
        val releaseList = hashMapOf<String, String>()
        return tryWithSuspend {
            val query = JSONObject().apply {
                try {
                    put("search", media.mangaName().utf8)
                    media.startDate?.let {
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
            val res = client.post(
                "https://api.mangaupdates.com/v1/releases/search",
                json = query
            ).parsed<ReleaseResponse>()
            res.results?.filter {
                it.record.volume.isNullOrBlank() && it.record.chapter != null
            }?.sortedByDescending { it.record.releaseDate }?.forEach {
                releaseList[it.record.chapter!!] = it.record.releaseDate
                Logger.log(it.toString())
            }
            releaseList.values.toList().sortedDescending()
        } ?: releaseList.values.toList()
    }

    private fun getCalendarInstance(releaseDate: String): Calendar {
        val calendar: Calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        calendar.timeZone = TimeZone.getDefault()
        dateFormat.parse(releaseDate)?.let { calendar.time = it }
        return calendar
    }

    suspend fun predictRelease(media: Media, latest: Long): Long? {
        val releaseDates = findReleaseDates(media)
        if (releaseDates.size < 3) return null
        releaseDates.forEach {
            Logger.log(it)
        }
        val dateLatest = getCalendarInstance(releaseDates[0])
        val dateMiddle = getCalendarInstance(releaseDates[1])
        val dateOldest = getCalendarInstance(releaseDates[2])
        val daysNew: Long = TimeUnit.MILLISECONDS.toDays(dateLatest.timeInMillis - dateMiddle.timeInMillis)
        val daysOld: Long = TimeUnit.MILLISECONDS.toDays(dateMiddle.timeInMillis - dateOldest.timeInMillis)

        val date: Calendar = Calendar.getInstance()
        date.timeInMillis = latest

        return when {
            daysNew in 5..14 && daysOld in 5..14 -> {
                latest + 604800000 // 7 days
            }
            daysNew in 28..36 && daysOld in 28..36 -> {
                date.add(Calendar.MONTH, 1)
                date.timeInMillis
            }
            daysNew in 84..98 && daysOld in 84..98 -> {
                date.add(Calendar.MONTH, 3)
                date.timeInMillis
            }
            daysNew >= 358 && daysOld >= 358 -> {
                date.add(Calendar.YEAR, 1)
                date.timeInMillis
            }
            else -> {
                null
            }
        }
    }

    @Serializable
    data class ReleaseResponse(
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
                val id: Long,
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
                        val asRfc3339: String?,
                        @SerialName("as_string")
                        val asString: String?
                    )
                }
            }
        }
    }

    @Serializable
    data class SeriesResponse(
        @SerialName("series_id")
        val seriesId: Long,
        @SerialName("title")
        val title: String,
        @SerialName("description")
        val description: String?,
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
            val asRfc3339: String?,
            @SerialName("as_string")
            val asString: String?
        )
    }
}
