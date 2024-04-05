package ani.dantotsu.connections.anilist

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.webkit.internal.ApiFeature.M
import androidx.webkit.internal.ApiFeature.P
import androidx.webkit.internal.StartupApiFeature
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.connections.discord.Discord
import ani.dantotsu.connections.mal.MAL
import ani.dantotsu.media.Media
import ani.dantotsu.others.AppUpdater
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

suspend fun getUserId(context: Context, block: () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        val token = PrefManager.getVal(PrefName.DiscordToken, null as String?)
        val userid = PrefManager.getVal(PrefName.DiscordId, null as String?)
        if (userid == null && token != null) {
            /*if (!Discord.getUserData())
                snackString(context.getString(R.string.error_loading_discord_user_data))*/
            //TODO: Discord.getUserData()
        }
    }

    val anilist = if (Anilist.userid == null && Anilist.token != null) {
        if (Anilist.query.getUserData()) {
            tryWithSuspend {
                if (MAL.token != null && !MAL.query.getUserData())
                    snackString(context.getString(R.string.error_loading_mal_user_data))
            }
            true
        } else {
            snackString(context.getString(R.string.error_loading_anilist_user_data))
            false
        }
    } else true

    if (anilist) block.invoke()
}

class AnilistHomeViewModel : ViewModel() {
    private val listImages: MutableLiveData<ArrayList<String?>> =
        MutableLiveData<ArrayList<String?>>(arrayListOf())

    fun getListImages(): LiveData<ArrayList<String?>> = listImages
    suspend fun setListImages() = listImages.postValue(Anilist.query.getBannerImages())

    private val animeContinue: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getAnimeContinue(): LiveData<ArrayList<Media>> = animeContinue

    private val animeFav: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getAnimeFav(): LiveData<ArrayList<Media>> = animeFav

    private val animePlanned: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getAnimePlanned(): LiveData<ArrayList<Media>> = animePlanned

    private val mangaContinue: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getMangaContinue(): LiveData<ArrayList<Media>> = mangaContinue

    private val mangaFav: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getMangaFav(): LiveData<ArrayList<Media>> = mangaFav

    private val mangaPlanned: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getMangaPlanned(): LiveData<ArrayList<Media>> = mangaPlanned

    private val recommendation: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getRecommendation(): LiveData<ArrayList<Media>> = recommendation

    suspend fun initHomePage() {
        val res = Anilist.query.initHomePage()
        Logger.log("AnilistHomeViewModel : res=$res")
        res["currentAnime"]?.let { animeContinue.postValue(it) }
        res["favoriteAnime"]?.let { animeFav.postValue(it) }
        res["plannedAnime"]?.let { animePlanned.postValue(it) }
        res["currentManga"]?.let { mangaContinue.postValue(it) }
        res["favoriteManga"]?.let { mangaFav.postValue(it) }
        res["plannedManga"]?.let { mangaPlanned.postValue(it) }
        res["recommendations"]?.let { recommendation.postValue(it) }
    }

    suspend fun loadMain(context: FragmentActivity) {
        Anilist.getSavedToken()
        MAL.getSavedToken()
        Discord.getSavedToken()
        if (!BuildConfig.FLAVOR.contains("fdroid")) {
            if (PrefManager.getVal(PrefName.CheckUpdate)) AppUpdater.check(context)
        }
        genres.postValue(Anilist.query.getGenresAndTags())
    }

    val empty = MutableLiveData<Boolean>(null)

    var loaded: Boolean = false
    val genres: MutableLiveData<Boolean?> = MutableLiveData(null)
}

class AnilistAnimeViewModel : ViewModel() {
    var searched = false
    var notSet = true
    lateinit var searchResults: SearchResults
    private val type = "ANIME"
    private val trending: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)

    fun getTrending(): LiveData<MutableList<Media>> = trending
    suspend fun loadTrending(i: Int) {
        val (season, year) = Anilist.currentSeasons[i]
        trending.postValue(
            Anilist.query.search(
                type,
                perPage = 12,
                sort = Anilist.sortBy[2],
                season = season,
                seasonYear = year,
                hd = true,
                adultOnly = PrefManager.getVal(PrefName.AdultOnly)
            )?.results
        )
    }


    private val animePopular = MutableLiveData<SearchResults?>(null)

    fun getPopular(): LiveData<SearchResults?> = animePopular
    suspend fun loadPopular(
        type: String,
        searchVal: String? = null,
        genres: ArrayList<String>? = null,
        sort: String = Anilist.sortBy[1],
        onList: Boolean = true,
    ) {
        animePopular.postValue(
            Anilist.query.search(
                type,
                search = searchVal,
                onList = if (onList) null else false,
                sort = sort,
                genres = genres,
                adultOnly = PrefManager.getVal(PrefName.AdultOnly)
            )
        )
    }


    suspend fun loadNextPage(r: SearchResults) = animePopular.postValue(
        Anilist.query.search(
            r.type,
            r.page + 1,
            r.perPage,
            r.search,
            r.sort,
            r.genres,
            r.tags,
            r.status,
            r.source,
            r.format,
            r.countryOfOrigin,
            r.isAdult,
            r.onList,
            adultOnly = PrefManager.getVal(PrefName.AdultOnly),
        )
    )

    var loaded: Boolean = false
    private val updated: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)
    fun getUpdated(): LiveData<MutableList<Media>> = updated

    private val popularMovies: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)
    fun getMovies(): LiveData<MutableList<Media>> = popularMovies

    private val topRatedAnime: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)
    fun getTopRated(): LiveData<MutableList<Media>> = topRatedAnime

    private val mostFavAnime: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)
    fun getMostFav(): LiveData<MutableList<Media>> = mostFavAnime
    suspend fun loadAll() {
        val list= Anilist.query.loadAnimeList()
        updated.postValue(list["recentUpdates"])
        popularMovies.postValue(list["trendingMovies"])
        topRatedAnime.postValue(list["topRated"])
        mostFavAnime.postValue(list["mostFav"])
    }
}

