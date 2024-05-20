package ani.dantotsu.connections.anilist

import android.util.Base64
import ani.dantotsu.R
import ani.dantotsu.checkGenreTime
import ani.dantotsu.checkId
import ani.dantotsu.connections.anilist.Anilist.authorRoles
import ani.dantotsu.connections.anilist.Anilist.executeQuery
import ani.dantotsu.connections.anilist.api.FeedResponse
import ani.dantotsu.connections.anilist.api.FuzzyDate
import ani.dantotsu.connections.anilist.api.NotificationResponse
import ani.dantotsu.connections.anilist.api.Page
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.connections.anilist.api.ReplyResponse
import ani.dantotsu.connections.anilist.api.ToggleLike
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
            response =
                executeQuery("""{Viewer{name options{displayAdultContent}avatar{medium}bannerImage id mediaListOptions{rowOrder animeList{sectionOrder customLists}mangaList{sectionOrder customLists}}statistics{anime{episodesWatched}manga{chaptersRead}}unreadNotificationCount}}""")
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
            """{Media(id:${media.id}){id favourites popularity episodes chapters mediaListEntry{id status score(format:POINT_100)progress private notes repeat customLists updatedAt startedAt{year month day}completedAt{year month day}}isFavourite siteUrl idMal nextAiringEpisode{episode airingAt}source countryOfOrigin format duration season seasonYear startDate{year month day}endDate{year month day}genres studios(isMain:true){nodes{id name siteUrl}}description trailer{site id}synonyms tags{name rank isMediaSpoiler}characters(sort:[ROLE,FAVOURITES_DESC],perPage:25,page:1){edges{role voiceActors { id name { first middle last full native userPreferred } image { large medium } languageV2 } node{id image{medium}name{userPreferred}isFavourite}}}relations{edges{relationType(version:2)node{id idMal mediaListEntry{progress private score(format:POINT_100)status}episodes chapters nextAiringEpisode{episode}popularity meanScore isAdult isFavourite format title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}staffPreview:staff(perPage:8,sort:[RELEVANCE,ID]){edges{role node{id image{large medium}name{userPreferred}}}}recommendations(sort:RATING_DESC){nodes{mediaRecommendation{id idMal mediaListEntry{progress private score(format:POINT_100)status}episodes chapters nextAiringEpisode{episode}meanScore isAdult isFavourite format title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}externalLinks{url site}}Page(page:1){pageInfo{total perPage currentPage lastPage hasNextPage}mediaList(isFollowing:true,sort:[STATUS],mediaId:${media.id}){id status score(format: POINT_100) progress progressVolumes user{id name avatar{large medium}}}}}"""
        runBlocking {
            val anilist = async {
                var response = executeQuery<Query.Media>(query, force = true)
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
                                        firstStudio.name ?: "N/A"
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
                        response = executeQuery(query, force = true, useToken = false)
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
    private fun continueMediaQuery(type: String, status: String): String {
        return """ MediaListCollection(userId: ${Anilist.userid}, type: $type, status: $status , sort: UPDATED_TIME ) { lists { entries { progress private score(format:POINT_100) status media { id idMal type isAdult status chapters episodes nextAiringEpisode {episode} meanScore isFavourite format bannerImage coverImage{large} title { english romaji userPreferred } } } } } """
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

    private fun favMediaQuery(anime: Boolean, page: Int, id: Int? = Anilist.userid): String {
        return """User(id:${id}){id favourites{${if (anime) "anime" else "manga"}(page:$page){pageInfo{hasNextPage}edges{favouriteOrder node{id idMal isAdult mediaListEntry{ progress private score(format:POINT_100) status } chapters isFavourite format episodes nextAiringEpisode{episode}meanScore isFavourite format startDate{year month day} title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}}}"""
    }

    private fun recommendationQuery(): String {
        return """ Page(page: 1, perPage:30) { pageInfo { total currentPage hasNextPage } recommendations(sort: RATING_DESC, onList: true) { rating userRating mediaRecommendation { id idMal isAdult mediaListEntry { progress private score(format:POINT_100) status } chapters isFavourite format episodes nextAiringEpisode {episode} popularity meanScore isFavourite format title {english romaji userPreferred } type status(version: 2) bannerImage coverImage { large } } } } """
    }

    private fun recommendationPlannedQuery(type: String): String {
        return """ MediaListCollection(userId: ${Anilist.userid}, type: $type, status: PLANNING${if (type == "ANIME") ", sort: MEDIA_POPULARITY_DESC" else ""} ) { lists { entries { media { id mediaListEntry { progress private score(format:POINT_100) status } idMal type isAdult popularity status(version: 2) chapters episodes nextAiringEpisode {episode} meanScore isFavourite format bannerImage coverImage{large} title { english romaji userPreferred } } } } }"""
    }

    suspend fun initHomePage(): Map<String, ArrayList<*>> {
        val removeList = PrefManager.getCustomVal("removeList", setOf<Int>())
        val removedMedia = ArrayList<Media>()
        val toShow: List<Boolean> =
            PrefManager.getVal(PrefName.HomeLayout) // anime continue, anime fav, anime planned, manga continue, manga fav, manga planned, recommendations
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
        if (toShow.getOrNull(7) == true) query += "Page1:${status(1)}Page2:${status(2)}"
        query += """}""".trimEnd(',')

        val response = executeQuery<Query.HomePageMedia>(query, show = true)
        val returnMap = mutableMapOf<String, ArrayList<*>>()
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
                    if (m.id !in removeList) {
                        m.cameFromContinue = true
                        subMap[m.id] = m
                    } else {
                        removedMedia.add(m)
                    }
                }
            }

            repeating?.lists?.forEach { li ->
                li.entries?.reversed()?.forEach {
                    val m = Media(it)
                    if (m.id !in removeList) {
                        m.cameFromContinue = true
                        subMap[m.id] = m
                    } else {
                        removedMedia.add(m)
                    }
                }
            }
            if (type != "Anime") {
                returnArray.addAll(subMap.values)
                returnMap["current$type"] = returnArray
                return
            }
            @Suppress("UNCHECKED_CAST")
            val list = PrefManager.getNullableCustomVal(
                "continueAnimeList",
                listOf<Int>(),
                List::class.java
            ) as List<Int>
            if (list.isNotEmpty()) {
                list.reversed().forEach {
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
                    if (m.id !in removeList) {
                        m.cameFromContinue = true
                        subMap[m.id] = m
                    } else {
                        removedMedia.add(m)
                    }
                }
            }
            @Suppress("UNCHECKED_CAST")
            val list = PrefManager.getNullableCustomVal(
                "continueAnimeList",
                listOf<Int>(),
                List::class.java
            ) as List<Int>
            if (list.isNotEmpty()) {
                list.reversed().forEach {
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
                    val m = Media(i).apply { isFav = true }
                    if (m.id !in removeList) {
                        returnArray.add(m)
                    } else {
                        removedMedia.add(m)
                    }
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
        if (toShow.getOrNull(7) == true) {
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
                    .filter { if (Anilist.adult) true else it.media?.isAdult == false }
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


                list.addAll(0, anilistActivities)
                returnMap["status"] = ArrayList(list)
            }
            returnMap["hidden"] = removedMedia.distinctBy { it.id } as ArrayList<Media>
        }
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

    suspend fun search(
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
    ): SearchResults? {
        val query = """
query (${"$"}page: Int = 1, ${"$"}id: Int, ${"$"}type: MediaType, ${"$"}isAdult: Boolean = false, ${"$"}search: String, ${"$"}format: [MediaFormat], ${"$"}status: MediaStatus, ${"$"}countryOfOrigin: CountryCode, ${"$"}source: MediaSource, ${"$"}season: MediaSeason, ${"$"}seasonYear: Int, ${"$"}year: String, ${"$"}onList: Boolean, ${"$"}yearLesser: FuzzyDateInt, ${"$"}yearGreater: FuzzyDateInt, ${"$"}episodeLesser: Int, ${"$"}episodeGreater: Int, ${"$"}durationLesser: Int, ${"$"}durationGreater: Int, ${"$"}chapterLesser: Int, ${"$"}chapterGreater: Int, ${"$"}volumeLesser: Int, ${"$"}volumeGreater: Int, ${"$"}licensedBy: [String], ${"$"}isLicensed: Boolean, ${"$"}genres: [String], ${"$"}excludedGenres: [String], ${"$"}tags: [String], ${"$"}excludedTags: [String], ${"$"}minimumTagRank: Int, ${"$"}sort: [MediaSort] = [POPULARITY_DESC, SCORE_DESC, START_DATE_DESC]) {
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

    private val onListAnime =
        (if (PrefManager.getVal(PrefName.IncludeAnimeList)) "" else "onList:false").replace(
            "\"",
            ""
        )
    private val isAdult =
        (if (PrefManager.getVal(PrefName.AdultOnly)) "isAdult:true" else "").replace("\"", "")

    private fun recentAnimeUpdates(page: Int): String {
        return """Page(page:$page,perPage:50){pageInfo{hasNextPage total}airingSchedules(airingAt_greater:0 airingAt_lesser:${System.currentTimeMillis() / 1000 - 10000} sort:TIME_DESC){episode airingAt media{id idMal status chapters episodes nextAiringEpisode{episode} isAdult type meanScore isFavourite format bannerImage countryOfOrigin coverImage{large} title{english romaji userPreferred} mediaListEntry{progress private score(format:POINT_100) status}}}}"""
    }

    private fun trendingMovies(page: Int): String {
        return """Page(page:$page,perPage:50){pageInfo{hasNextPage total}media(sort:POPULARITY_DESC, type: ANIME, format: MOVIE, $onListAnime, $isAdult){id idMal status chapters episodes nextAiringEpisode{episode}isAdult type meanScore isFavourite format bannerImage countryOfOrigin coverImage{large}title{english romaji userPreferred}mediaListEntry{progress private score(format:POINT_100)status}}}"""
    }

    private fun topRatedAnime(page: Int): String {
        return """Page(page:$page,perPage:50){pageInfo{hasNextPage total}media(sort: SCORE_DESC, type: ANIME, $onListAnime, $isAdult){id idMal status chapters episodes nextAiringEpisode{episode}isAdult type meanScore isFavourite format bannerImage countryOfOrigin coverImage{large}title{english romaji userPreferred}mediaListEntry{progress private score(format:POINT_100)status}}}"""
    }

    private fun mostFavAnime(page: Int): String {
        return """Page(page:$page,perPage:50){pageInfo{hasNextPage total}media(sort:FAVOURITES_DESC,type: ANIME, $onListAnime, $isAdult){id idMal status chapters episodes nextAiringEpisode{episode}isAdult type meanScore isFavourite format bannerImage countryOfOrigin coverImage{large}title{english romaji userPreferred}mediaListEntry{progress private score(format:POINT_100)status}}}"""
    }

    suspend fun loadAnimeList(): Map<String, ArrayList<Media>> {
        val list = mutableMapOf<String, ArrayList<Media>>()
        fun query(): String {
            return """{
                recentUpdates:${recentAnimeUpdates(1)}
                recentUpdates2:${recentAnimeUpdates(2)}
                trendingMovies:${trendingMovies(1)}
                trendingMovies2:${trendingMovies(2)}
                topRated:${topRatedAnime(1)}
                topRated2:${topRatedAnime(2)}
                mostFav:${mostFavAnime(1)}
                mostFav2:${mostFavAnime(2)}
            }""".trimIndent()
        }
        executeQuery<Query.AnimeList>(query(), force = true)?.data?.apply {
            val listOnly: Boolean = PrefManager.getVal(PrefName.RecentlyListOnly)
            val adultOnly: Boolean = PrefManager.getVal(PrefName.AdultOnly)
            val idArr = mutableListOf<Int>()
            list["recentUpdates"] = recentUpdates?.airingSchedules?.mapNotNull { i ->
                i.media?.let {
                    if (!idArr.contains(it.id))
                        if (!listOnly && it.countryOfOrigin == "JP" && Anilist.adult && adultOnly && it.isAdult == true) {
                            idArr.add(it.id)
                            Media(it)
                        } else if (!listOnly && !adultOnly && (it.countryOfOrigin == "JP" && it.isAdult == false)) {
                            idArr.add(it.id)
                            Media(it)
                        } else if ((listOnly && it.mediaListEntry != null)) {
                            idArr.add(it.id)
                            Media(it)
                        } else null
                    else null
                }
            } as ArrayList<Media>

            list["trendingMovies"] = trendingMovies?.media?.map { Media(it) } as ArrayList<Media>
            list["topRated"] = topRated?.media?.map { Media(it) } as ArrayList<Media>
            list["mostFav"] = mostFav?.media?.map { Media(it) } as ArrayList<Media>

            list["recentUpdates"]?.addAll(recentUpdates2?.airingSchedules?.mapNotNull { i ->
                i.media?.let {
                    if (!idArr.contains(it.id))
                        if (!listOnly && it.countryOfOrigin == "JP" && Anilist.adult && adultOnly && it.isAdult == true) {
                            idArr.add(it.id)
                            Media(it)
                        } else if (!listOnly && !adultOnly && (it.countryOfOrigin == "JP" && it.isAdult == false)) {
                            idArr.add(it.id)
                            Media(it)
                        } else if ((listOnly && it.mediaListEntry != null)) {
                            idArr.add(it.id)
                            Media(it)
                        } else null
                    else null
                }
            } as ArrayList<Media>)
            list["trendingMovies"]?.addAll(trendingMovies2?.media?.map { Media(it) } as ArrayList<Media>)
            list["topRated"]?.addAll(topRated2?.media?.map { Media(it) } as ArrayList<Media>)
            list["mostFav"]?.addAll(mostFav2?.media?.map { Media(it) } as ArrayList<Media>)
        }
        return list
    }

    private val onListManga =
        (if (PrefManager.getVal(PrefName.IncludeMangaList)) "" else "onList:false").replace(
            "\"",
            ""
        )

    private fun trendingManga(page: Int): String {
        return """Page(page:$page,perPage:50){pageInfo{hasNextPage total}media(sort:POPULARITY_DESC, type: MANGA,countryOfOrigin:JP, $onListManga, $isAdult){id idMal status chapters episodes nextAiringEpisode{episode}isAdult type meanScore isFavourite format bannerImage countryOfOrigin coverImage{large}title{english romaji userPreferred}mediaListEntry{progress private score(format:POINT_100)status}}}"""
    }

    private fun trendingManhwa(page: Int): String {
        return """Page(page:$page,perPage:50){pageInfo{hasNextPage total}media(sort:POPULARITY_DESC, type: MANGA, countryOfOrigin:KR, $onListManga, $isAdult){id idMal status chapters episodes nextAiringEpisode{episode}isAdult type meanScore isFavourite format bannerImage countryOfOrigin coverImage{large}title{english romaji userPreferred}mediaListEntry{progress private score(format:POINT_100)status}}}"""
    }

    private fun trendingNovel(page: Int): String {
        return """Page(page:$page,perPage:50){pageInfo{hasNextPage total}media(sort:POPULARITY_DESC, type: MANGA, format: NOVEL, countryOfOrigin:JP, $onListManga, $isAdult){id idMal status chapters episodes nextAiringEpisode{episode}isAdult type meanScore isFavourite format bannerImage countryOfOrigin coverImage{large}title{english romaji userPreferred}mediaListEntry{progress private score(format:POINT_100)status}}}"""
    }

    private fun topRatedManga(page: Int): String {
        return """Page(page:$page,perPage:50){pageInfo{hasNextPage total}media(sort: SCORE_DESC, type: MANGA, $onListManga, $isAdult){id idMal status chapters episodes nextAiringEpisode{episode}isAdult type meanScore isFavourite format bannerImage countryOfOrigin coverImage{large}title{english romaji userPreferred}mediaListEntry{progress private score(format:POINT_100)status}}}"""
    }

    private fun mostFavManga(page: Int): String {
        return """Page(page:$page,perPage:50){pageInfo{hasNextPage total}media(sort:FAVOURITES_DESC,type: MANGA, $onListManga, $isAdult){id idMal status chapters episodes nextAiringEpisode{episode}isAdult type meanScore isFavourite format bannerImage countryOfOrigin coverImage{large}title{english romaji userPreferred}mediaListEntry{progress private score(format:POINT_100)status}}}"""
    }

    suspend fun loadMangaList(): Map<String, ArrayList<Media>> {
        val list = mutableMapOf<String, ArrayList<Media>>()
        fun query(): String {
            return """{
                trendingManga:${trendingManga(1)}
                trendingManga2:${trendingManga(2)}
                trendingManhwa:${trendingManhwa(1)}
                trendingManhwa2:${trendingManhwa(2)}
                trendingNovel:${trendingNovel(1)}
                trendingNovel2:${trendingNovel(2)}
                topRated:${topRatedManga(1)}
                topRated2:${topRatedManga(2)}
                mostFav:${mostFavManga(1)}
                mostFav2:${mostFavManga(2)}
            }""".trimIndent()
        }

        executeQuery<Query.MangaList>(query(), force = true)?.data?.apply {
            list["trendingManga"] = trendingManga?.media?.map { Media(it) } as ArrayList<Media>
            list["trendingManhwa"] = trendingManhwa?.media?.map { Media(it) } as ArrayList<Media>
            list["trendingNovel"] = trendingNovel?.media?.map { Media(it) } as ArrayList<Media>
            list["topRated"] = topRated?.media?.map { Media(it) } as ArrayList<Media>
            list["mostFav"] = mostFav?.media?.map { Media(it) } as ArrayList<Media>
            list["trendingManga"]?.addAll(trendingManga2?.media?.map { Media(it) } as ArrayList<Media>)
            list["trendingManhwa"]?.addAll(trendingManhwa2?.media?.map { Media(it) } as ArrayList<Media>)
            list["trendingNovel"]?.addAll(trendingNovel2?.media?.map { Media(it) } as ArrayList<Media>)
            list["topRated"]?.addAll(topRated2?.media?.map { Media(it) } as ArrayList<Media>)
            list["mostFav"]?.addAll(mostFav2?.media?.map { Media(it) } as ArrayList<Media>)

        }

        return list
    }

    suspend fun recentlyUpdated(
        greater: Long = 0,
        lesser: Long = System.currentTimeMillis() / 1000 - 10000
    ): MutableList<Media> {
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
    characters(page: $page,sort:FAVOURITES_DESC) {
      pageInfo{
        hasNextPage
      }
      nodes{
        id
        name {
          first
          middle
          last
          full
          native
          userPreferred
        }
        image {
          large
          medium
        }
      }
    }
  }
}""".replace("\n", " ").replace("""  """, "")

        var hasNextPage = true
        val yearMedia = mutableMapOf<String, ArrayList<Media>>()
        var page = 0
        val characters = arrayListOf<Character>()
        while (hasNextPage) {
            page++
            val query = executeQuery<Query.Author>(
                query(page), force = true
            )?.data?.author
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

    suspend fun getReviews(mediaId: Int, page: Int = 1, sort: String = "CREATED_AT_DESC"): Query.ReviewsResponse? {
        return executeQuery<Query.ReviewsResponse>(
            """{Page(page:$page,perPage:10){pageInfo{currentPage,hasNextPage,total}reviews(mediaId:$mediaId,sort:$sort){id,mediaId,mediaType,summary,body(asHtml:true)rating,ratingAmount,userRating,score,private,siteUrl,createdAt,updatedAt,user{id,name,bannerImage avatar{medium,large}}}}}""",
            force = true
        )
    }

    suspend fun toggleFollow(id: Int): Query.ToggleFollow? {
        return executeQuery<Query.ToggleFollow>(
            """mutation{ToggleFollow(userId:$id){id, isFollowing, isFollower}}"""
        )
    }

    suspend fun toggleLike(id: Int, type: String): ToggleLike? {
        return executeQuery<ToggleLike>(
            """mutation Like{ToggleLikeV2(id:$id,type:$type){__typename}}"""
        )
    }

    suspend fun getUserProfile(id: Int): Query.UserProfileResponse? {
        return executeQuery<Query.UserProfileResponse>(
            """{followerPage:Page{followers(userId:$id){id}pageInfo{total}}followingPage:Page{following(userId:$id){id}pageInfo{total}}user:User(id:$id){id name about(asHtml:true)avatar{medium large}bannerImage isFollowing isFollower isBlocked favourites{anime{nodes{id coverImage{extraLarge large medium color}}}manga{nodes{id coverImage{extraLarge large medium color}}}characters{nodes{id name{first middle last full native alternative userPreferred}image{large medium}isFavourite}}staff{nodes{id name{first middle last full native alternative userPreferred}image{large medium}isFavourite}}studios{nodes{id name isFavourite}}}statistics{anime{count meanScore standardDeviation minutesWatched episodesWatched chaptersRead volumesRead}manga{count meanScore standardDeviation minutesWatched episodesWatched chaptersRead volumesRead}}siteUrl}}""",
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
        return """User(id:${id}){id favourites{${if (anime) "anime" else "manga"}(page:1){pageInfo{hasNextPage}edges{favouriteOrder node{id idMal isAdult mediaListEntry{ progress private score(format:POINT_100) status } chapters isFavourite format episodes nextAiringEpisode{episode}meanScore isFavourite format startDate{year month day} title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}}}"""
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
            }""".trimIndent(), force = true
        )
    }


    suspend fun getNotifications(
        id: Int,
        page: Int = 1,
        resetNotification: Boolean = true
    ): NotificationResponse? {
        val reset = if (resetNotification) "true" else "false"
        val res = executeQuery<NotificationResponse>(
            """{User(id:$id){unreadNotificationCount}Page(page:$page,perPage:$ITEMS_PER_PAGE){pageInfo{currentPage,hasNextPage}notifications(resetNotificationCount:$reset){__typename...on AiringNotification{id,type,animeId,episode,contexts,createdAt,media{id,title{romaji,english,native,userPreferred}bannerImage,coverImage{medium,large}},}...on FollowingNotification{id,userId,type,context,createdAt,user{id,name,bannerImage,avatar{medium,large,}}}...on ActivityMessageNotification{id,userId,type,activityId,context,createdAt,message{id}user{id,name,bannerImage,avatar{medium,large,}}}...on ActivityMentionNotification{id,userId,type,activityId,context,createdAt,activity{__typename}user{id,name,bannerImage,avatar{medium,large,}}}...on ActivityReplyNotification{id,userId,type,activityId,context,createdAt,activity{__typename}user{id,name,bannerImage,avatar{medium,large,}}}...on ActivityReplySubscribedNotification{id,userId,type,activityId,context,createdAt,activity{__typename}user{id,name,bannerImage,avatar{medium,large,}}}...on ActivityLikeNotification{id,userId,type,activityId,context,createdAt,activity{__typename}user{id,name,bannerImage,avatar{medium,large,}}}...on ActivityReplyLikeNotification{id,userId,type,activityId,context,createdAt,activity{__typename}user{id,name,bannerImage,avatar{medium,large,}}}...on ThreadCommentMentionNotification{id,userId,type,commentId,context,createdAt,thread{id}comment{id}user{id,name,bannerImage,avatar{medium,large,}}}...on ThreadCommentReplyNotification{id,userId,type,commentId,context,createdAt,thread{id}comment{id}user{id,name,bannerImage,avatar{medium,large,}}}...on ThreadCommentSubscribedNotification{id,userId,type,commentId,context,createdAt,thread{id}comment{id}user{id,name,bannerImage,avatar{medium,large,}}}...on ThreadCommentLikeNotification{id,userId,type,commentId,context,createdAt,thread{id}comment{id}user{id,name,bannerImage,avatar{medium,large,}}}...on ThreadLikeNotification{id,userId,type,threadId,context,createdAt,thread{id}comment{id}user{id,name,bannerImage,avatar{medium,large,}}}...on RelatedMediaAdditionNotification{id,type,context,createdAt,media{id,title{romaji,english,native,userPreferred}bannerImage,coverImage{medium,large}}}...on MediaDataChangeNotification{id,type,mediaId,context,reason,createdAt,media{id,title{romaji,english,native,userPreferred}bannerImage,coverImage{medium,large}}}...on MediaMergeNotification{id,type,mediaId,deletedMediaTitles,context,reason,createdAt,media{id,title{romaji,english,native,userPreferred}bannerImage,coverImage{medium,large}}}...on MediaDeletionNotification{id,type,deletedMediaTitle,context,reason,createdAt,}}}}""",
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
        return executeQuery<FeedResponse>(
            """{Page(page:$page,perPage:$ITEMS_PER_PAGE){activities(${filter}sort:ID_DESC){__typename ... on TextActivity{id userId type replyCount text(asHtml:true)siteUrl isLocked isSubscribed likeCount isLiked isPinned createdAt user{id name bannerImage avatar{medium large}}replies{id userId activityId text(asHtml:true)likeCount isLiked createdAt user{id name bannerImage avatar{medium large}}likes{id name bannerImage avatar{medium large}}}likes{id name bannerImage avatar{medium large}}}... on ListActivity{id userId type replyCount status progress siteUrl isLocked isSubscribed likeCount isLiked isPinned createdAt user{id name bannerImage avatar{medium large}}media{id title{english romaji native userPreferred}bannerImage coverImage{medium large}}replies{id userId activityId text(asHtml:true)likeCount isLiked createdAt user{id name bannerImage avatar{medium large}}likes{id name bannerImage avatar{medium large}}}likes{id name bannerImage avatar{medium large}}}... on MessageActivity{id recipientId messengerId type replyCount likeCount message(asHtml:true)isLocked isSubscribed isLiked isPrivate siteUrl createdAt recipient{id name bannerImage avatar{medium large}}messenger{id name bannerImage avatar{medium large}}replies{id userId activityId text(asHtml:true)likeCount isLiked createdAt user{id name bannerImage avatar{medium large}}likes{id name bannerImage avatar{medium large}}}likes{id name bannerImage avatar{medium large}}}}}}""",
            force = true
        )
    }

    suspend fun getReplies(
        activityId: Int,
        page: Int = 1
    ) : ReplyResponse? {
        val query = """{Page(page:$page,perPage:50){activityReplies(activityId:$activityId){id userId activityId text(asHtml:true)likeCount isLiked createdAt user{id name bannerImage avatar{medium large}}likes{id name bannerImage avatar{medium large}}}}}"""
        return executeQuery(query, force = true)
    }

    private fun status(page: Int = 1): String {
        return """Page(page:$page,perPage:50){activities(isFollowing: true,sort:ID_DESC){__typename ... on TextActivity{id userId type replyCount text(asHtml:true)siteUrl isLocked isSubscribed replyCount likeCount isLiked createdAt user{id name bannerImage avatar{medium large}}likes{id name bannerImage avatar{medium large}}}... on ListActivity{id userId type replyCount status progress siteUrl isLocked isSubscribed replyCount likeCount isLiked isPinned createdAt user{id name bannerImage avatar{medium large}}media{id isAdult title{english romaji native userPreferred}bannerImage coverImage{extraLarge medium large}}likes{id name bannerImage avatar{medium large}}}... on MessageActivity{id type createdAt}}}"""
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