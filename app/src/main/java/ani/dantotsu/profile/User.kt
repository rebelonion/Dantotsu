package ani.dantotsu.profile

data class User(
    val id: Int,
    val name: String,
    val pfp: String?,
    val banner: String?,
    // for media
    val status: String? = null,
    val score: Float? = null,
    val progress: Int? = null,
    val totalEpisodes : Int? = null,
    val nextAiringEpisode : Int? = null,
)