package tachiyomi.source.local.filter.manga

import android.content.Context
import eu.kanade.tachiyomi.source.model.Filter

sealed class MangaOrderBy(context: Context, selection: Selection) : Filter.Sort(
    "Order by",
    arrayOf("Title", "Date"),
    selection,
) {
    class Popular(context: Context) : MangaOrderBy(context, Selection(0, true))
    class Latest(context: Context) : MangaOrderBy(context, Selection(1, false))
}
