package ani.dantotsu.parsers

import android.os.Environment
import ani.dantotsu.currContext
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.media.anime.AnimeNameAdapter
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.SEpisodeImpl
import me.xdrop.fuzzywuzzy.FuzzySearch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

class OfflineAnimeParser : AnimeParser() {
    private val downloadManager = Injekt.get<DownloadsManager>()

    override val name = "Offline"
    override val saveName = "Offline"
    override val hostUrl = "Offline"
    override val isDubAvailableSeparately = false
    override val isNSFW = false

    override suspend fun loadEpisodes(
        animeLink: String,
        extra: Map<String, String>?,
        sAnime: SAnime
    ): List<Episode> {
        val directory = File(
            currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "${DownloadsManager.animeLocation}/$animeLink"
        )
        //get all of the folder names and add them to the list
        val episodes = mutableListOf<Episode>()
        if (directory.exists()) {
            directory.listFiles()?.forEach {
                if (it.isDirectory) {
                    val episode = Episode(
                        it.name,
                        "$animeLink - ${it.name}",
                        it.name,
                        null,
                        null,
                        sEpisode = SEpisodeImpl()
                    )
                    episodes.add(episode)
                }
            }
            episodes.sortBy { AnimeNameAdapter.findEpisodeNumber(it.number) }
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
                offline = true
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

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor {
        return OfflineVideoExtractor(server)
    }

}

class OfflineVideoExtractor(val videoServer: VideoServer) : VideoExtractor() {
    override val server: VideoServer
        get() = videoServer

    override suspend fun extract(): VideoContainer {
        val sublist = emptyList<Subtitle>()
        //we need to return a "fake" video so that the app doesn't crash
        val video = Video(
            null,
            VideoType.CONTAINER,
            "",
        )
        return VideoContainer(listOf(video), sublist)
    }

}