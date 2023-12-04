package ani.dantotsu.connections.discord

import ani.dantotsu.connections.discord.serializers.Activity
import ani.dantotsu.connections.discord.serializers.Presence
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext
import ani.dantotsu.client as app

@Suppress("MemberVisibilityCanBePrivate")
open class RPC(val token: String, val coroutineContext: CoroutineContext) {

    private val json = Json {
        encodeDefaults = true
        allowStructuredMapKeys = true
        ignoreUnknownKeys = true
    }

    enum class Type {
        PLAYING, STREAMING, LISTENING, WATCHING, COMPETING
    }

    data class Link(val label: String, val url: String)

    companion object {
        data class RPCData(
            val applicationId: String? = null,
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

        @Serializable
        data class KizzyApi(val id: String)

        val api = "https://kizzy-api.vercel.app/image?url="
        private suspend fun String.discordUrl(): String? {
            if (startsWith("mp:")) return this
            val json = app.get("$api$this").parsedSafe<KizzyApi>()
            return json?.id
        }

        suspend fun createPresence(data: RPCData): String {
            val json = Json {
                encodeDefaults = true
                allowStructuredMapKeys = true
                ignoreUnknownKeys = true
            }
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
                                smallImage = data.smallImage?.url?.discordUrl(),
                                smallText = data.smallImage?.label
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
                    status = data.status
                )
            ))
        }
    }

}


