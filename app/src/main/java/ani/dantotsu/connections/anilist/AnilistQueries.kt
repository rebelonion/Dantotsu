package ani.dantotsu.connections.anilist

import android.util.Base64
import ani.dantotsu.R
import ani.dantotsu.checkGenreTime
import ani.dantotsu.checkId
import ani.dantotsu.connections.anilist.Anilist.authorRoles
import ani.dantotsu.connections.anilist.Anilist.executeQuery
import ani.dantotsu.connections.anilist.api.FuzzyDate
import ani.dantotsu.connections.anilist.api.Page
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.currContext
import ani.dantotsu.isOnline
import ani.dantotsu.logError
import ani.dantotsu.media.Author
import ani.dantotsu.media.Character
import ani.dantotsu.media.Media
import ani.dantotsu.media.Studio
import ani.dantotsu.others.MalScraper
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.system.measureTimeMillis

class AnilistQueries {
    suspend fun getUserData(): Boolean {
        val response: Query.Viewer?
        measureTimeMillis {
            response =
                executeQuery("""{Viewer{name options{displayAdultContent}avatar{medium}bannerImage id mediaListOptions{rowOrder animeList{sectionOrder customLists}mangaList{sectionOrder customLists}}statistics{anime{episodesWatched}manga{chaptersRead}}}}""")
        }.also { println("time : $it") }
        val user = response?.data?.user ?: return false

        PrefManager.setVal(PrefName.AnilistUserName, user.name)

        Anilist.userid = user.id
        Anilist.username = user.name
        Anilist.bg = user.bannerImage
        Anilist.avatar = user.avatar?.medium
        Anilist.episodesWatched = user.statistics?.anime?.episodesWatched
        Anilist.chapterRead = user.statistics?.manga?.chaptersRead
        Anilist.adult = user.options?.displayAdultContent ?: false
        return true
    }

    suspend fun getMedia(id: Int, mal: Boolean = false): Media? {
        val response = executeQuery<Query.Media>(
            """{Media(${if (!mal) "id:" else "idMal:"}$id){id idMal status chapters episodes nextAiringEpisode{episode}type meanScore isAdult isFavourite format bannerImage coverImage{large}title{english romaji userPreferred}mediaListEntry{progress private score(format:POINT_100)status}}}""",
            force = true
        )
        val fetchedMedia = response?.data?.media ?: return null
        return Media(fetchedMedia)
    }

