package ani.dantotsu.media

import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.currContext
import ani.dantotsu.media.anime.Episode
import ani.dantotsu.media.anime.SelectorDialogFragment
import ani.dantotsu.media.manga.MangaChapter
import ani.dantotsu.others.AniSkip
import ani.dantotsu.others.Anify
import ani.dantotsu.others.Jikan
import ani.dantotsu.others.Kitsu
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.parsers.Book
import ani.dantotsu.parsers.MangaImage
import ani.dantotsu.parsers.MangaReadSources
import ani.dantotsu.parsers.MangaSources
import ani.dantotsu.parsers.NovelSources
import ani.dantotsu.parsers.ShowResponse
import ani.dantotsu.parsers.VideoExtractor
import ani.dantotsu.parsers.WatchSources
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.util.Logger
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MediaDetailsViewModel : ViewModel() {
    val scrolledToTop = MutableLiveData(true)

    fun saveSelected(id: Int, data: Selected) {
        PrefManager.setCustomVal("Selected-$id", data)
    }


    fun loadSelected(media: Media, isDownload: Boolean = false): Selected {
        val data =
            PrefManager.getNullableCustomVal("Selected-${media.id}", null, Selected::class.java)
                ?: Selected().let {
                    it.sourceIndex = 0
                    it.preferDub = PrefManager.getVal(PrefName.SettingsPreferDub)
                    saveSelected(media.id, it)
                    it
                }
        if (isDownload) {
            data.sourceIndex = when {
                media.anime != null -> {
                    AnimeSources.list.size - 1
                }

                media.format == "MANGA" || media.format == "ONE_SHOT" -> {
                    MangaSources.list.size - 1
                }

                else -> {
                    NovelSources.list.size - 1
                }
            }
        }
        return data
    }

    var continueMedia: Boolean? = null
    private var loading = false

    private val media: MutableLiveData<Media> = MutableLiveData<Media>(null)
    fun getMedia(): LiveData<Media> = media
    fun loadMedia(m: Media) {
        if (!loading) {
            loading = true
            media.postValue(Anilist.query.mediaDetails(m))
        }
        loading = false
    }

    fun setMedia(m: Media) {
        media.postValue(m)
    }

    val responses = MutableLiveData<List<ShowResponse>?>(null)


    //Anime
    private val kitsuEpisodes: MutableLiveData<Map<String, Episode>> =
        MutableLiveData<Map<String, Episode>>(null)

    fun getKitsuEpisodes(): LiveData<Map<String, Episode>> = kitsuEpisodes
    suspend fun loadKitsuEpisodes(s: Media) {
        tryWithSuspend {
            if (kitsuEpisodes.value == null) kitsuEpisodes.postValue(Kitsu.getKitsuEpisodesDetails(s))
        }
    }

    private val anifyEpisodes: MutableLiveData<Map<String, Episode>> =
        MutableLiveData<Map<String, Episode>>(null)

    fun getAnifyEpisodes(): LiveData<Map<String, Episode>> = anifyEpisodes
    suspend fun loadAnifyEpisodes(s: Int) {
        tryWithSuspend {
            if (anifyEpisodes.value == null) anifyEpisodes.postValue(Anify.fetchAndParseMetadata(s))
        }
    }

    private val fillerEpisodes: MutableLiveData<Map<String, Episode>> =
        MutableLiveData<Map<String, Episode>>(null)

    fun getFillerEpisodes(): LiveData<Map<String, Episode>> = fillerEpisodes
    suspend fun loadFillerEpisodes(s: Media) {
        tryWithSuspend {
            if (fillerEpisodes.value == null) fillerEpisodes.postValue(
                Jikan.getEpisodes(
                    s.idMAL ?: return@tryWithSuspend
                )
            )
        }
    }

    var watchSources: WatchSources? = null

    private val episodes = MutableLiveData<MutableMap<Int, MutableMap<String, Episode>>>(null)
    private val epsLoaded = mutableMapOf<Int, MutableMap<String, Episode>>()
    fun getEpisodes(): LiveData<MutableMap<Int, MutableMap<String, Episode>>> = episodes
    suspend fun loadEpisodes(media: Media, i: Int, invalidate: Boolean = false) {
        if (!epsLoaded.containsKey(i) || invalidate) {
            epsLoaded[i] = watchSources?.loadEpisodesFromMedia(i, media) ?: return
        }
        episodes.postValue(epsLoaded)
    }

    suspend fun forceLoadEpisode(media: Media, i: Int) {
        epsLoaded[i] = watchSources?.loadEpisodesFromMedia(i, media) ?: return
        episodes.postValue(epsLoaded)
    }

    suspend fun overrideEpisodes(i: Int, source: ShowResponse, id: Int) {
        watchSources?.saveResponse(i, id, source)
        epsLoaded[i] =
            watchSources?.loadEpisodes(i, source.link, source.extra, source.sAnime) ?: return
        episodes.postValue(epsLoaded)
    }

    private var episode = MutableLiveData<Episode?>(null)
    fun getEpisode(): LiveData<Episode?> = episode

    suspend fun loadEpisodeVideos(ep: Episode, i: Int, post: Boolean = true) {
        val link = ep.link ?: return
        if (!ep.allStreams || ep.extractors.isNullOrEmpty()) {
            val list = mutableListOf<VideoExtractor>()
            ep.extractors = list
            watchSources?.get(i)?.apply {
                if (!post && !allowsPreloading) return@apply
                ep.sEpisode?.let {
                    loadByVideoServers(link, ep.extra, it) { extractor ->
                        if (extractor.videos.isNotEmpty()) {
                            list.add(extractor)
                            ep.extractorCallback?.invoke(extractor)
                        }
                    }
                }
                ep.extractorCallback = null
                if (list.isNotEmpty())
                    ep.allStreams = true
            }
        }


        if (post) {
            episode.postValue(ep)
            MainScope().launch(Dispatchers.Main) {
                episode.value = null
            }
        }
    }

    val timeStamps = MutableLiveData<List<AniSkip.Stamp>?>()
    private val timeStampsMap: MutableMap<Int, List<AniSkip.Stamp>?> = mutableMapOf()
    suspend fun loadTimeStamps(
        malId: Int?,
        episodeNum: Int?,
        duration: Long,
        useProxyForTimeStamps: Boolean
    ) {
        malId ?: return
        episodeNum ?: return
        if (timeStampsMap.containsKey(episodeNum))
            return timeStamps.postValue(timeStampsMap[episodeNum])
        val result = AniSkip.getResult(malId, episodeNum, duration, useProxyForTimeStamps)
        timeStampsMap[episodeNum] = result
        timeStamps.postValue(result)
    }

    suspend fun loadEpisodeSingleVideo(
        ep: Episode,
        selected: Selected,
        post: Boolean = true
    ): Boolean {
        if (ep.extractors.isNullOrEmpty()) {

            val server = selected.server ?: return false
            val link = ep.link ?: return false

            ep.extractors = mutableListOf(watchSources?.get(selected.sourceIndex)?.let {
                selected.sourceIndex = selected.sourceIndex
                if (!post && !it.allowsPreloading) null
                else ep.sEpisode?.let { it1 ->
                    it.loadSingleVideoServer(
                        server, link, ep.extra,
                        it1, post
                    )
                }
            } ?: return false)
            ep.allStreams = false
        }
        if (post) {
            episode.postValue(ep)
            MainScope().launch(Dispatchers.Main) {
                episode.value = null
            }
        }
        return true
    }

    fun setEpisode(ep: Episode?, who: String) {
        Logger.log("set episode ${ep?.number} - $who")
        episode.postValue(ep)
        MainScope().launch(Dispatchers.Main) {
            episode.value = null
        }
    }

    val epChanged = MutableLiveData(true)
    fun onEpisodeClick(
        media: Media,
        i: String,
        manager: FragmentManager,
        launch: Boolean = true,
        prevEp: String? = null,
        isDownload: Boolean = false
    ) {
        Handler(Looper.getMainLooper()).post {
            if (manager.findFragmentByTag("dialog") == null && !manager.isDestroyed) {
                if (media.anime?.episodes?.get(i) != null) {
                    media.anime.selectedEpisode = i
                } else {
                    snackString(currContext()?.getString(R.string.episode_not_found, i))
                    return@post
                }
                media.selected = this.loadSelected(media)
                val selector =
                    SelectorDialogFragment.newInstance(
                        media.selected!!.server,
                        launch,
                        prevEp,
                        isDownload
                    )
                selector.show(manager, "dialog")
            }
        }
    }

    //Manga
    var mangaReadSources: MangaReadSources? = null

    private val mangaChapters =
        MutableLiveData<MutableMap<Int, MutableMap<String, MangaChapter>>>(null)
    private val mangaLoaded = mutableMapOf<Int, MutableMap<String, MangaChapter>>()
    fun getMangaChapters(): LiveData<MutableMap<Int, MutableMap<String, MangaChapter>>> =
        mangaChapters

    suspend fun loadMangaChapters(media: Media, i: Int, invalidate: Boolean = false) {
        Logger.log("Loading Manga Chapters : $mangaLoaded")
        if (!mangaLoaded.containsKey(i) || invalidate) tryWithSuspend {
            mangaLoaded[i] =
                mangaReadSources?.loadChaptersFromMedia(i, media) ?: return@tryWithSuspend
        }
        mangaChapters.postValue(mangaLoaded)
    }

    suspend fun overrideMangaChapters(i: Int, source: ShowResponse, id: Int) {
        mangaReadSources?.saveResponse(i, id, source)
        tryWithSuspend {
            mangaLoaded[i] = mangaReadSources?.loadChapters(i, source) ?: return@tryWithSuspend
        }
        mangaChapters.postValue(mangaLoaded)
    }

    private val mangaChapter = MutableLiveData<MangaChapter?>(null)
    fun getMangaChapter(): LiveData<MangaChapter?> = mangaChapter
    suspend fun loadMangaChapterImages(
        chapter: MangaChapter,
        selected: Selected,
        post: Boolean = true
    ): Boolean {

        return tryWithSuspend(true) {
            chapter.addImages(
                mangaReadSources?.get(selected.sourceIndex)
                    ?.loadImages(chapter.link, chapter.sChapter) ?: return@tryWithSuspend false
            )
            if (post) mangaChapter.postValue(chapter)
            true
        } ?: false
    }

    fun loadTransformation(mangaImage: MangaImage, source: Int): BitmapTransformation? {
        return if (mangaImage.useTransformation) mangaReadSources?.get(source)
            ?.getTransformation() else null
    }

    val novelSources = NovelSources
    val novelResponses = MutableLiveData<List<ShowResponse>>(null)
    suspend fun searchNovels(query: String, i: Int) {
        val position = if (i >= novelSources.list.size) 0 else i
        val source = novelSources[position]
        tryWithSuspend(post = true) {
            if (source != null) {
                novelResponses.postValue(source.search(query))
            }
        }
    }

    suspend fun autoSearchNovels(media: Media) {
        val source = novelSources[media.selected?.sourceIndex ?: 0]
        tryWithSuspend(post = true) {
            if (source != null) {
                novelResponses.postValue(source.sortedSearch(media))
            }
        }
    }

    val book: MutableLiveData<Book> = MutableLiveData(null)
    suspend fun loadBook(novel: ShowResponse, i: Int) {
        tryWithSuspend {
            book.postValue(
                novelSources[i]?.loadBook(novel.link, novel.extra) ?: return@tryWithSuspend
            )
        }
    }

}
