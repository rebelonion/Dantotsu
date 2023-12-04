package ani.dantotsu.connections.anilist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Recommendation(
    // The id of the recommendation
    @SerialName("id") var id: Int?,

    // Users rating of the recommendation
    @SerialName("rating") var rating: Int?,

    // The rating of the recommendation by currently authenticated user
    // @SerialName("userRating") var userRating: RecommendationRating?,

    // The media the recommendation is from
    @SerialName("media") var media: Media?,

    // The recommended media
    @SerialName("mediaRecommendation") var mediaRecommendation: Media?,

    // The user that first created the recommendation
    @SerialName("user") var user: User?,
)

@Serializable
data class RecommendationConnection(
    //@SerialName("edges") var edges: List<RecommendationEdge>?,

    @SerialName("nodes") var nodes: List<Recommendation>?,

    // The pagination information
    //@SerialName("pageInfo") var pageInfo: PageInfo?,

)