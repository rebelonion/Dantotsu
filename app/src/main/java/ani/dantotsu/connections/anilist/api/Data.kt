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
