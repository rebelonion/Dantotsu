@file:Suppress("unused")

package ani.dantotsu.connections.anilist.api

import ani.dantotsu.R
import ani.dantotsu.currContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Media(
    // The id of the media
    @SerialName("id") var id: Int,

    // The mal id of the media
    @SerialName("idMal") var idMal: Int?,

    // The official titles of the media in various languages
    @SerialName("title") var title: MediaTitle?,

    // The type of the media; anime or manga
    @SerialName("type") var type: MediaType?,

    // The format the media was released in
    @SerialName("format") var format: MediaFormat?,

    // The current releasing status of the media
    @SerialName("status") var status: MediaStatus?,

    // Short description of the media's story and characters
    @SerialName("description") var description: String?,

    // The first official release date of the media
    @SerialName("startDate") var startDate: FuzzyDate?,

    // The last official release date of the media
    @SerialName("endDate") var endDate: FuzzyDate?,

    // The season the media was initially released in
    @SerialName("season") var season: MediaSeason?,

    // The season year the media was initially released in
    @SerialName("seasonYear") var seasonYear: Int?,

    // The year & season the media was initially released in
    @SerialName("seasonInt") var seasonInt: Int?,

    // The amount of episodes the anime has when complete
    @SerialName("episodes") var episodes: Int?,

    // The general length of each anime episode in minutes
    @SerialName("duration") var duration: Int?,

    // The amount of chapters the manga has when complete
    @SerialName("chapters") var chapters: Int?,

    // The amount of volumes the manga has when complete
    @SerialName("volumes") var volumes: Int?,

    // Where the media was created. (ISO 3166-1 alpha-2)
    // Originally a "CountryCode"
    @SerialName("countryOfOrigin") var countryOfOrigin: String?,

    // If the media is officially licensed or a self-published doujin release
    @SerialName("isLicensed") var isLicensed: Boolean?,

    // Source type the media was adapted from.
    @SerialName("source") var source: MediaSource?,

    // Official Twitter hashtags for the media
    @SerialName("hashtag") var hashtag: String?,

    // Media trailer or advertisement
    @SerialName("trailer") var trailer: MediaTrailer?,

    // When the media's data was last updated
    @SerialName("updatedAt") var updatedAt: Int?,

    // The cover images of the media
    @SerialName("coverImage") var coverImage: MediaCoverImage?,

    // The banner image of the media
    @SerialName("bannerImage") var bannerImage: String?,

    // The genres of the media
    @SerialName("genres") var genres: List<String>?,

    // Alternative titles of the media
    @SerialName("synonyms") var synonyms: List<String>?,

    // A weighted average score of all the user's scores of the media
    @SerialName("averageScore") var averageScore: Int?,

    // Mean score of all the user's scores of the media
    @SerialName("meanScore") var meanScore: Int?,

    // The number of users with the media on their list
    @SerialName("popularity") var popularity: Int?,

    // Locked media may not be added to lists our favorited. This may be due to the entry pending for deletion or other reasons.
    @SerialName("isLocked") var isLocked: Boolean?,

    // The amount of related activity in the past hour
    @SerialName("trending") var trending: Int?,

    // The amount of user's who have favourited the media
    @SerialName("favourites") var favourites: Int?,

    // List of tags that describes elements and themes of the media
    @SerialName("tags") var tags: List<MediaTag>?,

    // Other media in the same or connecting franchise
    @SerialName("relations") var relations: MediaConnection?,

    // The characters in the media
    @SerialName("characters") var characters: CharacterConnection?,

    // The staff who produced the media
    @SerialName("staffPreview") var staff: StaffConnection?,

    // The companies who produced the media
    @SerialName("studios") var studios: StudioConnection?,

    // If the media is marked as favourite by the current authenticated user
    @SerialName("isFavourite") var isFavourite: Boolean?,

    // If the media is blocked from being added to favourites
    @SerialName("isFavouriteBlocked") var isFavouriteBlocked: Boolean?,

    // If the media is intended only for 18+ adult audiences
    @SerialName("isAdult") var isAdult: Boolean?,

    // The media's next episode airing schedule
    @SerialName("nextAiringEpisode") var nextAiringEpisode: AiringSchedule?,

    // The media's entire airing schedule
    // @SerialName("airingSchedule") var airingSchedule: AiringScheduleConnection?,

    // The media's daily trend stats
    // @SerialName("trends") var trends: MediaTrendConnection?,

    // External links to another site related to the media
    @SerialName("externalLinks") var externalLinks: List<MediaExternalLink>?,

    // Data and links to legal streaming episodes on external sites
    @SerialName("streamingEpisodes") var streamingEpisodes: List<MediaStreamingEpisode>?,

    // The ranking of the media in a particular time span and format compared to other media
    // @SerialName("rankings") var rankings: List<MediaRank>?,

    // The authenticated user's media list entry for the media
    @SerialName("mediaListEntry") var mediaListEntry: MediaList?,

    // User reviews of the media
    @SerialName("reviews") var reviews: ReviewConnection?,

    // User recommendations for similar media
    @SerialName("recommendations") var recommendations: RecommendationConnection?,

    //
    // @SerialName("stats") var stats: MediaStats?,

    // The url for the media page on the AniList website
    @SerialName("siteUrl") var siteUrl: String?,

    // If the media should have forum thread automatically created for it on airing episode release
    @SerialName("autoCreateForumThread") var autoCreateForumThread: Boolean?,

    // If the media is blocked from being recommended to/from
    @SerialName("isRecommendationBlocked") var isRecommendationBlocked: Boolean?,

    // If the media is blocked from being reviewed
    @SerialName("isReviewBlocked") var isReviewBlocked: Boolean?,

    // Notes for site moderators
    @SerialName("modNotes") var modNotes: String?,
) : java.io.Serializable

