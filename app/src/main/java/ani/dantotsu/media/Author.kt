package ani.dantotsu.media

import java.io.Serializable

data class Author(
    var id: Int,
    var name: String?,
    var image: String?,
    var role: String?,
    var yearMedia: MutableMap<String, ArrayList<Media>>? = null,
    var character: ArrayList<Character>? = null
) : Serializable
