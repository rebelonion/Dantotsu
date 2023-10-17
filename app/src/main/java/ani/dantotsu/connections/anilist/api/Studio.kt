package ani.dantotsu.connections.anilist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Studio(
    // The id of the studio
    @SerialName("id") var id: Int,

    // The name of the studio
    // Originally non-nullable, needs to be nullable due to it not being always queried
    @SerialName("name") var name: String?,

    // If the studio is an animation studio or a different kind of company
    @SerialName("isAnimationStudio") var isAnimationStudio: Boolean?,

    // The media the studio has worked on
    @SerialName("media") var media: MediaConnection?,

    // The url for the studio page on the AniList website
    @SerialName("siteUrl") var siteUrl: String?,

    // If the studio is marked as favourite by the currently authenticated user
    @SerialName("isFavourite") var isFavourite: Boolean?,

    // The amount of user's who have favourited the studio
    @SerialName("favourites") var favourites: Int?,
)

@Serializable
data class StudioConnection(
    //@SerialName("edges") var edges: List<StudioEdge>?,

    @SerialName("nodes") var nodes: List<Studio>?,

    // The pagination information
    //@SerialName("pageInfo") var pageInfo: PageInfo?,
)