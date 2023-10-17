package ani.dantotsu.media

import java.io.Serializable

data class Author(
    val id: String,
    val name: String,
    var yearMedia: MutableMap<String, ArrayList<Media>>? = null
) : Serializable
