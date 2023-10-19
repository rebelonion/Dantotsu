package ani.dantotsu.parsers

import ani.dantotsu.FileUrl
import ani.dantotsu.media.Media
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import java.io.Serializable

abstract class MangaParser : BaseParser() {

    /**
     * Takes ShowResponse.link and ShowResponse.extra (if any) as arguments & gives a list of total chapters present on the site.
     * **/
    abstract suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?, sManga: SManga): List<MangaChapter>

    /**
     * Takes ShowResponse.link, ShowResponse.extra & the Last Largest Chapter Number known by app as arguments
     *
     * Returns the latest chapter (If overriding, Make sure the chapter is actually the latest chapter)
     * Returns null, if no latest chapter is found.
     * **/
    open suspend fun getLatestChapter(mangaLink: String, extra: Map<String, String>?, sManga: SManga, latest: Float): MangaChapter? {
        return loadChapters(mangaLink, extra, sManga)
            .maxByOrNull { it.number.toFloatOrNull() ?: 0f }
            ?.takeIf { latest < (it.number.toFloatOrNull() ?: 0.001f) }
    }

    /**
     * Takes MangaChapter.link as an argument & returns a list of MangaImages with their Url (with headers & transformations, if needed)
     * **/
    abstract suspend fun loadImages(chapterLink: String, sChapter: SChapter): List<MangaImage>

    override suspend fun autoSearch(mediaObj: Media): ShowResponse? {
        var response = loadSavedShowResponse(mediaObj.id)
        if (response != null) {
            saveShowResponse(mediaObj.id, response, true)
        } else {
            setUserText("Searching : ${mediaObj.mangaName()}")
            response = search(mediaObj.mangaName()).let { if (it.isNotEmpty()) it[0] else null }

            if (response == null) {
                setUserText("Searching : ${mediaObj.nameRomaji}")
                response = search(mediaObj.nameRomaji).let { if (it.isNotEmpty()) it[0] else null }
            }
            saveShowResponse(mediaObj.id, response)
        }
        return response
    }

    open fun getTransformation(): BitmapTransformation? = null
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

    val sChapter: SChapter,
)

data class MangaImage(
    /**
     * The direct url to the Image of a page in a chapter
     *
     * Supports jpeg,jpg,png & gif(non animated) afaik
     * **/
    val url: FileUrl,

    val useTransformation: Boolean = false,

    val page: Page
) : Serializable{
    constructor(url: String,useTransformation: Boolean=false, page: Page)
            : this(FileUrl(url),useTransformation, page)
}
