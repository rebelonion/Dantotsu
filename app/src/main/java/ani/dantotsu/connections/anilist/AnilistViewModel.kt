package ani.dantotsu.connections.anilist

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.connections.discord.Discord
import ani.dantotsu.connections.mal.MAL
import ani.dantotsu.media.Media
import ani.dantotsu.others.AppUpdater
import ani.dantotsu.profile.User
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.tryWithSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun getUserId(context: Context, block: () -> Unit) {
    if (!Anilist.initialized && PrefManager.getVal<String>(PrefName.AnilistToken) != "") {
        if (Anilist.query.getUserData()) {
            tryWithSuspend {
                if (MAL.token != null && !MAL.query.getUserData())
                    snackString(context.getString(R.string.error_loading_mal_user_data))
            }
        } else {
            snackString(context.getString(R.string.error_loading_anilist_user_data))
        }
    }
    block.invoke()
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

    private val userStatus: MutableLiveData<ArrayList<User>> =
        MutableLiveData<ArrayList<User>>(null)

    fun getUserStatus(): LiveData<ArrayList<User>> = userStatus
    suspend fun initUserStatus() {
        val res = Anilist.query.getUserStatus()
        res?.let { userStatus.postValue(it) }
    }

    private val hidden: MutableLiveData<ArrayList<Media>> =
        MutableLiveData<ArrayList<Media>>(null)

    fun getHidden(): LiveData<ArrayList<Media>> = hidden

    suspend fun initHomePage() {
        val res = Anilist.query.initHomePage()
        res["currentAnime"]?.let { animeContinue.postValue(it) }
        res["favoriteAnime"]?.let { animeFav.postValue(it) }
        res["currentAnimePlanned"]?.let { animePlanned.postValue(it) }
        res["currentManga"]?.let { mangaContinue.postValue(it) }
        res["favoriteManga"]?.let { mangaFav.postValue(it) }
        res["currentMangaPlanned"]?.let { mangaPlanned.postValue(it) }
        res["recommendations"]?.let { recommendation.postValue(it) }
        res["hidden"]?.let { hidden.postValue(it) }
    }

    suspend fun loadMain(context: FragmentActivity) {
        Anilist.getSavedToken()
        MAL.getSavedToken()
        Discord.getSavedToken()
        if (!BuildConfig.FLAVOR.contains("fdroid")) {
            if (PrefManager.getVal(PrefName.CheckUpdate))
                context.lifecycleScope.launch(Dispatchers.IO) {
                    AppUpdater.check(context, false)
                }
        }
        val ret = Anilist.query.getGenresAndTags()
        withContext(Dispatchers.Main) {
            genres.value = ret
        }
    }

    val empty = MutableLiveData<Boolean>(null)

    var loaded: Boolean = false
    val genres: MutableLiveData<Boolean?> = MutableLiveData(null)
}