    fun mediaDetails(media: Media): Media {
        media.cameFromContinue = false

        val query =
            """{Media(id:${media.id}){id mediaListEntry{id status score(format:POINT_100) progress private notes repeat customLists updatedAt startedAt{year month day}completedAt{year month day}}isFavourite siteUrl idMal nextAiringEpisode{episode airingAt}source countryOfOrigin format duration season seasonYear startDate{year month day}endDate{year month day}genres studios(isMain:true){nodes{id name siteUrl}}description trailer { site id } synonyms tags { name rank isMediaSpoiler } characters(sort:[ROLE,FAVOURITES_DESC],perPage:25,page:1){edges{role node{id image{medium}name{userPreferred}}}}relations{edges{relationType(version:2)node{id idMal mediaListEntry{progress private score(format:POINT_100) status} episodes chapters nextAiringEpisode{episode} popularity meanScore isAdult isFavourite format title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}staffPreview: staff(perPage: 8, sort: [RELEVANCE, ID]) {edges{role node{id name{userPreferred}}}}recommendations(sort:RATING_DESC){nodes{mediaRecommendation{id idMal mediaListEntry{progress private score(format:POINT_100) status} episodes chapters nextAiringEpisode{episode}meanScore isAdult isFavourite format title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}externalLinks{url site}}}"""
        runBlocking {
            val anilist = async {
                var response = executeQuery<Query.Media>(query, force = true, show = true)
                if (response != null) {
                    fun parse() {
                        val fetchedMedia = response?.data?.media ?: return

                        media.source = fetchedMedia.source?.toString()
                        media.countryOfOrigin = fetchedMedia.countryOfOrigin
                        media.format = fetchedMedia.format?.toString()

                        media.startDate = fetchedMedia.startDate
                        media.endDate = fetchedMedia.endDate

                        if (fetchedMedia.genres != null) {
                            media.genres = arrayListOf()
                            fetchedMedia.genres?.forEach { i ->
                                media.genres.add(i)
                            }
                        }

                        media.trailer = fetchedMedia.trailer?.let { i ->
                            if (i.site != null && i.site.toString() == "youtube")
                                "https://www.youtube.com/embed/${i.id.toString().trim('"')}"
                            else null
                        }

                        fetchedMedia.synonyms?.apply {
                            media.synonyms = arrayListOf()
                            this.forEach { i ->
                                media.synonyms.add(
                                    i
                                )
                            }
                        }

                        fetchedMedia.tags?.apply {
                            media.tags = arrayListOf()
                            this.forEach { i ->
                                if (i.isMediaSpoiler == false)
                                    media.tags.add("${i.name} : ${i.rank.toString()}%")
                            }
                        }

                        media.description = fetchedMedia.description.toString()

                        if (fetchedMedia.characters != null) {
                            media.characters = arrayListOf()
                            fetchedMedia.characters?.edges?.forEach { i ->
                                i.node?.apply {
                                    media.characters?.add(
                                        Character(
                                            id = id,
                                            name = i.node?.name?.userPreferred,
                                            image = i.node?.image?.medium,
                                            banner = media.banner ?: media.cover,
                                            role = when (i.role.toString()) {
                                                "MAIN" -> currContext()?.getString(R.string.main_role)
                                                    ?: "MAIN"

                                                "SUPPORTING" -> currContext()?.getString(R.string.supporting_role)
                                                    ?: "SUPPORTING"

                                                else -> i.role.toString()
                                            }
                                        )
                                    )
                                }
                            }
                        }
                        if (fetchedMedia.relations != null) {
                            media.relations = arrayListOf()
                            fetchedMedia.relations?.edges?.forEach { mediaEdge ->
                                val m = Media(mediaEdge)
                                media.relations?.add(m)
                                if (m.relation == "SEQUEL") {
                                    media.sequel =
                                        if ((media.sequel?.popularity ?: 0) < (m.popularity
                                                ?: 0)
                                        ) m else media.sequel

                                } else if (m.relation == "PREQUEL") {
                                    media.prequel =
                                        if ((media.prequel?.popularity ?: 0) < (m.popularity
                                                ?: 0)
                                        ) m else media.prequel
                                }
                            }
                            media.relations?.sortByDescending { it.popularity }
                            media.relations?.sortByDescending { it.startDate?.year }
                            media.relations?.sortBy { it.relation }
                        }
                        if (fetchedMedia.recommendations != null) {
                            media.recommendations = arrayListOf()
                            fetchedMedia.recommendations?.nodes?.forEach { i ->
                                i.mediaRecommendation?.apply {
                                    media.recommendations?.add(
                                        Media(this)
                                    )
                                }
                            }
                        }

                        if (fetchedMedia.mediaListEntry != null) {
                            fetchedMedia.mediaListEntry?.apply {
                                media.userProgress = progress
                                media.isListPrivate = private ?: false
                                media.notes = notes
                                media.userListId = id
                                media.userScore = score?.toInt() ?: 0
                                media.userStatus = status?.toString()
                                media.inCustomListsOf = customLists?.toMutableMap()
                                media.userRepeat = repeat ?: 0
                                media.userUpdatedAt = updatedAt?.toString()?.toLong()?.times(1000)
                                media.userCompletedAt = completedAt ?: FuzzyDate()
                                media.userStartedAt = startedAt ?: FuzzyDate()
                            }
                        } else {
                            media.isListPrivate = false
                            media.userStatus = null
                            media.userListId = null
                            media.userProgress = null
                            media.userScore = 0
                            media.userRepeat = 0
                            media.userUpdatedAt = null
                            media.userCompletedAt = FuzzyDate()
                            media.userStartedAt = FuzzyDate()
                        }

                        if (media.anime != null) {
                            media.anime.episodeDuration = fetchedMedia.duration
                            media.anime.season = fetchedMedia.season?.toString()
                            media.anime.seasonYear = fetchedMedia.seasonYear

                            fetchedMedia.studios?.nodes?.apply {
                                if (isNotEmpty()) {
                                    val firstStudio = get(0)
                                    media.anime.mainStudio = Studio(
                                        firstStudio.id.toString(),
                                        firstStudio.name ?: "N/A"
                                    )
                                }
                            }

                            fetchedMedia.staff?.edges?.find { authorRoles.contains(it.role?.trim()) }?.node?.let {
                                media.anime.author = Author(
                                    it.id.toString(),
                                    it.name?.userPreferred ?: "N/A"
                                )
                            }

                            media.anime.nextAiringEpisodeTime =
                                fetchedMedia.nextAiringEpisode?.airingAt?.toLong()

                            fetchedMedia.externalLinks?.forEach { i ->
                                when (i.site.lowercase()) {
                                    "youtube" -> media.anime.youtube = i.url
                                    "crunchyroll" -> media.crunchySlug =
                                        i.url?.split("/")?.getOrNull(3)

                                    "vrv" -> media.vrvId = i.url?.split("/")?.getOrNull(4)
                                }
                            }
                        } else if (media.manga != null) {
                            fetchedMedia.staff?.edges?.find { authorRoles.contains(it.role?.trim()) }?.node?.let {
                                media.manga.author = Author(
                                    it.id.toString(),
                                    it.name?.userPreferred ?: "N/A"
                                )
                            }
                        }
                        media.shareLink = fetchedMedia.siteUrl
                    }

                    if (response.data?.media != null) parse()
                    else {
                        snackString(currContext()?.getString(R.string.adult_stuff))
                        response = executeQuery(query, force = true, useToken = false)
                        if (response?.data?.media != null) parse()
                        else snackString(currContext()?.getString(R.string.what_did_you_open))
                    }
                } else {
                    if (currContext()?.let { isOnline(it) } == true) {
                        snackString(currContext()?.getString(R.string.error_getting_data))
                    }
                }
            }
            val mal = async {
                if (media.idMAL != null) {
                    MalScraper.loadMedia(media)
                }
            }
            awaitAll(anilist, mal)
        }
        return media
    }

