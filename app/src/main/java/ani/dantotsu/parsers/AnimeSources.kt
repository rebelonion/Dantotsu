package ani.dantotsu.parsers

import ani.dantotsu.Lazier
import ani.dantotsu.aniyomi.anime.model.AnimeExtension
import ani.dantotsu.lazyList
//import ani.dantotsu.parsers.anime.AllAnime
//import ani.dantotsu.parsers.anime.AnimeDao
//import ani.dantotsu.parsers.anime.AnimePahe
//import ani.dantotsu.parsers.anime.Gogo
//import ani.dantotsu.parsers.anime.Haho
//import ani.dantotsu.parsers.anime.HentaiFF
//import ani.dantotsu.parsers.anime.HentaiMama
//import ani.dantotsu.parsers.anime.HentaiStream
//import ani.dantotsu.parsers.anime.Marin
//import ani.dantotsu.parsers.anime.AniWave
//import ani.dantotsu.parsers.anime.Kaido
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
/*
object AnimeSources_old : WatchSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        "AllAnime" to ::AllAnime,
        "Gogo" to ::Gogo,
        "Kaido" to ::Kaido,
        "Marin" to ::Marin,
        "AnimePahe" to ::AnimePahe,
        "AniWave" to ::AniWave,
        "AnimeDao" to ::AnimeDao,
    )
}
*/
object AnimeSources : WatchSources() {
    override var list: List<Lazier<BaseParser>> = emptyList()

    suspend fun init(fromExtensions: StateFlow<List<AnimeExtension.Installed>>) {
        // Initialize with the first value from StateFlow
        val initialExtensions = fromExtensions.first()
        list = createParsersFromExtensions(initialExtensions)

        // Update as StateFlow emits new values
        fromExtensions.collect { extensions ->
            list = createParsersFromExtensions(extensions)
        }
    }

    private fun createParsersFromExtensions(extensions: List<AnimeExtension.Installed>): List<Lazier<BaseParser>> {
        return extensions.map { extension ->
            val name = extension.name
            Lazier({ DynamicAnimeParser(extension) }, name)
        }
    }
}



object HAnimeSources : WatchSources() {
    private val aList: List<Lazier<BaseParser>>  = lazyList(
        //"HentaiMama" to ::HentaiMama,
        //"Haho" to ::Haho,
        //"HentaiStream" to ::HentaiStream,
        //"HentaiFF" to ::HentaiFF,
    )

    override val list = listOf(aList,AnimeSources.list).flatten()
}
