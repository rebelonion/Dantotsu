package ani.dantotsu.home.status.data

import ani.dantotsu.connections.anilist.api.User

data class StoryItem(
    val id : Int? = null,
    val activityId : Int? = null,
    val mediaId: Int? = null,
    val userName: String? = null,
    val userAvatar: String? = null,
    val time: String? = null,
    val info: String? = null,
    val cover: String? = null,
    val banner: String? = null,
    var likes : Int = 0,
    val likedBy: List<User>? = null,
    var isLiked : Boolean = false
)


