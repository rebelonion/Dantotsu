package ani.dantotsu.connections.anilist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Staff(
    // The id of the staff member
    @SerialName("id") var id: Int,

    // The names of the staff member
    @SerialName("name") var name: StaffName?,

    // The primary language of the staff member. Current values: Japanese, English, Korean, Italian, Spanish, Portuguese, French, German, Hebrew, Hungarian, Chinese, Arabic, Filipino, Catalan, Finnish, Turkish, Dutch, Swedish, Thai, Tagalog, Malaysian, Indonesian, Vietnamese, Nepali, Hindi, Urdu
    @SerialName("languageV2") var languageV2: String?,

    // The staff images
    @SerialName("image") var image: StaffImage?,

    // A general description of the staff member
    @SerialName("description") var description: String?,

    // The person's primary occupations
    @SerialName("primaryOccupations") var primaryOccupations: List<String>?,

    // The staff's gender. Usually Male, Female, or Non-binary but can be any string.
    @SerialName("gender") var gender: String?,

    @SerialName("dateOfBirth") var dateOfBirth: FuzzyDate?,

    @SerialName("dateOfDeath") var dateOfDeath: FuzzyDate?,

    // The person's age in years
    @SerialName("age") var age: Int?,

    // [startYear, endYear] (If the 2nd value is not present staff is still active)
    @SerialName("yearsActive") var yearsActive: List<Int>?,

    // The persons birthplace or hometown
    @SerialName("homeTown") var homeTown: String?,

    // The persons blood type
    @SerialName("bloodType") var bloodType: String?,

    // If the staff member is marked as favourite by the currently authenticated user
    @SerialName("isFavourite") var isFavourite: Boolean?,

    // If the staff member is blocked from being added to favourites
    @SerialName("isFavouriteBlocked") var isFavouriteBlocked: Boolean?,

    // The url for the staff page on the AniList website
    @SerialName("siteUrl") var siteUrl: String?,

    // Media where the staff member has a production role
    @SerialName("staffMedia") var staffMedia: MediaConnection?,

    // Characters voiced by the actor
    @SerialName("characters") var characters: CharacterConnection?,

    // Media the actor voiced characters in. (Same data as characters with media as node instead of characters)
    @SerialName("characterMedia") var characterMedia: MediaConnection?,

    // Staff member that the submission is referencing
    @SerialName("staff") var staff: Staff?,

    // Submitter for the submission
    @SerialName("submitter") var submitter: User?,

    // Status of the submission
    @SerialName("submissionStatus") var submissionStatus: Int?,

    // Inner details of submission status
    @SerialName("submissionNotes") var submissionNotes: String?,

    // The amount of user's who have favourited the staff member
    @SerialName("favourites") var favourites: Int?,

    // Notes for site moderators
    @SerialName("modNotes") var modNotes: String?,
)

@Serializable
data class StaffName(
    var userPreferred: String?
)

@Serializable
data class StaffConnection(
    @SerialName("edges") var edges: List<StaffEdge>?,

    @SerialName("nodes") var nodes: List<Staff>?,

    // The pagination information
    // @SerialName("pageInfo") var pageInfo: PageInfo?,
)

@Serializable
data class StaffImage(
    // The character's image of media at its largest size
    @SerialName("large") var large: String?,

    // The character's image of media at medium size
    @SerialName("medium") var medium: String?,
) : java.io.Serializable

@Serializable
data class StaffEdge(
    var role: String?,
    var node: Staff?
)