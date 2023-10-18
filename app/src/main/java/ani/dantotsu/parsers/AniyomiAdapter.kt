package ani.dantotsu.parsers

import android.widget.Toast
import ani.dantotsu.FileUrl
import ani.dantotsu.aniyomi.anime.model.AnimeExtension
import ani.dantotsu.aniyomi.animesource.AnimeCatalogueSource
import ani.dantotsu.aniyomi.util.network.interceptor.CloudflareBypassException
import ani.dantotsu.currContext
import ani.dantotsu.logger
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import java.net.URL
import java.net.URLDecoder

class AniyomiAdapter {
    fun aniyomiToAnimeParser(extension: AnimeExtension.Installed): DynamicAnimeParser {
        return DynamicAnimeParser(extension)
    }


}

class DynamicAnimeParser(extension: AnimeExtension.Installed) : AnimeParser() {
    val extension: AnimeExtension.Installed
    init {
        this.extension = extension
    }
    override val name = extension.name
    override val saveName = extension.name
    override val hostUrl = extension.sources.first().name
    override val isDubAvailableSeparately = false
    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?, sAnime: SAnime): List<Episode> {
        val source = extension.sources.first()
        if (source is AnimeCatalogueSource) {
            var res: SEpisode? = null
            try {
                val res = source.getEpisodeList(sAnime)
                var EpisodeList: List<Episode> = emptyList()
                for (episode in res) {
                    println("episode: $episode")
                    EpisodeList += SEpisodeToEpisode(episode)
                }
                return EpisodeList
            }
            catch (e: Exception) {
                println("Exception: $e")
            }
            return emptyList()
        }
        return emptyList()  // Return an empty list if source is not an AnimeCatalogueSource
    }
    override suspend fun loadVideoServers(episodeLink: String, extra: Map<String, String>?, sEpisode: SEpisode): List<VideoServer> {
        val source = extension.sources.first()
        if (source is AnimeCatalogueSource) {
            val video = source.getVideoList(sEpisode)
            var VideoList: List<VideoServer> = emptyList()
            for (videoServer in video) {
                VideoList += VideoToVideoServer(videoServer)
            }
            return VideoList
        }
        return emptyList()
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        return VideoServerPassthrough(server)
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val source = extension.sources.first()
        if (source is AnimeCatalogueSource) {

            var res: AnimesPage? = null
            try {
                res = source.fetchSearchAnime(1, query, AnimeFilterList()).toBlocking().first()
                logger("res observable: $res")
            }
            catch (e: CloudflareBypassException) {
                logger("Exception in search: $e")
                //toast
                Toast.makeText(currContext(), "Failed to bypass Cloudflare", Toast.LENGTH_SHORT).show()
            }

            val conv = convertAnimesPageToShowResponse(res!!)
            return conv
        }
        return emptyList()  // Return an empty list if source is not an AnimeCatalogueSource
    }

    fun convertAnimesPageToShowResponse(animesPage: AnimesPage): List<ShowResponse> {
        return animesPage.animes.map { sAnime ->
            // Extract required fields from sAnime
            val name = sAnime.title
            val link = sAnime.url
            val coverUrl = sAnime.thumbnail_url ?: ""
            val otherNames = emptyList<String>() // Populate as needed
            val total = 1
            val extra: Map<String, String>? = null // Populate as needed

            // Create a new ShowResponse
            ShowResponse(name, link, coverUrl, sAnime)
        }
    }

    fun SEpisodeToEpisode(sEpisode: SEpisode): Episode {
        val episode = Episode(
            sEpisode.episode_number.toString(),
            sEpisode.url,
            sEpisode.name,
            null,
            null,
            false,
            null,
            sEpisode)
        return episode
    }

    fun VideoToVideoServer(video: Video): VideoServer {
        val videoServer = VideoServer(
            video.quality,
            video.url,
            null,
            video)
        return videoServer
    }
}

class VideoServerPassthrough : VideoExtractor{
    val videoServer: VideoServer
    constructor(videoServer: VideoServer) {
        this.videoServer = videoServer
    }
    override val server: VideoServer
        get() {
            return videoServer
        }

    override suspend fun extract(): VideoContainer {
        val vidList = listOfNotNull(videoServer.video?.let { AniVideoToSaiVideo(it) })
        var subList: List<Subtitle> = emptyList()
        for(sub in videoServer.video?.subtitleTracks ?: emptyList()) {
            subList += TrackToSubtitle(sub)
        }
        if(vidList.isEmpty()) {
            throw Exception("No videos found")
        }else{
            return VideoContainer(vidList, subList)
        }
    }

    private fun AniVideoToSaiVideo(aniVideo: eu.kanade.tachiyomi.animesource.model.Video) : ani.dantotsu.parsers.Video {
        //try to find the number value from the .quality string
        val regex = Regex("""\d+""")
        val result = regex.find(aniVideo.quality)
        val number = result?.value?.toInt() ?: 0
        val videoUrl = aniVideo.videoUrl ?: throw Exception("Video URL is null")
        val urlObj = URL(videoUrl)
        val path = urlObj.path
        val query = urlObj.query


        var format = when {
            path.endsWith(".mp4", ignoreCase = true) || videoUrl.endsWith(".mkv", ignoreCase = true) -> VideoType.CONTAINER
            path.endsWith(".m3u8", ignoreCase = true) -> VideoType.M3U8
            path.endsWith(".mpd", ignoreCase = true) -> VideoType.DASH
            else -> null
        }
        if (format == null) {
            val queryPairs: List<Pair<String, String>> = query.split("&").map {
                val idx = it.indexOf("=")
                val key = URLDecoder.decode(it.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(it.substring(idx + 1), "UTF-8")
                Pair(key, value)
            }

            // Assume the file is named under the "file" query parameter
            val fileName = queryPairs.find { it.first == "file" }?.second ?: ""

            format = when {
                fileName.endsWith(".mp4", ignoreCase = true) || fileName.endsWith(".mkv", ignoreCase = true) -> VideoType.CONTAINER
                fileName.endsWith(".m3u8", ignoreCase = true) -> VideoType.M3U8
                fileName.endsWith(".mpd", ignoreCase = true) -> VideoType.DASH
                else -> null
            }
        }
        // If the format is still undetermined, log an error or handle it appropriately
        if (format == null) {
            logger("Unknown video format: $videoUrl")
            throw Exception("Unknown video format")
        }
        val headersMap: Map<String, String> = aniVideo.headers?.toMultimap()?.mapValues { it.value.joinToString() } ?: mapOf()


        return ani.dantotsu.parsers.Video(
            number,
            format,
            FileUrl(videoUrl, headersMap),
            aniVideo.totalContentLength.toDouble()
        )
    }

    private fun TrackToSubtitle(track: Track, type: SubtitleType = SubtitleType.VTT): Subtitle {
        return Subtitle(track.lang, track.url, type)
    }
}