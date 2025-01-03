package ani.dantotsu.media

import java.io.Serializable

data class Studio(
    val id: String,
    val name: String,
    val isFavourite: Boolean?,
    val favourites: Int?,
    val imageUrl: String?,
    var yearMedia: MutableMap<String, ArrayList<Media>>? = null
) : Serializable
