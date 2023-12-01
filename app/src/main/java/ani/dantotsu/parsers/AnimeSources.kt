package ani.dantotsu.parsers

import ani.dantotsu.Lazier
import ani.dantotsu.lazyList
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

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
    private val aList: List<Lazier<BaseParser>> = lazyList(
    )

    override val list = listOf(aList, AnimeSources.list).flatten()
}
