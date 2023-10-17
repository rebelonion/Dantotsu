package ani.dantotsu.connections.anilist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Page(
    // The pagination information
    @SerialName("pageInfo") var pageInfo: PageInfo?,

    @SerialName("users") var users: List<User>?,

    @SerialName("media") var media: List<Media>?,

    @SerialName("characters") var characters: List<Character>?,

    @SerialName("staff") var staff: List<Staff>?,

    @SerialName("studios") var studios: List<Studio>?,

    @SerialName("mediaList") var mediaList: List<MediaList>?,

    @SerialName("airingSchedules") var airingSchedules: List<AiringSchedule>?,

    // @SerialName("mediaTrends") var mediaTrends: List<MediaTrend>?,

    // @SerialName("notifications") var notifications: List<NotificationUnion>?,

    @SerialName("followers") var followers: List<User>?,

    @SerialName("following") var following: List<User>?,

    // @SerialName("activities") var activities: List<ActivityUnion>?,

    // @SerialName("activityReplies") var activityReplies: List<ActivityReply>?,

    // @SerialName("threads") var threads: List<Thread>?,

    // @SerialName("threadComments") var threadComments: List<ThreadComment>?,

    // @SerialName("reviews") var reviews: List<Review>?,

    @SerialName("recommendations") var recommendations: List<Recommendation>?,

    @SerialName("likes") var likes: List<User>?,
)

@Serializable
data class PageInfo(
    // The total number of items. Note: This value is not guaranteed to be accurate, do not rely on this for logic
    @SerialName("total") var total: Int?,

    // The count on a page
    @SerialName("perPage") var perPage: Int?,

    // The current page
    @SerialName("currentPage") var currentPage: Int?,

    // The last page
    @SerialName("lastPage") var lastPage: Int?,

    // If there is another page
    @SerialName("hasNextPage") var hasNextPage: Boolean?,
)