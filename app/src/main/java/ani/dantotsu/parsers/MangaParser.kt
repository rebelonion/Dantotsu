package ani.dantotsu.parsers

import ani.dantotsu.FileUrl
import ani.dantotsu.media.MediaNameAdapter
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import java.io.Serializable

abstract class MangaParser : BaseParser() {

    /**
     * Takes ShowResponse.link and ShowResponse.extra (if any) as arguments & gives a list of total chapters present on the site.
     * **/
    abstract suspend fun loadChapters(
        mangaLink: String,
        extra: Map<String, String>?,
        sManga: SManga
    ): List<MangaChapter>

    /**
     * Takes ShowResponse.link, ShowResponse.extra & the Last Largest Chapter Number known by app as arguments
     *
     * Returns the latest chapter (If overriding, Make sure the chapter is actually the latest chapter)
     * Returns null, if no latest chapter is found.
     * **/
    open suspend fun getLatestChapter(
        mangaLink: String,
        extra: Map<String, String>?,
        sManga: SManga,
        latest: Float
    ): MangaChapter? {
        val chapter = loadChapters(mangaLink, extra, sManga)
        val max = chapter
            .maxByOrNull { MediaNameAdapter.findChapterNumber(it.number) ?: 0f }
        return max
            ?.takeIf { latest < (MediaNameAdapter.findChapterNumber(it.number) ?: 0.001f) }
    }

    /**
     * Takes MangaChapter.link as an argument & returns a list of MangaImages with their Url (with headers & transformations, if needed)
     * **/
    abstract suspend fun loadImages(chapterLink: String, sChapter: SChapter): List<MangaImage>


    open fun getTransformation(): BitmapTransformation? = null
}

class EmptyMangaParser : MangaParser() {
    override val name: String = "None"
    override val saveName: String = "None"

    override suspend fun loadChapters(
        mangaLink: String,
        extra: Map<String, String>?,
        sManga: SManga
    ): List<MangaChapter> = emptyList()

    override suspend fun loadImages(chapterLink: String, sChapter: SChapter): List<MangaImage> =
        emptyList()

    override suspend fun search(query: String): List<ShowResponse> = emptyList()
}

data class MangaChapter(
    /**
     * Number of the Chapter in "String",
     *
     * useful in cases where chapter is not a number
     * **/
    val number: String,

    /**
     * Link that links to the chapter page containing videos
     * **/
    val link: String,

    //Self-Descriptive
    val title: String? = null,
    val description: String? = null,
    val scanlator: String,
    val sChapter: SChapter,
    val date: Long? = null,
)

data class MangaImage(
    /**
     * The direct url to the Image of a page in a chapter
     *
     * Supports jpeg,jpg,png & gif(non animated) afaik
     * **/
    val url: FileUrl,

    val useTransformation: Boolean = false,

    val page: Page? = null,
) : Serializable {
    constructor(url: String, useTransformation: Boolean = false, page: Page? = null)
            : this(FileUrl(url), useTransformation, page)
}
