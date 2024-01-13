package ani.dantotsu.parsers

import ani.dantotsu.FileUrl
import ani.dantotsu.media.Media

abstract class NovelParser : BaseParser() {

    abstract val volumeRegex: Regex

    abstract suspend fun loadBook(link: String, extra: Map<String, String>?): Book

    fun List<ShowResponse>.sortByVolume(query: String): List<ShowResponse> {
        val sorted = groupBy { res ->
            val match = volumeRegex.find(res.name)?.groupValues
                ?.firstOrNull { it.isNotEmpty() }
                ?.substringAfter(" ")
                ?.toDoubleOrNull() ?: Double.MAX_VALUE
            match
        }.toSortedMap().values

        val volumes = sorted.map { showList ->
            val nonDefaultCoverShows = showList.filter { it.coverUrl.url != defaultImage }
            val bestShow = nonDefaultCoverShows.firstOrNull { it.name.contains(query) }
                ?: nonDefaultCoverShows.firstOrNull()
                ?: showList.first()
            bestShow
        }
        val remainingShows = sorted.flatten() - volumes.toSet()

        return volumes + remainingShows
    }

    suspend fun sortedSearch(mediaObj: Media): List<ShowResponse> {
        //val query = mediaObj.name ?: mediaObj.nameRomaji
        //return search(query).sortByVolume(query)
        val results: List<ShowResponse>
        return if (mediaObj.name != null) {
            val query = mediaObj.name
            results = search(query).sortByVolume(query)
            results.ifEmpty {
                val q = mediaObj.nameRomaji
                search(q).sortByVolume(q)
            }
        } else {
            val query = mediaObj.nameRomaji
            search(query).sortByVolume(query)
        }
    }
}

data class Book(
    val name: String,
    val img: FileUrl,
    val description: String? = null,
    val links: List<FileUrl>
) {
    constructor (
        name: String,
        img: String,
        description: String? = null,
        links: List<String>
    ) : this(
        name,
        FileUrl(img),
        description,
        links.map { FileUrl(it) }
    )
}