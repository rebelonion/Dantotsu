package ani.dantotsu.connections.discord

import ani.dantotsu.connections.discord.serializers.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit.*
import kotlin.coroutines.CoroutineContext
import ani.dantotsu.client as app

@Suppress("MemberVisibilityCanBePrivate")
open class RPC(val token: String, val coroutineContext: CoroutineContext) {

    private val json = Json {
        encodeDefaults = true
        allowStructuredMapKeys = true
        ignoreUnknownKeys = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, SECONDS)
        .readTimeout(10, SECONDS)
        .writeTimeout(10, SECONDS)
        .build()

    private val request = Request.Builder()
        .url("wss://gateway.discord.gg/?encoding=json&v=10")
        .build()

    private var webSocket = client.newWebSocket(request, Listener())

    var applicationId: String? = null
    var type: Type? = null
    var activityName: String? = null
    var details: String? = null
    var state: String? = null
    var largeImage: Link? = null
    var smallImage: Link? = null
    var status: String? = null
    var startTimestamp: Long? = null
    var stopTimestamp: Long? = null

    enum class Type {
        PLAYING, STREAMING, LISTENING, WATCHING, COMPETING
    }

    var buttons = mutableListOf<Link>()

    data class Link(val label: String, val url: String)

    private suspend fun createPresence(): String {
        return json.encodeToString(Presence.Response(
            3,
            Presence(
                activities = listOf(
                    Activity(
                        name = activityName,
                        state = state,
                        details = details,
                        type = type?.ordinal,
                        timestamps = if (startTimestamp != null)
                            Activity.Timestamps(startTimestamp, stopTimestamp)
                        else null,
                        assets = Activity.Assets(
                            largeImage = largeImage?.url?.discordUrl(),
                            largeText = largeImage?.label,
                            smallImage = smallImage?.url?.discordUrl(),
                            smallText = smallImage?.label
                        ),
                        buttons = buttons.map { it.label },
                        metadata = Activity.Metadata(
                            buttonUrls = buttons.map { it.url }
                        ),
                        applicationId = applicationId,
                    )
                ),
                afk = true,
                since = startTimestamp,
                status = status
            )
        ))
    }

    @Serializable
    data class KizzyApi(val id: String)
    val api = "https://kizzy-api.vercel.app/image?url="
    private suspend fun String.discordUrl(): String? {
        if (startsWith("mp:")) return this
        val json = app.get("$api$this").parsedSafe<KizzyApi>()
        return json?.id
    }

    private fun sendIdentify() {
        val response = Identity.Response(
            op = 2,
            d = Identity(
                token = token,
                properties = Identity.Properties(
                    os = "windows",
                    browser = "Chrome",
                    device = "disco"
                ),
                compress = false,
                intents = 0
            )
        )
        webSocket.send(json.encodeToString(response))
    }

    fun send(block: RPC.() -> Unit) {
        block.invoke(this)
        send()
    }

    var started = false
    var whenStarted: ((User) -> Unit)? = null

    fun send() {
        val send = {
            CoroutineScope(coroutineContext).launch {
                webSocket.send(createPresence())
            }
        }
        if (!started) whenStarted = {
            send.invoke()
            whenStarted = null
        }
        else send.invoke()
    }

    fun close() {
        webSocket.send(
            json.encodeToString(
                Presence.Response(
                    3,
                    Presence(status = "offline")
                )
            )
        )
        webSocket.close(4000, "Interrupt")
    }

    //I hate this, but couldn't find any better way to solve it
    suspend fun getUserData(): User {
        var user : User? = null
        whenStarted = {
            user = it
            whenStarted = null
        }
        while (user == null) {
            delay(100)
        }
        return user!!
    }

    var onReceiveUserData: ((User) -> Deferred<Unit>)? = null

    inner class Listener : WebSocketListener() {
        private var seq: Int? = null
        private var heartbeatInterval: Long? = null

        var scope = CoroutineScope(coroutineContext)

        private fun sendHeartBeat() {
            scope.cancel()
            scope = CoroutineScope(coroutineContext)
            scope.launch {
                delay(heartbeatInterval!!)
                webSocket.send("{\"op\":1, \"d\":$seq}")
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            println("Message : $text")

            val map = json.decodeFromString<Res>(text)
            seq = map.s

            when (map.op) {
                10 -> {
                    map.d as JsonObject
                    heartbeatInterval = map.d["heartbeat_interval"]!!.jsonPrimitive.long
                    sendHeartBeat()
                    sendIdentify()
                }

                0  -> if (map.t == "READY") {
                    val user = json.decodeFromString<User.Response>(text).d.user
                    started = true
                    whenStarted?.invoke(user)
                }

                1  -> {
                    if (scope.isActive) scope.cancel()
                    webSocket.send("{\"op\":1, \"d\":$seq}")
                }

                11 -> sendHeartBeat()
                7  -> webSocket.close(400, "Reconnect")
                9  -> {
                    sendHeartBeat()
                    sendIdentify()
                }
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            println("Server Closed : $code $reason")
            if (code == 4000) {
                scope.cancel()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("Failure : ${t.message}")
            if (t.message != "Interrupt") {
                this@RPC.webSocket = client.newWebSocket(request, Listener())
            }
        }
    }

}


