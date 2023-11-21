package ani.dantotsu.parsers

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.ContextCompat
import ani.dantotsu.FileUrl
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.network.interceptor.CloudflareBypassException
import ani.dantotsu.currContext
import ani.dantotsu.download.manga.MangaDownloaderService
import ani.dantotsu.download.manga.ServiceDataSingleton
import ani.dantotsu.logger
import ani.dantotsu.media.manga.ImageData
import ani.dantotsu.media.manga.MangaCache
import com.google.firebase.crashlytics.FirebaseCrashlytics
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URL
import java.net.URLDecoder
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.*
import java.io.UnsupportedEncodingException
import java.util.regex.Pattern

class AniyomiAdapter {
    fun aniyomiToAnimeParser(extension: AnimeExtension.Installed): DynamicAnimeParser {
        return DynamicAnimeParser(extension)
    }


}

class DynamicAnimeParser(extension: AnimeExtension.Installed) : AnimeParser() {
    val extension: AnimeExtension.Installed
    var sourceLanguage = 0

    init {
        this.extension = extension
    }

    override val name = extension.name
    override val saveName = extension.name
    override val hostUrl = extension.sources.first().name
    override val isDubAvailableSeparately = false
    override val isNSFW = extension.isNsfw
    override suspend fun loadEpisodes(
        animeLink: String,
        extra: Map<String, String>?,
        sAnime: SAnime
    ): List<Episode> {
        val source = try {
            extension.sources[sourceLanguage]
        } catch (e: Exception) {
            sourceLanguage = 0
            extension.sources[sourceLanguage]
        }
        if (source is AnimeCatalogueSource) {
            try {
                val res = source.getEpisodeList(sAnime)

                val sortedEpisodes = if (res[0].episode_number == -1f) {
                    // Find the number in the string and sort by that number
                    val sortedByStringNumber = res.sortedBy {
                        val matchResult = "\\d+".toRegex().find(it.name)
                        val number = matchResult?.value?.toFloat() ?: Float.MAX_VALUE
                        it.episode_number = number  // Store the found number in episode_number
                        number
                    }

                    // If there is no number, reverse the order and give them an incrementing number
                    var incrementingNumber = 1f
                    sortedByStringNumber.map {
                        if (it.episode_number == Float.MAX_VALUE) {
                            it.episode_number =
                                incrementingNumber++  // Update episode_number with the incrementing number
                        }
                        it
                    }
                } else {
                    // Sort by the episode_number field
                    res.sortedBy { it.episode_number }
                }

                // Transform SEpisode objects to Episode objects

                return sortedEpisodes.map { SEpisodeToEpisode(it) }
            } catch (e: Exception) {
                println("Exception: $e")
            }
            return emptyList()
        }
        return emptyList()  // Return an empty list if source is not an AnimeCatalogueSource
    }

    override suspend fun loadVideoServers(
        episodeLink: String,
        extra: Map<String, String>?,
        sEpisode: SEpisode
    ): List<VideoServer> {
        val source = try {
            extension.sources[sourceLanguage]
        } catch (e: Exception) {
            sourceLanguage = 0
            extension.sources[sourceLanguage]
        } as? AnimeCatalogueSource ?: return emptyList()

        return try {
            val videos = source.getVideoList(sEpisode)
            videos.map { VideoToVideoServer(it) }
        } catch (e: Exception) {
            logger("Exception occurred: ${e.message}")
            emptyList()
        }
    }


    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor {
        return VideoServerPassthrough(server)
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val source = try {
            extension.sources[sourceLanguage]
        } catch (e: Exception) {
            sourceLanguage = 0
            extension.sources[sourceLanguage]
        } as? AnimeCatalogueSource ?: return emptyList()
        return try {
            val res = source.fetchSearchAnime(1, query, AnimeFilterList()).toBlocking().first()
            convertAnimesPageToShowResponse(res)
        } catch (e: CloudflareBypassException) {
            logger("Exception in search: $e")
            withContext(Dispatchers.Main) {
                Toast.makeText(currContext(), "Failed to bypass Cloudflare", Toast.LENGTH_SHORT)
                    .show()
            }
            emptyList()
        } catch (e: Exception) {
            logger("General exception in search: $e")
            emptyList()
        }
    }


