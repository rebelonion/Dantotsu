package ani.dantotsu.connections.anilist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class User(
    // The id of the user
    @SerialName("id") var id: Int,

    // The name of the user
    @SerialName("name") var name: String?,

    // The bio written by user (Markdown)
    //    @SerialName("about") var about: String?,

    // The user's avatar images
    @SerialName("avatar") var avatar: UserAvatar?,

    // The user's banner images
    @SerialName("bannerImage") var bannerImage: String?,

    // If the authenticated user if following this user
    //    @SerialName("isFollowing") var isFollowing: Boolean?,

    // If this user if following the authenticated user
    //    @SerialName("isFollower") var isFollower: Boolean?,

    // If the user is blocked by the authenticated user
    //    @SerialName("isBlocked") var isBlocked: Boolean?,

    // FIXME: No documentation is provided for "Json"
    // @SerialName("bans") var bans: Json?,

    // The user's general options
    @SerialName("options") var options: UserOptions?,

    // The user's media list options
    @SerialName("mediaListOptions") var mediaListOptions: MediaListOptions?,

    // The users favourites
    @SerialName("favourites") var favourites: Favourites?,

    // The users anime & manga list statistics
    @SerialName("statistics") var statistics: UserStatisticTypes?,

    // The number of unread notifications the user has
    @SerialName("unreadNotificationCount") var unreadNotificationCount: Int?,

    // The url for the user page on the AniList website
    //    @SerialName("siteUrl") var siteUrl: String?,

    // The donation tier of the user
    //    @SerialName("donatorTier") var donatorTier: Int?,

    // Custom donation badge text
    //    @SerialName("donatorBadge") var donatorBadge: String?,

    // The user's moderator roles if they are a site moderator
    // @SerialName("moderatorRoles") var moderatorRoles: List<ModRole>?,

    // When the user's account was created. (Does not exist for accounts created before 2020)
    //    @SerialName("createdAt") var createdAt: Int?,

    // When the user's data was last updated
    //    @SerialName("updatedAt") var updatedAt: Int?,

    // The user's previously used names.
    // @SerialName("previousNames") var previousNames: List<UserPreviousName>?,

) : java.io.Serializable

@Serializable
data class UserOptions(
    // The language the user wants to see media titles in
    @SerialName("titleLanguage") var titleLanguage: UserTitleLanguage?,

    // Whether the user has enabled viewing of 18+ content
    @SerialName("displayAdultContent") var displayAdultContent: Boolean?,

    // Whether the user receives notifications when a show they are watching aires
    @SerialName("airingNotifications") var airingNotifications: Boolean?,
    //
    // Profile highlight color (blue, purple, pink, orange, red, green, gray)
    @SerialName("profileColor") var profileColor: String?,
    //
    //    // Notification options
    //    // @SerialName("notificationOptions") var notificationOptions: List<NotificationOption>?,
    //
    // The user's timezone offset (Auth user only)
    @SerialName("timezone") var timezone: String?,
    //
    // Minutes between activity for them to be merged together. 0 is Never, Above 2 weeks (20160 mins) is Always.
    @SerialName("activityMergeTime") var activityMergeTime: Int?,
    //
    // The language the user wants to see staff and character names in
    @SerialName("staffNameLanguage") var staffNameLanguage: UserStaffNameLanguage?,
    //
    // Whether the user only allow messages from users they follow
    @SerialName("restrictMessagesToFollowing") var restrictMessagesToFollowing: Boolean?,

    // The list activity types the user has disabled from being created from list updates
    // @SerialName("disabledListActivity") var disabledListActivity: List<ListActivityOption>?,
)

@Serializable
data class UserAvatar(
    // The avatar of user at its largest size
    @SerialName("large") var large: String?,

    // The avatar of user at medium size
    @SerialName("medium") var medium: String?,
) : java.io.Serializable

@Serializable
data class UserStatisticTypes(
    @SerialName("anime") var anime: UserStatistics?,
    @SerialName("manga") var manga: UserStatistics?
)

