package ani.dantotsu.parsers

import android.content.Context
import ani.dantotsu.Lazier
import ani.dantotsu.lazyList
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

object AnimeSources : WatchSources() {
    override var list: List<Lazier<BaseParser>> = emptyList()
    var pinnedAnimeSources: Set<String> = emptySet()

    suspend fun init(fromExtensions: StateFlow<List<AnimeExtension.Installed>>, context: Context) {
        val sharedPrefs = context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)
        pinnedAnimeSources =
            sharedPrefs.getStringSet("pinned_anime_sources", emptySet()) ?: emptySet()

        // Initialize with the first value from StateFlow
        val initialExtensions = fromExtensions.first()
        list = createParsersFromExtensions(initialExtensions) + Lazier(
            { OfflineAnimeParser() },
            "Downloaded"
        )

        // Update as StateFlow emits new values
        fromExtensions.collect { extensions ->
            list = sortPinnedAnimeSources(
                createParsersFromExtensions(extensions),
                pinnedAnimeSources
            ) + Lazier(
                { OfflineAnimeParser() },
                "Downloaded"
            )
        }
    }

    fun performReorderAnimeSources() {
        //remove the downloaded source from the list to avoid duplicates
        list = list.filter { it.name != "Downloaded" }
        list = sortPinnedAnimeSources(list, pinnedAnimeSources) + Lazier(
            { OfflineAnimeParser() },
            "Downloaded"
        )
    }

    private fun createParsersFromExtensions(extensions: List<AnimeExtension.Installed>): List<Lazier<BaseParser>> {
        return extensions.map { extension ->
            val name = extension.name
            Lazier({ DynamicAnimeParser(extension) }, name)
        }
    }

    private fun sortPinnedAnimeSources(
        Sources: List<Lazier<BaseParser>>,
        pinnedAnimeSources: Set<String>
    ): List<Lazier<BaseParser>> {
        //find the pinned sources
        val pinnedSources = Sources.filter { pinnedAnimeSources.contains(it.name) }
        //find the unpinned sources
        val unpinnedSources = Sources.filter { !pinnedAnimeSources.contains(it.name) }
        //put the pinned sources at the top of the list
        return pinnedSources + unpinnedSources
    }
}


object HAnimeSources : WatchSources() {
    private val aList: List<Lazier<BaseParser>> = lazyList(
    )

    override val list = listOf(aList, AnimeSources.list).flatten()
}