@Serializable
data class MediaTitle(
    // The romanization of the native language title
    @SerialName("romaji") var romaji: String,

    // The official english title
    @SerialName("english") var english: String?,

    // Official title in it's native language
    @SerialName("native") var native: String?,

    // The currently authenticated users preferred title language. Default romaji for non-authenticated
    @SerialName("userPreferred") var userPreferred: String,
) : java.io.Serializable

@Serializable
enum class MediaType {
    ANIME, MANGA;

    override fun toString(): String {
        return super.toString().replace("_", " ")
    }
}

@Serializable
enum class MediaStatus {
    FINISHED, RELEASING, NOT_YET_RELEASED, CANCELLED, HIATUS;

    override fun toString(): String {
        currContext()?.let {
            return when (super.toString()) {
                "FINISHED" -> it.getString(R.string.status_finished)
                "RELEASING" -> it.getString(R.string.status_releasing)
                "NOT_YET_RELEASED" -> it.getString(R.string.status_not_yet_released)
                "CANCELLED" -> it.getString(R.string.status_cancelled)
                "HIATUS" -> it.getString(R.string.status_hiatus)
                else -> ""
            }
        }
        return super.toString().replace("_", " ")
    }
}

@Serializable
data class AiringSchedule(
    // The id of the airing schedule item
    @SerialName("id") var id: Int?,

    // The time the episode airs at
    @SerialName("airingAt") var airingAt: Int?,

    // Seconds until episode starts airing
    @SerialName("timeUntilAiring") var timeUntilAiring: Int?,

    // The airing episode number
    @SerialName("episode") var episode: Int?,

    // The associate media id of the airing episode
    @SerialName("mediaId") var mediaId: Int?,

    // The associate media of the airing episode
    @SerialName("media") var media: Media?,
)

@Serializable
data class MediaStreamingEpisode(
    // The title of the episode
    @SerialName("title") var title: String?,

    // The thumbnail image of the episode
    @SerialName("thumbnail") var thumbnail: String?,

    // The url of the episode
    @SerialName("url") var url: String?,

    // The site location of the streaming episode
    @SerialName("site") var site: String?,
) : java.io.Serializable

@Serializable
data class MediaCoverImage(
    // The cover image url of the media at its largest size. If this size isn't available, large will be provided instead.
    @SerialName("extraLarge") var extraLarge: String?,

    // The cover image url of the media at a large size
    @SerialName("large") var large: String?,

    // The cover image url of the media at medium size
    @SerialName("medium") var medium: String?,

    // Average #hex color of cover image
    @SerialName("color") var color: String?,
) : java.io.Serializable

