package ani.dantotsu.media

import ani.dantotsu.connections.anilist.api.FuzzyDate
import java.io.Serializable

data class Character(
    val id: Int,
    val name: String?,
    val image: String?,
    val banner: String?,
    val role: String,
    var isFav: Boolean,
    var description: String? = null,
    var age: String? = null,
    var gender: String? = null,
    var dateOfBirth: FuzzyDate? = null,
    var roles: ArrayList<Media>? = null,
    val voiceActor: ArrayList<Author>? = null,
) : Serializable