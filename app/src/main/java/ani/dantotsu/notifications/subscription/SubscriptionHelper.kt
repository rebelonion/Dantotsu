package ani.dantotsu.notifications.subscription

import ani.dantotsu.R
import ani.dantotsu.currContext
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaNameAdapter
import ani.dantotsu.media.Selected
import ani.dantotsu.parsers.AnimeParser
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.parsers.BaseParser
import ani.dantotsu.parsers.Episode
import ani.dantotsu.parsers.MangaChapter
import ani.dantotsu.parsers.MangaParser
import ani.dantotsu.parsers.MangaSources
import ani.dantotsu.parsers.ShowResponse
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.toast
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.util.Logger
import kotlinx.coroutines.withTimeoutOrNull

class SubscriptionHelper {
    companion object {
        private fun loadSelected(
            mediaId: Int
        ): Selected {
            val data =
                PrefManager.getNullableCustomVal("Selected-${mediaId}", null, Selected::class.java)
                    ?: Selected().let {
                        it.sourceIndex = 0
                        it.preferDub = PrefManager.getVal(PrefName.SettingsPreferDub)
                        it
                    }
            return data
        }

        private fun saveSelected(mediaId: Int, data: Selected) {
            PrefManager.setCustomVal("Selected-${mediaId}", data)
        }

        fun getAnimeParser(id: Int): AnimeParser {
            val sources = AnimeSources
            Logger.log("getAnimeParser size: ${sources.list.size}")
            val selected = loadSelected(id)
            if (selected.sourceIndex >= sources.list.size) {
                selected.sourceIndex = 0
            }
            val parser = sources[selected.sourceIndex]
            parser.selectDub = selected.preferDub
            return parser
        }

        suspend fun getEpisode(
            parser: AnimeParser,
            subscribeMedia: SubscribeMedia
        ): Episode? {

            val selected = loadSelected(subscribeMedia.id)
            val ep = withTimeoutOrNull(10 * 1000) {
                tryWithSuspend {
                    val show = parser.loadSavedShowResponse(subscribeMedia.id)
                        ?: forceLoadShowResponse(subscribeMedia, selected, parser)
                        ?: throw Exception(
                            currContext()?.getString(
                                R.string.failed_to_load_data,
                                subscribeMedia.id
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
                saveSelected(subscribeMedia.id, selected)
            }
        }

        fun getMangaParser(id: Int): MangaParser {
            val sources = MangaSources
            Logger.log("getMangaParser size: ${sources.list.size}")
            val selected = loadSelected(id)
            if (selected.sourceIndex >= sources.list.size) {
                selected.sourceIndex = 0
            }
            return sources[selected.sourceIndex]
        }

        suspend fun getChapter(
            parser: MangaParser,
            subscribeMedia: SubscribeMedia
        ): MangaChapter? {
            val selected = loadSelected(subscribeMedia.id)
            val chp = withTimeoutOrNull(10 * 1000) {
                tryWithSuspend {
                    val show = parser.loadSavedShowResponse(subscribeMedia.id)
                        ?: forceLoadShowResponse(subscribeMedia, selected, parser)
                        ?: throw Exception(
                            currContext()?.getString(
                                R.string.failed_to_load_data,
                                subscribeMedia.id
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
                selected.latest = MediaNameAdapter.findChapterNumber(number) ?: 0f
                saveSelected(subscribeMedia.id, selected)
            }
        }

        private suspend fun forceLoadShowResponse(
            subscribeMedia: SubscribeMedia,
            selected: Selected,
            parser: BaseParser
        ): ShowResponse? {
            val tempMedia = Media(
                id = subscribeMedia.id,
                name = null,
                nameRomaji = subscribeMedia.name,
                userPreferredName = subscribeMedia.name,
                isAdult = subscribeMedia.isAdult,
                isFav = false,
                isListPrivate = false,
                userScore = 0,
                userRepeat = 0,
                format = null,
                selected = selected
            )
            parser.autoSearch(tempMedia)
            return parser.loadSavedShowResponse(subscribeMedia.id)
        }

        data class SubscribeMedia(
            val isAnime: Boolean,
            val isAdult: Boolean,
            val id: Int,
            val name: String,
            val image: String?,
            val banner: String? = null
        ) : java.io.Serializable {
            companion object {
                private const val serialVersionUID = 1L
            }
        }

        private const val SUBSCRIPTIONS = "subscriptions"

        @Suppress("UNCHECKED_CAST")
        fun getSubscriptions(): Map<Int, SubscribeMedia> =
            (PrefManager.getNullableCustomVal(
                SUBSCRIPTIONS,
                null,
                Map::class.java
            ) as? Map<Int, SubscribeMedia>)
                ?: mapOf<Int, SubscribeMedia>().also { PrefManager.setCustomVal(SUBSCRIPTIONS, it) }

        @Suppress("UNCHECKED_CAST")
        fun deleteSubscription(id: Int, showSnack: Boolean = false) {
            val data = PrefManager.getNullableCustomVal(
                SUBSCRIPTIONS,
                null,
                Map::class.java
            ) as? MutableMap<Int, SubscribeMedia>
                ?: mutableMapOf()
            data.remove(id)
            PrefManager.setCustomVal(SUBSCRIPTIONS, data)
            if (showSnack) toast(R.string.subscription_deleted)
        }

        @Suppress("UNCHECKED_CAST")
        fun saveSubscription(media: Media, subscribed: Boolean) {
            val data = PrefManager.getNullableCustomVal(
                SUBSCRIPTIONS,
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
                        media.cover,
                        media.banner
                    )
                    data[media.id] = new
                }
            } else {
                data.remove(media.id)
            }
            PrefManager.setCustomVal(SUBSCRIPTIONS, data)
        }
    }
}