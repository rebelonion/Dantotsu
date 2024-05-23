package tachiyomi.source.local.filter.manga

import eu.kanade.tachiyomi.source.model.Filter

sealed class MangaOrderBy(selection: Selection) : Filter.Sort(
    "Order by",
    arrayOf("Title", "Date"),
    selection,
) {
    class Popular : MangaOrderBy(Selection(0, true))
    class Latest : MangaOrderBy(Selection(1, false))
}
