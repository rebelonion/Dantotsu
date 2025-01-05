package ani.dantotsu.media.manga

import ani.dantotsu.parsers.MangaChapter
import ani.dantotsu.parsers.MangaImage
import eu.kanade.tachiyomi.source.model.SChapter
import java.io.Serializable
import kotlin.math.floor

data class MangaChapter(
    val number: String,
    var link: String,
    var title: String? = null,
    var description: String? = null,
    var sChapter: SChapter,
    val scanlator: String? = null,
    val date: Long? = null,
    var progress: String? = ""
) : Serializable {
    constructor(chapter: MangaChapter) : this(
        chapter.number,
        chapter.link,
        chapter.title,
        chapter.description,
        chapter.sChapter,
        chapter.scanlator,
        chapter.date
    )

    private val images = mutableListOf<MangaImage>()
    fun images(): List<MangaImage> = images
    fun addImages(image: List<MangaImage>) {
        if (images.isNotEmpty()) return
        image.forEach { images.add(it) }
        (0..floor((images.size.toFloat() - 1f) / 2).toInt()).forEach {
            val i = it * 2
            dualPages.add(images[i] to images.getOrNull(i + 1))
        }
    }

    private val dualPages = mutableListOf<Pair<MangaImage, MangaImage?>>()
    fun dualPages(): List<Pair<MangaImage, MangaImage?>> = dualPages

    fun uniqueNumber(): String = "${number}-${scanlator ?: "Unknown"}"

}
