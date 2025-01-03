package ani.dantotsu.media

import java.io.Serializable

data class Author(
    var id: Int,
    var name: String?,
    var image: String?,
    var role: String?,
    var age: Int? = null,
    var yearsActive: List<Int>? = null,
    var dateOfBirth: String? = null,
    var dateOfDeath: String? = null,
    var homeTown: String? = null,
    var yearMedia: MutableMap<String, ArrayList<Media>>? = null,
    var character: ArrayList<Character>? = null,
    var isFav: Boolean = false
) : Serializable
