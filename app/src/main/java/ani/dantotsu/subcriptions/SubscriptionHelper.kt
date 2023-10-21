package ani.dantotsu.subcriptions

import android.content.Context
import ani.dantotsu.currContext
import ani.dantotsu.loadData
import ani.dantotsu.media.Media
import ani.dantotsu.media.Selected
import ani.dantotsu.parsers.*
import ani.dantotsu.saveData
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.R
import kotlinx.coroutines.withTimeoutOrNull

class SubscriptionHelper {
    companion object {
        private fun loadSelected(context: Context, mediaId: Int, isAdult: Boolean, isAnime: Boolean): Selected {
            val data = loadData<Selected>("${mediaId}-select", context) ?: Selected().let {
                it.sourceIndex =
                    if (isAdult) 0
                    else if (isAnime) {loadData("settings_def_anime_source_s_r", context) ?: 0}
                    else loadData("settings_def_manga_source_s_r", context) ?: 0
                it.preferDub = loadData("settings_prefer_dub", context) ?: false
                it
            }
            return data
        }

        private fun saveSelected(context: Context, mediaId: Int, data: Selected) {
            saveData("$mediaId-select", data, context)
        }

        fun getAnimeParser(context: Context, isAdult: Boolean, id: Int): AnimeParser {
            val sources = if (isAdult) HAnimeSources else AnimeSources
            val selected = loadSelected(context, id, isAdult, true)
            val parser = sources[selected.sourceIndex]
            parser.selectDub = selected.preferDub
            return parser
        }

        suspend fun getEpisode(context: Context, parser: AnimeParser, id: Int, isAdult: Boolean): Episode? {

            val selected = loadSelected(context, id, isAdult, true)
            val ep = withTimeoutOrNull(10 * 1000) {
                tryWithSuspend {
                    val show = parser.loadSavedShowResponse(id) ?: throw Exception(currContext()?.getString(R.string.failed_to_load_data, id))
                    show.sAnime?.let {
                        parser.getLatestEpisode(show.link, show.extra,
                            it, selected.latest)
                    }
                }
            }

            return ep?.apply {
                selected.latest = number.toFloat()
                saveSelected(context, id, selected)
            }
        }

        fun getMangaParser(context: Context, isAdult: Boolean, id: Int): MangaParser {
            val sources = if (isAdult) HMangaSources else MangaSources
            val selected = loadSelected(context, id, isAdult, false)
            return sources[selected.sourceIndex]
        }

        suspend fun getChapter(context: Context, parser: MangaParser, id: Int, isAdult: Boolean): MangaChapter? {
            val selected = loadSelected(context, id, isAdult, true)
            val chp = withTimeoutOrNull(10 * 1000) {
                tryWithSuspend {
                    val show = parser.loadSavedShowResponse(id) ?: throw Exception(currContext()?.getString(R.string.failed_to_load_data, id))
                    show.sManga?.let {
                        parser.getLatestChapter(show.link, show.extra,
                            it, selected.latest)
                    }
                }
            }

            return chp?.apply {
                selected.latest = number.toFloat()
                saveSelected(context, id, selected)
            }
        }

        data class SubscribeMedia(
            val isAnime: Boolean,
            val isAdult: Boolean,
            val id: Int,
            val name: String,
            val image: String?
        ) : java.io.Serializable

        private const val subscriptions = "subscriptions"
        fun getSubscriptions(context: Context): Map<Int, SubscribeMedia> = loadData(subscriptions, context)
            ?: mapOf<Int, SubscribeMedia>().also { saveData(subscriptions, it, context) }

        fun saveSubscription(context: Context, media: Media, subscribed: Boolean) {
            val data = loadData<Map<Int, SubscribeMedia>>(subscriptions, context)!!.toMutableMap()
            if (subscribed) {
                if (!data.containsKey(media.id)) {
                    val new = SubscribeMedia(
                        media.anime != null,
                        media.isAdult,
                        media.id,
                        media.userPreferredName,
                        media.cover
                    )
                    data[media.id] = new
                }
            } else {
                data.remove(media.id)
            }
            saveData(subscriptions, data, context)
        }
    }
}