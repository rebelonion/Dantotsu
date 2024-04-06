package ani.dantotsu.profile

data class User(
    val id: Int,
    val name: String,
    val pfp: String?,
    val banner: String?,
    val info: String? = null,
)