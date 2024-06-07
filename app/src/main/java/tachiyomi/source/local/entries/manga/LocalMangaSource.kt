package tachiyomi.source.local.entries.manga

import android.content.Context
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.MangaSource
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import rx.Observable
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.source.local.filter.manga.MangaOrderBy
import java.util.concurrent.TimeUnit

class LocalMangaSource(
    private val context: Context,
) : CatalogueSource, UnmeteredSource {


    private val POPULAR_FILTERS = FilterList(MangaOrderBy.Popular())
    private val LATEST_FILTERS = FilterList(MangaOrderBy.Latest())

    override val name: String = "Local manga source"

    override val id: Long = ID

    override val lang: String = "other"

    override fun toString() = name

    override val supportsLatest: Boolean = true

    // Browse related
    override fun fetchPopularManga(page: Int) = fetchSearchManga(page, "", POPULAR_FILTERS)

    override fun fetchLatestUpdates(page: Int) = fetchSearchManga(page, "", LATEST_FILTERS)

    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList
    ): Observable<MangasPage> {
        return Observable.just(MangasPage(emptyList(), false))
    }

    // Manga details related
    override suspend fun getMangaDetails(manga: SManga): SManga = withIOContext {
        manga
    }

    // Chapters
    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        return emptyList()
    }

    // Filters
    override fun getFilterList() = FilterList(MangaOrderBy.Popular())

    // Unused stuff
    override suspend fun getPageList(chapter: SChapter) =
        throw UnsupportedOperationException("Unused")

    companion object {
        const val ID = 0L
        const val HELP_URL = "https://aniyomi.org/help/guides/local-manga/"

        private val LATEST_THRESHOLD = TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS)
    }
}

fun Manga.isLocal(): Boolean = source == LocalMangaSource.ID

fun MangaSource.isLocal(): Boolean = id == LocalMangaSource.ID
