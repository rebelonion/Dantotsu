package ani.dantotsu.parsers

import android.app.Application
import ani.dantotsu.currContext
import ani.dantotsu.download.DownloadCompat.Companion.loadEpisodesCompat
import ani.dantotsu.download.DownloadCompat.Companion.loadSubtitleCompat
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.DownloadsManager.Companion.getSubDirectory
import ani.dantotsu.download.anime.AnimeDownloaderService.AnimeDownloadTask.Companion.getTaskName
import ani.dantotsu.media.MediaNameAdapter
import ani.dantotsu.media.MediaType
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.SEpisodeImpl
import me.xdrop.fuzzywuzzy.FuzzySearch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

class OfflineAnimeParser : AnimeParser() {
    private val downloadManager = Injekt.get<DownloadsManager>()
    private val context = Injekt.get<Application>()

    override val name = "Offline"
    override val saveName = "Offline"
    override val hostUrl = "Offline"
    override val isNSFW = false

    override suspend fun loadEpisodes(
        animeLink: String,
        extra: Map<String, String>?,
        sAnime: SAnime
    ): List<Episode> {
        val directory = getSubDirectory(context, MediaType.ANIME, false, animeLink)
        //get all of the folder names and add them to the list
        val episodes = mutableListOf<Episode>()
        if (directory?.exists() == true) {
            directory.listFiles().forEach {
                //put the title and episdode number in the extra data
                val extraData = mutableMapOf<String, String>()
                extraData["title"] = animeLink
                extraData["episode"] = it.name!!
                if (it.isDirectory) {
                    val episode = Episode(
                        it.name!!,
                        getTaskName(animeLink, it.name!!),
                        it.name,
                        null,
                        null,
                        extra = extraData,
                        sEpisode = SEpisodeImpl()
                    )
                    episodes.add(episode)
                }
            }
        }
        episodes.addAll(loadEpisodesCompat(animeLink, extra, sAnime))
        //filter those with the same name
        return episodes.distinctBy { it.number }
            .sortedBy { MediaNameAdapter.findEpisodeNumber(it.number) }
    }

    override suspend fun loadVideoServers(
        episodeLink: String,
        extra: Map<String, String>?,
        sEpisode: SEpisode
    ): List<VideoServer> {
        return listOf(
            VideoServer(
                episodeLink,
                offline = true,
                extraData = extra
            )
        )
    }


    override suspend fun search(query: String): List<ShowResponse> {
        val titles = downloadManager.animeDownloadedTypes.map { it.titleName }.distinct()
        val returnTitlesPair: MutableList<Pair<String, Int>> = mutableListOf()
        for (title in titles) {
            Logger.log("Comparing $title to $query")
            val score = FuzzySearch.ratio(title.lowercase(), query.lowercase())
            if (score > 80) {
                returnTitlesPair.add(Pair(title, score))
            }
        }
        val returnTitles = returnTitlesPair.sortedByDescending { it.second }.map { it.first }
        val returnList: MutableList<ShowResponse> = mutableListOf()
        for (title in returnTitles) {
            returnList.add(ShowResponse(title, title, title))
        }
        return returnList
    }

    override suspend fun loadByVideoServers(
        episodeUrl: String,
        extra: Map<String, String>?,
        sEpisode: SEpisode,
        callback: (VideoExtractor) -> Unit
    ) {
        val server = loadVideoServers(episodeUrl, extra, sEpisode).first()
        OfflineVideoExtractor(server).apply {
            tryWithSuspend {
                load()
            }
            callback.invoke(this)
        }
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor {
        return OfflineVideoExtractor(server)
    }

}

class OfflineVideoExtractor(private val videoServer: VideoServer) : VideoExtractor() {
    override val server: VideoServer
        get() = videoServer

    override suspend fun extract(): VideoContainer {
        val sublist = getSubtitle(
            videoServer.extraData?.get("title") ?: "",
            videoServer.extraData?.get("episode") ?: ""
        ) ?: emptyList()
        //we need to return a "fake" video so that the app doesn't crash
        val video = Video(
            null,
            VideoType.CONTAINER,
            "",
        )
        return VideoContainer(listOf(video), sublist)
    }

    private fun getSubtitle(title: String, episode: String): List<Subtitle>? {
        currContext()?.let {
            getSubDirectory(
                it,
                MediaType.ANIME,
                false,
                title,
                episode
            )?.listFiles()?.forEach { file ->
                if (file.name?.contains("subtitle") == true) {
                    return listOf(
                        Subtitle(
                            "Downloaded Subtitle",
                            file.uri.toString(),
                            determineSubtitleType(file.name ?: "")
                        )
                    )
                }
            }
            loadSubtitleCompat(title, episode)?.let { return it }
        }
        return null
    }

    private fun determineSubtitleType(url: String): SubtitleType {
        return when {
            url.lowercase(Locale.ROOT).endsWith("ass") -> SubtitleType.ASS
            url.lowercase(Locale.ROOT).endsWith("vtt") -> SubtitleType.VTT
            else -> SubtitleType.SRT
        }
    }
}