    private fun convertAnimesPageToShowResponse(animesPage: AnimesPage): List<ShowResponse> {
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

    private fun SEpisodeToEpisode(sEpisode: SEpisode): Episode {
        //if the float episode number is a whole number, convert it to an int
        val episodeNumberInt =
            if (sEpisode.episode_number % 1 == 0f) {
                sEpisode.episode_number.toInt()
            } else {
                sEpisode.episode_number
            }
        return Episode(
            if (episodeNumberInt.toInt() != -1) {
                episodeNumberInt.toString()
            } else {
                sEpisode.name
            },
            sEpisode.url,
            sEpisode.name,
            null,
            null,
            false,
            null,
            sEpisode
        )
    }

    private fun VideoToVideoServer(video: Video): VideoServer {
        return VideoServer(
            video.quality,
            video.url,
            null,
            video
        )
    }
}

class DynamicMangaParser(extension: MangaExtension.Installed) : MangaParser() {
    val mangaCache = Injekt.get<MangaCache>()
    val extension: MangaExtension.Installed
    var sourceLanguage = 0

    init {
        this.extension = extension
    }

    override val name = extension.name
    override val saveName = extension.name
    override val hostUrl = extension.sources.first().name
    override val isNSFW = extension.isNsfw

    override suspend fun loadChapters(
        mangaLink: String,
        extra: Map<String, String>?,
        sManga: SManga
    ): List<MangaChapter> {
        val source = try {
            extension.sources[sourceLanguage]
        } catch (e: Exception) {
            sourceLanguage = 0
            extension.sources[sourceLanguage]
        } as? HttpSource ?: return emptyList()

        return try {
            val res = source.getChapterList(sManga)
            val reversedRes = res.reversed()
            val chapterList = reversedRes.map { SChapterToMangaChapter(it) }
            logger("chapterList size: ${chapterList.size}")
            logger("chapterList: ${chapterList[1].title}")
            logger("chapterList: ${chapterList[1].description}")
            chapterList
        } catch (e: Exception) {
            logger("loadChapters Exception: $e")
            emptyList()
        }
    }


    override suspend fun loadImages(chapterLink: String, sChapter: SChapter): List<MangaImage> {
        val source = try {
            extension.sources[sourceLanguage]
        } catch (e: Exception) {
            sourceLanguage = 0
            extension.sources[sourceLanguage]
        } as? HttpSource ?: return emptyList()
        var imageDataList: List<ImageData> = listOf()
        val ret = coroutineScope {
            try {
                println("source.name " + source.name)
                val res = source.getPageList(sChapter)
                val reIndexedPages =
                    res.mapIndexed { index, page -> Page(index, page.url, page.imageUrl, page.uri) }

                val deferreds = reIndexedPages.map { page ->
                    async(Dispatchers.IO) {
                        mangaCache.put(page.imageUrl ?: "", ImageData(page, source))
                        imageDataList += ImageData(page, source)
                        logger("put page: ${page.imageUrl}")
                        pageToMangaImage(page)
                    }
                }

                deferreds.awaitAll()

            } catch (e: Exception) {
                logger("loadImages Exception: $e")
                Toast.makeText(currContext(), "Failed to load images: $e", Toast.LENGTH_SHORT)
                    .show()
                emptyList()
            }
        }
        return ret
    }

    suspend fun imageList(chapterLink: String, sChapter: SChapter): List<ImageData>{
        val source = try {
            extension.sources[sourceLanguage]
        } catch (e: Exception) {
            sourceLanguage = 0
            extension.sources[sourceLanguage]
        } as? HttpSource ?: return emptyList()
        var imageDataList: List<ImageData> = listOf()
        coroutineScope {
            try {
                println("source.name " + source.name)
                val res = source.getPageList(sChapter)
                val reIndexedPages =
                    res.mapIndexed { index, page -> Page(index, page.url, page.imageUrl, page.uri) }

                val deferreds = reIndexedPages.map { page ->
                    async(Dispatchers.IO) {
                        imageDataList += ImageData(page, source)
                    }
                }

                deferreds.awaitAll()

            } catch (e: Exception) {
                logger("loadImages Exception: $e")
                Toast.makeText(currContext(), "Failed to load images: $e", Toast.LENGTH_SHORT)
                    .show()
                emptyList()
            }
        }
        return imageDataList
    }

    suspend fun fetchAndProcessImage(
        page: Page,
        httpSource: HttpSource,
        context: Context
    ): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch the image
                val response = httpSource.getImage(page)
                println("Response: ${response.code}")
                println("Response: ${response.message}")

                // Convert the Response to an InputStream
                val inputStream = response.body?.byteStream()

                // Convert InputStream to Bitmap
                val bitmap = BitmapFactory.decodeStream(inputStream)