class AnilistMangaViewModel : ViewModel() {
    var searched = false
    var notSet = true
    lateinit var searchResults: SearchResults
    private val type = "MANGA"
    private val trending: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)

    fun getTrending(): LiveData<MutableList<Media>> = trending
    suspend fun loadTrending() =
        trending.postValue(
            Anilist.query.search(
                type,
                perPage = 10,
                sort = Anilist.sortBy[2],
                hd = true,
                adultOnly = PrefManager.getVal(PrefName.AdultOnly)
            )?.results
        )


    private val mangaPopular = MutableLiveData<SearchResults?>(null)
    fun getPopular(): LiveData<SearchResults?> = mangaPopular
    suspend fun loadPopular(
        type: String,
        searchVal: String? = null,
        genres: ArrayList<String>? = null,
        sort: String = Anilist.sortBy[1],
        onList: Boolean = true,
    ) {
        mangaPopular.postValue(
            Anilist.query.search(
                type,
                search = searchVal,
                onList = if (onList) null else false,
                sort = sort,
                genres = genres,
                adultOnly = PrefManager.getVal(PrefName.AdultOnly)
            )
        )
    }


    suspend fun loadNextPage(r: SearchResults) = mangaPopular.postValue(
        Anilist.query.search(
            r.type,
            r.page + 1,
            r.perPage,
            r.search,
            r.sort,
            r.genres,
            r.tags,
            r.status,
            r.source,
            r.format,
            r.countryOfOrigin,
            r.isAdult,
            r.onList,
            r.excludedGenres,
            r.excludedTags,
            r.startYear,
            r.seasonYear,
            r.season,
            adultOnly = PrefManager.getVal(PrefName.AdultOnly)
        )
    )

    var loaded: Boolean = false

    private val popularManga: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)
    fun getPopularManga(): LiveData<MutableList<Media>> = popularManga

    private val popularManhwa: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)
    fun getPopularManhwa(): LiveData<MutableList<Media>> = popularManhwa

    private val popularNovel: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)
    fun getPopularNovel(): LiveData<MutableList<Media>> = popularNovel

    private val topRatedManga: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)
    fun getTopRated(): LiveData<MutableList<Media>> = topRatedManga

    private val mostFavManga: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)
    fun getMostFav(): LiveData<MutableList<Media>> = mostFavManga
    suspend fun loadAll() {
        val list = Anilist.query.loadMangaList()
        popularManga.postValue(list["trendingManga"])
        popularManhwa.postValue(list["trendingManhwa"])
        popularNovel.postValue(list["trendingNovel"])
        topRatedManga.postValue(list["topRated"])
        mostFavManga.postValue(list["mostFav"])
    }
}

class AnilistSearch : ViewModel() {
    var searched = false
    var notSet = true
    lateinit var searchResults: SearchResults
    private val result: MutableLiveData<SearchResults?> = MutableLiveData<SearchResults?>(null)

    fun getSearch(): LiveData<SearchResults?> = result
    suspend fun loadSearch(r: SearchResults) = result.postValue(
        Anilist.query.search(
            r.type,
            r.page,
            r.perPage,
            r.search,
            r.sort,
            r.genres,
            r.tags,
            r.status,
            r.source,
            r.format,
            r.countryOfOrigin,
            r.isAdult,
            r.onList,
            r.excludedGenres,
            r.excludedTags,
            r.startYear,
            r.seasonYear,
            r.season,
        )
    )

    suspend fun loadNextPage(r: SearchResults) = result.postValue(
        Anilist.query.search(
            r.type,
            r.page + 1,
            r.perPage,
            r.search,
            r.sort,
            r.genres,
            r.tags,
            r.status,
            r.source,
            r.format,
            r.countryOfOrigin,
            r.isAdult,
            r.onList,
            r.excludedGenres,
            r.excludedTags,
            r.startYear,
            r.seasonYear,
            r.season
        )
    )
}

class GenresViewModel : ViewModel() {
    var genres: MutableMap<String, String>? = null
    var done = false
    var doneListener: (() -> Unit)? = null
    suspend fun loadGenres(genre: ArrayList<String>, listener: (Pair<String, String>) -> Unit) {
        if (genres == null) {
            genres = mutableMapOf()
            Anilist.query.getGenres(genre) {
                genres!![it.first] = it.second
                listener.invoke(it)
                if (genres!!.size == genre.size) {
                    done = true
                    doneListener?.invoke()
                }
            }
        }
    }
}

class ProfileViewModel : ViewModel() {

    private val mangaFav: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getMangaFav(): LiveData<ArrayList<Media>> = mangaFav

    private val animeFav: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getAnimeFav(): LiveData<ArrayList<Media>> = animeFav

    suspend fun setData(id: Int) {
        val res = Anilist.query.initProfilePage(id)
        val mangaList = res?.data?.favoriteManga?.favourites?.manga?.edges?.mapNotNull {
            it.node?.let { i ->
                Media(i).apply { isFav = true }
            }
        }
        mangaFav.postValue(ArrayList(mangaList ?: arrayListOf()))
        val animeList = res?.data?.favoriteAnime?.favourites?.anime?.edges?.mapNotNull {
            it.node?.let { i ->
                Media(i).apply { isFav = true }
            }
        }
        animeFav.postValue(ArrayList(animeList ?: arrayListOf()))

    }

    fun refresh() {
        mangaFav.postValue(mangaFav.value)
        animeFav.postValue(animeFav.value)

    }
}