    suspend fun continueMedia(type: String, planned: Boolean = false): ArrayList<Media> {
        val returnArray = arrayListOf<Media>()
        val map = mutableMapOf<Int, Media>()
        val query = if (planned) {
            """{ planned: ${continueMediaQuery(type, "PLANNING")} }"""
        } else {
            """{ 
                current: ${continueMediaQuery(type, "CURRENT")},
                repeating: ${continueMediaQuery(type, "REPEATING")}
            }"""
        }

        val response = executeQuery<Query.CombinedMediaListResponse>(query)
        if (planned) {
            response?.data?.planned?.lists?.forEach { li ->
                li.entries?.reversed()?.forEach {
                    val m = Media(it)
                    m.cameFromContinue = true
                    map[m.id] = m
                }
            }
        } else {
            response?.data?.current?.lists?.forEach { li ->
                li.entries?.reversed()?.forEach {
                    val m = Media(it)
                    m.cameFromContinue = true
                    map[m.id] = m
                }
            }
            response?.data?.repeating?.lists?.forEach { li ->
                li.entries?.reversed()?.forEach {
                    val m = Media(it)
                    m.cameFromContinue = true
                    map[m.id] = m
                }
            }
        }
        val set = PrefManager.getCustomVal<Set<Int>>("continue_$type", setOf()).toMutableSet()
        if (set.isNotEmpty()) {
            set.reversed().forEach {
                if (map.containsKey(it)) returnArray.add(map[it]!!)
            }
            for (i in map) {
                if (i.value !in returnArray) returnArray.add(i.value)
            }
        } else returnArray.addAll(map.values)
        return returnArray
    }

    private fun continueMediaQuery(type: String, status: String): String {
        return """ MediaListCollection(userId: ${Anilist.userid}, type: $type, status: $status , sort: UPDATED_TIME ) { lists { entries { progress private score(format:POINT_100) status media { id idMal type isAdult status chapters episodes nextAiringEpisode {episode} meanScore isFavourite format bannerImage coverImage{large} title { english romaji userPreferred } } } } } """
    }

    suspend fun favMedia(anime: Boolean): ArrayList<Media> {
        var hasNextPage = true
        var page = 0

        suspend fun getNextPage(page: Int): List<Media> {
            val response = executeQuery<Query.User>("""{${favMediaQuery(anime, page)}}""")
            val favourites = response?.data?.user?.favourites
            val apiMediaList = if (anime) favourites?.anime else favourites?.manga
            hasNextPage = apiMediaList?.pageInfo?.hasNextPage ?: false
            return apiMediaList?.edges?.mapNotNull {
                it.node?.let { i ->
                    Media(i).apply { isFav = true }
                }
            } ?: return listOf()
        }

        val responseArray = arrayListOf<Media>()
        while (hasNextPage) {
            page++
            responseArray.addAll(getNextPage(page))
        }
        return responseArray
    }

    private fun favMediaQuery(anime: Boolean, page: Int): String {
        return """User(id:${Anilist.userid}){id favourites{${if (anime) "anime" else "manga"}(page:$page){pageInfo{hasNextPage}edges{favouriteOrder node{id idMal isAdult mediaListEntry{ progress private score(format:POINT_100) status } chapters isFavourite format episodes nextAiringEpisode{episode}meanScore isFavourite format startDate{year month day} title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}}}"""
    }

    suspend fun recommendations(): ArrayList<Media> {
        val response = executeQuery<Query.Page>("""{${recommendationQuery()}}""")
        val map = mutableMapOf<Int, Media>()
        response?.data?.page?.apply {
            recommendations?.onEach {
                val json = it.mediaRecommendation
                if (json != null) {
                    val m = Media(json)
                    m.relation = json.type?.toString()
                    map[m.id] = m
                }
            }
        }

        val types = arrayOf("ANIME", "MANGA")
        suspend fun repeat(type: String) {
            val res =
                executeQuery<Query.MediaListCollection>("""{${recommendationPlannedQuery(type)}}""")
            res?.data?.mediaListCollection?.lists?.forEach { li ->
                li.entries?.forEach {
                    val m = Media(it)
                    if (m.status == "RELEASING" || m.status == "FINISHED") {
                        m.relation = it.media?.type?.toString()
                        map[m.id] = m
                    }
                }
            }
        }
        types.forEach { repeat(it) }

        val list = ArrayList(map.values.toList())
        list.sortByDescending { it.meanScore }
        return list
    }

    private fun recommendationQuery(): String {
        return """ Page(page: 1, perPage:30) { pageInfo { total currentPage hasNextPage } recommendations(sort: RATING_DESC, onList: true) { rating userRating mediaRecommendation { id idMal isAdult mediaListEntry { progress private score(format:POINT_100) status } chapters isFavourite format episodes nextAiringEpisode {episode} popularity meanScore isFavourite format title {english romaji userPreferred } type status(version: 2) bannerImage coverImage { large } } } } """
    }

    private fun recommendationPlannedQuery(type: String): String {
        return """ MediaListCollection(userId: ${Anilist.userid}, type: $type, status: PLANNING , sort: MEDIA_POPULARITY_DESC ) { lists { entries { media { id mediaListEntry { progress private score(format:POINT_100) status } idMal type isAdult popularity status(version: 2) chapters episodes nextAiringEpisode {episode} meanScore isFavourite format bannerImage coverImage{large} title { english romaji userPreferred } } } } }"""
    }

