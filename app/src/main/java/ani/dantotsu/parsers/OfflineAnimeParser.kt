package ani.dantotsu.parsers

import android.app.Application
import android.net.Uri
import android.os.Environment
import ani.dantotsu.currContext
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.DownloadsManager.Companion.getSubDirectory
import ani.dantotsu.download.anime.AnimeDownloaderService.AnimeDownloadTask.Companion.getTaskName
import ani.dantotsu.media.MediaType
import ani.dantotsu.media.MediaNameAdapter
import ani.dantotsu.tryWithSuspend
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.SEpisodeImpl
import me.xdrop.fuzzywuzzy.FuzzySearch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
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
                        getTaskName(animeLink,it.name!!),
                        it.name,
                        null,
                        null,
                        extra = extraData,
                        sEpisode = SEpisodeImpl()
                    )
                    episodes.add(episode)
                }
            }
            episodes.sortBy { MediaNameAdapter.findEpisodeNumber(it.number) }
            return episodes
        }
        return emptyList()
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
        val titles = downloadManager.animeDownloadedTypes.map { it.title }.distinct()
        val returnTitles: MutableList<String> = mutableListOf()
        for (title in titles) {
            if (FuzzySearch.ratio(title.lowercase(), query.lowercase()) > 80) {
                returnTitles.add(title)
            }
        }
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

class OfflineVideoExtractor(val videoServer: VideoServer) : VideoExtractor() {
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
            DownloadsManager.getSubDirectory(
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
                            determineSubtitletype(file.name ?: "")
                        )
                    )
                }
            }
        }
        return null
    }

    fun determineSubtitletype(url: String): SubtitleType {
        return when {
            url.lowercase(Locale.ROOT).endsWith("ass") -> SubtitleType.ASS
            url.lowercase(Locale.ROOT).endsWith("vtt") -> SubtitleType.VTT
            else -> SubtitleType.SRT
        }
    }
}