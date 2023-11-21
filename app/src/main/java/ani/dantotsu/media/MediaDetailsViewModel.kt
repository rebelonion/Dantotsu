package ani.dantotsu.media

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ani.dantotsu.FileUrl
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.media.anime.Episode
import ani.dantotsu.media.anime.SelectorDialogFragment
import ani.dantotsu.loadData
import ani.dantotsu.logger
import ani.dantotsu.media.manga.MangaChapter
import ani.dantotsu.others.AniSkip
import ani.dantotsu.others.Jikan
import ani.dantotsu.others.Kitsu
import ani.dantotsu.parsers.Book
import ani.dantotsu.parsers.MangaImage
import ani.dantotsu.parsers.MangaReadSources
import ani.dantotsu.parsers.NovelSources
import ani.dantotsu.parsers.ShowResponse
import ani.dantotsu.parsers.VideoExtractor
import ani.dantotsu.parsers.WatchSources
import ani.dantotsu.saveData
import ani.dantotsu.snackString
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.currContext
import ani.dantotsu.R
import ani.dantotsu.download.Download
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.parsers.AniyomiAdapter
import ani.dantotsu.parsers.DynamicMangaParser
import ani.dantotsu.parsers.HAnimeSources
import ani.dantotsu.parsers.HMangaSources
import ani.dantotsu.parsers.MangaSources
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

class MediaDetailsViewModel : ViewModel() {
    val scrolledToTop = MutableLiveData(true)

    fun saveSelected(id: Int, data: Selected, activity: Activity? = null) {
        saveData("$id-select", data, activity)
    }


    fun loadSelected(media: Media, isDownload: Boolean = false): Selected {
        val sharedPreferences = Injekt.get<SharedPreferences>()
        val data = loadData<Selected>("${media.id}-select") ?: Selected().let {
            it.sourceIndex = if (media.isAdult) 0 else when (media.anime != null) {
                true ->sharedPreferences.getInt("settings_def_anime_source_s_r", 0)
                else ->sharedPreferences.getInt(("settings_def_manga_source_s_r"), 0)
            }
            it.preferDub = loadData("settings_prefer_dub") ?: false
            saveSelected(media.id, it)
            it
        }
        if (isDownload) {
            data.sourceIndex = when (media.anime != null) {
                true -> AnimeSources.list.size - 1
                else -> MangaSources.list.size - 1
            }
        }
        return data
    }

