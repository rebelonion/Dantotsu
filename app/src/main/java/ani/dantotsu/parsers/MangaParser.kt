package ani.dantotsu.parsers

import ani.dantotsu.FileUrl
import ani.dantotsu.media.Media
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.io.Serializable

abstract class MangaParser : BaseParser() {

    /**
     * Takes ShowResponse.link and ShowResponse.extra (if any) as arguments & gives a list of total chapters present on the site.
     * **/
    abstract suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter>

    /**
     * Takes ShowResponse.link, ShowResponse.extra & the Last Largest Chapter Number known by app as arguments
     *
     * Returns the latest chapter (If overriding, Make sure the chapter is actually the latest chapter)
     * Returns null, if no latest chapter is found.
     * **/
    open suspend fun getLatestChapter(mangaLink: String, extra: Map<String, String>?, latest: Float): MangaChapter? {
        return loadChapters(mangaLink, extra)
            .maxByOrNull { it.number.toFloatOrNull() ?: 0f }
            ?.takeIf { latest < (it.number.toFloatOrNull() ?: 0.001f) }
    }

    /**
     * Takes MangaChapter.link as an argument & returns a list of MangaImages with their Url (with headers & transformations, if needed)
     * **/
    abstract suspend fun loadImages(chapterLink: String): List<MangaImage>

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
)

data class MangaImage(
    /**
     * The direct url to the Image of a page in a chapter
     *
     * Supports jpeg,jpg,png & gif(non animated) afaik
     * **/
    val url: FileUrl,

    val useTransformation: Boolean = false
) : Serializable{
    constructor(url: String,useTransformation: Boolean=false)
            : this(FileUrl(url),useTransformation)
}
