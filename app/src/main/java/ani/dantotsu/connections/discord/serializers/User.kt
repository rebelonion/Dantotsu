package ani.dantotsu.connections.discord.serializers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


@Serializable
data class User(
    val verified: Boolean? = null,
    val username: String,

    @SerialName("purchased_flags")
    val purchasedFlags: Long? = null,

    @SerialName("public_flags")
    val publicFlags: Long? = null,

    val pronouns: String? = null,

    @SerialName("premium_type")
    val premiumType: Long? = null,

    val premium: Boolean? = null,
    val phone: String? = null,

    @SerialName("nsfw_allowed")
    val nsfwAllowed: Boolean? = null,

    val mobile: Boolean? = null,

    @SerialName("mfa_enabled")
    val mfaEnabled: Boolean? = null,

    val id: String,

    @SerialName("global_name")
    val globalName: String? = null,

    val flags: Long? = null,
    val email: String? = null,
    val discriminator: String? = null,
    val desktop: Boolean? = null,
    val bio: String? = null,

    @SerialName("banner_color")
    val bannerColor: String? = null,

    val banner: JsonElement? = null,

    @SerialName("avatar_decoration")
    val avatarDecoration: JsonElement? = null,

    val avatar: String? = null,

    @SerialName("accent_color")
    val accentColor: Long? = null
) {
    @Serializable
    data class Response(
        val t: String,
        val s: Long,
        val op: Long,
        val d: D
    ) {
        @Serializable
        data class D(
            val v: Long,
            val user: User,
        )
    }

    fun userAvatar(): String {
        return "https://cdn.discordapp.com/avatars/$id/$avatar.png"
    }
}