class AnilistAnimeViewModel : ViewModel() {
    var searched = false
    var notSet = true
    lateinit var aniMangaSearchResults: AniMangaSearchResults
    private val type = "ANIME"
    private val trending: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)

    fun getTrending(): LiveData<MutableList<Media>> = trending
    suspend fun loadTrending(i: Int) {
        val (season, year) = Anilist.currentSeasons[i]
        trending.postValue(
            Anilist.query.searchAniManga(
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


    private val animePopular = MutableLiveData<AniMangaSearchResults?>(null)

    fun getPopular(): LiveData<AniMangaSearchResults?> = animePopular
    suspend fun loadPopular(
        type: String,
        searchVal: String? = null,
        genres: ArrayList<String>? = null,
        sort: String = Anilist.sortBy[1],
        onList: Boolean = true,
    ) {
        animePopular.postValue(
            Anilist.query.searchAniManga(
                type,
                search = searchVal,
                onList = if (onList) null else false,
                sort = sort,
                genres = genres,
                adultOnly = PrefManager.getVal(PrefName.AdultOnly)
            )
        )
    }


    suspend fun loadNextPage(r: AniMangaSearchResults) = animePopular.postValue(
        Anilist.query.searchAniManga(
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
        val list = Anilist.query.loadAnimeList()
        updated.postValue(list["recentUpdates"])
        popularMovies.postValue(list["trendingMovies"])
        topRatedAnime.postValue(list["topRated"])
        mostFavAnime.postValue(list["mostFav"])
    }
}

class AnilistMangaViewModel : ViewModel() {
    var searched = false
    var notSet = true
    lateinit var aniMangaSearchResults: AniMangaSearchResults
    private val type = "MANGA"
    private val trending: MutableLiveData<MutableList<Media>> =
        MutableLiveData<MutableList<Media>>(null)

    fun getTrending(): LiveData<MutableList<Media>> = trending
    suspend fun loadTrending() =
        trending.postValue(
            Anilist.query.searchAniManga(
                type,
                perPage = 10,
                sort = Anilist.sortBy[2],
                hd = true,
                adultOnly = PrefManager.getVal(PrefName.AdultOnly)
            )?.results
        )


    private val mangaPopular = MutableLiveData<AniMangaSearchResults?>(null)
    fun getPopular(): LiveData<AniMangaSearchResults?> = mangaPopular
    suspend fun loadPopular(
        type: String,
        searchVal: String? = null,
        genres: ArrayList<String>? = null,
        sort: String = Anilist.sortBy[1],
        onList: Boolean = true,
    ) {
        mangaPopular.postValue(
            Anilist.query.searchAniManga(
                type,
                search = searchVal,
                onList = if (onList) null else false,
                sort = sort,
                genres = genres,
                adultOnly = PrefManager.getVal(PrefName.AdultOnly)
            )
        )
    }


    suspend fun loadNextPage(r: AniMangaSearchResults) = mangaPopular.postValue(
        Anilist.query.searchAniManga(
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

    enum class SearchType {
        ANIME, MANGA, CHARACTER, STAFF, STUDIO, USER;

        companion object {

            fun SearchType.toAnilistString(): String {
                return when (this) {
                    ANIME -> "ANIME"
                    MANGA -> "MANGA"
                    CHARACTER -> "CHARACTER"
                    STAFF -> "STAFF"
                    STUDIO -> "STUDIO"
                    USER -> "USER"
                }
            }

            fun fromString(string: String): SearchType {
                return when (string.uppercase()) {
                    "ANIME" -> ANIME
                    "MANGA" -> MANGA
                    "CHARACTER" -> CHARACTER
                    "STAFF" -> STAFF
                    "STUDIO" -> STUDIO
                    "USER" -> USER
                    else -> throw IllegalArgumentException("Invalid search type")
                }
            }
        }
    }

    var searched = false
    var notSet = true
    lateinit var aniMangaSearchResults: AniMangaSearchResults
    private val aniMangaResult: MutableLiveData<AniMangaSearchResults?> =
        MutableLiveData<AniMangaSearchResults?>(null)

    lateinit var characterSearchResults: CharacterSearchResults
    private val characterResult: MutableLiveData<CharacterSearchResults?> =
        MutableLiveData<CharacterSearchResults?>(null)

    lateinit var studioSearchResults: StudioSearchResults
    private val studioResult: MutableLiveData<StudioSearchResults?> =
        MutableLiveData<StudioSearchResults?>(null)

    lateinit var staffSearchResults: StaffSearchResults
    private val staffResult: MutableLiveData<StaffSearchResults?> =
        MutableLiveData<StaffSearchResults?>(null)

    lateinit var userSearchResults: UserSearchResults
    private val userResult: MutableLiveData<UserSearchResults?> =
        MutableLiveData<UserSearchResults?>(null)

    fun <T> getSearch(type: SearchType): MutableLiveData<T?> {
        return when (type) {
            SearchType.ANIME, SearchType.MANGA -> aniMangaResult as MutableLiveData<T?>
            SearchType.CHARACTER -> characterResult as MutableLiveData<T?>
            SearchType.STUDIO -> studioResult as MutableLiveData<T?>
            SearchType.STAFF -> staffResult as MutableLiveData<T?>
            SearchType.USER -> userResult as MutableLiveData<T?>
        }
    }

    suspend fun loadSearch(type: SearchType) {
        when (type) {
            SearchType.ANIME, SearchType.MANGA -> loadAniMangaSearch(aniMangaSearchResults)
            SearchType.CHARACTER -> loadCharacterSearch(characterSearchResults)
            SearchType.STUDIO -> loadStudiosSearch(studioSearchResults)
            SearchType.STAFF -> loadStaffSearch(staffSearchResults)
            SearchType.USER -> loadUserSearch(userSearchResults)
        }
    }

    suspend fun loadNextPage(type: SearchType) {
        when (type) {
            SearchType.ANIME, SearchType.MANGA -> loadNextAniMangaPage(aniMangaSearchResults)
            SearchType.CHARACTER -> loadNextCharacterPage(characterSearchResults)
            SearchType.STUDIO -> loadNextStudiosPage(studioSearchResults)
            SearchType.STAFF -> loadNextStaffPage(staffSearchResults)
            SearchType.USER -> loadNextUserPage(userSearchResults)
        }
    }

    fun hasNextPage(type: SearchType): Boolean {
        return when (type) {
            SearchType.ANIME, SearchType.MANGA -> aniMangaSearchResults.hasNextPage
            SearchType.CHARACTER -> characterSearchResults.hasNextPage
            SearchType.STUDIO -> studioSearchResults.hasNextPage
            SearchType.STAFF -> staffSearchResults.hasNextPage
            SearchType.USER -> userSearchResults.hasNextPage
        }
    }

    fun resultsIsNotEmpty(type: SearchType): Boolean {
        return when (type) {
            SearchType.ANIME, SearchType.MANGA -> aniMangaSearchResults.results.isNotEmpty()
            SearchType.CHARACTER -> characterSearchResults.results.isNotEmpty()
            SearchType.STUDIO -> studioSearchResults.results.isNotEmpty()
            SearchType.STAFF -> staffSearchResults.results.isNotEmpty()
            SearchType.USER -> userSearchResults.results.isNotEmpty()
        }
    }

    fun size(type: SearchType): Int {
        return when (type) {
            SearchType.ANIME, SearchType.MANGA -> aniMangaSearchResults.results.size
            SearchType.CHARACTER -> characterSearchResults.results.size
            SearchType.STUDIO -> studioSearchResults.results.size
            SearchType.STAFF -> staffSearchResults.results.size
            SearchType.USER -> userSearchResults.results.size
        }
    }

    fun clearResults(type: SearchType) {
        when (type) {
            SearchType.ANIME, SearchType.MANGA -> aniMangaSearchResults.results.clear()
            SearchType.CHARACTER -> characterSearchResults.results.clear()
            SearchType.STUDIO -> studioSearchResults.results.clear()
            SearchType.STAFF -> staffSearchResults.results.clear()
            SearchType.USER -> userSearchResults.results.clear()
        }
    }

    private suspend fun loadAniMangaSearch(r: AniMangaSearchResults) = aniMangaResult.postValue(
        Anilist.query.searchAniManga(
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

    private suspend fun loadCharacterSearch(r: CharacterSearchResults) = characterResult.postValue(
        Anilist.query.searchCharacters(
            r.page,
            r.search,
        )
    )

    private suspend fun loadStudiosSearch(r: StudioSearchResults) = studioResult.postValue(
        Anilist.query.searchStudios(
            r.page,
            r.search,
        )
    )

    private suspend fun loadStaffSearch(r: StaffSearchResults) = staffResult.postValue(
        Anilist.query.searchStaff(
            r.page,
            r.search,
        )
    )

    private suspend fun loadUserSearch(r: UserSearchResults) = userResult.postValue(
        Anilist.query.searchUsers(
            r.page,
            r.search,
        )
    )

    private suspend fun loadNextAniMangaPage(r: AniMangaSearchResults) = aniMangaResult.postValue(
        Anilist.query.searchAniManga(
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

    private suspend fun loadNextCharacterPage(r: CharacterSearchResults) =
        characterResult.postValue(
            Anilist.query.searchCharacters(
                r.page + 1,
                r.search,
            )
        )

    private suspend fun loadNextStudiosPage(r: StudioSearchResults) = studioResult.postValue(
        Anilist.query.searchStudios(
            r.page + 1,
            r.search,
        )
    )

    private suspend fun loadNextStaffPage(r: StaffSearchResults) = staffResult.postValue(
        Anilist.query.searchStaff(
            r.page + 1,
            r.search,
        )
    )

    private suspend fun loadNextUserPage(r: UserSearchResults) = userResult.postValue(
        Anilist.query.searchUsers(
            r.page + 1,
            r.search,
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