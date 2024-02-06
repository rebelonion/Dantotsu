package ani.dantotsu.subcriptions

import android.content.Context
import ani.dantotsu.R
import ani.dantotsu.currContext
import ani.dantotsu.media.Media
import ani.dantotsu.media.Selected
import ani.dantotsu.media.manga.MangaNameAdapter
import ani.dantotsu.parsers.AnimeParser
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.parsers.Episode
import ani.dantotsu.parsers.HAnimeSources
import ani.dantotsu.parsers.HMangaSources
import ani.dantotsu.parsers.MangaChapter
import ani.dantotsu.parsers.MangaParser
import ani.dantotsu.parsers.MangaSources
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.tryWithSuspend
import kotlinx.coroutines.withTimeoutOrNull

class SubscriptionHelper {
    companion object {
        private fun loadSelected(
            context: Context,
            mediaId: Int,
            isAdult: Boolean,
            isAnime: Boolean
        ): Selected {
            val data =
                PrefManager.getNullableCustomVal("${mediaId}-select", null, Selected::class.java)
                    ?: Selected().let {
                        it.sourceIndex = 0
                        it.preferDub = PrefManager.getVal(PrefName.SettingsPreferDub)
                        it
                    }
            return data
        }

        private fun saveSelected(context: Context, mediaId: Int, data: Selected) {
            PrefManager.setCustomVal("${mediaId}-select", data)
        }

        fun getAnimeParser(context: Context, isAdult: Boolean, id: Int): AnimeParser {
            val sources = if (isAdult) HAnimeSources else AnimeSources
            val selected = loadSelected(context, id, isAdult, true)
            val parser = sources[selected.sourceIndex]
            parser.selectDub = selected.preferDub
            return parser
        }

        suspend fun getEpisode(
            context: Context,
            parser: AnimeParser,
            id: Int,
            isAdult: Boolean
        ): Episode? {

            val selected = loadSelected(context, id, isAdult, true)
            val ep = withTimeoutOrNull(10 * 1000) {
                tryWithSuspend {
                    val show = parser.loadSavedShowResponse(id) ?: throw Exception(
                        currContext()?.getString(
                            R.string.failed_to_load_data,
                            id
                        )
                    )
                    show.sAnime?.let {
                        parser.getLatestEpisode(
                            show.link, show.extra,
                            it, selected.latest
                        )
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

        suspend fun getChapter(
            context: Context,
            parser: MangaParser,
            id: Int,
            isAdult: Boolean
        ): MangaChapter? {
            val selected = loadSelected(context, id, isAdult, true)
            val chp = withTimeoutOrNull(10 * 1000) {
                tryWithSuspend {
                    val show = parser.loadSavedShowResponse(id) ?: throw Exception(
                        currContext()?.getString(
                            R.string.failed_to_load_data,
                            id
                        )
                    )
                    show.sManga?.let {
                        parser.getLatestChapter(
                            show.link, show.extra,
                            it, selected.latest
                        )
                    }
                }
            }

            return chp?.apply {
                selected.latest = MangaNameAdapter.findChapterNumber(number) ?: 0f
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

        @Suppress("UNCHECKED_CAST")
        fun getSubscriptions(): Map<Int, SubscribeMedia> =
            (PrefManager.getNullableCustomVal(
                subscriptions,
                null,
                Map::class.java
            ) as? Map<Int, SubscribeMedia>)
                ?: mapOf<Int, SubscribeMedia>().also { PrefManager.setCustomVal(subscriptions, it) }

        @Suppress("UNCHECKED_CAST")
        fun saveSubscription(context: Context, media: Media, subscribed: Boolean) {
            val data = PrefManager.getNullableCustomVal(
                subscriptions,
                null,
                Map::class.java
            ) as? MutableMap<Int, SubscribeMedia>
                ?: mutableMapOf()
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
            PrefManager.setCustomVal(subscriptions, data)
        }
    }
}