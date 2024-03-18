package ani.dantotsu.parsers

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import ani.dantotsu.FileUrl
import ani.dantotsu.currContext
import ani.dantotsu.util.Logger
import ani.dantotsu.media.anime.AnimeNameAdapter
import ani.dantotsu.media.manga.ImageData
import ani.dantotsu.media.manga.MangaCache
import ani.dantotsu.snackString
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.interceptor.CloudflareBypassException
import eu.kanade.tachiyomi.source.anime.getPreferenceKey
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.lang.awaitSingle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.FileOutputStream
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLDecoder
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
    override val isNSFW = extension.isNsfw
    override var selectDub: Boolean
        get() = getDub()
        set(value) {
            setDub(value)
        }

    private fun getDub(): Boolean {
        if (sourceLanguage >= extension.sources.size) {
            sourceLanguage = extension.sources.size - 1
        }
        val configurableSource = extension.sources[sourceLanguage] as? ConfigurableAnimeSource
            ?: return false
        currContext()?.let { context ->
            val sharedPreferences =
                context.getSharedPreferences(
                    configurableSource.getPreferenceKey(),
                    Context.MODE_PRIVATE
                )
            sharedPreferences.all.filterValues { AnimeNameAdapter.getSubDub(it.toString()) != AnimeNameAdapter.Companion.SubDubType.NULL }
                .forEach { value ->
                    return when (AnimeNameAdapter.getSubDub(value.value.toString())) {
                        AnimeNameAdapter.Companion.SubDubType.SUB -> false
                        AnimeNameAdapter.Companion.SubDubType.DUB -> true
                        AnimeNameAdapter.Companion.SubDubType.NULL -> false
                    }
                }
        }
        return false
    }

    fun setDub(setDub: Boolean) {
        if (sourceLanguage >= extension.sources.size) {
            sourceLanguage = extension.sources.size - 1
        }
        val configurableSource = extension.sources[sourceLanguage] as? ConfigurableAnimeSource
            ?: return
        val type = when (setDub) {
            true -> AnimeNameAdapter.Companion.SubDubType.DUB
            false -> AnimeNameAdapter.Companion.SubDubType.SUB
        }
        currContext()?.let { context ->
            val sharedPreferences =
                context.getSharedPreferences(
                    configurableSource.getPreferenceKey(),
                    Context.MODE_PRIVATE
                )
            sharedPreferences.all.filterValues { AnimeNameAdapter.getSubDub(it.toString()) != AnimeNameAdapter.Companion.SubDubType.NULL }
                .forEach { value ->
                    val setValue = AnimeNameAdapter.setSubDub(value.value.toString(), type)
                    if (setValue != null) {
                        sharedPreferences.edit().putString(value.key, setValue).apply()
                    }
                }
        }
    }

    override fun isDubAvailableSeparately(sourceLang: Int?): Boolean {
        val configurableSource = extension.sources[sourceLanguage] as? ConfigurableAnimeSource
            ?: return false
        currContext()?.let { context ->
            Logger.log("isDubAvailableSeparately: ${configurableSource.getPreferenceKey()}")
            val sharedPreferences =
                context.getSharedPreferences(
                    configurableSource.getPreferenceKey(),
                    Context.MODE_PRIVATE
                )
            sharedPreferences.all.filterValues {
                AnimeNameAdapter.setSubDub(
                    it.toString(),
                    AnimeNameAdapter.Companion.SubDubType.NULL
                ) != null
            }
                .forEach { _ -> return true }
        }
        return false
    }

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
        } as? AnimeHttpSource ?: (extension.sources[sourceLanguage] as? AnimeCatalogueSource
            ?: return emptyList())
        try {
            val res = source.getEpisodeList(sAnime)

            val sortedEpisodes = if (res[0].episode_number == -1f) {
                // Find the number in the string and sort by that number
                val sortedByStringNumber = res.sortedBy {
                    val matchResult = AnimeNameAdapter.findEpisodeNumber(it.name)
                    val number = matchResult ?: Float.MAX_VALUE
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
            } else if (episodesAreIncrementing(res)) {
                res.sortedBy { it.episode_number }
            } else {
                var episodeCounter = 1f
                // Group by season, sort within each season, and then renumber while keeping episode number 0 as is
                val seasonGroups =
                    res.groupBy { AnimeNameAdapter.findSeasonNumber(it.name) ?: 0 }
                seasonGroups.keys.sortedBy { it.toInt() }
                    .flatMap { season ->
                        seasonGroups[season]?.sortedBy { it.episode_number }?.map { episode ->
                            if (episode.episode_number != 0f) { // Skip renumbering for episode number 0
                                val potentialNumber =
                                    AnimeNameAdapter.findEpisodeNumber(episode.name)
                                if (potentialNumber != null) {
                                    episode.episode_number = potentialNumber
                                } else {
                                    episode.episode_number = episodeCounter
                                }
                                episodeCounter++
                            }
                            episode
                        } ?: emptyList()
                    }
            }
            return sortedEpisodes.map { SEpisodeToEpisode(it) }
        } catch (e: Exception) {
            Logger.log("Exception: $e")
        }
        return emptyList()
    }

    private fun episodesAreIncrementing(episodes: List<SEpisode>): Boolean {
        val sortedEpisodes = episodes.sortedBy { it.episode_number }
        val takenNumbers = mutableListOf<Float>()
        sortedEpisodes.forEach {
            if (it.episode_number !in takenNumbers) {
                takenNumbers.add(it.episode_number)
            } else {
                return false
            }
        }
        return true
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
        } as? AnimeHttpSource ?: (extension.sources[sourceLanguage] as? AnimeCatalogueSource
            ?: return emptyList())

        return try {
            val videos = source.getVideoList(sEpisode)
            videos.map { VideoToVideoServer(it) }
        } catch (e: Exception) {
            Logger.log("Exception occurred: ${e.message}")
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
        } as? AnimeHttpSource ?: (extension.sources[sourceLanguage] as? AnimeCatalogueSource
            ?: return emptyList())
        return try {
            val res = source.fetchSearchAnime(1, query, source.getFilterList()).awaitSingle()
            Logger.log("query: $query")
            convertAnimesPageToShowResponse(res)
        } catch (e: CloudflareBypassException) {
            Logger.log("Exception in search: $e")
            withContext(Dispatchers.Main) {
                snackString("Failed to bypass Cloudflare")
            }
            emptyList()
        } catch (e: Exception) {
            Logger.log("General exception in search: $e")
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
                if (sEpisode.episode_number % 1 == 0f) {
                    episodeNumberInt.toInt().toString()
                } else {
                    sEpisode.episode_number.toString()
                }
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
            Logger.log("chapterList size: ${chapterList.size}")
            Logger.log("chapterList: ${chapterList[1].title}")
            Logger.log("chapterList: ${chapterList[1].description}")
            chapterList
        } catch (e: Exception) {
            Logger.log("loadChapters Exception: $e")
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
                Logger.log("source.name " + source.name)
                val res = source.getPageList(sChapter)
                val reIndexedPages =
                    res.mapIndexed { index, page -> Page(index, page.url, page.imageUrl, page.uri) }

                val deferreds = reIndexedPages.map { page ->
                    async(Dispatchers.IO) {
                        mangaCache.put(page.imageUrl ?: "", ImageData(page, source))
                        imageDataList += ImageData(page, source)
                        Logger.log("put page: ${page.imageUrl}")
                        pageToMangaImage(page)
                    }
                }

                deferreds.awaitAll()

            } catch (e: Exception) {
                Logger.log("loadImages Exception: $e")
                snackString("Failed to load images: $e")
                emptyList()
            }
        }
        return ret
    }

    suspend fun imageList(chapterLink: String, sChapter: SChapter): List<ImageData> {
        val source = try {
            extension.sources[sourceLanguage]
        } catch (e: Exception) {
            sourceLanguage = 0
            extension.sources[sourceLanguage]
        } as? HttpSource ?: return emptyList()

        return coroutineScope {
            try {
                Logger.log("source.name " + source.name)
                val res = source.getPageList(sChapter)
                val reIndexedPages =
                    res.mapIndexed { index, page -> Page(index, page.url, page.imageUrl, page.uri) }

                val semaphore = Semaphore(5)
                val deferreds = reIndexedPages.map { page ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            ImageData(page, source)
                        }
                    }
                }

                deferreds.awaitAll()
            } catch (e: Exception) {
                Logger.log("loadImages Exception: $e")
                snackString("Failed to load images: $e")
                emptyList()
            }
        }
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
                Logger.log("Response: ${response.code}")
                Logger.log("Response: ${response.message}")

                // Convert the Response to an InputStream
                val inputStream = response.body.byteStream()

                // Convert InputStream to Bitmap
                val bitmap = BitmapFactory.decodeStream(inputStream)

                inputStream.close()
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
                Logger.log("An error occurred: ${e.message}")
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
                val inputStream = response.body.byteStream()

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

                inputStream.close()
            } catch (e: Exception) {
                // Handle any exceptions
                Logger.log("An error occurred: ${e.message}")
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
            Logger.log("Exception while saving image: ${e.message}")
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
            val res = source.fetchSearchManga(1, query, source.getFilterList()).awaitSingle()
            Logger.log("res observable: $res")
            convertMangasPageToShowResponse(res)
        } catch (e: CloudflareBypassException) {
            Logger.log("Exception in search: $e")
            withContext(Dispatchers.Main) {
                snackString("Failed to bypass Cloudflare")
            }
            emptyList()
        } catch (e: Exception) {
            Logger.log("General exception in search: $e")
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
            sChapter.name,
            null,
            sChapter.scanlator,
            sChapter,
            sChapter.date_upload
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

    private fun AniVideoToSaiVideo(aniVideo: Video): ani.dantotsu.parsers.Video {
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
            // this solves a problem no one has, so I'm commenting it out for now
            //if (format == null) {
            //    val networkHelper = Injekt.get<NetworkHelper>()
            //    format = headRequest(videoUrl, networkHelper)
            //}
        }

        // If the format is still undetermined, log an error
        if (format == null) {
            Logger.log("Unknown video format: $videoUrl")
            format = VideoType.CONTAINER
        }
        val headersMap: Map<String, String> =
            aniVideo.headers?.toMultimap()?.mapValues { it.value.joinToString() } ?: mapOf()


        return Video(
            number,
            format,
            FileUrl(videoUrl, headersMap),
            if (aniVideo.totalContentLength == 0L) null else aniVideo.bytesDownloaded.toDouble()
        )
    }

    private fun getVideoType(fileName: String): VideoType? {
        val type = when {
            fileName.endsWith(".mp4", ignoreCase = true) || fileName.endsWith(
                ".mkv",
                ignoreCase = true
            ) -> VideoType.CONTAINER

            fileName.endsWith(".m3u8", ignoreCase = true) -> VideoType.M3U8
            fileName.endsWith(".mpd", ignoreCase = true) -> VideoType.DASH
            else -> null
        }

        return type
    }

    private fun headRequest(fileName: String, networkHelper: NetworkHelper): VideoType? {
        return try {
            Logger.log("attempting head request for $fileName")
            val request = Request.Builder()
                .url(fileName)
                .head()
                .build()

            networkHelper.client.newCall(request).execute().use { response ->
                val contentType = response.header("Content-Type")
                val contentDisposition = response.header("Content-Disposition")

                if (contentType != null) {
                    when {
                        contentType.contains("mpegurl", ignoreCase = true) -> VideoType.M3U8
                        contentType.contains("dash", ignoreCase = true) -> VideoType.DASH
                        contentType.contains("mp4", ignoreCase = true) -> VideoType.CONTAINER
                        else -> null
                    }
                } else if (contentDisposition != null) {
                    when {
                        contentDisposition.contains("mpegurl", ignoreCase = true) -> VideoType.M3U8
                        contentDisposition.contains("dash", ignoreCase = true) -> VideoType.DASH
                        contentDisposition.contains("mp4", ignoreCase = true) -> VideoType.CONTAINER
                        else -> null
                    }
                } else {
                    Logger.log("failed head request for $fileName")
                    null
                }

            }
        } catch (e: Exception) {
            Logger.log("Exception in headRequest: $e")
            null
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

    private fun findSubtitleType(url: String): SubtitleType {
        // First, try to determine the type based on the URL file extension
        val type: SubtitleType = when {
            url.endsWith(".vtt", true) -> SubtitleType.VTT
            url.endsWith(".ass", true) -> SubtitleType.ASS
            url.endsWith(".srt", true) -> SubtitleType.SRT
            else -> SubtitleType.UNKNOWN
        }

        return type
    }
}