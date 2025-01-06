package ani.dantotsu.parsers

import android.app.Application
import ani.dantotsu.download.DownloadCompat.Companion.loadChaptersCompat
import ani.dantotsu.download.DownloadCompat.Companion.loadImagesCompat
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.DownloadsManager.Companion.getSubDirectory
import ani.dantotsu.media.MediaNameAdapter
import ani.dantotsu.media.MediaType
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import me.xdrop.fuzzywuzzy.FuzzySearch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class OfflineMangaParser : MangaParser() {
    private val downloadManager = Injekt.get<DownloadsManager>()
    private val context = Injekt.get<Application>()

    override val hostUrl: String = "Offline"
    override val name: String = "Offline"
    override val saveName: String = "Offline"
    override suspend fun loadChapters(
        mangaLink: String,
        extra: Map<String, String>?,
        sManga: SManga
    ): List<MangaChapter> {
        val directory = getSubDirectory(context, MediaType.MANGA, false, mangaLink)
        //get all of the folder names and add them to the list
        val chapters = mutableListOf<MangaChapter>()
        if (directory?.exists() == true) {
            directory.listFiles().forEach {
                val scanlator = downloadManager.mangaDownloadedTypes.find { items ->
                    items.titleName == mangaLink &&
                            items.chapterName == it.name
                }?.scanlator ?: "Unknown"
                if (it.isDirectory) {
                    val chapter = MangaChapter(
                        it.name!!,
                        "$mangaLink/${it.name}",
                        it.name,
                        null,
                        scanlator,
                        SChapter.create()
                    )
                    chapters.add(chapter)
                }
            }
        }
        chapters.addAll(loadChaptersCompat(mangaLink, extra, sManga))
        return chapters.sortedBy { MediaNameAdapter.findChapterNumber(it.number) }
    }

    override suspend fun loadImages(chapterLink: String, sChapter: SChapter): List<MangaImage> {
        val title = chapterLink.split("/").first()
        val chapter = chapterLink.split("/").last()
        val directory = getSubDirectory(context, MediaType.MANGA, false, title, chapter)
        val images = mutableListOf<MangaImage>()
        val imageNumberRegex = Regex("""(\d+)\.jpg$""")
        if (directory?.exists() == true) {
            directory.listFiles().forEach {
                if (it.isFile) {
                    val image = MangaImage(it.uri.toString(), false, null)
                    images.add(image)
                }
            }
            for (image in images) {
                Logger.log("imageNumber: ${image.url.url}")
            }
        }
        return if (images.isNotEmpty()) {
            images.sortBy { image ->
                val matchResult = imageNumberRegex.find(image.url.url)
                matchResult?.groups?.get(1)?.value?.toIntOrNull() ?: Int.MAX_VALUE
            }
            images
        } else {
            loadImagesCompat(chapterLink, sChapter)
        }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val titles = downloadManager.mangaDownloadedTypes.map { it.titleName }.distinct()
        val returnTitlesPair: MutableList<Pair<String, Int>> = mutableListOf()
        for (title in titles) {
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

}