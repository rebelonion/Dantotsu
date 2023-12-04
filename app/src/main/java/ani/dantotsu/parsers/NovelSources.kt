package ani.dantotsu.parsers

import android.util.Log
import ani.dantotsu.Lazier
import ani.dantotsu.parsers.novel.DynamicNovelParser
import ani.dantotsu.parsers.novel.NovelExtension
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

object NovelSources : NovelReadSources() {
    override var list: List<Lazier<BaseParser>> = emptyList()

    suspend fun init(fromExtensions: StateFlow<List<NovelExtension.Installed>>) {
        // Initialize with the first value from StateFlow
        val initialExtensions = fromExtensions.first()
        list = createParsersFromExtensions(initialExtensions) + Lazier(
            { OfflineNovelParser() },
            "Downloaded"
        )

        // Update as StateFlow emits new values
        fromExtensions.collect { extensions ->
            list = createParsersFromExtensions(extensions) + Lazier(
                { OfflineNovelParser() },
                "Downloaded"
            )
        }
    }

    private fun createParsersFromExtensions(extensions: List<NovelExtension.Installed>): List<Lazier<BaseParser>> {
        Log.d("NovelSources", "createParsersFromExtensions")
        Log.d("NovelSources", extensions.toString())
        return extensions.map { extension ->
            val name = extension.name
            Lazier({ DynamicNovelParser(extension) }, name)
        }
    }
}