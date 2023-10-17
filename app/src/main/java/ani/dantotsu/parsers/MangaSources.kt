package ani.dantotsu.parsers

import ani.dantotsu.Lazier
import ani.dantotsu.lazyList

object MangaSources : MangaReadSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
    )
}

object HMangaSources : MangaReadSources() {
    val aList: List<Lazier<BaseParser>> = lazyList(
    )
    override val list = listOf(aList,MangaSources.list).flatten()
}