    suspend fun initHomePage(): Map<String, ArrayList<Media>> {
        val toShow: List<Boolean> =
            PrefManager.getVal(PrefName.HomeLayoutShow) // anime continue, anime fav, anime planned, manga continue, manga fav, manga planned, recommendations
        var query = """{"""
        if (toShow.getOrNull(0) == true) query += """currentAnime: ${
            continueMediaQuery(
                "ANIME",
                "CURRENT"
            )
        }, repeatingAnime: ${continueMediaQuery("ANIME", "REPEATING")}"""
        if (toShow.getOrNull(1) == true) query += """favoriteAnime: ${favMediaQuery(true, 1)}"""
        if (toShow.getOrNull(2) == true) query += """plannedAnime: ${
            continueMediaQuery(
                "ANIME",
                "PLANNING"
            )
        }"""
        if (toShow.getOrNull(3) == true) query += """currentManga: ${
            continueMediaQuery(
                "MANGA",
                "CURRENT"
            )
        }, repeatingManga: ${continueMediaQuery("MANGA", "REPEATING")}"""
        if (toShow.getOrNull(4) == true) query += """favoriteManga: ${favMediaQuery(false, 1)}"""
        if (toShow.getOrNull(5) == true) query += """plannedManga: ${
            continueMediaQuery(
                "MANGA",
                "PLANNING"
            )
        }"""
        if (toShow.getOrNull(6) == true) query += """recommendationQuery: ${recommendationQuery()}, recommendationPlannedQueryAnime: ${
            recommendationPlannedQuery(
                "ANIME"
            )
        }, recommendationPlannedQueryManga: ${recommendationPlannedQuery("MANGA")}"""
        query += """}""".trimEnd(',')

        val response = executeQuery<Query.HomePageMedia>(query)
        val returnMap = mutableMapOf<String, ArrayList<Media>>()
        fun current(type: String) {
            val subMap = mutableMapOf<Int, Media>()
            val returnArray = arrayListOf<Media>()
            val current =
                if (type == "Anime") response?.data?.currentAnime else response?.data?.currentManga
            val repeating =
                if (type == "Anime") response?.data?.repeatingAnime else response?.data?.repeatingManga
            current?.lists?.forEach { li ->
                li.entries?.reversed()?.forEach {
                    val m = Media(it)
                    m.cameFromContinue = true
                    subMap[m.id] = m
                }
            }
            repeating?.lists?.forEach { li ->
                li.entries?.reversed()?.forEach {
                    val m = Media(it)
                    m.cameFromContinue = true
                    subMap[m.id] = m
                }
            }
            val set = PrefManager.getCustomVal<Set<Int>>("continue_${type.uppercase()}", setOf())
                .toMutableSet()
            if (set.isNotEmpty()) {
                set.reversed().forEach {
                    if (subMap.containsKey(it)) returnArray.add(subMap[it]!!)
                }
                for (i in subMap) {
                    if (i.value !in returnArray) returnArray.add(i.value)
                }
            } else returnArray.addAll(subMap.values)
            returnMap["current$type"] = returnArray

        }

        fun planned(type: String) {
            val subMap = mutableMapOf<Int, Media>()
            val returnArray = arrayListOf<Media>()
            val current =
                if (type == "Anime") response?.data?.plannedAnime else response?.data?.plannedManga
            current?.lists?.forEach { li ->
                li.entries?.reversed()?.forEach {
                    val m = Media(it)
                    m.cameFromContinue = true
                    subMap[m.id] = m
                }
            }
            val set = PrefManager.getCustomVal<Set<Int>>("continue_$type", setOf()).toMutableSet()
            if (set.isNotEmpty()) {
                set.reversed().forEach {
                    if (subMap.containsKey(it)) returnArray.add(subMap[it]!!)
                }
                for (i in subMap) {
                    if (i.value !in returnArray) returnArray.add(i.value)
                }
            } else returnArray.addAll(subMap.values)
            returnMap["planned$type"] = returnArray
        }

        fun favorite(type: String) {
            val favourites =
                if (type == "Anime") response?.data?.favoriteAnime?.favourites else response?.data?.favoriteManga?.favourites
            val apiMediaList = if (type == "Anime") favourites?.anime else favourites?.manga
            val returnArray = arrayListOf<Media>()
            apiMediaList?.edges?.forEach {
                it.node?.let { i ->
                    returnArray.add(Media(i).apply { isFav = true })
                }
            }
            returnMap["favorite$type"] = returnArray
        }

        if (toShow.getOrNull(0) == true) {
            current("Anime")
        }
        if (toShow.getOrNull(1) == true) {
            favorite("Anime")
        }
        if (toShow.getOrNull(2) == true) {
            planned("Anime")
        }
        if (toShow.getOrNull(3) == true) {
            current("Manga")
        }
        if (toShow.getOrNull(4) == true) {
            favorite("Manga")
        }
        if (toShow.getOrNull(5) == true) {
            planned("Manga")
        }
        if (toShow.getOrNull(6) == true) {
            val subMap = mutableMapOf<Int, Media>()
            response?.data?.recommendationQuery?.apply {
                recommendations?.onEach {
                    val json = it.mediaRecommendation
                    if (json != null) {
                        val m = Media(json)
                        m.relation = json.type?.toString()
                        subMap[m.id] = m
                    }
                }
            }
            response?.data?.recommendationPlannedQueryAnime?.apply {
                lists?.forEach { li ->
                    li.entries?.forEach {
                        val m = Media(it)
                        if (m.status == "RELEASING" || m.status == "FINISHED") {
                            m.relation = it.media?.type?.toString()
                            subMap[m.id] = m
                        }
                    }
                }
            }
            response?.data?.recommendationPlannedQueryManga?.apply {
                lists?.forEach { li ->
                    li.entries?.forEach {
                        val m = Media(it)
                        if (m.status == "RELEASING" || m.status == "FINISHED") {
                            m.relation = it.media?.type?.toString()
                            subMap[m.id] = m
                        }
                    }
                }
            }
            val list = ArrayList(subMap.values.toList())
            list.sortByDescending { it.meanScore }
            returnMap["recommendations"] = list
        }
        return returnMap
    }


