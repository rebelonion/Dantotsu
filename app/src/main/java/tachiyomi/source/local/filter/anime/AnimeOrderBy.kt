package tachiyomi.source.local.filter.anime

import android.content.Context
import eu.kanade.tachiyomi.animesource.model.AnimeFilter

sealed class AnimeOrderBy(context: Context, selection: Selection) : AnimeFilter.Sort(

    "Order by",
    arrayOf("Title", "Date"),
    selection,
) {
    class Popular(context: Context) : AnimeOrderBy(context, Selection(0, true))
    class Latest(context: Context) : AnimeOrderBy(context, Selection(1, false))
}
