package ani.dantotsu.parsers

import android.app.Application
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
                if (it.isDirectory) {
                    val chapter = MangaChapter(
                        it.name!!,
                        "$mangaLink/${it.name}",
                        it.name,
                        null,
                        null,
                        SChapter.create()
                    )
                    chapters.add(chapter)
                }
            }
            chapters.sortBy { MediaNameAdapter.findChapterNumber(it.number) }
            return chapters
        }
        return emptyList()
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
            images.sortBy { image ->
                val matchResult = imageNumberRegex.find(image.url.url)
                matchResult?.groups?.get(1)?.value?.toIntOrNull() ?: Int.MAX_VALUE
            }
            for (image in images) {
                Logger.log("imageNumber: ${image.url.url}")
            }
            return images
        }
        return emptyList()
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val titles = downloadManager.mangaDownloadedTypes.map { it.title }.distinct()
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

}