    private suspend fun bannerImage(type: String): String? {
        //var image = loadData<BannerImage>("banner_$type")
        val image: BannerImage? = BannerImage(
            PrefManager.getCustomVal("banner_${type}_url", null),
            PrefManager.getCustomVal("banner_${type}_time", 0L)
        )
        if (image == null || image.checkTime()) {
            val response =
                executeQuery<Query.MediaListCollection>("""{ MediaListCollection(userId: ${Anilist.userid}, type: $type, chunk:1,perChunk:25, sort: [SCORE_DESC,UPDATED_TIME_DESC]) { lists { entries{ media { id bannerImage } } } } } """)
            val random = response?.data?.mediaListCollection?.lists?.mapNotNull {
                it.entries?.mapNotNull { entry ->
                    val imageUrl = entry.media?.bannerImage
                    if (imageUrl != null && imageUrl != "null") imageUrl
                    else null
                }
            }?.flatten()?.randomOrNull() ?: return null
            PrefManager.setCustomVal("banner_${type}_url", random)
            PrefManager.setCustomVal("banner_${type}_time", System.currentTimeMillis())
            return random
        } else return image.url
    }

    suspend fun getBannerImages(): ArrayList<String?> {
        val default = arrayListOf<String?>(null, null)
        default[0] = bannerImage("ANIME")
        default[1] = bannerImage("MANGA")
        return default
    }

    suspend fun getMediaLists(
        anime: Boolean,
        userId: Int,
        sortOrder: String? = null
    ): MutableMap<String, ArrayList<Media>> {
        val response =
            executeQuery<Query.MediaListCollection>("""{ MediaListCollection(userId: $userId, type: ${if (anime) "ANIME" else "MANGA"}) { lists { name isCustomList entries { status progress private score(format:POINT_100) updatedAt media { id idMal isAdult type status chapters episodes nextAiringEpisode {episode} bannerImage meanScore isFavourite format coverImage{large} startDate{year month day} title {english romaji userPreferred } } } } user { id mediaListOptions { rowOrder animeList { sectionOrder } mangaList { sectionOrder } } } } }""")
        val sorted = mutableMapOf<String, ArrayList<Media>>()
        val unsorted = mutableMapOf<String, ArrayList<Media>>()
        val all = arrayListOf<Media>()
        val allIds = arrayListOf<Int>()

        response?.data?.mediaListCollection?.lists?.forEach { i ->
            val name = i.name.toString().trim('"')
            unsorted[name] = arrayListOf()
            i.entries?.forEach {
                val a = Media(it)
                unsorted[name]?.add(a)
                if (!allIds.contains(a.id)) {
                    allIds.add(a.id)
                    all.add(a)
                }
            }
        }

        val options = response?.data?.mediaListCollection?.user?.mediaListOptions
        val mediaList = if (anime) options?.animeList else options?.mangaList
        mediaList?.sectionOrder?.forEach {
            if (unsorted.containsKey(it)) sorted[it] = unsorted[it]!!
        }
        unsorted.forEach {
            if (!sorted.containsKey(it.key)) sorted[it.key] = it.value
        }

        sorted["Favourites"] = favMedia(anime)
        sorted["Favourites"]?.sortWith(compareBy { it.userFavOrder })
        //favMedia doesn't fill userProgress, so we need to fill it manually by searching :(
        sorted["Favourites"]?.forEach { fav ->
            all.find { it.id == fav.id }?.let {
                fav.userProgress = it.userProgress
            }
        }

        sorted["All"] = all
        val listSort: String = if (anime) PrefManager.getVal(PrefName.AnimeListSortOrder)
        else PrefManager.getVal(PrefName.MangaListSortOrder)
        val sort = listSort ?: sortOrder ?: options?.rowOrder
        for (i in sorted.keys) {
            when (sort) {
                "score" -> sorted[i]?.sortWith { b, a ->
                    compareValuesBy(
                        a,
                        b,
                        { it.userScore },
                        { it.meanScore })
                }

                "title" -> sorted[i]?.sortWith(compareBy { it.userPreferredName })
                "updatedAt" -> sorted[i]?.sortWith(compareByDescending { it.userUpdatedAt })
                "release" -> sorted[i]?.sortWith(compareByDescending { it.startDate })
                "id" -> sorted[i]?.sortWith(compareBy { it.id })
            }
        }
        return sorted
    }


    suspend fun getGenresAndTags(): Boolean {
        var genres: ArrayList<String>? = PrefManager.getVal<Set<String>>(PrefName.GenresList)
            .toMutableList() as ArrayList<String>?
        val adultTags = PrefManager.getVal<Set<String>>(PrefName.TagsListIsAdult).toMutableList()
        val nonAdultTags =
            PrefManager.getVal<Set<String>>(PrefName.TagsListNonAdult).toMutableList()
        var tags = if (adultTags.isEmpty() || nonAdultTags.isEmpty()) null else
            mapOf(
                true to adultTags,
                false to nonAdultTags
            )

        if (genres.isNullOrEmpty()) {
            executeQuery<Query.GenreCollection>(
                """{GenreCollection}""",
                force = true,
                useToken = false
            )?.data?.genreCollection?.apply {
                genres = arrayListOf()
                forEach {
                    genres?.add(it)
                }
                PrefManager.setVal(PrefName.GenresList, genres?.toSet())
            }
        }
        if (tags == null) {
            executeQuery<Query.MediaTagCollection>(
                """{ MediaTagCollection { name isAdult } }""",
                force = true
            )?.data?.mediaTagCollection?.apply {
                val adult = mutableListOf<String>()
                val good = mutableListOf<String>()
                forEach { node ->
                    if (node.isAdult == true) adult.add(node.name)
                    else good.add(node.name)
                }
                tags = mapOf(
                    true to adult,
                    false to good
                )
                PrefManager.setVal(PrefName.TagsListIsAdult, adult.toSet())
                PrefManager.setVal(PrefName.TagsListNonAdult, good.toSet())
            }
        }
        return if (!genres.isNullOrEmpty() && tags != null) {
            Anilist.genres = genres
            Anilist.tags = tags
            true
        } else false
    }

