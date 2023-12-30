package ani.dantotsu.parsers

import android.os.Environment
import ani.dantotsu.currContext
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.logger
import ani.dantotsu.media.manga.MangaNameAdapter
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import me.xdrop.fuzzywuzzy.FuzzySearch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

class OfflineMangaParser : MangaParser() {
    private val downloadManager = Injekt.get<DownloadsManager>()

    override val hostUrl: String = "Offline"
    override val name: String = "Offline"
    override val saveName: String = "Offline"
    override suspend fun loadChapters(
        mangaLink: String,
        extra: Map<String, String>?,
        sManga: SManga
    ): List<MangaChapter> {
        val directory = File(
            currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/Manga/$mangaLink"
        )
        //get all of the folder names and add them to the list
        val chapters = mutableListOf<MangaChapter>()
        if (directory.exists()) {
            directory.listFiles()?.forEach {
                if (it.isDirectory) {
                    val chapter = MangaChapter(
                        it.name,
                        "$mangaLink/${it.name}",
                        it.name,
                        null,
                        null,
                        SChapter.create()
                    )
                    chapters.add(chapter)
                }
            }
            chapters.sortBy { MangaNameAdapter.findChapterNumber(it.number) }
            return chapters
        }
        return emptyList()
    }

    override suspend fun loadImages(chapterLink: String, sChapter: SChapter): List<MangaImage> {
        val directory = File(
            currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/Manga/$chapterLink"
        )
        val images = mutableListOf<MangaImage>()
        val imageNumberRegex = Regex("""(\d+)\.jpg$""")
        if (directory.exists()) {
            directory.listFiles()?.forEach {
                if (it.isFile) {
                    val image = MangaImage(it.absolutePath, false, null)
                    images.add(image)
                }
            }
            images.sortBy { image ->
                val matchResult = imageNumberRegex.find(image.url.url)
                matchResult?.groups?.get(1)?.value?.toIntOrNull() ?: Int.MAX_VALUE
            }
            for (image in images) {
                logger("imageNumber: ${image.url.url}")
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