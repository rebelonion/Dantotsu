package ani.dantotsu.connections.comments

import android.content.Context
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.isOnline
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.toast
import ani.dantotsu.util.Logger
import com.lagradost.nicehttp.NiceResponse
import com.lagradost.nicehttp.Requests
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okio.IOException
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object CommentsAPI {
    private const val API_ADDRESS: String = "https://api.dantotsu.app"
    private const val LOCAL_HOST: String = "https://127.0.0.1"
    private var isOnline: Boolean = true
    private var commentsEnabled = PrefManager.getVal<Int>(PrefName.CommentsEnabled) == 1
    private val ADDRESS: String get() = if (commentsEnabled) API_ADDRESS else LOCAL_HOST
    var authToken: String? = null
    var userId: String? = null
    var isBanned: Boolean = false
    var isAdmin: Boolean = false
    var isMod: Boolean = false
    var totalVotes: Int = 0

    suspend fun getCommentsForId(
        id: Int,
        page: Int = 1,
        tag: Int?,
        sort: String?
    ): CommentResponse? {
        var url = "$ADDRESS/comments/$id/$page"
        val request = requestBuilder()
        tag?.let {
            url += "?tag=$it"
        }
        sort?.let {
            url += if (tag != null) "&sort=$it" else "?sort=$it"
        }
        val json = try {
            request.get(url)
        } catch (e: IOException) {
            Logger.log(e)
            errorMessage("Failed to fetch comments")
            return null
        }
        if (!json.text.startsWith("{")) return null
        val res = json.code == 200
        if (!res && json.code != 404) {
            errorReason(json.code, json.text)
        }
        val parsed = try {
            Json.decodeFromString<CommentResponse>(json.text)
        } catch (e: Exception) {
            return null
        }
        return parsed
    }

    suspend fun getRepliesFromId(id: Int, page: Int = 1): CommentResponse? {
        val url = "$ADDRESS/comments/parent/$id/$page"
        val request = requestBuilder()
        val json = try {
            request.get(url)
        } catch (e: IOException) {
            Logger.log(e)
            errorMessage("Failed to fetch comments")
            return null
        }
        if (!json.text.startsWith("{")) return null
        val res = json.code == 200
        if (!res && json.code != 404) {
            errorReason(json.code, json.text)
        }
        val parsed = try {
            Json.decodeFromString<CommentResponse>(json.text)
        } catch (e: Exception) {
            return null
        }
        return parsed
    }

    suspend fun getSingleComment(id: Int): Comment? {
        val url = "$ADDRESS/comments/$id"
        val request = requestBuilder()
        val json = try {
            request.get(url)
        } catch (e: IOException) {
            Logger.log(e)
            errorMessage("Failed to fetch comment")
            return null
        }
        if (!json.text.startsWith("{")) return null
        val res = json.code == 200
        if (!res && json.code != 404) {
            errorReason(json.code, json.text)
        }
        val parsed = try {
            Json.decodeFromString<Comment>(json.text)
        } catch (e: Exception) {
            return null
        }
        return parsed
    }

    suspend fun vote(commentId: Int, voteType: Int): Boolean {
        val url = "$ADDRESS/comments/vote/$commentId/$voteType"
        val request = requestBuilder()
        val json = try {
            request.post(url)
        } catch (e: IOException) {
            Logger.log(e)
            errorMessage("Failed to vote")
            return false
        }
        val res = json.code == 200
        if (!res) {
            errorReason(json.code, json.text)
        }
        return res
    }

    suspend fun comment(mediaId: Int, parentCommentId: Int?, content: String, tag: Int?): Comment? {
        val url = "$ADDRESS/comments"
        val body = FormBody.Builder()
            .add("user_id", userId ?: return null)
            .add("media_id", mediaId.toString())
            .add("content", content)
        if (tag != null) {
            body.add("tag", tag.toString())
        }
        parentCommentId?.let {
            body.add("parent_comment_id", it.toString())
        }
        val request = requestBuilder()
        val json = try {
            request.post(url, requestBody = body.build())
        } catch (e: IOException) {
            Logger.log(e)
            errorMessage("Failed to comment")
            return null
        }
        val res = json.code == 200
        if (!res) {
            errorReason(json.code, json.text)
            return null
        }
        val parsed = try {
            Json.decodeFromString<ReturnedComment>(json.text)
        } catch (e: Exception) {
            Logger.log(e)
            errorMessage("Failed to parse comment")
            return null
        }
        return Comment(
            parsed.id,
            parsed.userId,
            parsed.mediaId,
            parsed.parentCommentId,
            parsed.content,
            parsed.timestamp,
            parsed.deleted,
            parsed.tag,
            0,
            0,
            null,
            Anilist.username ?: "",
            Anilist.avatar,
            totalVotes = totalVotes
        )
    }

    suspend fun deleteComment(commentId: Int): Boolean {
        val url = "$ADDRESS/comments/$commentId"
        val request = requestBuilder()
        val json = try {
            request.delete(url)
        } catch (e: IOException) {
            Logger.log(e)
            errorMessage("Failed to delete comment")
            return false
        }
        val res = json.code == 200
        if (!res) {
            errorReason(json.code, json.text)
        }
        return res
    }

    suspend fun editComment(commentId: Int, content: String): Boolean {
        val url = "$ADDRESS/comments/$commentId"
        val body = FormBody.Builder()
            .add("content", content)
            .build()
        val request = requestBuilder()
        val json = try {
            request.put(url, requestBody = body)
        } catch (e: IOException) {
            Logger.log(e)
            errorMessage("Failed to edit comment")
            return false
        }
        val res = json.code == 200
        if (!res) {
            errorReason(json.code, json.text)
        }
        return res
    }

    suspend fun banUser(userId: String): Boolean {
        val url = "$ADDRESS/ban/$userId"
        val request = requestBuilder()
        val json = try {
            request.post(url)
        } catch (e: IOException) {
            Logger.log(e)
            errorMessage("Failed to ban user")
            return false
        }
        val res = json.code == 200
        if (!res) {
            errorReason(json.code, json.text)
        }
        return res
    }

    suspend fun reportComment(
        commentId: Int,
        username: String,
        mediaTitle: String,
        reportedId: String
    ): Boolean {
        val url = "$ADDRESS/report/$commentId"
        val body = FormBody.Builder()
            .add("username", username)
            .add("mediaName", mediaTitle)
            .add("reporter", Anilist.username ?: "unknown")
            .add("reportedId", reportedId)
            .build()
        val request = requestBuilder()
        val json = try {
            request.post(url, requestBody = body)
        } catch (e: IOException) {
            Logger.log(e)
            errorMessage("Failed to report comment")
            return false
        }
        val res = json.code == 200
        if (!res) {
            errorReason(json.code, json.text)
        }
        return res
    }

    suspend fun getNotifications(client: OkHttpClient): NotificationResponse? {
        val url = "$ADDRESS/notification/reply"
        val request = requestBuilder(client)
        val json = try {
            request.get(url)
        } catch (e: IOException) {
            return null
        }
        if (!json.text.startsWith("{")) return null
        val res = json.code == 200
        if (!res) {
            return null
        }
        val parsed = try {
            Json.decodeFromString<NotificationResponse>(json.text)
        } catch (e: Exception) {
            return null
        }
        return parsed
    }

    private suspend fun getUserDetails(client: OkHttpClient? = null): User? {
        val url = "$ADDRESS/user"
        val request = if (client != null) requestBuilder(client) else requestBuilder()
        val json = try {
            request.get(url)
        } catch (e: IOException) {
            return null
        }
        if (json.code == 200) {
            val parsed = try {
                Json.decodeFromString<UserResponse>(json.text)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
            isBanned = parsed.user.isBanned ?: false
            isAdmin = parsed.user.isAdmin ?: false
            isMod = parsed.user.isMod ?: false
            totalVotes = parsed.user.totalVotes
            return parsed.user
        }
        return null
    }

    suspend fun fetchAuthToken(context: Context, client: OkHttpClient? = null) {
        isOnline = isOnline(context)
        if (authToken != null) return
        val MAX_RETRIES = 5
        val tokenLifetime: Long = 1000 * 60 * 60 * 24 * 6 // 6 days
        val tokenExpiry = PrefManager.getVal<Long>(PrefName.CommentTokenExpiry)
        if (tokenExpiry < System.currentTimeMillis() + tokenLifetime) {
            val commentResponse =
                PrefManager.getNullableVal<AuthResponse>(PrefName.CommentAuthResponse, null)
            if (commentResponse != null) {
                authToken = commentResponse.authToken
                userId = commentResponse.user.id
                isBanned = commentResponse.user.isBanned ?: false
                isAdmin = commentResponse.user.isAdmin ?: false
                isMod = commentResponse.user.isMod ?: false
                totalVotes = commentResponse.user.totalVotes
                if (getUserDetails(client) != null) return
            }

        }
        val url = "$ADDRESS/authenticate"
        val token = PrefManager.getVal(PrefName.AnilistToken, null as String?) ?: return
        repeat(MAX_RETRIES) {
            try {
                val json = authRequest(token, url, client)
                if (json.code == 200) {
                    if (!json.text.startsWith("{")) throw IOException("Invalid response")
                    val parsed = try {
                        Json.decodeFromString<AuthResponse>(json.text)
                    } catch (e: Exception) {
                        Logger.log(e)
                        errorMessage("Failed to login to comments API: ${e.printStackTrace()}")
                        return
                    }
                    PrefManager.setVal(PrefName.CommentAuthResponse, parsed)
                    PrefManager.setVal(
                        PrefName.CommentTokenExpiry,
                        System.currentTimeMillis() + tokenLifetime
                    )
                    authToken = parsed.authToken
                    userId = parsed.user.id
                    isBanned = parsed.user.isBanned ?: false
                    isAdmin = parsed.user.isAdmin ?: false
                    isMod = parsed.user.isMod ?: false
                    totalVotes = parsed.user.totalVotes
                    return
                } else if (json.code != 429) {
                    errorReason(json.code, json.text)
                    return
                }
            } catch (e: IOException) {
                Logger.log(e)
                errorMessage("Failed to login to comments API")
                return
            }
            kotlinx.coroutines.delay(60000)
        }
        errorMessage("Failed to login after multiple attempts")
    }

    private fun errorMessage(reason: String) {
        if (commentsEnabled) Logger.log(reason)
        if (isOnline && commentsEnabled) snackString(reason)
    }

    fun logout() {
        PrefManager.removeVal(PrefName.CommentAuthResponse)
        PrefManager.removeVal(PrefName.CommentTokenExpiry)
        authToken = null
        userId = null
        isBanned = false
        isAdmin = false
        isMod = false
        totalVotes = 0
    }

    private suspend fun authRequest(
        token: String,
        url: String,
        client: OkHttpClient? = null
    ): NiceResponse {
        val body: FormBody = FormBody.Builder()
            .add("token", token)
            .build()
        val request = if (client != null) requestBuilder(client) else requestBuilder()
        return request.post(url, requestBody = body)
    }

    private fun headerBuilder(): Map<String, String> {
        val map = mutableMapOf(
            "appauth" to "6*45Qp%W2RS@t38jkXoSKY588Ynj%n"
        )
        if (authToken != null) {
            map["Authorization"] = authToken!!
        }
        return map
    }

    fun requestBuilder(client: OkHttpClient = Injekt.get<NetworkHelper>().client): Requests {
        return Requests(
            client,
            headerBuilder()
        )
    }

    private fun errorReason(code: Int, reason: String? = null) {
        val error = when (code) {
            429 -> "Rate limited. :("
            else -> "Failed to connect"
        }
        val parsed = try {
            Json.decodeFromString<ErrorResponse>(reason!!)
        } catch (e: Exception) {
            null
        }
        val message = parsed?.message ?: reason ?: error
        val fullMessage = if (code == 500) message else "$code: $message"

        toast(fullMessage)
    }
}

@Serializable
data class ErrorResponse(
    @SerialName("message")
    val message: String
)

@Serializable
data class NotificationResponse(
    @SerialName("notifications")
    val notifications: List<Notification>
)

@Serializable
data class Notification(
    @SerialName("username")
    val username: String,
    @SerialName("media_id")
    val mediaId: Int,
    @SerialName("comment_id")
    val commentId: Int,
    @SerialName("type")
    val type: Int? = null,
    @SerialName("content")
    val content: String? = null,
    @SerialName("notification_id")
    val notificationId: Int
)


@Serializable
data class AuthResponse(
    @SerialName("authToken")
    val authToken: String,
    @SerialName("user")
    val user: User
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1
    }
}