                inputStream?.close()
                ani.dantotsu.media.manga.saveImage(
                    bitmap,
                    context.contentResolver,
                    page.imageUrl!!,
                    Bitmap.CompressFormat.JPEG,
                    100
                )

                return@withContext bitmap
            } catch (e: Exception) {
                // Handle any exceptions
                println("An error occurred: ${e.message}")
                return@withContext null
            }
        }
    }


    fun fetchAndSaveImage(page: Page, httpSource: HttpSource, contentResolver: ContentResolver) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch the image
                val response = httpSource.getImage(page)

                // Convert the Response to an InputStream
                val inputStream = response.body?.byteStream()

                // Convert InputStream to Bitmap
                val bitmap = BitmapFactory.decodeStream(inputStream)

                withContext(Dispatchers.IO) {
                    // Save the Bitmap using MediaStore API
                    saveImage(
                        bitmap,
                        contentResolver,
                        "image_${System.currentTimeMillis()}.jpg",
                        Bitmap.CompressFormat.JPEG,
                        100
                    )
                }

                inputStream?.close()
            } catch (e: Exception) {
                // Handle any exceptions
                println("An error occurred: ${e.message}")
            }
        }
    }

    fun saveImage(
        bitmap: Bitmap,
        contentResolver: ContentResolver,
        filename: String,
        format: Bitmap.CompressFormat,
        quality: Int
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/${format.name.lowercase()}")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/Dantotsu/Anime"
                    )
                }

                val uri: Uri? = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let {
                    contentResolver.openOutputStream(it)?.use { os ->
                        bitmap.compress(format, quality, os)
                    }
                }
            } else {
                val directory =
                    File("${Environment.getExternalStorageDirectory()}${File.separator}Dantotsu${File.separator}Anime")
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                val file = File(directory, filename)
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(format, quality, outputStream)
                }
            }
        } catch (e: Exception) {
            // Handle exception here
            println("Exception while saving image: ${e.message}")
        }
    }


    override suspend fun search(query: String): List<ShowResponse> {
        val source = try {
            extension.sources[sourceLanguage]
        } catch (e: Exception) {
            sourceLanguage = 0
            extension.sources[sourceLanguage]
        } as? HttpSource ?: return emptyList()

        return try {
            val res = source.fetchSearchManga(1, query, FilterList()).toBlocking().first()
            logger("res observable: $res")
            convertMangasPageToShowResponse(res)
        } catch (e: CloudflareBypassException) {
            logger("Exception in search: $e")
            withContext(Dispatchers.Main) {
                Toast.makeText(currContext(), "Failed to bypass Cloudflare", Toast.LENGTH_SHORT)
                    .show()
            }
            emptyList()
        } catch (e: Exception) {
            logger("General exception in search: $e")
            emptyList()
        }
    }


    private fun convertMangasPageToShowResponse(mangasPage: MangasPage): List<ShowResponse> {
        return mangasPage.mangas.map { sManga ->
            // Extract required fields from sManga
            val name = sManga.title
            val link = sManga.url
            val coverUrl = sManga.thumbnail_url ?: ""
            val otherNames = emptyList<String>() // Populate as needed
            val total = 1
            val extra: Map<String, String>? = null // Populate as needed

            // Create a new ShowResponse
            ShowResponse(name, link, coverUrl, sManga)
        }
    }

    private fun pageToMangaImage(page: Page): MangaImage {
        var headersMap = mapOf<String, String>()
        var urlWithoutHeaders = ""
        var url = ""

        page.imageUrl?.let {
            val splitUrl = it.split("&")
            urlWithoutHeaders = splitUrl.getOrNull(0) ?: ""
            url = it

            headersMap = splitUrl.mapNotNull { part ->
                val idx = part.indexOf("=")
                if (idx != -1) {
                    try {
                        val key = URLDecoder.decode(part.substring(0, idx), "UTF-8")
                        val value = URLDecoder.decode(part.substring(idx + 1), "UTF-8")
                        Pair(key, value)
                    } catch (e: UnsupportedEncodingException) {
                        null
                    }
                } else {
                    null
                }
            }.toMap()
        }

        return MangaImage(
            FileUrl(url, headersMap),
            false,
            page
        )
    }


    private fun SChapterToMangaChapter(sChapter: SChapter): MangaChapter {
        return MangaChapter(
            sChapter.name,
            sChapter.url,
            "",
            null,
            sChapter.scanlator,
            sChapter
        )
    }

    fun parseChapterTitle(title: String): Triple<String?, String?, String> {
        val volumePattern =
            Pattern.compile("(?:vol\\.?|v|volume\\s?)(\\d+)", Pattern.CASE_INSENSITIVE)
        val chapterPattern =
            Pattern.compile("(?:ch\\.?|chapter\\s?)(\\d+)", Pattern.CASE_INSENSITIVE)

        val volumeMatcher = volumePattern.matcher(title)
        val chapterMatcher = chapterPattern.matcher(title)

        val volumeNumber = if (volumeMatcher.find()) volumeMatcher.group(1) else null
        val chapterNumber = if (chapterMatcher.find()) chapterMatcher.group(1) else null

        var remainingTitle = title
        if (volumeNumber != null) {
            remainingTitle =
                volumeMatcher.group(0)?.let { remainingTitle.replace(it, "") }.toString()
        }
        if (chapterNumber != null) {
            remainingTitle =
                chapterMatcher.group(0)?.let { remainingTitle.replace(it, "") }.toString()
        }

        return Triple(volumeNumber, chapterNumber, remainingTitle.trim())
    }

}

