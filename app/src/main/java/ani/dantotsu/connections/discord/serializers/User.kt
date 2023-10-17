package ani.dantotsu.connections.discord.serializers

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

@Serializable
data class User (
    val verified: Boolean,
    val username: String,

    @SerialName("purchased_flags")
    val purchasedFlags: Long,

    @SerialName("public_flags")
    val publicFlags: Long,

    val pronouns: String,

    @SerialName("premium_type")
    val premiumType: Long,

    val premium: Boolean,
    val phone: String,

    @SerialName("nsfw_allowed")
    val nsfwAllowed: Boolean,

    val mobile: Boolean,

    @SerialName("mfa_enabled")
    val mfaEnabled: Boolean,

    val id: String,

    @SerialName("global_name")
    val globalName: String,

    val flags: Long,
    val email: String,
    val discriminator: String,
    val desktop: Boolean,
    val bio: String,

    @SerialName("banner_color")
    val bannerColor: String,

    val banner: JsonElement? = null,

    @SerialName("avatar_decoration")
    val avatarDecoration: JsonElement? = null,

    val avatar: String,

    @SerialName("accent_color")
    val accentColor: Long
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

    fun userAvatar():String{
        return "https://cdn.discordapp.com/avatars/$id/$avatar.png"
    }
}