    fun loadSelectedStringLocation(sourceName: String): Int {
        //find the location of the source in the list
        var location = watchSources?.list?.indexOfFirst { it.name == sourceName } ?: 0
        if (location == -1) {location = 0}
        return location
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
    private val kitsuEpisodes: MutableLiveData<Map<String, Episode>> = MutableLiveData<Map<String, Episode>>(null)
    fun getKitsuEpisodes(): LiveData<Map<String, Episode>> = kitsuEpisodes
    suspend fun loadKitsuEpisodes(s: Media) {
        tryWithSuspend {
            if (kitsuEpisodes.value == null) kitsuEpisodes.postValue(Kitsu.getKitsuEpisodesDetails(s))
        }
    }

    private val fillerEpisodes: MutableLiveData<Map<String, Episode>> = MutableLiveData<Map<String, Episode>>(null)
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
        epsLoaded[i] = watchSources?.loadEpisodes(i, source.link, source.extra, source.sAnime) ?: return
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
                    loadByVideoServers(link, ep.extra, it) {
                        if (it.videos.isNotEmpty()) {
                            list.add(it)
                            ep.extractorCallback?.invoke(it)
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
    suspend fun loadTimeStamps(malId: Int?, episodeNum: Int?, duration: Long, useProxyForTimeStamps: Boolean) {
        malId ?: return
        episodeNum ?: return
        if (timeStampsMap.containsKey(episodeNum))
            return timeStamps.postValue(timeStampsMap[episodeNum])
        val result = AniSkip.getResult(malId, episodeNum, duration, useProxyForTimeStamps)
        timeStampsMap[episodeNum] = result
        timeStamps.postValue(result)
    }

    suspend fun loadEpisodeSingleVideo(ep: Episode, selected: Selected, post: Boolean = true): Boolean {
        if (ep.extractors.isNullOrEmpty()) {

            val server = selected.server ?: return false
            val link = ep.link ?: return false

            ep.extractors = mutableListOf(watchSources?.get(selected.sourceIndex)?.let {
                selected.sourceIndex = selected.sourceIndex
                if (!post && !it.allowsPreloading) null
                else ep.sEpisode?.let { it1 ->
                    it.loadSingleVideoServer(server, link, ep.extra,
                        it1, post)
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
        logger("set episode ${ep?.number} - $who", false)
        episode.postValue(ep)
        MainScope().launch(Dispatchers.Main) {
            episode.value = null
        }
    }

    val epChanged = MutableLiveData(true)
    fun onEpisodeClick(media: Media, i: String, manager: FragmentManager, launch: Boolean = true, prevEp: String? = null) {
        Handler(Looper.getMainLooper()).post {
            if (manager.findFragmentByTag("dialog") == null && !manager.isDestroyed) {
                if (media.anime?.episodes?.get(i) != null) {
                    media.anime.selectedEpisode = i
                } else {
                    snackString(currContext()?.getString(R.string.episode_not_found, i))
                    return@post
                }
                media.selected = this.loadSelected(media)
                val selector = SelectorDialogFragment.newInstance(media.selected!!.server, launch, prevEp)
                selector.show(manager, "dialog")
            }
        }
    }


    //Manga
    var mangaReadSources: MangaReadSources? = null

    private val mangaChapters = MutableLiveData<MutableMap<Int, MutableMap<String, MangaChapter>>>(null)
    private val mangaLoaded = mutableMapOf<Int, MutableMap<String, MangaChapter>>()
    fun getMangaChapters(): LiveData<MutableMap<Int, MutableMap<String, MangaChapter>>> = mangaChapters
    suspend fun loadMangaChapters(media: Media, i: Int, invalidate: Boolean = false) {
        logger("Loading Manga Chapters : $mangaLoaded")
        if (!mangaLoaded.containsKey(i) || invalidate) tryWithSuspend {
            mangaLoaded[i] = mangaReadSources?.loadChaptersFromMedia(i, media) ?: return@tryWithSuspend
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
    suspend fun loadMangaChapterImages(chapter: MangaChapter, selected: Selected, series: String, post: Boolean = true): Boolean {
        //check if the chapter has been downloaded already
        val downloadsManager = Injekt.get<DownloadsManager>()
        if(downloadsManager.mangaDownloads.contains(Download(series, chapter.title!!, Download.Type.MANGA))) {
            val download = downloadsManager.mangaDownloads.find { it.title == series && it.chapter == chapter.title!! } ?: return false
            //look in the downloads folder for the chapter and add all the numerically named images to the chapter
            val directory = File(
                currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/Manga/$series/${chapter.title!!}"
            )
            val images = mutableListOf<MangaImage>()
            directory.listFiles()?.forEach {
                if (it.nameWithoutExtension.toIntOrNull() != null) {
                    images.add(MangaImage(FileUrl(it.absolutePath), false))
                }
            }
            //sort the images by name
            images.sortBy { it.url.url }
            chapter.addImages(images)
            if (post) mangaChapter.postValue(chapter)
            return true
        }
        return tryWithSuspend(true) {
            chapter.addImages(
                mangaReadSources?.get(selected.sourceIndex)?.loadImages(chapter.link, chapter.sChapter) ?: return@tryWithSuspend false
            )
            if (post) mangaChapter.postValue(chapter)
            true
        } ?: false
    }

    fun loadTransformation(mangaImage: MangaImage, source: Int): BitmapTransformation? {
        return if (mangaImage.useTransformation) mangaReadSources?.get(source)?.getTransformation() else null
    }

    val novelSources = NovelSources
    val novelResponses = MutableLiveData<List<ShowResponse>>(null)
    suspend fun searchNovels(query: String, i: Int) {
        val source = novelSources[i]
        tryWithSuspend(post = true) {
            if (source != null) {
                novelResponses.postValue(source.search(query))
            }
        }
    }

    suspend fun autoSearchNovels(media: Media) {
        val source = novelSources[media.selected?.sourceIndex?:0]
        tryWithSuspend(post = true) {
            if (source != null) {
                novelResponses.postValue(source.sortedSearch(media))
            }
        }
    }

    val book: MutableLiveData<Book> = MutableLiveData(null)
    suspend fun loadBook(novel: ShowResponse, i: Int) {
        tryWithSuspend {
            book.postValue(novelSources[i]?.loadBook(novel.link, novel.extra) ?: return@tryWithSuspend)
        }
    }

}
