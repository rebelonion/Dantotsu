package ani.dantotsu.parsers

import ani.dantotsu.Lazier
import ani.dantotsu.lazyList
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

object MangaSources : MangaReadSources() {
    // Instantiate the static parser
    private val offlineMangaParser by lazy { OfflineMangaParser() }

    override var list: List<Lazier<BaseParser>> = emptyList()

    suspend fun init(fromExtensions: StateFlow<List<MangaExtension.Installed>>) {
        // Initialize with the first value from StateFlow
        val initialExtensions = fromExtensions.first()
        list = createParsersFromExtensions(initialExtensions) + Lazier({ OfflineMangaParser() }, "Downloaded")

        // Update as StateFlow emits new values
        fromExtensions.collect { extensions ->
            list = createParsersFromExtensions(extensions) + Lazier({ OfflineMangaParser() }, "Downloaded")
        }
    }

    private fun createParsersFromExtensions(extensions: List<MangaExtension.Installed>): List<Lazier<BaseParser>> {
        return extensions.map { extension ->
            val name = extension.name
            Lazier({ DynamicMangaParser(extension) }, name)
        }
    }
}

object HMangaSources : MangaReadSources() {
    val aList: List<Lazier<BaseParser>> = lazyList()
    suspend fun init(fromExtensions: StateFlow<List<MangaExtension.Installed>>) {
         //todo
    }
    override val list = listOf(aList,MangaSources.list).flatten()
}
