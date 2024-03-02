package ani.dantotsu.connections.anilist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class Query {
    @Serializable
    data class Viewer(
        @SerialName("data")
        val data: Data?
    ) {
        @Serializable
        data class Data(
            @SerialName("Viewer")
            val user: ani.dantotsu.connections.anilist.api.User?
        )
    }

    @Serializable
    data class Media(
        @SerialName("data")
        val data: Data?
    ) {
        @Serializable
        data class Data(
            @SerialName("Media")
            val media: ani.dantotsu.connections.anilist.api.Media?
        )
    }

    @Serializable
    data class Page(
        @SerialName("data")
        val data: Data?
    ) {
        @Serializable
        data class Data(
            @SerialName("Page")
            val page: ani.dantotsu.connections.anilist.api.Page?
        )
    }
//    data class AiringSchedule(
//        val data : Data?
//    ){
//        data class Data(
//            val AiringSchedule: ani.dantotsu.connections.anilist.api.AiringSchedule?
//        )
//    }

    @Serializable
    data class Character(
        @SerialName("data")
        val data: Data?
    ) {

        @Serializable
        data class Data(
            @SerialName("Character")
            val character: ani.dantotsu.connections.anilist.api.Character?
        )
    }

    @Serializable
    data class Studio(
        @SerialName("data")
        val data: Data?
    ) {
        @Serializable
        data class Data(
            @SerialName("Studio")
            val studio: ani.dantotsu.connections.anilist.api.Studio?
        )
    }


    @Serializable
    data class Author(
        @SerialName("data")
        val data: Data?
    ) {
        @Serializable
        data class Data(
            @SerialName("Staff")
            val author: Staff?
        )
    }

    //    data class MediaList(
//        val data: Data?
//    ){
//        data class Data(
//            val MediaList: ani.dantotsu.connections.anilist.api.MediaList?
//        )
//    }

    @Serializable
    data class MediaListCollection(
        @SerialName("data")
        val data: Data?
    ) {
        @Serializable
        data class Data(
            @SerialName("MediaListCollection")
            val mediaListCollection: ani.dantotsu.connections.anilist.api.MediaListCollection?
        )
    }

    @Serializable
    data class CombinedMediaListResponse(
        @SerialName("data")
        val data: Data?
    ) {
        @Serializable
        data class Data(
            @SerialName("current") val current: ani.dantotsu.connections.anilist.api.MediaListCollection?,
            @SerialName("planned") val planned: ani.dantotsu.connections.anilist.api.MediaListCollection?,
            @SerialName("repeating") val repeating: ani.dantotsu.connections.anilist.api.MediaListCollection?,
        )
    }

    @Serializable
    data class HomePageMedia(
        @SerialName("data")
        val data: Data?
    ) {
        @Serializable
        data class Data(
            @SerialName("currentAnime") val currentAnime: ani.dantotsu.connections.anilist.api.MediaListCollection?,
            @SerialName("repeatingAnime") val repeatingAnime: ani.dantotsu.connections.anilist.api.MediaListCollection?,
            @SerialName("favoriteAnime") val favoriteAnime: ani.dantotsu.connections.anilist.api.User?,
            @SerialName("plannedAnime") val plannedAnime: ani.dantotsu.connections.anilist.api.MediaListCollection?,
            @SerialName("currentManga") val currentManga: ani.dantotsu.connections.anilist.api.MediaListCollection?,
            @SerialName("repeatingManga") val repeatingManga: ani.dantotsu.connections.anilist.api.MediaListCollection?,
            @SerialName("favoriteManga") val favoriteManga: ani.dantotsu.connections.anilist.api.User?,
            @SerialName("plannedManga") val plannedManga: ani.dantotsu.connections.anilist.api.MediaListCollection?,
            @SerialName("recommendationQuery") val recommendationQuery: ani.dantotsu.connections.anilist.api.Page?,
            @SerialName("recommendationPlannedQueryAnime") val recommendationPlannedQueryAnime: ani.dantotsu.connections.anilist.api.MediaListCollection?,
            @SerialName("recommendationPlannedQueryManga") val recommendationPlannedQueryManga: ani.dantotsu.connections.anilist.api.MediaListCollection?,
        )
    }

    @Serializable
    data class GenreCollection(
        @SerialName("data")
        val data: Data
    ) {
        @Serializable
        data class Data(
            @SerialName("GenreCollection")
            val genreCollection: List<String>?
        )
    }

    @Serializable
    data class MediaTagCollection(
        @SerialName("data")
        val data: Data
    ) {
        @Serializable
        data class Data(
            @SerialName("MediaTagCollection")
            val mediaTagCollection: List<MediaTag>?
        )
    }

    @Serializable
    data class User(
        @SerialName("data")
        val data: Data
    ) {
        @Serializable
        data class Data(
            @SerialName("User")
            val user: ani.dantotsu.connections.anilist.api.User?
        )
    }

    @Serializable
    data class UserProfileResponse(
        @SerialName("data")
        val data: Data
    ) {
        @Serializable
        data class Data(
            @SerialName("user")
            val user: UserProfile?
        )
    }

    @Serializable
    data class UserProfile(
        @SerialName("id")
        val id: Int,
        @SerialName("name")
        val name: String,
        @SerialName("about")
        val about: String?,
        @SerialName("avatar")
        val avatar: UserAvatar?,
        @SerialName("bannerImage")
        val bannerImage: String?,
        @SerialName("isFollowing")
        val isFollowing: Boolean,
        @SerialName("isFollower")
        val isFollower: Boolean,
        @SerialName("isBlocked")
        val isBlocked: Boolean,
        @SerialName("favorites")
        val favorites: UserFavorites?,
        @SerialName("statistics")
        val statistics: NNUserStatisticTypes,
        @SerialName("siteUrl")
        val siteUrl: String,
    )

    @Serializable
    data class NNUserStatisticTypes(
        @SerialName("anime") var anime: NNUserStatistics,
        @SerialName("manga") var manga: NNUserStatistics
    )

    @Serializable
    data class NNUserStatistics(
        //
        @SerialName("count") var count: Int,
        @SerialName("meanScore") var meanScore: Float,
        @SerialName("standardDeviation") var standardDeviation: Float,
        @SerialName("minutesWatched") var minutesWatched: Int,
        @SerialName("episodesWatched") var episodesWatched: Int,
        @SerialName("chaptersRead") var chaptersRead: Int,
        @SerialName("volumesRead") var volumesRead: Int,
    )

        @Serializable
    data class UserFavorites(
        @SerialName("anime")
        val anime: UserMediaFavoritesCollection,
        @SerialName("manga")
        val manga: UserMediaFavoritesCollection,
        @SerialName("characters")
        val characters: UserCharacterFavoritesCollection,
        @SerialName("staff")
        val staff: UserStaffFavoritesCollection,
        @SerialName("studios")
        val studios: UserStudioFavoritesCollection,
    )

    @Serializable
    data class UserMediaFavoritesCollection(
        @SerialName("nodes")
        val nodes: List<UserMediaImageFavorite>,
    )

    @Serializable
    data class UserMediaImageFavorite(
        @SerialName("id")
        val id: Int,
        @SerialName("coverImage")
        val coverImage: MediaCoverImage
    )

    @Serializable
    data class UserCharacterFavoritesCollection(
        @SerialName("nodes")
        val nodes: List<UserCharacterImageFavorite>,
    )

    @Serializable
    data class UserCharacterImageFavorite(
        @SerialName("id")
        val id: Int,
        @SerialName("image")
        val image: CharacterImage
    )

    @Serializable
    data class UserStaffFavoritesCollection(
        @SerialName("nodes")
        val nodes: List<UserCharacterImageFavorite>, //downstream it's the same as character
    )

    @Serializable
    data class UserStudioFavoritesCollection(
        @SerialName("nodes")
        val nodes: List<UserStudioFavorite>,
    )

    @Serializable
    data class UserStudioFavorite(
        @SerialName("id")
        val id: Int,
        @SerialName("name")
        val name: String,
    )

    //----------------------------------------
    // Statistics

    @Serializable
    data class StatisticsResponse(
        @SerialName("data")
        val data: Data
    ) {
        @Serializable
        data class Data(
            @SerialName("User")
            val user: StatisticsUser?
        )
    }

    @Serializable
    data class StatisticsUser(
        @SerialName("id")
        val id: Int,
        @SerialName("name")
        val name: String,
        @SerialName("statistics")
        val statistics: StatisticsTypes
    )

    @Serializable
    data class StatisticsTypes(
        @SerialName("anime")
        val anime: Statistics,
        @SerialName("manga")
        val manga: Statistics
    )

    @Serializable
    data class Statistics(
        @SerialName("count")
        val count: Int,
        @SerialName("meanScore")
        val meanScore: Float,
        @SerialName("standardDeviation")
        val standardDeviation: Float,
        @SerialName("minutesWatched")
        val minutesWatched: Int,
        @SerialName("episodesWatched")
        val episodesWatched: Int,
        @SerialName("chaptersRead")
        val chaptersRead: Int,
        @SerialName("volumesRead")
        val volumesRead: Int,
        @SerialName("formats")
        val formats: List<StatisticsFormat>,
        @SerialName("statuses")
        val statuses: List<StatisticsStatus>,
        @SerialName("scores")
        val scores: List<StatisticsScore>,
        @SerialName("lengths")
        val lengths: List<StatisticsLength>,
        @SerialName("releaseYears")
        val releaseYears: List<StatisticsReleaseYear>,
        @SerialName("startYears")
        val startYears: List<StatisticsStartYear>,
        @SerialName("genres")
        val genres: List<StatisticsGenre>,
        @SerialName("tags")
        val tags: List<StatisticsTag>,
        @SerialName("countries")
        val countries: List<StatisticsCountry>,
        @SerialName("voiceActors")
        val voiceActors: List<StatisticsVoiceActor>,
        @SerialName("staff")
        val staff: List<StatisticsStaff>,
        @SerialName("studios")
        val studios: List<StatisticsStudio>
    )

    @Serializable
    data class StatisticsFormat(
        @SerialName("count")
        val count: Int,
        @SerialName("meanScore")
        val meanScore: Float,
        @SerialName("minutesWatched")
        val minutesWatched: Int,
        @SerialName("chaptersRead")
        val chaptersRead: Int,
        @SerialName("mediaIds")
        val mediaIds: List<Int>,
        @SerialName("format")
        val format: String
    )

    @Serializable
    data class StatisticsStatus(
        @SerialName("count")
        val count: Int,
        @SerialName("meanScore")
        val meanScore: Float,
        @SerialName("minutesWatched")
        val minutesWatched: Int,
        @SerialName("chaptersRead")
        val chaptersRead: Int,
        @SerialName("mediaIds")
        val mediaIds: List<Int>,
        @SerialName("status")
        val status: String
    )

    @Serializable
    data class StatisticsScore(
        @SerialName("count")
        val count: Int,
        @SerialName("meanScore")
        val meanScore: Float,
        @SerialName("minutesWatched")
        val minutesWatched: Int,
        @SerialName("chaptersRead")
        val chaptersRead: Int,
        @SerialName("mediaIds")
        val mediaIds: List<Int>,
        @SerialName("score")
        val score: Int
    )

    @Serializable
    data class StatisticsLength(
        @SerialName("count")
        val count: Int,
        @SerialName("meanScore")
        val meanScore: Float,
        @SerialName("minutesWatched")
        val minutesWatched: Int,
        @SerialName("chaptersRead")
        val chaptersRead: Int,
        @SerialName("mediaIds")
        val mediaIds: List<Int>,
        @SerialName("length")
        val length: String? //can be null for manga
    )

    @Serializable
    data class StatisticsReleaseYear(
        @SerialName("count")
        val count: Int,
        @SerialName("meanScore")
        val meanScore: Float,
        @SerialName("minutesWatched")
        val minutesWatched: Int,
        @SerialName("chaptersRead")
        val chaptersRead: Int,
        @SerialName("mediaIds")
        val mediaIds: List<Int>,
        @SerialName("releaseYear")
        val releaseYear: Int
    )

    @Serializable
    data class StatisticsStartYear(
        @SerialName("count")
        val count: Int,
        @SerialName("meanScore")
        val meanScore: Float,
        @SerialName("minutesWatched")
        val minutesWatched: Int,
        @SerialName("chaptersRead")
        val chaptersRead: Int,
        @SerialName("mediaIds")
        val mediaIds: List<Int>,
        @SerialName("startYear")
        val startYear: Int
    )

    @Serializable
    data class StatisticsGenre(
        @SerialName("count")
        val count: Int,
        @SerialName("meanScore")
        val meanScore: Float,
        @SerialName("minutesWatched")
        val minutesWatched: Int,
        @SerialName("chaptersRead")
        val chaptersRead: Int,
        @SerialName("mediaIds")
        val mediaIds: List<Int>,
        @SerialName("genre")
        val genre: String
    )

    @Serializable
    data class StatisticsTag(
        @SerialName("count")
        val count: Int,
        @SerialName("meanScore")
        val meanScore: Float,
        @SerialName("minutesWatched")
        val minutesWatched: Int,
        @SerialName("chaptersRead")
        val chaptersRead: Int,
        @SerialName("mediaIds")
        val mediaIds: List<Int>,
        @SerialName("tag")
        val tag: Tag
    )

    @Serializable
    data class Tag(
        @SerialName("id")
        val id: Int,
        @SerialName("name")
        val name: String
    )

    @Serializable
    data class StatisticsCountry(
        @SerialName("count")
        val count: Int,
        @SerialName("meanScore")
        val meanScore: Float,
        @SerialName("minutesWatched")
        val minutesWatched: Int,
        @SerialName("chaptersRead")
        val chaptersRead: Int,
        @SerialName("mediaIds")
        val mediaIds: List<Int>,
        @SerialName("country")
        val country: String
    )

    @Serializable
    data class StatisticsVoiceActor(
        @SerialName("count")
        val count: Int,
        @SerialName("meanScore")
        val meanScore: Float,
        @SerialName("minutesWatched")
        val minutesWatched: Int,
        @SerialName("chaptersRead")
        val chaptersRead: Int,
        @SerialName("mediaIds")
        val mediaIds: List<Int>,
        @SerialName("voiceActor")
        val voiceActor: VoiceActor,
        @SerialName("characterIds")
        val characterIds: List<Int>
    )

    @Serializable
    data class VoiceActor(
        @SerialName("id")
        val id: Int,
        @SerialName("name")
        val name: StaffName
    )

    @Serializable
    data class StaffName(
        @SerialName("first")
        val first: String?,
        @SerialName("middle")
        val middle: String?,
        @SerialName("last")
        val last: String?,
        @SerialName("full")
        val full: String?,
        @SerialName("native")
        val native: String?,
        @SerialName("alternative")
        val alternative: List<String>?,
        @SerialName("userPreferred")
        val userPreferred: String?
    )

    @Serializable
    data class StatisticsStaff(
        @SerialName("count")
        val count: Int,
        @SerialName("meanScore")
        val meanScore: Float,
        @SerialName("minutesWatched")
        val minutesWatched: Int,
        @SerialName("chaptersRead")
        val chaptersRead: Int,
        @SerialName("mediaIds")
        val mediaIds: List<Int>,
        @SerialName("staff")
        val staff: VoiceActor
    )

    @Serializable
    data class StatisticsStudio(
        @SerialName("count")
        val count: Int,
        @SerialName("meanScore")
        val meanScore: Float,
        @SerialName("minutesWatched")
        val minutesWatched: Int,
        @SerialName("chaptersRead")
        val chaptersRead: Int,
        @SerialName("mediaIds")
        val mediaIds: List<Int>,
        @SerialName("studio")
        val studio: StatStudio
    )

    @Serializable
    data class StatStudio(
        @SerialName("id")
        val id: Int,
        @SerialName("name")
        val name: String,
        @SerialName("isAnimationStudio")
        val isAnimationStudio: Boolean
    )

}

//data class WhaData(
//    val Studio: Studio?,
//
//    // Follow query
//    val Following: User?,
//
//    // Follow query
//    val Follower: User?,
//
//    // Thread query
//    val Thread: Thread?,
//
//    // Recommendation query
//    val Recommendation: Recommendation?,
//
//    // Like query
//    val Like: User?,

//    // Review query
//    val Review: Review?,
//
//    // Activity query
//    val Activity: ActivityUnion?,
//
//    // Activity reply query
//    val ActivityReply: ActivityReply?,

//    // Comment query
//    val ThreadComment: List<ThreadComment>?,

//    // Notification query
//    val Notification: NotificationUnion?,

//    // Media Trend query
//    val MediaTrend: MediaTrend?,

//    // Provide AniList markdown to be converted to html (Requires auth)
//    val Markdown: ParsedMarkdown?,

//    // SiteStatistics: SiteStatistics
//    val AniChartUser: AniChartUser?,
//)
