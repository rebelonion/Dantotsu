package ani.dantotsu.parsers

import ani.dantotsu.Lazier
import ani.dantotsu.lazyList

object NovelSources : NovelReadSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
    )
}