@Serializable
enum class UserTitleLanguage {
    @SerialName("ENGLISH")
    ENGLISH,

    @SerialName("ROMAJI")
    ROMAJI,

    @SerialName("NATIVE")
    NATIVE
}

@Serializable
enum class UserStaffNameLanguage {
    @SerialName("ROMAJI_WESTERN")
    ROMAJI_WESTERN,

    @SerialName("ROMAJI")
    ROMAJI,

    @SerialName("NATIVE")
    NATIVE
}

@Serializable
enum class ScoreFormat {
    @SerialName("POINT_100")
    POINT_100,

    @SerialName("POINT_10_DECIMAL")
    POINT_10_DECIMAL,

    @SerialName("POINT_10")
    POINT_10,

    @SerialName("POINT_5")
    POINT_5,

    @SerialName("POINT_3")
    POINT_3,
}

@Serializable
data class UserStatistics(
    //
    @SerialName("count") var count: Int?,
    @SerialName("meanScore") var meanScore: Float?,
    @SerialName("standardDeviation") var standardDeviation: Float?,
    @SerialName("minutesWatched") var minutesWatched: Int?,
    @SerialName("episodesWatched") var episodesWatched: Int?,
    @SerialName("chaptersRead") var chaptersRead: Int?,
    @SerialName("volumesRead") var volumesRead: Int?,
    //    @SerialName("formats") var formats: List<UserFormatStatistic>?,
    //    @SerialName("statuses") var statuses: List<UserStatusStatistic>?,
    //    @SerialName("scores") var scores: List<UserScoreStatistic>?,
    //    @SerialName("lengths") var lengths: List<UserLengthStatistic>?,
    //    @SerialName("releaseYears") var releaseYears: List<UserReleaseYearStatistic>?,
    //    @SerialName("startYears") var startYears: List<UserStartYearStatistic>?,
    //    @SerialName("genres") var genres: List<UserGenreStatistic>?,
    //    @SerialName("tags") var tags: List<UserTagStatistic>?,
    //    @SerialName("countries") var countries: List<UserCountryStatistic>?,
    //    @SerialName("voiceActors") var voiceActors: List<UserVoiceActorStatistic>?,
    //    @SerialName("staff") var staff: List<UserStaffStatistic>?,
    //    @SerialName("studios") var studios: List<UserStudioStatistic>?,
)

@Serializable
data class Favourites(
    // Favourite anime
    @SerialName("anime") var anime: MediaConnection?,

    // Favourite manga
    @SerialName("manga") var manga: MediaConnection?,

    // Favourite characters
    @SerialName("characters") var characters: CharacterConnection?,

    // Favourite staff
    @SerialName("staff") var staff: StaffConnection?,

    // Favourite studios
    @SerialName("studios") var studios: StudioConnection?,
)

@Serializable
data class MediaListOptions(
    // The score format the user is using for media lists
    @SerialName("scoreFormat") var scoreFormat: ScoreFormat?,

    // The default order list rows should be displayed in
    @SerialName("rowOrder") var rowOrder: String?,

    // The user's anime list options
    @SerialName("animeList") var animeList: MediaListTypeOptions?,

    // The user's manga list options
    @SerialName("mangaList") var mangaList: MediaListTypeOptions?,
)

@Serializable
data class MediaListTypeOptions(
    // The order each list should be displayed in
    @SerialName("sectionOrder") var sectionOrder: List<String>?,

    //    // If the completed sections of the list should be separated by format
    //    @SerialName("splitCompletedSectionByFormat") var splitCompletedSectionByFormat: Boolean?,

    // The names of the user's custom lists
    @SerialName("customLists") var customLists: List<String>?,
    //
    //    // The names of the user's advanced scoring sections
    //    @SerialName("advancedScoring") var advancedScoring: List<String>?,
    //
    //    // If advanced scoring is enabled
    //    @SerialName("advancedScoringEnabled") var advancedScoringEnabled: Boolean?,
)