@Serializable
data class MediaList(
    // The id of the list entry
    @SerialName("id") var id: Int?,

    // The id of the user owner of the list entry
    @SerialName("userId") var userId: Int?,

    // The id of the media
    @SerialName("mediaId") var mediaId: Int?,

    // The watching/reading status
    @SerialName("status") var status: MediaListStatus?,

    // The score of the entry
    @SerialName("score") var score: Float?,

    // The amount of episodes/chapters consumed by the user
    @SerialName("progress") var progress: Int?,

    // The amount of volumes read by the user
    @SerialName("progressVolumes") var progressVolumes: Int?,

    // The amount of times the user has rewatched/read the media
    @SerialName("repeat") var repeat: Int?,

    // Priority of planning
    @SerialName("priority") var priority: Int?,

    // If the entry should only be visible to authenticated user
    @SerialName("private") var private: Boolean?,

    // Text notes
    @SerialName("notes") var notes: String?,

    // If the entry shown be hidden from non-custom lists
    @SerialName("hiddenFromStatusLists") var hiddenFromStatusLists: Boolean?,

    // Map of booleans for which custom lists the entry are in
    @SerialName("customLists") var customLists: Map<String, Boolean>?,

    // Map of advanced scores with name keys
    // @SerialName("advancedScores") var advancedScores: Json?,

    // When the entry was started by the user
    @SerialName("startedAt") var startedAt: FuzzyDate?,

    // When the entry was completed by the user
    @SerialName("completedAt") var completedAt: FuzzyDate?,

    // When the entry data was last updated
    @SerialName("updatedAt") var updatedAt: Int?,

    // When the entry data was created
    @SerialName("createdAt") var createdAt: Int?,

    @SerialName("media") var media: Media?,

    @SerialName("user") var user: User?
)

@Serializable
enum class MediaListStatus {
    CURRENT, PLANNING, COMPLETED, DROPPED, PAUSED, REPEATING;

    override fun toString(): String {
        return super.toString().replace("_", " ")
    }
}

@Serializable
enum class MediaSource {
    ORIGINAL, MANGA, LIGHT_NOVEL, VISUAL_NOVEL, VIDEO_GAME, OTHER, NOVEL, DOUJINSHI, ANIME, WEB_NOVEL, LIVE_ACTION, GAME, COMIC, MULTIMEDIA_PROJECT, PICTURE_BOOK;

    override fun toString(): String {
        return super.toString().replace("_", " ")
    }
}

@Serializable
enum class MediaFormat {
    TV, TV_SHORT, MOVIE, SPECIAL, OVA, ONA, MUSIC, MANGA, NOVEL, ONE_SHOT;

    override fun toString(): String {
        return super.toString().replace("_", " ")
    }
}

@Serializable
data class MediaTrailer(
    // The trailer video id
    @SerialName("id") var id: String?,

    // The site the video is hosted by (Currently either youtube or dailymotion)
    @SerialName("site") var site: String?,

    // The url for the thumbnail image of the video
    @SerialName("thumbnail") var thumbnail: String?,
)

@Serializable
data class MediaTagCollection(
    @SerialName("tags") var tags: List<MediaTag>?
)

@Serializable
data class MediaTag(
    // The id of the tag
    @SerialName("id") var id: Int?,

    // The name of the tag
    @SerialName("name") var name: String,

    // A general description of the tag
    @SerialName("description") var description: String?,

    // The categories of tags this tag belongs to
    @SerialName("category") var category: String?,

    // The relevance ranking of the tag out of the 100 for this media
    @SerialName("rank") var rank: Int?,

    // If the tag could be a spoiler for any media
    @SerialName("isGeneralSpoiler") var isGeneralSpoiler: Boolean?,

    // If the tag is a spoiler for this media
    @SerialName("isMediaSpoiler") var isMediaSpoiler: Boolean?,

    // If the tag is only for adult 18+ media
    @SerialName("isAdult") var isAdult: Boolean?,

    // The user who submitted the tag
    @SerialName("userId") var userId: Int?,
)

@Serializable
data class MediaConnection(
    @SerialName("edges") var edges: List<MediaEdge>?,

    @SerialName("nodes") var nodes: List<Media>?,

    // The pagination information
    @SerialName("pageInfo") var pageInfo: PageInfo?,
)

