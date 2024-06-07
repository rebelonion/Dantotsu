package tachiyomi.source.local.filter.anime

import eu.kanade.tachiyomi.animesource.model.AnimeFilter

sealed class AnimeOrderBy(selection: Selection) : AnimeFilter.Sort(

    "Order by",
    arrayOf("Title", "Date"),
    selection,
) {
    class Popular : AnimeOrderBy(Selection(0, true))
    class Latest : AnimeOrderBy(Selection(1, false))
}
