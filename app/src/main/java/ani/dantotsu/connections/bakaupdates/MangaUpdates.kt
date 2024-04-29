package ani.dantotsu.connections.mangaplus

import android.content.Context
import ani.dantotsu.R
import ani.dantotsu.client
import ani.dantotsu.media.Media
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.Charset
import ani.dantotsu.connections.anilist.api.FuzzyDate
import okio.ByteString.Companion.encode
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class MangaPlus {

    val String.utf8: String get() = URLEncoder.encode(this, Charset.forName("UTF-8").name())

    private val Int?.dateFormat get() = String.format("%02d", this)

    suspend fun findLatestRelease(media: Media): ReleaseResponse? {
        return tryWithSuspend {
            val res = client.get(
                "https://mangaplus.shueisha.co.jp/api/series/${media.mangaId()}"
            ).parsed<ReleaseResponse>()
            res
        }
    }

    suspend fun getSeries(seriesId: String): SeriesResponse? {
        return tryWithSuspend {
            val res = client.get(
                "https://mangaplus.shueisha.co.jp/api/series/$seriesId"
            ).parsed<SeriesResponse>()
            Logger.log(res.toString())
            res
        }
    }

    fun getLatestChapter(context: Context, results: ReleaseResponse): String {
        return context.getString(R.string.chapter_number, results.metadata.series.latestChapter ?: "")
    }

    private suspend fun findLatestReleaseDate(media: Media): Long? {
        return tryWithSuspend {
            val seriesId = findLatestRelease(media)?.record?.id ?: return@tryWithSuspend null
            val series = getSeries(seriesId.toString()) ?: return@tryWithSuspend null
            series.lastUpdated ?: return@tryWithSuspend null
        }
    }

    suspend fun predictRelease(media: Media, latestChapterReleaseDate: Long): Long? {
        val latestReleaseDate = findLatestReleaseDate(media) ?: return null
        val daysBetweenChapters = latestChapterReleaseDate - latestReleaseDate

        val date: Calendar = Calendar.getInstance()
        date.timeInMillis = latestChapterReleaseDate

        when {
            daysBetweenChapters in 5..14 -> {
                date.add(Calendar.DAY_OF_YEAR, 7)
            }
            daysBetweenChapters in 28..36 -> {
                date.add(Calendar.MONTH, 1)
            }
            daysBetweenChapters in 84..98 -> {
                date.add(Calendar.MONTH, 3)
            }
            daysBetweenChapters >= 358 -> {
                date.add(Calendar.YEAR, 1)
            }
            else -> return null
        }
        return date.timeInMillis
    }

    @Serializable
    data class ReleaseResponse(
        val record: Record,
        val metadata: MetaData
    ) {
        @Serializable
        data class Record(
            val id: String,
            val title: String,
            @SerialName("latestChapter")
            val lastChapter: String,
            @SerialName("lastUpdated")
            val lastChapterDate: Long
        )

        @Serializable
        data class MetaData(
            val series: Series
        ) {
            @Serializable
            data class Series(
                val title: String,
                val latestChapter: Int,
                val lastUpdated: Long
            )
        }
    }

    @Serializable
    data class SeriesResponse(
        val title: String,
        val description: String?,
        val latestChapter: Int,
        val lastUpdated: Long
    )
}
