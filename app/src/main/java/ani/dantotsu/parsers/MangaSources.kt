package ani.dantotsu.parsers

import android.content.Context
import ani.dantotsu.Lazier
import ani.dantotsu.lazyList
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

object MangaSources : MangaReadSources() {
    override var list: List<Lazier<BaseParser>> = emptyList()
    var pinnedMangaSources: Set<String> = emptySet()

    suspend fun init(fromExtensions: StateFlow<List<MangaExtension.Installed>>, context: Context) {
        val sharedPrefs = context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)
        pinnedMangaSources =
            sharedPrefs.getStringSet("pinned_manga_sources", emptySet()) ?: emptySet()

        // Initialize with the first value from StateFlow
        val initialExtensions = fromExtensions.first()
        list = createParsersFromExtensions(initialExtensions) + Lazier(
            { OfflineMangaParser() },
            "Downloaded"
        )

        // Update as StateFlow emits new values
        fromExtensions.collect { extensions ->
            list = sortPinnedMangaSources(
                createParsersFromExtensions(extensions),
                pinnedMangaSources
            ) + Lazier(
                { OfflineMangaParser() },
                "Downloaded"
            )
        }
    }

    fun performReorderMangaSources() {
        //remove the downloaded source from the list to avoid duplicates
        list = list.filter { it.name != "Downloaded" }
        list = sortPinnedMangaSources(list, pinnedMangaSources) + Lazier(
            { OfflineMangaParser() },
            "Downloaded"
        )
    }

    private fun createParsersFromExtensions(extensions: List<MangaExtension.Installed>): List<Lazier<BaseParser>> {
        return extensions.map { extension ->
            val name = extension.name
            Lazier({ DynamicMangaParser(extension) }, name)
        }
    }

    private fun sortPinnedMangaSources(
        Sources: List<Lazier<BaseParser>>,
        pinnedMangaSources: Set<String>
    ): List<Lazier<BaseParser>> {
        //find the pinned sources
        val pinnedSources = Sources.filter { pinnedMangaSources.contains(it.name) }
        //find the unpinned sources
        val unpinnedSources = Sources.filter { !pinnedMangaSources.contains(it.name) }
        //put the pinned sources at the top of the list
        return pinnedSources + unpinnedSources
    }
}

object HMangaSources : MangaReadSources() {
    val aList: List<Lazier<BaseParser>> = lazyList()
    suspend fun init(fromExtensions: StateFlow<List<MangaExtension.Installed>>) {
        //todo
    }

    override val list = listOf(aList, MangaSources.list).flatten()
}
