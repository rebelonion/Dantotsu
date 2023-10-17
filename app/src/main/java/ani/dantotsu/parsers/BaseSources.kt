package ani.dantotsu.parsers

import ani.dantotsu.Lazier
import ani.dantotsu.media.anime.Episode
import ani.dantotsu.media.manga.MangaChapter
import ani.dantotsu.media.Media
import ani.dantotsu.tryWithSuspend
import eu.kanade.tachiyomi.animesource.model.SAnime

abstract class WatchSources : BaseSources() {

    override operator fun get(i: Int): AnimeParser {
        return (list.getOrNull(i)?:list[0]).get.value as AnimeParser
    }

    suspend fun loadEpisodesFromMedia(i: Int, media: Media): MutableMap<String, Episode> {
        return tryWithSuspend(true) {
            val res = get(i).autoSearch(media) ?: return@tryWithSuspend mutableMapOf()
            loadEpisodes(i, res.link, res.extra, res.sAnime)
        } ?: mutableMapOf()
    }

    suspend fun loadEpisodes(i: Int, showLink: String, extra: Map<String, String>?, sAnime: SAnime?): MutableMap<String, Episode> {
        println("finder333 $showLink")
        val map = mutableMapOf<String, Episode>()
        val parser = get(i)
        tryWithSuspend(true) {
            if (sAnime != null) {
                parser.loadEpisodes(showLink,extra, sAnime).forEach {
                    map[it.number] = Episode(it.number, it.link, it.title, it.description, it.thumbnail, it.isFiller, extra = it.extra, sEpisode = it.sEpisode)
                }
            }
        }
        return map
    }

}

abstract class MangaReadSources : BaseSources() {

    override operator fun get(i: Int): MangaParser {
        return (list.getOrNull(i)?:list[0]).get.value as MangaParser
    }

    suspend fun loadChaptersFromMedia(i: Int, media: Media): MutableMap<String, MangaChapter> {
        return tryWithSuspend(true) {
            val res = get(i).autoSearch(media) ?: return@tryWithSuspend mutableMapOf()
            loadChapters(i, res)
        } ?: mutableMapOf()
    }

    suspend fun loadChapters(i: Int, show: ShowResponse): MutableMap<String, MangaChapter> {
        val map = mutableMapOf<String, MangaChapter>()
        val parser = get(i)
        tryWithSuspend(true) {
            parser.loadChapters(show.link, show.extra).forEach {
                map[it.number] = MangaChapter(it)
            }
        }
        return map
    }
}

abstract class NovelReadSources : BaseSources(){
    override operator fun get(i: Int): NovelParser? {
        return if (list.isNotEmpty()) {
            (list.getOrNull(i) ?: list[0]).get.value as NovelParser
        } else {
            return EmptyNovelParser()
        }
    }

}

class EmptyNovelParser : NovelParser() {

    override val volumeRegex: Regex = Regex("")

    override suspend fun loadBook(link: String, extra: Map<String, String>?): Book {
        return Book("","", null, emptyList())  // Return an empty Book object or some default value
    }

    override suspend fun search(query: String): List<ShowResponse> {
        return listOf() // Return an empty list or some default value
    }
}

abstract class BaseSources {
    abstract val list: List<Lazier<BaseParser>>

    val names: List<String> get() = list.map { it.name }

    fun flushText() {
        list.forEach {
            if (it.get.isInitialized())
                it.get.value?.showUserText = ""
        }
    }

    open operator fun get(i: Int): BaseParser? {
        return list[i].get.value
    }

    fun saveResponse(i: Int, mediaId: Int, response: ShowResponse) {
        get(i)?.saveShowResponse(mediaId, response, true)
    }
}



