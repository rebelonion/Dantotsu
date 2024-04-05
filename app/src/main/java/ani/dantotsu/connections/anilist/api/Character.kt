package ani.dantotsu.connections.anilist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Character(
    // The id of the character
    @SerialName("id") var id: Int,

    // The names of the character
    @SerialName("name") var name: CharacterName?,

    // Character images
    @SerialName("image") var image: CharacterImage?,

    // A general description of the character
    @SerialName("description") var description: String?,

    // The character's gender. Usually Male, Female, or Non-binary but can be any string.
    @SerialName("gender") var gender: String?,

    // The character's birth date
    @SerialName("dateOfBirth") var dateOfBirth: FuzzyDate?,

    // The character's age. Note this is a string, not an int, it may contain further text and additional ages.
    @SerialName("age") var age: String?,

    // The characters blood type
    @SerialName("bloodType") var bloodType: String?,

    // If the character is marked as favourite by the currently authenticated user
    @SerialName("isFavourite") var isFavourite: Boolean?,

    // If the character is blocked from being added to favourites
    @SerialName("isFavouriteBlocked") var isFavouriteBlocked: Boolean?,

    // The url for the character page on the AniList website
    @SerialName("siteUrl") var siteUrl: String?,

    // Media that includes the character
    @SerialName("media") var media: MediaConnection?,

    // The amount of user's who have favourited the character
    @SerialName("favourites") var favourites: Int?,

    // Notes for site moderators
    @SerialName("modNotes") var modNotes: String?,
) : java.io.Serializable

@Serializable
data class CharacterConnection(
    @SerialName("edges") var edges: List<CharacterEdge>?,

    @SerialName("nodes") var nodes: List<Character>?,

    // The pagination information
    @SerialName("pageInfo") var pageInfo: PageInfo?,
) : java.io.Serializable

@Serializable
data class CharacterEdge(
    @SerialName("node") var node: Character?,

    // The id of the connection
    @SerialName("id") var id: Int?,

    // The characters role in the media
    @SerialName("role") var role: String?,

    // Media specific character name
    @SerialName("name") var name: String?,

    // The voice actors of the character
    @SerialName("voiceActors") var voiceActors: List<Staff>?,

    // The voice actors of the character with role date
    // @SerialName("voiceActorRoles") var voiceActorRoles: List<StaffRoleType>?,

    // The media the character is in
    @SerialName("media") var media: List<Media>?,

    // The order the character should be displayed from the users favourites
    @SerialName("favouriteOrder") var favouriteOrder: Int?,
) : java.io.Serializable

@Serializable
data class CharacterName(
    // The character's given name
    @SerialName("first") var first: String?,

    // The character's middle name
    @SerialName("middle") var middle: String?,

    // The character's surname
    @SerialName("last") var last: String?,

    // The character's first and last name
    @SerialName("full") var full: String?,

    // The character's full name in their native language
    @SerialName("native") var native: String?,

    // Other names the character might be referred to as
    @SerialName("alternative") var alternative: List<String>?,

    // Other names the character might be referred to as but are spoilers
    @SerialName("alternativeSpoiler") var alternativeSpoiler: List<String>?,

    // The currently authenticated users preferred name language. Default romaji for non-authenticated
    @SerialName("userPreferred") var userPreferred: String?,
) : java.io.Serializable

@Serializable
data class CharacterImage(
    // The character's image of media at its largest size
    @SerialName("large") var large: String?,

    // The character's image of media at medium size
    @SerialName("medium") var medium: String?,
) : java.io.Serializable