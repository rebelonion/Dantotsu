package ani.dantotsu.connections.anilist

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
    suspend fun setAnimeContinue() = animeContinue.postValue(Anilist.query.continueMedia("ANIME"))

    private val animeFav: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getAnimeFav(): LiveData<ArrayList<Media>> = animeFav
    suspend fun setAnimeFav() = animeFav.postValue(Anilist.query.favMedia(true))

    private val animePlanned: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getAnimePlanned(): LiveData<ArrayList<Media>> = animePlanned
    suspend fun setAnimePlanned() =
        animePlanned.postValue(Anilist.query.continueMedia("ANIME", true))

    private val mangaContinue: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getMangaContinue(): LiveData<ArrayList<Media>> = mangaContinue
    suspend fun setMangaContinue() = mangaContinue.postValue(Anilist.query.continueMedia("MANGA"))

    private val mangaFav: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getMangaFav(): LiveData<ArrayList<Media>> = mangaFav
    suspend fun setMangaFav() = mangaFav.postValue(Anilist.query.favMedia(false))

    private val mangaPlanned: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getMangaPlanned(): LiveData<ArrayList<Media>> = mangaPlanned
    suspend fun setMangaPlanned() =
        mangaPlanned.postValue(Anilist.query.continueMedia("MANGA", true))

    private val recommendation: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getRecommendation(): LiveData<ArrayList<Media>> = recommendation
    suspend fun setRecommendation() = recommendation.postValue(Anilist.query.recommendations())

    suspend fun initHomePage() {
        val res = Anilist.query.initHomePage()
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
        MAL.getSavedToken(context)
        Discord.getSavedToken(context)
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
                hd = true
            )?.results
        )
    }

    private val updated: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)

    fun getUpdated(): LiveData<MutableList<Media>> = updated
    suspend fun loadUpdated() = updated.postValue(Anilist.query.recentlyUpdated())

    private val animePopular = MutableLiveData<SearchResults?>(null)
    fun getPopular(): LiveData<SearchResults?> = animePopular
    suspend fun loadPopular(
        type: String,
        search_val: String? = null,
        genres: ArrayList<String>? = null,
        sort: String = Anilist.sortBy[1],
        onList: Boolean = true,
    ) {
        animePopular.postValue(
            Anilist.query.search(
                type,
                search = search_val,
                onList = if (onList) null else false,
                sort = sort,
                genres = genres
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
            r.format,
            r.isAdult,
            r.onList
        )
    )

    var loaded: Boolean = false
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
                hd = true
            )?.results
        )

    private val updated: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)

    fun getTrendingNovel(): LiveData<MutableList<Media>> = updated
    suspend fun loadTrendingNovel() =
        updated.postValue(
            Anilist.query.search(
                type,
                perPage = 10,
                sort = Anilist.sortBy[2],
                format = "NOVEL"
            )?.results
        )

    private val mangaPopular = MutableLiveData<SearchResults?>(null)
    fun getPopular(): LiveData<SearchResults?> = mangaPopular
    suspend fun loadPopular(
        type: String,
        search_val: String? = null,
        genres: ArrayList<String>? = null,
        sort: String = Anilist.sortBy[1],
        onList: Boolean = true,
    ) {
        mangaPopular.postValue(
            Anilist.query.search(
                type,
                search = search_val,
                onList = if (onList) null else false,
                sort = sort,
                genres = genres
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
            r.format,
            r.isAdult,
            r.onList,
            r.excludedGenres,
            r.excludedTags,
            r.seasonYear,
            r.season
        )
    )

    var loaded: Boolean = false
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
            r.format,
            r.isAdult,
            r.onList,
            r.excludedGenres,
            r.excludedTags,
            r.seasonYear,
            r.season
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
            r.format,
            r.isAdult,
            r.onList,
            r.excludedGenres,
            r.excludedTags,
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