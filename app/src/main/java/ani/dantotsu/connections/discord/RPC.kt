package ani.dantotsu.connections.discord

import ani.dantotsu.connections.discord.Discord.token
import ani.dantotsu.connections.discord.serializers.Activity
import ani.dantotsu.connections.discord.serializers.Presence
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.coroutines.CoroutineContext

@Suppress("MemberVisibilityCanBePrivate")
open class RPC(val token: String, val coroutineContext: CoroutineContext) {

    enum class Type {
        PLAYING, STREAMING, LISTENING, WATCHING, COMPETING
    }

    data class Link(val label: String, val url: String)

    companion object {
        data class RPCData(
            val applicationId: String,
            val type: Type? = null,
            val activityName: String? = null,
            val details: String? = null,
            val state: String? = null,
            val largeImage: Link? = null,
            val smallImage: Link? = null,
            val status: String? = null,
            val startTimestamp: Long? = null,
            val stopTimestamp: Long? = null,
            val buttons: MutableList<Link> = mutableListOf()
        )

        suspend fun createPresence(data: RPCData): String {
            val json = Json {
                encodeDefaults = true
                allowStructuredMapKeys = true
                ignoreUnknownKeys = true
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(10, SECONDS)
                .readTimeout(10, SECONDS)
                .writeTimeout(10, SECONDS)
                .build()

            val assetApi = RPCExternalAsset(data.applicationId, token!!, client, json)
            suspend fun String.discordUrl() = assetApi.getDiscordUri(this)

            return json.encodeToString(Presence.Response(
                3,
                Presence(
                    activities = listOf(
                        Activity(
                            name = data.activityName,
                            state = data.state,
                            details = data.details,
                            type = data.type?.ordinal,
                            timestamps = if (data.startTimestamp != null)
                                Activity.Timestamps(data.startTimestamp, data.stopTimestamp)
                            else null,
                            assets = Activity.Assets(
                                largeImage = data.largeImage?.url?.discordUrl(),
                                largeText = data.largeImage?.label,
                                smallImage = if (PrefManager.getVal(PrefName.ShowAniListIcon)) Discord.small_Image_AniList.discordUrl() else Discord.small_Image.discordUrl(),
                                smallText = if (PrefManager.getVal(PrefName.ShowAniListIcon)) "Anilist" else "Dantotsu",
                            ),
                            buttons = data.buttons.map { it.label },
                            metadata = Activity.Metadata(
                                buttonUrls = data.buttons.map { it.url }
                            ),
                            applicationId = data.applicationId,
                        )
                    ),
                    afk = true,
                    since = data.startTimestamp,
                    status = PrefManager.getVal(PrefName.DiscordStatus)
                )
            ))
        }
    }

}