class VideoServerPassthrough(val videoServer: VideoServer) : VideoExtractor() {
    override val server: VideoServer
        get() = videoServer

    override suspend fun extract(): VideoContainer {
        val vidList = listOfNotNull(videoServer.video?.let { AniVideoToSaiVideo(it) })
        val subList = videoServer.video?.subtitleTracks?.map { TrackToSubtitle(it) } ?: emptyList()

        return if (vidList.isNotEmpty()) {
            VideoContainer(vidList, subList)
        } else {
            throw Exception("No videos found")
        }
    }

    private fun AniVideoToSaiVideo(aniVideo: eu.kanade.tachiyomi.animesource.model.Video): ani.dantotsu.parsers.Video {
        // Find the number value from the .quality string
        val number = Regex("""\d+""").find(aniVideo.quality)?.value?.toInt() ?: 0

        // Check for null video URL
        val videoUrl = aniVideo.videoUrl ?: throw Exception("Video URL is null")

        val urlObj = URL(videoUrl)
        val path = urlObj.path
        val query = urlObj.query

        var format = getVideoType(path)

        if (format == null && query != null) {
            val queryPairs: List<Pair<String, String>> = query.split("&").map {
                val idx = it.indexOf("=")
                val key = URLDecoder.decode(it.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(it.substring(idx + 1), "UTF-8")
                Pair(key, value)
            }

            // Assume the file is named under the "file" query parameter
            val fileName = queryPairs.find { it.first == "file" }?.second ?: ""

            format = getVideoType(fileName)
        }

        // If the format is still undetermined, log an error or handle it appropriately
        if (format == null) {
            logger("Unknown video format: $videoUrl")
            FirebaseCrashlytics.getInstance().recordException(Exception("Unknown video format: $videoUrl"))
            format = VideoType.CONTAINER
        }
        val headersMap: Map<String, String> =
            aniVideo.headers?.toMultimap()?.mapValues { it.value.joinToString() } ?: mapOf()


        return ani.dantotsu.parsers.Video(
            number,
            format,
            FileUrl(videoUrl, headersMap),
            aniVideo.totalContentLength.toDouble()
        )
    }

    private fun getVideoType(fileName: String): VideoType? {
        return when {
            fileName.endsWith(".mp4", ignoreCase = true) || fileName.endsWith(
                ".mkv",
                ignoreCase = true
            ) -> VideoType.CONTAINER

            fileName.endsWith(".m3u8", ignoreCase = true) -> VideoType.M3U8
            fileName.endsWith(".mpd", ignoreCase = true) -> VideoType.DASH
            else -> null
        }
    }

    private fun TrackToSubtitle(track: Track): Subtitle {
        //use Dispatchers.IO to make a HTTP request to determine the subtitle type
        var type: SubtitleType? = null
        runBlocking {
            type = findSubtitleType(track.url)
        }
        return Subtitle(track.lang, track.url, type ?: SubtitleType.SRT)
    }

    private fun findSubtitleType(url: String): SubtitleType? {
        // First, try to determine the type based on the URL file extension
        val type: SubtitleType? = when {
            url.endsWith(".vtt", true) -> SubtitleType.VTT
            url.endsWith(".ass", true) -> SubtitleType.ASS
            url.endsWith(".srt", true) -> SubtitleType.SRT
            else -> SubtitleType.UNKNOWN
        }

        return type
    }
}