@Serializable
data class MediaEdge(
    //
    @SerialName("node") var node: Media?,

    // The id of the connection
    @SerialName("id") var id: Int?,

    // The type of relation to the parent model
    @SerialName("relationType") var relationType: MediaRelation?,

    // If the studio is the main animation studio of the media (For Studio->MediaConnection field only)
    @SerialName("isMainStudio") var isMainStudio: Boolean?,

    // The characters in the media voiced by the parent actor
    @SerialName("characters") var characters: List<Character>?,

    // The characters role in the media
    @SerialName("characterRole") var characterRole: String?,

    // Media specific character name
    @SerialName("characterName") var characterName: String?,

    // Notes regarding the VA's role for the character
    @SerialName("roleNotes") var roleNotes: String?,

    // Used for grouping roles where multiple dubs exist for the same language. Either dubbing company name or language variant.
    @SerialName("dubGroup") var dubGroup: String?,

    // The role of the staff member in the production of the media
    @SerialName("staffRole") var staffRole: String?,

    // The voice actors of the character
    @SerialName("voiceActors") var voiceActors: List<Staff>?,

    // The voice actors of the character with role date
    // @SerialName("voiceActorRoles") var voiceActorRoles: List<StaffRoleType>?,

    // The order the media should be displayed from the users favourites
    @SerialName("favouriteOrder") var favouriteOrder: Int?,
)

@Serializable
enum class MediaRelation {
    ADAPTATION, PREQUEL, SEQUEL, PARENT, SIDE_STORY, CHARACTER, SUMMARY, ALTERNATIVE, SPIN_OFF, OTHER, SOURCE, COMPILATION, CONTAINS;

    override fun toString(): String {
        currContext()?.let {
            return when (super.toString()) {
                "ADAPTATION" -> it.getString(R.string.type_adaptation)
                "PARENT" -> it.getString(R.string.type_parent)
                "CHARACTER" -> it.getString(R.string.type_character)
                "SUMMARY" -> it.getString(R.string.type_summary)
                "ALTERNATIVE" -> it.getString(R.string.type_alternative)
                "OTHER" -> it.getString(R.string.type_other)
                "SOURCE" -> it.getString(R.string.type_source)
                "CONTAINS" -> it.getString(R.string.type_contains)
                else -> super.toString().replace("_", " ")
            }
        }
        return super.toString().replace("_", " ")
    }
}

@Serializable
enum class MediaSeason {
    WINTER, SPRING, SUMMER, FALL;
}

@Serializable
data class MediaExternalLink(
    // The id of the external link
    @SerialName("id") var id: Int?,

    // The url of the external link or base url of link source
    @SerialName("url") var url: String?,

    // The links website site name
    @SerialName("site") var site: String,

    // The links website site id
    @SerialName("siteId") var siteId: Int?,

    @SerialName("type") var type: ExternalLinkType?,

    // Language the site content is in. See Staff language field for values.
    @SerialName("language") var language: String?,

    @SerialName("color") var color: String?,

    // The icon image url of the site. Not available for all links. Transparent PNG 64x64
    @SerialName("icon") var icon: String?,

    // isDisabled: Boolean
    @SerialName("notes") var notes: String?,
) : java.io.Serializable

@Serializable
enum class ExternalLinkType {
    INFO, STREAMING, SOCIAL;

    override fun toString(): String {
        return super.toString().replace("_", " ")
    }
}

@Serializable
data class MediaListCollection(
    // Grouped media list entries
    @SerialName("lists") var lists: List<MediaListGroup>?,

    // The owner of the list
    @SerialName("user") var user: User?,

    // If there is another chunk
    @SerialName("hasNextChunk") var hasNextChunk: Boolean?,

    ) : java.io.Serializable

@Serializable
data class FollowData(
    @SerialName("id") var id: Int,
    @SerialName("isFollowing") var isFollowing: Boolean,
) : java.io.Serializable

@Serializable
data class MediaListGroup(
    // Media list entries
    @SerialName("entries") var entries: List<MediaList>?,

    @SerialName("name") var name: String?,

    @SerialName("isCustomList") var isCustomList: Boolean?,

    @SerialName("isSplitCompletedList") var isSplitCompletedList: Boolean?,

    @SerialName("status") var status: MediaListStatus?,
) : java.io.Serializable

@Serializable
data class ReviewConnection(
    @SerialName("nodes") var nodes: List<Query.Review>?,
)