    suspend fun getGenres(genres: ArrayList<String>, listener: ((Pair<String, String>) -> Unit)) {
        genres.forEach {
            getGenreThumbnail(it).apply {
                if (this != null) {
                    listener.invoke(it to this.thumbnail)
                }
            }
        }
    }

    private fun <K, V : Serializable> saveSerializableMap(prefKey: String, map: Map<K, V>) {
        val byteStream = ByteArrayOutputStream()

        ObjectOutputStream(byteStream).use { outputStream ->
            outputStream.writeObject(map)
        }
        val serializedMap = Base64.encodeToString(byteStream.toByteArray(), Base64.DEFAULT)
        PrefManager.setCustomVal(prefKey, serializedMap)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <K, V : Serializable> loadSerializableMap(prefKey: String): Map<K, V>? {
        try {
            val serializedMap = PrefManager.getCustomVal(prefKey, "")
            if (serializedMap.isEmpty()) return null

            val bytes = Base64.decode(serializedMap, Base64.DEFAULT)
            val byteArrayStream = ByteArrayInputStream(bytes)

            return ObjectInputStream(byteArrayStream).use { inputStream ->
                inputStream.readObject() as? Map<K, V>
            }
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun getGenreThumbnail(genre: String): Genre? {
        val genres: MutableMap<String, Genre> =
            loadSerializableMap<String, Genre>("genre_thumb")?.toMutableMap()
                ?: mutableMapOf()
        if (genres.checkGenreTime(genre)) {
            try {
                val genreQuery =
                    """{ Page(perPage: 10){media(genre:"$genre", sort: TRENDING_DESC, type: ANIME, countryOfOrigin:"JP") {id bannerImage title{english romaji userPreferred} } } }"""
                executeQuery<Query.Page>(genreQuery, force = true)?.data?.page?.media?.forEach {
                    if (genres.checkId(it.id) && it.bannerImage != null) {
                        genres[genre] = Genre(
                            genre,
                            it.id,
                            it.bannerImage!!,
                            System.currentTimeMillis()
                        )
                        saveSerializableMap("genre_thumb", genres)
                        return genres[genre]
                    }
                }
            } catch (e: Exception) {
                logError(e)
            }
        } else {
            return genres[genre]
        }
        return null
    }

    suspend fun search(
        type: String,
        page: Int? = null,
        perPage: Int? = null,
        search: String? = null,
        sort: String? = null,
        genres: MutableList<String>? = null,
        tags: MutableList<String>? = null,
        format: String? = null,
        isAdult: Boolean = false,
        onList: Boolean? = null,
        excludedGenres: MutableList<String>? = null,
        excludedTags: MutableList<String>? = null,
        seasonYear: Int? = null,
        season: String? = null,
        id: Int? = null,
        hd: Boolean = false,
    ): SearchResults? {
        val query = """
query (${"$"}page: Int = 1, ${"$"}id: Int, ${"$"}type: MediaType, ${"$"}isAdult: Boolean = false, ${"$"}search: String, ${"$"}format: [MediaFormat], ${"$"}status: MediaStatus, ${"$"}countryOfOrigin: CountryCode, ${"$"}source: MediaSource, ${"$"}season: MediaSeason, ${"$"}seasonYear: Int, ${"$"}year: String, ${"$"}onList: Boolean, ${"$"}yearLesser: FuzzyDateInt, ${"$"}yearGreater: FuzzyDateInt, ${"$"}episodeLesser: Int, ${"$"}episodeGreater: Int, ${"$"}durationLesser: Int, ${"$"}durationGreater: Int, ${"$"}chapterLesser: Int, ${"$"}chapterGreater: Int, ${"$"}volumeLesser: Int, ${"$"}volumeGreater: Int, ${"$"}licensedBy: [String], ${"$"}isLicensed: Boolean, ${"$"}genres: [String], ${"$"}excludedGenres: [String], ${"$"}tags: [String], ${"$"}excludedTags: [String], ${"$"}minimumTagRank: Int, ${"$"}sort: [MediaSort] = [POPULARITY_DESC, SCORE_DESC]) {
  Page(page: ${"$"}page, perPage: ${perPage ?: 50}) {
    pageInfo {
      total
      perPage
      currentPage
      lastPage
      hasNextPage
    }
    media(id: ${"$"}id, type: ${"$"}type, season: ${"$"}season, format_in: ${"$"}format, status: ${"$"}status, countryOfOrigin: ${"$"}countryOfOrigin, source: ${"$"}source, search: ${"$"}search, onList: ${"$"}onList, seasonYear: ${"$"}seasonYear, startDate_like: ${"$"}year, startDate_lesser: ${"$"}yearLesser, startDate_greater: ${"$"}yearGreater, episodes_lesser: ${"$"}episodeLesser, episodes_greater: ${"$"}episodeGreater, duration_lesser: ${"$"}durationLesser, duration_greater: ${"$"}durationGreater, chapters_lesser: ${"$"}chapterLesser, chapters_greater: ${"$"}chapterGreater, volumes_lesser: ${"$"}volumeLesser, volumes_greater: ${"$"}volumeGreater, licensedBy_in: ${"$"}licensedBy, isLicensed: ${"$"}isLicensed, genre_in: ${"$"}genres, genre_not_in: ${"$"}excludedGenres, tag_in: ${"$"}tags, tag_not_in: ${"$"}excludedTags, minimumTagRank: ${"$"}minimumTagRank, sort: ${"$"}sort, isAdult: ${"$"}isAdult) {
      id
      idMal
      isAdult
      status
      chapters
      episodes
      nextAiringEpisode {
        episode
      }
      type
      genres
      meanScore
      isFavourite
      format
      bannerImage
      coverImage {
        large
        extraLarge
      }
      title {
        english
        romaji
        userPreferred
      }
      mediaListEntry {
        progress
        private
        score(format: POINT_100)
        status
      }
    }
  }
}
        """.replace("\n", " ").replace("""  """, "")
        val variables = """{"type":"$type","isAdult":$isAdult
            ${if (onList != null) ""","onList":$onList""" else ""}
            ${if (page != null) ""","page":"$page"""" else ""}
            ${if (id != null) ""","id":"$id"""" else ""}
            ${if (seasonYear != null) ""","seasonYear":"$seasonYear"""" else ""}
            ${if (season != null) ""","season":"$season"""" else ""}
            ${if (search != null) ""","search":"$search"""" else ""}
            ${if (sort != null) ""","sort":"$sort"""" else ""}
            ${if (format != null) ""","format":"${format.replace(" ", "_")}"""" else ""}
            ${if (genres?.isNotEmpty() == true) ""","genres":[${genres.joinToString { "\"$it\"" }}]""" else ""}
            ${
            if (excludedGenres?.isNotEmpty() == true)
                ""","excludedGenres":[${
                    excludedGenres.joinToString {
                        "\"${
                            it.replace(
                                "Not ",
                                ""
                            )
                        }\""
                    }
                }]"""
            else ""
        }
            ${if (tags?.isNotEmpty() == true) ""","tags":[${tags.joinToString { "\"$it\"" }}]""" else ""}
            ${
            if (excludedTags?.isNotEmpty() == true)
                ""","excludedTags":[${
                    excludedTags.joinToString {
                        "\"${
                            it.replace(
                                "Not ",
                                ""
                            )
                        }\""
                    }
                }]"""
            else ""
        }
            }""".replace("\n", " ").replace("""  """, "")

        val response = executeQuery<Query.Page>(query, variables, true)?.data?.page
        if (response?.media != null) {
            val responseArray = arrayListOf<Media>()
            response.media?.forEach { i ->
                val userStatus = i.mediaListEntry?.status.toString()
                val genresArr = arrayListOf<String>()
                if (i.genres != null) {
                    i.genres?.forEach { genre ->
                        genresArr.add(genre)
                    }
                }
                val media = Media(i)
                if (!hd) media.cover = i.coverImage?.large
                media.relation = if (onList == true) userStatus else null
                media.genres = genresArr
                responseArray.add(media)
            }

            val pageInfo = response.pageInfo ?: return null

            return SearchResults(
                type = type,
                perPage = perPage,
                search = search,
                sort = sort,
                isAdult = isAdult,
                onList = onList,
                genres = genres,
                excludedGenres = excludedGenres,
                tags = tags,
                excludedTags = excludedTags,
                format = format,
                seasonYear = seasonYear,
                season = season,
                results = responseArray,
                page = pageInfo.currentPage.toString().toIntOrNull() ?: 0,
                hasNextPage = pageInfo.hasNextPage == true,
            )
        }
        return null
    }

    suspend fun recentlyUpdated(
        smaller: Boolean = true,
        greater: Long = 0,
        lesser: Long = System.currentTimeMillis() / 1000 - 10000
    ): MutableList<Media>? {
        suspend fun execute(page: Int = 1): Page? {
            val query = """{
Page(page:$page,perPage:50) {
    pageInfo {
        hasNextPage
        total
    }
    airingSchedules(
        airingAt_greater: $greater
        airingAt_lesser: $lesser
        sort:TIME_DESC
    ) {
        episode
        airingAt
        media {
            id
            idMal
            status
            chapters
            episodes
            nextAiringEpisode { episode }
            isAdult
            type
            meanScore
            isFavourite
            format
            bannerImage
            countryOfOrigin
            coverImage { large }
            title {
                english
                romaji
                userPreferred
            }
            mediaListEntry {
                progress
                private
                score(format: POINT_100)
                status
            }
        }
    }
}
        }""".replace("\n", " ").replace("""  """, "")
            return executeQuery<Query.Page>(query, force = true)?.data?.page
        }
        if (smaller) {
            val response = execute()?.airingSchedules ?: return null
            val idArr = mutableListOf<Int>()
            val listOnly: Boolean = PrefManager.getVal(PrefName.RecentlyListOnly)
            return response.mapNotNull { i ->
                i.media?.let {
                    if (!idArr.contains(it.id))
                        if (!listOnly && (it.countryOfOrigin == "JP" && (if (!Anilist.adult) it.isAdult == false else true)) || (listOnly && it.mediaListEntry != null)) {
                            idArr.add(it.id)
                            Media(it)
                        } else null
                    else null
                }
            }.toMutableList()
        } else {
            var i = 1
            val list = mutableListOf<Media>()
            var res: Page? = null
            suspend fun next() {
                res = execute(i)
                list.addAll(res?.airingSchedules?.mapNotNull { j ->
                    j.media?.let {
                        if (it.countryOfOrigin == "JP" && (if (!Anilist.adult) it.isAdult == false else true)) {
                            Media(it).apply { relation = "${j.episode},${j.airingAt}" }
                        } else null
                    }
                } ?: listOf())
            }
            next()
            while (res?.pageInfo?.hasNextPage == true) {
                next()
                i++
            }
            return list.reversed().toMutableList()
        }
    }

    suspend fun getCharacterDetails(character: Character): Character {
        val query = """ {
  Character(id: ${character.id}) {
    id
    age
    gender
    description
    dateOfBirth {
      year
      month
      day
    }
    media(page: 0,sort:[POPULARITY_DESC,SCORE_DESC]) {
      pageInfo {
        total
        perPage
        currentPage
        lastPage
        hasNextPage
      }
      edges {
        id
        characterRole
        node {
          id
          idMal
          isAdult
          status
          chapters
          episodes
          nextAiringEpisode { episode }
          type
          meanScore
          isFavourite
          format
          bannerImage
          countryOfOrigin
          coverImage { large }
          title {
              english
              romaji
              userPreferred
          }
          mediaListEntry {
              progress
              private
              score(format: POINT_100)
              status
          }
        }
      }
    }
  }
}""".replace("\n", " ").replace("""  """, "")
        executeQuery<Query.Character>(query, force = true)?.data?.character?.apply {
            character.age = age
            character.gender = gender
            character.description = description
            character.dateOfBirth = dateOfBirth
            character.roles = arrayListOf()
            media?.edges?.forEach { i ->
                val m = Media(i)
                m.relation = i.characterRole.toString()
                character.roles?.add(m)
            }
        }
        return character
    }

    suspend fun getStudioDetails(studio: Studio): Studio {
        fun query(page: Int = 0) = """ {
  Studio(id: ${studio.id}) {
    id
    media(page: $page,sort:START_DATE_DESC) {
      pageInfo{
        hasNextPage
      }
      edges {
        id
        node {
          id
          idMal
          isAdult
          status
          chapters
          episodes
          nextAiringEpisode { episode }
          type
          meanScore
          startDate{ year }
          isFavourite
          format
          bannerImage
          countryOfOrigin
          coverImage { large }
          title {
              english
              romaji
              userPreferred
          }
          mediaListEntry {
              progress
              private
              score(format: POINT_100)
              status
          }
        }
      }
    }
  }
}""".replace("\n", " ").replace("""  """, "")

        var hasNextPage = true
        val yearMedia = mutableMapOf<String, ArrayList<Media>>()
        var page = 0
        while (hasNextPage) {
            page++
            hasNextPage =
                executeQuery<Query.Studio>(query(page), force = true)?.data?.studio?.media?.let {
                    it.edges?.forEach { i ->
                        i.node?.apply {
                            val status = status.toString()
                            val year = startDate?.year?.toString() ?: "TBA"
                            val title = if (status != "CANCELLED") year else status
                            if (!yearMedia.containsKey(title))
                                yearMedia[title] = arrayListOf()
                            yearMedia[title]?.add(Media(this))
                        }
                    }
                    it.pageInfo?.hasNextPage == true
                } ?: false
        }
        if (yearMedia.contains("CANCELLED")) {
            val a = yearMedia["CANCELLED"]!!
            yearMedia.remove("CANCELLED")
            yearMedia["CANCELLED"] = a
        }
        studio.yearMedia = yearMedia
        return studio
    }


    suspend fun getAuthorDetails(author: Author): Author {
        fun query(page: Int = 0) = """ {
  Staff(id: ${author.id}) {
    id
    staffMedia(page: $page,sort:START_DATE_DESC) {
      pageInfo{
        hasNextPage
      }
      edges {
        staffRole
        id
        node {
          id
          idMal
          isAdult
          status
          chapters
          episodes
          nextAiringEpisode { episode }
          type
          meanScore
          startDate{ year }
          isFavourite
          format
          bannerImage
          countryOfOrigin
          coverImage { large }
          title {
              english
              romaji
              userPreferred
          }
          mediaListEntry {
              progress
              private
              score(format: POINT_100)
              status
          }
        }
      }
    }
  }
}""".replace("\n", " ").replace("""  """, "")

        var hasNextPage = true
        val yearMedia = mutableMapOf<String, ArrayList<Media>>()
        var page = 0

        while (hasNextPage) {
            page++
            hasNextPage = executeQuery<Query.Author>(
                query(page),
                force = true
            )?.data?.author?.staffMedia?.let {
                it.edges?.forEach { i ->
                    i.node?.apply {
                        val status = status.toString()
                        val year = startDate?.year?.toString() ?: "TBA"
                        val title = if (status != "CANCELLED") year else status
                        if (!yearMedia.containsKey(title))
                            yearMedia[title] = arrayListOf()
                        val media = Media(this)
                        media.relation = i.staffRole
                        yearMedia[title]?.add(media)
                    }
                }
                it.pageInfo?.hasNextPage == true
            } ?: false
        }

        if (yearMedia.contains("CANCELLED")) {
            val a = yearMedia["CANCELLED"]!!
            yearMedia.remove("CANCELLED")
            yearMedia["CANCELLED"] = a
        }
        author.yearMedia = yearMedia
        return author
    }

}