package ani.dantotsu.media

import java.io.Serializable

data class Author(
    val id: Int,
    val name: String?,
    val image: String?,
    val role: String?,
    var yearMedia: MutableMap<String, ArrayList<Media>>? = null
) : Serializable