@Serializable
data class UserResponse(
    @SerialName("user")
    val user: User
)

@Serializable
data class User(
    @SerialName("user_id")
    val id: String,
    @SerialName("username")
    val username: String,
    @SerialName("profile_picture_url")
    val profilePictureUrl: String? = null,
    @SerialName("is_banned")
    @Serializable(with = NumericBooleanSerializer::class)
    val isBanned: Boolean? = null,
    @SerialName("is_mod")
    @Serializable(with = NumericBooleanSerializer::class)
    val isAdmin: Boolean? = null,
    @SerialName("is_admin")
    @Serializable(with = NumericBooleanSerializer::class)
    val isMod: Boolean? = null,
    @SerialName("total_votes")
    val totalVotes: Int,
    @SerialName("warnings")
    val warnings: Int
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1
    }
}

@Serializable
data class CommentResponse(
    @SerialName("comments")
    val comments: List<Comment>,
    @SerialName("totalPages")
    val totalPages: Int
)

@Serializable
data class Comment(
    @SerialName("comment_id")
    val commentId: Int,
    @SerialName("user_id")
    val userId: String,
    @SerialName("media_id")
    val mediaId: Int,
    @SerialName("parent_comment_id")
    val parentCommentId: Int?,
    @SerialName("content")
    var content: String,
    @SerialName("timestamp")
    var timestamp: String,
    @SerialName("deleted")
    @Serializable(with = NumericBooleanSerializer::class)
    val deleted: Boolean?,
    @SerialName("tag")
    val tag: Int?,
    @SerialName("upvotes")
    var upvotes: Int,
    @SerialName("downvotes")
    var downvotes: Int,
    @SerialName("user_vote_type")
    var userVoteType: Int?,
    @SerialName("username")
    val username: String,
    @SerialName("profile_picture_url")
    val profilePictureUrl: String?,
    @SerialName("is_mod")
    @Serializable(with = NumericBooleanSerializer::class)
    val isMod: Boolean? = null,
    @SerialName("is_admin")
    @Serializable(with = NumericBooleanSerializer::class)
    val isAdmin: Boolean? = null,
    @SerialName("reply_count")
    val replyCount: Int? = null,
    @SerialName("total_votes")
    val totalVotes: Int
)

@Serializable
data class ReturnedComment(
    @SerialName("id")
    var id: Int,
    @SerialName("comment_id")
    var commentId: Int?,
    @SerialName("user_id")
    val userId: String,
    @SerialName("media_id")
    val mediaId: Int,
    @SerialName("parent_comment_id")
    val parentCommentId: Int? = null,
    @SerialName("content")
    val content: String,
    @SerialName("timestamp")
    val timestamp: String,
    @SerialName("deleted")
    @Serializable(with = NumericBooleanSerializer::class)
    val deleted: Boolean?,
    @SerialName("tag")
    val tag: Int? = null,
)

object NumericBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("NumericBoolean", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeInt(if (value) 1 else 0)
    }

    override fun deserialize(decoder: Decoder): Boolean {
        return decoder.decodeInt() != 0
    }
}