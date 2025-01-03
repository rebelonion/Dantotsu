package ani.dantotsu.connections.anilist

import android.util.Base64
import ani.dantotsu.R
import ani.dantotsu.checkGenreTime
import ani.dantotsu.checkId
import ani.dantotsu.connections.anilist.Anilist.authorRoles
import ani.dantotsu.connections.anilist.Anilist.executeQuery
import ani.dantotsu.connections.anilist.api.FeedResponse
import ani.dantotsu.connections.anilist.api.FuzzyDate
import ani.dantotsu.connections.anilist.api.MediaEdge
import ani.dantotsu.connections.anilist.api.MediaList
import ani.dantotsu.connections.anilist.api.NotificationResponse
import ani.dantotsu.connections.anilist.api.Page
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.connections.anilist.api.ReplyResponse
import ani.dantotsu.currContext
import ani.dantotsu.isOnline
import ani.dantotsu.logError
import ani.dantotsu.media.Author
import ani.dantotsu.media.Character
import ani.dantotsu.media.Media
import ani.dantotsu.media.Studio
import ani.dantotsu.others.MalScraper
import ani.dantotsu.profile.User
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.Calendar
import kotlin.system.measureTimeMillis

class AnilistQueries {

    suspend fun getUserData(): Boolean {
        val response: Query.Viewer?
        measureTimeMillis {
            response = executeQuery(
                """{Viewer{name options{timezone titleLanguage staffNameLanguage activityMergeTime airingNotifications displayAdultContent restrictMessagesToFollowing} avatar{medium} bannerImage id mediaListOptions{scoreFormat rowOrder animeList{customLists} mangaList{customLists}} statistics{anime{episodesWatched} manga{chaptersRead}} unreadNotificationCount}}"""
            )
        }.also { println("time : $it") }
        val user = response?.data?.user ?: return false

        PrefManager.setVal(PrefName.AnilistUserName, user.name)
        Anilist.userid = user.id
        PrefManager.setVal(PrefName.AnilistUserId, user.id.toString())
        Anilist.username = user.name
        Anilist.bg = user.bannerImage
        Anilist.avatar = user.avatar?.medium
        Anilist.episodesWatched = user.statistics?.anime?.episodesWatched
        Anilist.chapterRead = user.statistics?.manga?.chaptersRead
        Anilist.adult = user.options?.displayAdultContent ?: false
        Anilist.unreadNotificationCount = user.unreadNotificationCount ?: 0
        val unread = PrefManager.getVal<Int>(PrefName.UnreadCommentNotifications)
        Anilist.unreadNotificationCount += unread
        Anilist.initialized = true

        user.options?.let {
            Anilist.titleLanguage = it.titleLanguage.toString()
            Anilist.staffNameLanguage = it.staffNameLanguage.toString()
            Anilist.airingNotifications = it.airingNotifications ?: false
            Anilist.restrictMessagesToFollowing = it.restrictMessagesToFollowing ?: false
            Anilist.timezone = it.timezone
            Anilist.activityMergeTime = it.activityMergeTime
        }
        user.mediaListOptions?.let {
            Anilist.scoreFormat = it.scoreFormat.toString()
            Anilist.rowOrder = it.rowOrder

            it.animeList?.let { animeList ->
                Anilist.animeCustomLists = animeList.customLists
            }

            it.mangaList?.let { mangaList ->
                Anilist.mangaCustomLists = mangaList.customLists
            }
        }
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
        runBlocking {
            val anilist = async {
                var response =
                    executeQuery<Query.Media>(fullMediaInformation(media.id), force = true)
                if (response != null) {
                    fun parse() {
                        val fetchedMedia = response?.data?.media ?: return
                        val user = response?.data?.page
                        media.source = fetchedMedia.source?.toString()
                        media.countryOfOrigin = fetchedMedia.countryOfOrigin
                        media.format = fetchedMedia.format?.toString()
                        media.favourites = fetchedMedia.favourites
                        media.popularity = fetchedMedia.popularity
                        media.startDate = fetchedMedia.startDate
                        media.endDate = fetchedMedia.endDate
                        media.streamingEpisodes = fetchedMedia.streamingEpisodes
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
                                            isFav = i.node?.isFavourite ?: false,
                                            role = when (i.role.toString()) {
                                                "MAIN" -> currContext()?.getString(R.string.main_role)
                                                    ?: "MAIN"

                                                "SUPPORTING" -> currContext()?.getString(R.string.supporting_role)
                                                    ?: "SUPPORTING"

                                                else -> i.role.toString()
                                            },
                                            voiceActor = i.voiceActors?.map {
                                                Author(
                                                    id = it.id,
                                                    name = it.name?.userPreferred,
                                                    image = it.image?.large,
                                                    role = it.languageV2
                                                )
                                            } as ArrayList<Author>
                                        )
                                    )
                                }
                            }
                        }
                        if (fetchedMedia.staff != null) {
                            media.staff = arrayListOf()
                            fetchedMedia.staff?.edges?.forEach { i ->
                                i.node?.apply {
                                    media.staff?.add(
                                        Author(
                                            id = id,
                                            name = i.node?.name?.userPreferred,
                                            image = i.node?.image?.large,
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
                        if (fetchedMedia.reviews?.nodes != null) {
                            media.review = fetchedMedia.reviews!!.nodes as ArrayList<Query.Review>
                        }
                        if (user?.mediaList?.isNotEmpty() == true) {
                            media.users = user.mediaList?.mapNotNull {
                                it.user?.let { user ->
                                    if (user.id != Anilist.userid) {
                                        User(
                                            user.id,
                                            user.name ?: "Unknown",
                                            user.avatar?.large,
                                            "",
                                            it.status?.toString(),
                                            it.score,
                                            it.progress,
                                            fetchedMedia.episodes ?: fetchedMedia.chapters,
                                        )
                                    } else null
                                }
                            }?.toCollection(arrayListOf()) ?: arrayListOf()
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
                                        firstStudio.name ?: "N/A",
                                        firstStudio.isFavourite ?: false,
                                        firstStudio.favourites ?: 0,
                                        null
                                    )
                                }
                            }

                            fetchedMedia.staff?.edges?.find { authorRoles.contains(it.role?.trim()) }?.node?.let {
                                media.anime.author = Author(
                                    it.id,
                                    it.name?.userPreferred ?: "N/A",
                                    it.image?.medium,
                                    "AUTHOR"
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
                                    it.id,
                                    it.name?.userPreferred ?: "N/A",
                                    it.image?.medium,
                                    "AUTHOR"
                                )
                            }
                        }
                        media.shareLink = fetchedMedia.siteUrl
                    }

                    if (response.data?.media != null) parse()
                    else {
                        snackString(currContext()?.getString(R.string.adult_stuff))
                        response = executeQuery(
                            fullMediaInformation(media.id),
                            force = true,
                            useToken = false
                        )
                        if (response?.data?.media != null) parse()
                        else snackString(currContext()?.getString(R.string.what_did_you_open))
                    }
                } else {
                    if (currContext()?.let { isOnline(it) } == true) {
                        snackString(currContext()?.getString(R.string.error_getting_data))
                    } else {
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

    fun userMediaDetails(media: Media): Media {
        val query =
            """{Media(id:${media.id}){id mediaListEntry{id status progress private repeat customLists updatedAt startedAt{year month day}completedAt{year month day}}isFavourite idMal}}"""
        runBlocking {
            val anilist = async {
                var response = executeQuery<Query.Media>(query, force = true, show = true)
                if (response != null) {
                    fun parse() {
                        val fetchedMedia = response?.data?.media ?: return

                        if (fetchedMedia.mediaListEntry != null) {
                            fetchedMedia.mediaListEntry?.apply {
                                media.userProgress = progress
                                media.isListPrivate = private ?: false
                                media.userListId = id
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
                            media.userRepeat = 0
                            media.userUpdatedAt = null
                            media.userCompletedAt = FuzzyDate()
                            media.userStartedAt = FuzzyDate()
                        }
                    }

                    if (response.data?.media != null) parse()
                    else {
                        response = executeQuery(query, force = true, useToken = false)
                        if (response?.data?.media != null) parse()
                    }
                }
            }
            awaitAll(anilist)
        }
        return media
    }

    private suspend fun favMedia(anime: Boolean, id: Int? = Anilist.userid): ArrayList<Media> {
        var hasNextPage = true
        var page = 0

        suspend fun getNextPage(page: Int): List<Media> {
            val response = executeQuery<Query.User>("""{${favMediaQuery(anime, page, id)}}""")
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

    suspend fun getUserStatus(): ArrayList<User>? {
        val toShow: List<Boolean> =
            PrefManager.getVal(PrefName.HomeLayout)
        if (toShow.getOrNull(7) != true) return null
        val query = """{Page1:${status(1)}Page2:${status(2)}}"""
        val response = executeQuery<Query.HomePageMedia>(query)
        val list = mutableListOf<User>()
        val threeDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -3)
        }.timeInMillis
        if (response?.data?.page1 != null && response.data.page2 != null) {
            val activities = listOf(
                response.data.page1.activities,
                response.data.page2.activities
            ).asSequence().flatten()
                .filter { it.typename != "MessageActivity" }
                .filter { if (Anilist.adult) true else it.media?.isAdult != true }
                .filter { it.createdAt * 1000L > threeDaysAgo }.toList()
                .sortedByDescending { it.createdAt }
            val anilistActivities = mutableListOf<User>()
            val groupedActivities = activities.groupBy { it.userId }

            groupedActivities.forEach { (_, userActivities) ->
                val user = userActivities.firstOrNull()?.user
                if (user != null) {
                    val userToAdd = User(
                        user.id,
                        user.name ?: "",
                        user.avatar?.medium,
                        user.bannerImage,
                        activity = userActivities.sortedBy { it.createdAt }.toList()
                    )
                    if (user.id == Anilist.userid) {
                        anilistActivities.add(0, userToAdd)

                    } else {
                        list.add(userToAdd)
                    }
                }
            }

            if (anilistActivities.isEmpty() && Anilist.token != null) {
                anilistActivities.add(
                    0,
                    User(
                        Anilist.userid!!,
                        Anilist.username!!,
                        Anilist.avatar,
                        Anilist.bg,
                        activity = listOf()
                    )
                )
            }
            list.addAll(0, anilistActivities)
            return list.toCollection(ArrayList())
        } else return null
    }

    private fun favMediaQuery(anime: Boolean, page: Int, id: Int? = Anilist.userid): String {
        return """User(id:${id}){id favourites{${if (anime) "anime" else "manga"}(page:$page){$standardPageInformation edges{favouriteOrder node{id idMal isAdult mediaListEntry{ progress private score(format:POINT_100) status } chapters isFavourite format episodes nextAiringEpisode{episode}meanScore isFavourite format startDate{year month day} title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}}}"""
    }

    private fun recommendationQuery(): String {
        return """ Page(page: 1, perPage:30) { $standardPageInformation recommendations(sort: RATING_DESC, onList: true) { rating userRating mediaRecommendation { id idMal isAdult mediaListEntry { progress private score(format:POINT_100) status } chapters isFavourite format episodes nextAiringEpisode {episode} popularity meanScore isFavourite format title {english romaji userPreferred } type status(version: 2) bannerImage coverImage { large } } } } """
    }

    private fun recommendationPlannedQuery(type: String): String {
        return """ MediaListCollection(userId: ${Anilist.userid}, type: $type, status: PLANNING${if (type == "ANIME") ", sort: MEDIA_POPULARITY_DESC" else ""} ) { lists { entries { media { id mediaListEntry { progress private score(format:POINT_100) status } idMal type isAdult popularity status(version: 2) chapters episodes nextAiringEpisode {episode} meanScore isFavourite format bannerImage coverImage{large} title { english romaji userPreferred } } } } }"""
    }

    private fun continueMediaQuery(type: String, status: String): String {
        return """ MediaListCollection(userId: ${Anilist.userid}, type: $type, status: $status , sort: UPDATED_TIME ) { lists { entries { progress private score(format:POINT_100) status media { id idMal type isAdult status chapters episodes nextAiringEpisode {episode} meanScore isFavourite format bannerImage coverImage{large} title { english romaji userPreferred } } } } } """
    }

    suspend fun initHomePage(): Map<String, ArrayList<Media>> {
        val removeList = PrefManager.getCustomVal("removeList", setOf<Int>())
        val hidePrivate = PrefManager.getVal<Boolean>(PrefName.HidePrivate)
        val removedMedia = ArrayList<Media>()
        val toShow: List<Boolean> =
            PrefManager.getVal(PrefName.HomeLayout) // list of booleans for what to show

        val queries = mutableListOf<String>()
        if (toShow.getOrNull(0) == true) {
            queries.add("""currentAnime: ${continueMediaQuery("ANIME", "CURRENT")}""")
            queries.add("""repeatingAnime: ${continueMediaQuery("ANIME", "REPEATING")}""")
        }
        if (toShow.getOrNull(1) == true) queries.add("""favoriteAnime: ${favMediaQuery(true, 1)}""")
        if (toShow.getOrNull(2) == true) queries.add(
            """plannedAnime: ${
                continueMediaQuery(
                    "ANIME",
                    "PLANNING"
                )
            }"""
        )
        if (toShow.getOrNull(3) == true) {
            queries.add("""currentManga: ${continueMediaQuery("MANGA", "CURRENT")}""")
            queries.add("""repeatingManga: ${continueMediaQuery("MANGA", "REPEATING")}""")
        }
        if (toShow.getOrNull(4) == true) queries.add(
            """favoriteManga: ${
                favMediaQuery(
                    false,
                    1
                )
            }"""
        )
        if (toShow.getOrNull(5) == true) queries.add(
            """plannedManga: ${
                continueMediaQuery(
                    "MANGA",
                    "PLANNING"
                )
            }"""
        )
        if (toShow.getOrNull(6) == true) {
            queries.add("""recommendationQuery: ${recommendationQuery()}""")
            queries.add("""recommendationPlannedQueryAnime: ${recommendationPlannedQuery("ANIME")}""")
            queries.add("""recommendationPlannedQueryManga: ${recommendationPlannedQuery("MANGA")}""")
        }

        val query = "{${queries.joinToString(",")}}"
        val response = executeQuery<Query.HomePageMedia>(query, show = true)
        val returnMap = mutableMapOf<String, ArrayList<Media>>()

        fun processMedia(
            type: String,
            currentMedia: List<MediaList>?,
            repeatingMedia: List<MediaList>?
        ) {
            val subMap = mutableMapOf<Int, Media>()
            val returnArray = arrayListOf<Media>()

            (currentMedia ?: emptyList()).forEach { entry ->
                val media = Media(entry)
                if (media.id !in removeList && (!hidePrivate || !media.isListPrivate)) {
                    media.cameFromContinue = true
                    subMap[media.id] = media
                } else {
                    removedMedia.add(media)
                }
            }

            (repeatingMedia ?: emptyList()).forEach { entry ->
                val media = Media(entry)
                if (media.id !in removeList && (!hidePrivate || !media.isListPrivate)) {
                    media.cameFromContinue = true
                    subMap[media.id] = media
                } else {
                    removedMedia.add(media)
                }
            }
            @Suppress("UNCHECKED_CAST")
            val list = PrefManager.getNullableCustomVal(
                "continue${type}List",
                listOf<Int>(),
                List::class.java
            ) as List<Int>
            if (list.isNotEmpty()) {
                list.reversed().forEach { id ->
                    subMap[id]?.let { returnArray.add(it) }
                }
                subMap.values.forEach {
                    if (!returnArray.contains(it)) returnArray.add(it)
                }
            } else {
                returnArray.addAll(subMap.values)
            }
            returnMap["current$type"] = returnArray
        }

        if (toShow.getOrNull(0) == true) processMedia(
            "Anime",
            response?.data?.currentAnime?.lists?.flatMap { it.entries ?: emptyList() }?.reversed(),
            response?.data?.repeatingAnime?.lists?.flatMap { it.entries ?: emptyList() }?.reversed()
        )
        if (toShow.getOrNull(2) == true) processMedia(
            "AnimePlanned",
            response?.data?.plannedAnime?.lists?.flatMap { it.entries ?: emptyList() }?.reversed(),
            null
        )
        if (toShow.getOrNull(3) == true) processMedia(
            "Manga",
            response?.data?.currentManga?.lists?.flatMap { it.entries ?: emptyList() }?.reversed(),
            response?.data?.repeatingManga?.lists?.flatMap { it.entries ?: emptyList() }?.reversed()
        )
        if (toShow.getOrNull(5) == true) processMedia(
            "MangaPlanned",
            response?.data?.plannedManga?.lists?.flatMap { it.entries ?: emptyList() }?.reversed(),
            null
        )

        fun processFavorites(type: String, favorites: List<MediaEdge>?) {
            val returnArray = arrayListOf<Media>()
            favorites?.forEach { edge ->
                edge.node?.let {
                    val media = Media(it).apply { isFav = true }
                    if (media.id !in removeList && (!hidePrivate || !media.isListPrivate)) {
                        returnArray.add(media)
                    } else {
                        removedMedia.add(media)
                    }
                }
            }
            returnMap["favorite$type"] = returnArray
        }

        if (toShow.getOrNull(1) == true) processFavorites(
            "Anime",
            response?.data?.favoriteAnime?.favourites?.anime?.edges
        )
        if (toShow.getOrNull(4) == true) processFavorites(
            "Manga",
            response?.data?.favoriteManga?.favourites?.manga?.edges
        )

        if (toShow.getOrNull(6) == true) {
            val subMap = mutableMapOf<Int, Media>()
            response?.data?.recommendationQuery?.recommendations?.forEach {
                it.mediaRecommendation?.let { json ->
                    val media = Media(json)
                    media.relation = json.type?.toString()
                    subMap[media.id] = media
                }
            }
            response?.data?.recommendationPlannedQueryAnime?.lists?.flatMap {
                it.entries ?: emptyList()
            }?.forEach {
                val media = Media(it)
                if (media.status in listOf("RELEASING", "FINISHED")) {
                    media.relation = it.media?.type?.toString()
                    subMap[media.id] = media
                }
            }
            response?.data?.recommendationPlannedQueryManga?.lists?.flatMap {
                it.entries ?: emptyList()
            }?.forEach {
                val media = Media(it)
                if (media.status in listOf("RELEASING", "FINISHED")) {
                    media.relation = it.media?.type?.toString()
                    subMap[media.id] = media
                }
            }
            val list = ArrayList(subMap.values).apply { sortByDescending { it.meanScore } }
            returnMap["recommendations"] = list
        }

        returnMap["hidden"] = removedMedia.distinctBy { it.id }.toCollection(arrayListOf())
        return returnMap
    }


    private suspend fun bannerImage(type: String): String? {
        val image = BannerImage(
            PrefManager.getCustomVal("banner_${type}_url", ""),
            PrefManager.getCustomVal("banner_${type}_time", 0L)
        )
        if (image.url.isNullOrEmpty() || image.checkTime()) {
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
            executeQuery<Query.MediaListCollection>("""{ MediaListCollection(userId: $userId, type: ${if (anime) "ANIME" else "MANGA"}) { lists { name isCustomList entries { status progress private score(format:POINT_100) updatedAt media { id idMal isAdult type status chapters episodes nextAiringEpisode {episode} bannerImage genres meanScore isFavourite format coverImage{large} startDate{year month day} title {english romaji userPreferred } } } } user { id mediaListOptions { rowOrder animeList { sectionOrder } mangaList { sectionOrder } } } } }""")
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

        sorted["Favourites"] = favMedia(anime, userId)
        sorted["Favourites"]?.sortWith(compareBy { it.userFavOrder })
        //favMedia doesn't fill userProgress, so we need to fill it manually by searching :(
        sorted["Favourites"]?.forEach { fav ->
            all.find { it.id == fav.id }?.let {
                fav.userProgress = it.userProgress
            }
        }

        sorted["All"] = all
        val listSort: String? = if (anime) PrefManager.getVal(PrefName.AnimeListSortOrder)
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
                true to adultTags.sortedBy { it },
                false to nonAdultTags.sortedBy { it }
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
            Anilist.genres = genres?.sortedBy { it }?.toMutableList() as ArrayList<String>
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

    suspend fun searchAniManga(
        type: String,
        page: Int? = null,
        perPage: Int? = null,
        search: String? = null,
        sort: String? = null,
        genres: MutableList<String>? = null,
        tags: MutableList<String>? = null,
        status: String? = null,
        source: String? = null,
        format: String? = null,
        countryOfOrigin: String? = null,
        isAdult: Boolean = false,
        onList: Boolean? = null,
        excludedGenres: MutableList<String>? = null,
        excludedTags: MutableList<String>? = null,
        startYear: Int? = null,
        seasonYear: Int? = null,
        season: String? = null,
        id: Int? = null,
        hd: Boolean = false,
        adultOnly: Boolean = false
    ): AniMangaSearchResults? {
        val variables = """{"type":"$type","isAdult":$isAdult
            ${if (adultOnly) ""","isAdult":true""" else ""}
            ${if (onList != null) ""","onList":$onList""" else ""}
            ${if (page != null) ""","page":"$page"""" else ""}
            ${if (id != null) ""","id":"$id"""" else ""}
            ${if (type == "ANIME" && seasonYear != null) ""","seasonYear":"$seasonYear"""" else ""}
            ${if (type == "MANGA" && startYear != null) ""","yearGreater":${startYear}0000,"yearLesser":${startYear + 1}0000""" else ""}
            ${if (season != null) ""","season":"$season"""" else ""}
            ${if (search != null) ""","search":"$search"""" else ""}
            ${if (source != null) ""","source":"$source"""" else ""}
            ${if (sort != null) ""","sort":"$sort"""" else ""}
            ${if (status != null) ""","status":"$status"""" else ""}
            ${if (format != null) ""","format":"${format.replace(" ", "_")}"""" else ""}
            ${if (countryOfOrigin != null) ""","countryOfOrigin":"$countryOfOrigin"""" else ""}
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
            }""".prepare()
        val response =
            executeQuery<Query.Page>(aniMangaSearch(perPage), variables, true)?.data?.page
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

            return AniMangaSearchResults(
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
                status = status,
                source = source,
                format = format,
                countryOfOrigin = countryOfOrigin,
                startYear = startYear,
                seasonYear = seasonYear,
                season = season,
                results = responseArray,
                page = pageInfo.currentPage.toString().toIntOrNull() ?: 0,
                hasNextPage = pageInfo.hasNextPage == true,
            )
        }
        return null
    }

    suspend fun searchCharacters(page: Int, search: String?): CharacterSearchResults? {
        if (search.isNullOrBlank()) return null
        val query = """
           {
             Page(page: $page, perPage: $ITEMS_PER_PAGE) {
               $standardPageInformation
               characters(search: "$search") {
                  ${characterInformation(false)}
               }
             }
           }
        """.prepare()

        val response = executeQuery<Query.Page>(query, force = true)?.data?.page

        if (response?.characters != null) {
            val responseArray = arrayListOf<Character>()
            response.characters?.forEach { i ->
                responseArray.add(
                    Character(
                        i.id,
                        i.name?.full,
                        i.image?.medium ?: i.image?.large,
                        null,
                        null.toString(),
                        i.isFavourite ?: false,
                        i.description,
                        i.age,
                        i.gender,
                        i.dateOfBirth,
                    )
                )
            }

            val pageInfo = response.pageInfo ?: return null

            return CharacterSearchResults(
                search = search,
                results = responseArray,
                page = pageInfo.currentPage ?: 0,
                hasNextPage = pageInfo.hasNextPage == true
            )
        }
        return null
    }

    suspend fun searchStudios(page: Int, search: String?): StudioSearchResults? {
        if (search.isNullOrBlank()) return null
        val query = """
           {
             Page(page: $page, perPage: $ITEMS_PER_PAGE) {
               $standardPageInformation
               studios(search: "$search") {
                  ${studioInformation(1, 1)}
               }
             }
           }
        """.prepare()

        val response = executeQuery<Query.Page>(query, force = true)?.data?.page

        if (response?.studios != null) {
            val responseArray = arrayListOf<Studio>()
            response.studios?.forEach { i ->
                responseArray.add(
                    Studio(
                        i.id.toString(),
                        i.name ?: return null,
                        i.isFavourite ?: false,
                        i.favourites,
                        i.media?.edges?.firstOrNull()?.node?.let { it.coverImage?.large }
                    )
                )
            }

            val pageInfo = response.pageInfo ?: return null

            return StudioSearchResults(
                search = search,
                results = responseArray,
                page = pageInfo.currentPage ?: 0,
                hasNextPage = pageInfo.hasNextPage == true
            )
        }
        return null
    }

    suspend fun searchStaff(page: Int, search: String?): StaffSearchResults? {
        if (search.isNullOrBlank()) return null
        val query = """
           {
             Page(page: $page, perPage: $ITEMS_PER_PAGE) {
               $standardPageInformation
               staff(search: "$search") {
                  ${staffInformation(1, 1)}
               }
             }
           }
        """.prepare()

        val response = executeQuery<Query.Page>(query, force = true)?.data?.page

        if (response?.staff != null) {
            val responseArray = arrayListOf<Author>()
            response.staff?.forEach { i ->
                responseArray.add(
                    Author(
                        i.id,
                        i.name?.userPreferred ?: return null,
                        i.image?.large,
                        null,
                        null,
                        null
                    )
                )
            }

            val pageInfo = response.pageInfo ?: return null

            return StaffSearchResults(
                search = search,
                results = responseArray,
                page = pageInfo.currentPage ?: 0,
                hasNextPage = pageInfo.hasNextPage == true
            )
        }
        return null
    }

    suspend fun searchUsers(page: Int, search: String?): UserSearchResults? {
        val query = """
           {
             Page(page: $page, perPage: $ITEMS_PER_PAGE) {
               $standardPageInformation
               users(search: "$search") {
                  ${userInformation()}
               }
             }
           }
        """.prepare()

        val response = executeQuery<Query.Page>(query, force = true)?.data?.page

        if (response?.users != null) {
            val users = response.users?.map { user ->
                User(
                    user.id,
                    user.name ?: return null,
                    user.avatar?.medium,
                    user.bannerImage
                )
            } ?: return null

            return UserSearchResults(
                search = search,
                results = users.toMutableList(),
                page = response.pageInfo?.currentPage ?: 0,
                hasNextPage = response.pageInfo?.hasNextPage == true
            )
        }
        return null
    }

    private fun mediaList(media1: Page?): ArrayList<Media> {
        val combinedList = arrayListOf<Media>()
        media1?.media?.mapTo(combinedList) { Media(it) }
        return combinedList
    }

    private fun getPreference(pref: PrefName): Boolean = PrefManager.getVal(pref)

    private fun buildQueryString(
        sort: String,
        type: String,
        format: String? = null,
        country: String? = null
    ): String {
        val includeList = when {
            type == "ANIME" && !getPreference(PrefName.IncludeAnimeList) -> "onList:false"
            type == "MANGA" && !getPreference(PrefName.IncludeMangaList) -> "onList:false"
            else -> ""
        }
        val isAdult = if (getPreference(PrefName.AdultOnly)) "isAdult:true" else ""
        val formatFilter = format?.let { "format:$it, " } ?: ""
        val countryFilter = country?.let { "countryOfOrigin:$it, " } ?: ""

        return buildString {
            append("""Page(page:1,perPage:50){$standardPageInformation media(sort:$sort, type:$type, $formatFilter $countryFilter $includeList $isAdult){id idMal status chapters episodes nextAiringEpisode{episode} isAdult type meanScore isFavourite format bannerImage countryOfOrigin coverImage{large} title{english romaji userPreferred} mediaListEntry{progress private score(format:POINT_100) status}}}""")
        }
    }

    private fun recentAnimeUpdates(page: Int): String {
        val currentTime = System.currentTimeMillis() / 1000
        return buildString {
            append("""Page(page:$page,perPage:50){$standardPageInformation airingSchedules(airingAt_greater:0 airingAt_lesser:${currentTime - 10000} sort:TIME_DESC){episode airingAt media{id idMal status chapters episodes nextAiringEpisode{episode} isAdult type meanScore isFavourite format bannerImage countryOfOrigin coverImage{large} title{english romaji userPreferred} mediaListEntry{progress private score(format:POINT_100) status}}}}""")
        }
    }

    private fun queryAnimeList(): String {
        return buildString {
            append(
                """{recentUpdates:${recentAnimeUpdates(1)} recentUpdates2:${recentAnimeUpdates(2)} trendingMovies:${
                    buildQueryString(
                        "POPULARITY_DESC",
                        "ANIME",
                        "MOVIE"
                    )
                } topRated:${
                    buildQueryString(
                        "SCORE_DESC",
                        "ANIME"
                    )
                } mostFav:${buildQueryString("FAVOURITES_DESC", "ANIME")}}"""
            )
        }
    }

    private fun queryMangaList(): String {
        return buildString {
            append(
                """{trendingManga:${
                    buildQueryString(
                        "POPULARITY_DESC",
                        "MANGA",
                        country = "JP"
                    )
                } trendingManhwa:${
                    buildQueryString(
                        "POPULARITY_DESC",
                        "MANGA",
                        country = "KR"
                    )
                } trendingNovel:${
                    buildQueryString(
                        "POPULARITY_DESC",
                        "MANGA",
                        format = "NOVEL",
                        country = "JP"
                    )
                } topRated:${
                    buildQueryString(
                        "SCORE_DESC",
                        "MANGA"
                    )
                } mostFav:${buildQueryString("FAVOURITES_DESC", "MANGA")}}"""
            )
        }
    }

    suspend fun loadAnimeList(): Map<String, ArrayList<Media>> = coroutineScope {
        val list = mutableMapOf<String, ArrayList<Media>>()

        fun filterRecentUpdates(page: Page?): ArrayList<Media> {
            val listOnly = getPreference(PrefName.RecentlyListOnly)
            val adultOnly = getPreference(PrefName.AdultOnly)
            val idArr = mutableSetOf<Int>()
            return page?.airingSchedules?.mapNotNull { i ->
                i.media?.takeIf { !idArr.contains(it.id) }?.let {
                    val shouldAdd = when {
                        !listOnly && it.countryOfOrigin == "JP" && adultOnly && it.isAdult == true -> true
                        !listOnly && !adultOnly && it.countryOfOrigin == "JP" && it.isAdult == false -> true
                        listOnly && it.mediaListEntry != null -> true
                        else -> false
                    }
                    if (shouldAdd) {
                        idArr.add(it.id)
                        Media(it)
                    } else null
                }
            }?.toCollection(ArrayList()) ?: arrayListOf()
        }

        val animeList = async { executeQuery<Query.AnimeList>(queryAnimeList(), force = true) }

        animeList.await()?.data?.apply {
            list["recentUpdates"] = filterRecentUpdates(recentUpdates)
            list["trendingMovies"] = mediaList(trendingMovies)
            list["topRated"] = mediaList(topRated)
            list["mostFav"] = mediaList(mostFav)
        }

        list
    }

    suspend fun loadMangaList(): Map<String, ArrayList<Media>> = coroutineScope {
        val list = mutableMapOf<String, ArrayList<Media>>()

        val mangaList = async { executeQuery<Query.MangaList>(queryMangaList(), force = true) }

        mangaList.await()?.data?.apply {
            list["trendingManga"] = mediaList(trendingManga)
            list["trendingManhwa"] = mediaList(trendingManhwa)
            list["trendingNovel"] = mediaList(trendingNovel)
            list["topRated"] = mediaList(topRated)
            list["mostFav"] = mediaList(mostFav)
        }

        list
    }

    suspend fun recentlyUpdated(
        greater: Long = 0,
        lesser: Long = System.currentTimeMillis() / 1000 - 10000
    ): MutableList<Media> {
        suspend fun execute(page: Int = 1): Page? {
            val query = """{
Page(page:$page,perPage:50) {
    $standardPageInformation
    airingSchedules(
        airingAt_greater: $greater
        airingAt_lesser: $lesser
        sort:TIME_DESC
    ) {
        episode
        airingAt
        media {
            ${standardMediaInformation()}
        }
    }
}
        }""".prepare()
            return executeQuery<Query.Page>(query, force = true)?.data?.page
        }

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

    suspend fun getCharacterDetails(character: Character): Character {
        val query = """ {
  Character(id: ${character.id}) {
    ${characterInformation(true)}
  }
}""".prepare()
        executeQuery<Query.Character>(query, force = true)?.data?.character?.let { i ->
            return Character(
                i.id,
                i.name?.full,
                i.image?.large ?: i.image?.medium,
                null,
                null.toString(),
                i.isFavourite ?: false,
                i.description,
                i.age,
                i.gender,
                i.dateOfBirth,
                i.media?.edges?.map {
                    val m = Media(it)
                    m.relation = it.characterRole.toString()
                    m
                }?.let { ArrayList(it) },
                i.media?.edges?.flatMap { edge ->
                    edge.voiceActors?.map { va ->
                        Author(
                            va.id,
                            va.name?.userPreferred,
                            va.image?.large ?: va.image?.medium,
                            va.languageV2
                        )
                    } ?: emptyList()
                } as ArrayList<Author>?
            )
        }
        return character
    }

    suspend fun getStudioDetails(studio: Studio): Studio {
        fun query(page: Int = 0) = """ {
  Studio(id: ${studio.id}) {
    ${studioInformation(page, ITEMS_PER_PAGE)}
  }
}""".prepare()

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
    ${staffInformation(page, ITEMS_PER_PAGE)}
    characters(page: $page,sort:FAVOURITES_DESC) {
      $standardPageInformation
      nodes{
        ${characterInformation(false)}
      }
    }
  }
}""".prepare()

        var hasNextPage = true
        val yearMedia = mutableMapOf<String, ArrayList<Media>>()
        var page = 0
        val characters = arrayListOf<Character>()
        while (hasNextPage) {
            page++
            val query = executeQuery<Query.Author>(
                query(page), force = true
            )?.data?.author
            author.age = query?.age
            author.yearsActive =
                if (query?.yearsActive?.isEmpty() == true) null else query?.yearsActive
            author.homeTown = if (query?.homeTown?.isBlank() == true) null else query?.homeTown
            author.dateOfDeath = if (query?.dateOfDeath?.toStringOrEmpty()
                    ?.isBlank() == true
            ) null else query?.dateOfDeath?.toStringOrEmpty()
            author.dateOfBirth = if (query?.dateOfBirth?.toStringOrEmpty()
                    ?.isBlank() == true
            ) null else query?.dateOfBirth?.toStringOrEmpty()
            hasNextPage = query?.staffMedia?.let {
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
            query?.characters?.let {
                it.nodes?.forEach { i ->
                    characters.add(
                        Character(
                            i.id,
                            i.name?.userPreferred,
                            i.image?.large,
                            i.image?.medium,
                            "",
                            false
                        )
                    )
                }
            }
        }

        if (yearMedia.contains("CANCELLED")) {
            val a = yearMedia["CANCELLED"]!!
            yearMedia.remove("CANCELLED")
            yearMedia["CANCELLED"] = a
        }
        author.character = characters
        author.yearMedia = yearMedia
        return author
    }

    suspend fun getReviews(
        mediaId: Int,
        page: Int = 1,
        sort: String = "SCORE_DESC"
    ): Query.ReviewsResponse? {
        return executeQuery<Query.ReviewsResponse>(
            """{Page(page:$page,perPage:10){$standardPageInformation reviews(mediaId:$mediaId,sort:$sort){id,mediaId,mediaType,summary,body(asHtml:true)rating,ratingAmount,userRating,score,private,siteUrl,createdAt,updatedAt,user{id,name,bannerImage avatar{medium,large}}}}}""",
            force = true
        )
    }

    suspend fun getUserProfile(id: Int): Query.UserProfileResponse? {
        return executeQuery<Query.UserProfileResponse>(
            """{followerPage:Page{followers(userId:$id){id}$standardPageInformation}followingPage:Page{following(userId:$id){id}$standardPageInformation}user:User(id:$id){id name about(asHtml:true)avatar{medium large}bannerImage isFollowing isFollower isBlocked favourites{anime{nodes{id coverImage{extraLarge large medium color}}}manga{nodes{id coverImage{extraLarge large medium color}}}characters{nodes{id name{first middle last full native alternative userPreferred}image{large medium}isFavourite}}staff{nodes{id name{first middle last full native alternative userPreferred}image{large medium}isFavourite}}studios{nodes{id name isFavourite}}}statistics{anime{count meanScore standardDeviation minutesWatched episodesWatched chaptersRead volumesRead}manga{count meanScore standardDeviation minutesWatched episodesWatched chaptersRead volumesRead}}siteUrl}}""",
            force = true
        )
    }

    suspend fun getUserProfile(username: String): Query.UserProfileResponse? {
        val id = getUserId(username) ?: return null
        return getUserProfile(id)
    }

    private suspend fun getUserId(username: String): Int? {
        return executeQuery<Query.User>(
            """{User(name:"$username"){id}}""",
            force = true
        )?.data?.user?.id
    }

    suspend fun getUserStatistics(id: Int, sort: String = "ID"): Query.StatisticsResponse? {
        return executeQuery<Query.StatisticsResponse>(
            """{User(id:$id){id name mediaListOptions{scoreFormat}statistics{anime{...UserStatistics}manga{...UserStatistics}}}}fragment UserStatistics on UserStatistics{count meanScore standardDeviation minutesWatched episodesWatched chaptersRead volumesRead formats(sort:$sort){count meanScore minutesWatched chaptersRead mediaIds format}statuses(sort:$sort){count meanScore minutesWatched chaptersRead mediaIds status}scores(sort:$sort){count meanScore minutesWatched chaptersRead mediaIds score}lengths(sort:$sort){count meanScore minutesWatched chaptersRead mediaIds length}releaseYears(sort:$sort){count meanScore minutesWatched chaptersRead mediaIds releaseYear}startYears(sort:$sort){count meanScore minutesWatched chaptersRead mediaIds startYear}genres(sort:$sort){count meanScore minutesWatched chaptersRead mediaIds genre}tags(sort:$sort){count meanScore minutesWatched chaptersRead mediaIds tag{id name}}countries(sort:$sort){count meanScore minutesWatched chaptersRead mediaIds country}voiceActors(sort:$sort){count meanScore minutesWatched chaptersRead mediaIds voiceActor{id name{first middle last full native alternative userPreferred}}characterIds}staff(sort:$sort){count meanScore minutesWatched chaptersRead mediaIds staff{id name{first middle last full native alternative userPreferred}}}studios(sort:$sort){count meanScore minutesWatched chaptersRead mediaIds studio{id name isAnimationStudio}}}""",
            force = true,
            show = true
        )
    }

    private fun userFavMediaQuery(anime: Boolean, id: Int): String {
        return """User(id:${id}){id favourites{${if (anime) "anime" else "manga"}(page:1){$standardPageInformation edges{favouriteOrder node{id idMal isAdult mediaListEntry{ progress private score(format:POINT_100) status } chapters isFavourite format episodes nextAiringEpisode{episode}meanScore isFavourite format startDate{year month day} title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}}}"""
    }

    suspend fun userFollowing(id: Int): Query.Following? {
        return executeQuery<Query.Following>(
            """{Page {following(userId:${id},sort:[USERNAME]){id name avatar{large medium}bannerImage}}}""",
            force = true
        )
    }

    suspend fun userFollowers(id: Int): Query.Follower? {
        return executeQuery<Query.Follower>(
            """{Page {followers(userId:${id},sort:[USERNAME]){id name avatar{large medium}bannerImage}}}""",
            force = true
        )
    }

    suspend fun initProfilePage(id: Int): Query.ProfilePageMedia? {
        return executeQuery<Query.ProfilePageMedia>(
            """{
            favoriteAnime:${userFavMediaQuery(true, id)}
            favoriteManga:${userFavMediaQuery(false, id)}
            }""".prepare(), force = true
        )
    }


    suspend fun getNotifications(
        id: Int,
        page: Int = 1,
        resetNotification: Boolean = true,
        type: Boolean? = null
    ): NotificationResponse? {
        val typeIn = "type_in:[AIRING,MEDIA_MERGE,MEDIA_DELETION,MEDIA_DATA_CHANGE]"
        val reset = if (resetNotification) "true" else "false"
        val res = executeQuery<NotificationResponse>(
            """{User(id:$id){unreadNotificationCount}Page(page:$page,perPage:$ITEMS_PER_PAGE){$standardPageInformation notifications(resetNotificationCount:$reset , ${if (type == true) typeIn else ""}){__typename...on AiringNotification{id,type,animeId,episode,contexts,createdAt,media{id,title{romaji,english,native,userPreferred}bannerImage,coverImage{medium,large}},}...on FollowingNotification{id,userId,type,context,createdAt,user{id,name,bannerImage,avatar{medium,large,}}}...on ActivityMessageNotification{id,userId,type,activityId,context,createdAt,message{id}user{id,name,bannerImage,avatar{medium,large,}}}...on ActivityMentionNotification{id,userId,type,activityId,context,createdAt,activity{__typename}user{id,name,bannerImage,avatar{medium,large,}}}...on ActivityReplyNotification{id,userId,type,activityId,context,createdAt,activity{__typename}user{id,name,bannerImage,avatar{medium,large,}}}...on ActivityReplySubscribedNotification{id,userId,type,activityId,context,createdAt,activity{__typename}user{id,name,bannerImage,avatar{medium,large,}}}...on ActivityLikeNotification{id,userId,type,activityId,context,createdAt,activity{__typename}user{id,name,bannerImage,avatar{medium,large,}}}...on ActivityReplyLikeNotification{id,userId,type,activityId,context,createdAt,activity{__typename}user{id,name,bannerImage,avatar{medium,large,}}}...on ThreadCommentMentionNotification{id,userId,type,commentId,context,createdAt,thread{id}comment{id}user{id,name,bannerImage,avatar{medium,large,}}}...on ThreadCommentReplyNotification{id,userId,type,commentId,context,createdAt,thread{id}comment{id}user{id,name,bannerImage,avatar{medium,large,}}}...on ThreadCommentSubscribedNotification{id,userId,type,commentId,context,createdAt,thread{id}comment{id}user{id,name,bannerImage,avatar{medium,large,}}}...on ThreadCommentLikeNotification{id,userId,type,commentId,context,createdAt,thread{id}comment{id}user{id,name,bannerImage,avatar{medium,large,}}}...on ThreadLikeNotification{id,userId,type,threadId,context,createdAt,thread{id}comment{id}user{id,name,bannerImage,avatar{medium,large,}}}...on RelatedMediaAdditionNotification{id,type,context,createdAt,media{id,title{romaji,english,native,userPreferred}bannerImage,coverImage{medium,large}}}...on MediaDataChangeNotification{id,type,mediaId,context,reason,createdAt,media{id,title{romaji,english,native,userPreferred}bannerImage,coverImage{medium,large}}}...on MediaMergeNotification{id,type,mediaId,deletedMediaTitles,context,reason,createdAt,media{id,title{romaji,english,native,userPreferred}bannerImage,coverImage{medium,large}}}...on MediaDeletionNotification{id,type,deletedMediaTitle,context,reason,createdAt,}}}}""",
            force = true
        )
        if (res != null && resetNotification) {
            val commentNotifications = PrefManager.getVal(PrefName.UnreadCommentNotifications, 0)
            res.data.user.unreadNotificationCount += commentNotifications
            PrefManager.setVal(PrefName.UnreadCommentNotifications, 0)
            Anilist.unreadNotificationCount = 0
        }
        return res
    }

    suspend fun getFeed(
        userId: Int?,
        global: Boolean = false,
        page: Int = 1,
        activityId: Int? = null
    ): FeedResponse? {
        val filter = if (activityId != null) "id:$activityId,"
        else if (userId != null) "userId:$userId,"
        else if (global) "isFollowing:false,hasRepliesOrTypeText:true,"
        else "isFollowing:true,"
        val typeIn =
            if (filter == "isFollowing:true,") "type_in:[TEXT,ANIME_LIST,MANGA_LIST,MEDIA_LIST]," else ""
        return executeQuery<FeedResponse>(
            """{Page(page:$page,perPage:$ITEMS_PER_PAGE){activities(${filter}${typeIn}sort:ID_DESC){__typename ... on TextActivity{id userId type replyCount text(asHtml:true)siteUrl isLocked isSubscribed likeCount isLiked isPinned createdAt user{id name bannerImage avatar{medium large}}replies{id userId activityId text(asHtml:true)likeCount isLiked createdAt user{id name bannerImage avatar{medium large}}likes{id name bannerImage avatar{medium large}}}likes{id name bannerImage avatar{medium large}}}... on ListActivity{id userId type replyCount status progress siteUrl isLocked isSubscribed likeCount isLiked isPinned createdAt user{id name bannerImage avatar{medium large}}media{id title{english romaji native userPreferred}bannerImage coverImage{medium large}isAdult}replies{id userId activityId text(asHtml:true)likeCount isLiked createdAt user{id name bannerImage avatar{medium large}}likes{id name bannerImage avatar{medium large}}}likes{id name bannerImage avatar{medium large}}}... on MessageActivity{id recipientId messengerId type replyCount likeCount message(asHtml:true)isLocked isSubscribed isLiked isPrivate siteUrl createdAt recipient{id name bannerImage avatar{medium large}}messenger{id name bannerImage avatar{medium large}}replies{id userId activityId text(asHtml:true)likeCount isLiked createdAt user{id name bannerImage avatar{medium large}}likes{id name bannerImage avatar{medium large}}}likes{id name bannerImage avatar{medium large}}}}}}""",
            force = true
        )
    }

    suspend fun getReplies(
        activityId: Int,
        page: Int = 1
    ): ReplyResponse? {
        val query =
            """{Page(page:$page,perPage:50){activityReplies(activityId:$activityId){id userId activityId text(asHtml:true)likeCount isLiked createdAt user{id name bannerImage avatar{medium large}}likes{id name bannerImage avatar{medium large}}}}}"""
        return executeQuery(query, force = true)
    }

    private fun status(page: Int = 1): String {
        return """Page(page:$page,perPage:50){activities(isFollowing: true,type_in:[TEXT,ANIME_LIST,MANGA_LIST,MEDIA_LIST],sort:ID_DESC){__typename ... on TextActivity{id userId type replyCount text(asHtml:true)siteUrl isLocked isSubscribed replyCount likeCount isLiked createdAt user{id name bannerImage avatar{medium large}}likes{id name bannerImage avatar{medium large}}}... on ListActivity{id userId type replyCount status progress siteUrl isLocked isSubscribed replyCount likeCount isLiked isPinned createdAt user{id name bannerImage avatar{medium large}}media{id isAdult title{english romaji native userPreferred}bannerImage coverImage{extraLarge medium large}}likes{id name bannerImage avatar{medium large}}}... on MessageActivity{id type createdAt}}}"""
    }

    suspend fun getUpcomingAnime(id: String): List<Media> {
        val res = executeQuery<Query.MediaListCollection>(
            """{MediaListCollection(userId:$id,type:ANIME){lists{name entries{media{id,isFavourite,title{userPreferred,romaji}coverImage{medium}nextAiringEpisode{timeUntilAiring}}}}}}""",
            force = true
        )
        val list = mutableListOf<Media>()
        res?.data?.mediaListCollection?.lists?.forEach { listEntry ->
            listEntry.entries?.forEach { entry ->
                entry.media?.nextAiringEpisode?.timeUntilAiring?.let {
                    list.add(Media(entry.media!!))
                }
            }
        }
        return list.sortedBy { it.timeUntilAiring }
            .distinctBy { it.id }
            .filter { it.timeUntilAiring != null }
    }

    suspend fun isUserFav(
        favType: AnilistMutations.FavType,
        id: Int
    ): Boolean {   //anilist isFavourite is broken, so we need to check it manually
        val res = getUserProfile(Anilist.userid ?: return false)
        return when (favType) {
            AnilistMutations.FavType.ANIME -> res?.data?.user?.favourites?.anime?.nodes?.any { it.id == id }
                ?: false

            AnilistMutations.FavType.MANGA -> res?.data?.user?.favourites?.manga?.nodes?.any { it.id == id }
                ?: false

            AnilistMutations.FavType.CHARACTER -> res?.data?.user?.favourites?.characters?.nodes?.any { it.id == id }
                ?: false

            AnilistMutations.FavType.STAFF -> res?.data?.user?.favourites?.staff?.nodes?.any { it.id == id }
                ?: false

            AnilistMutations.FavType.STUDIO -> res?.data?.user?.favourites?.studios?.nodes?.any { it.id == id }
                ?: false
        }
    }

    companion object {
        const val ITEMS_PER_PAGE = 25
    }
}
