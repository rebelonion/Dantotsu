package tachiyomi.source.local.entries.anime

//import eu.kanade.tachiyomi.util.storage.toFFmpegString
import android.content.Context
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.UnmeteredSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.runBlocking
import rx.Observable
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.source.local.filter.anime.AnimeOrderBy
import java.io.File
import java.util.concurrent.TimeUnit

class LocalAnimeSource(
    private val context: Context,
) : AnimeCatalogueSource, UnmeteredSource {

    private val POPULAR_FILTERS = AnimeFilterList(AnimeOrderBy.Popular())
    private val LATEST_FILTERS = AnimeFilterList(AnimeOrderBy.Latest())

    override val name = "Local anime source"

    override val id: Long = ID

    override val lang = "other"

    override fun toString() = name

    override val supportsLatest = true

    // Browse related
    override suspend fun getPopularAnime(page: Int) = getSearchAnime(page, "", POPULAR_FILTERS)

    override suspend fun getLatestUpdates(page: Int) = getSearchAnime(page, "", LATEST_FILTERS)

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getPopularAnime"))
    override fun fetchPopularAnime(page: Int) = fetchSearchAnime(page, "", POPULAR_FILTERS)

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getLatestUpdates"))
    override fun fetchLatestUpdates(page: Int) = fetchSearchAnime(page, "", LATEST_FILTERS)

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getSearchAnime"))
    override fun fetchSearchAnime(
        page: Int,
        query: String,
        filters: AnimeFilterList
    ): Observable<AnimesPage> {
        return runBlocking {
            Observable.just(getSearchAnime(page, query, filters))
        }
    }

    // Anime details related
    override suspend fun getAnimeDetails(anime: SAnime): SAnime = withIOContext {
        //return empty
        anime
    }

    // Episodes
    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        //return empty
        return emptyList()
    }

    // Filters
    override fun getFilterList() = AnimeFilterList(AnimeOrderBy.Popular())

    // Unused stuff
    override suspend fun getVideoList(episode: SEpisode) =
        throw UnsupportedOperationException("Unused")

    companion object {
        const val ID = 0L
        const val HELP_URL = "https://aniyomi.org/help/guides/local-anime/"

        private const val DEFAULT_COVER_NAME = "cover.jpg"
        private val LATEST_THRESHOLD = TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS)

        private fun getBaseDirectories(context: Context): Sequence<File> {
            val localFolder = "Aniyomi" + File.separator + "localanime"
            return DiskUtil.getExternalStorages(context)
                .map { File(it.absolutePath, localFolder) }
                .asSequence()
        }

        private fun getBaseDirectoriesFiles(context: Context): Sequence<File> {
            return getBaseDirectories(context)
                // Get all the files inside all baseDir
                .flatMap { it.listFiles().orEmpty().toList() }
        }

        private fun getAnimeDir(animeUrl: String, baseDirsFile: Sequence<File>): File? {
            return baseDirsFile
                // Get the first animeDir or null
                .firstOrNull { it.isDirectory && it.name == animeUrl }
        }
    }
}

fun Anime.isLocal(): Boolean = source == LocalAnimeSource.ID

fun AnimeSource.isLocal(): Boolean = id == LocalAnimeSource.ID
