package ani.dantotsu.connections.comments

import ani.dantotsu.BuildConfig
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
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
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object CommentsAPI {
    val address: String = "https://1224665.xyz:443"
    var authToken: String? = null
    var userId: String? = null
    var isBanned: Boolean = false
    var isAdmin: Boolean = false
    var isMod: Boolean = false
    var totalVotes: Int = 0

    suspend fun getCommentsForId(id: Int, page: Int = 1): CommentResponse? {
        val url = "$address/comments/$id/$page"
        val request = requestBuilder()
        val json = request.get(url)
        if (!json.text.startsWith("{")) return null
        val res = json.code == 200
        if (!res && json.code != 404) {
            errorReason(json.code, json.text)
        }
        val parsed = try {
            Json.decodeFromString<CommentResponse>(json.text)
        } catch (e: Exception) {
            println("comments: $e")
            return null
        }
        return parsed
    }

    suspend fun getRepliesFromId(id: Int, page: Int = 1): CommentResponse? {
        val url = "$address/comments/parent/$id/$page"
        val request = requestBuilder()
        val json = request.get(url)
        if (!json.text.startsWith("{")) return null
        val res = json.code == 200
        if (!res && json.code != 404) {
            errorReason(json.code, json.text)
        }
        val parsed = try {
            Json.decodeFromString<CommentResponse>(json.text)
        } catch (e: Exception) {
            println("comments: $e")
            return null
        }
        return parsed
    }

    suspend fun vote(commentId: Int, voteType: Int): Boolean {
        val url = "$address/comments/vote/$commentId/$voteType"
        val request = requestBuilder()
        val json = request.post(url)
        val res = json.code == 200
        if (!res) {
            errorReason(json.code, json.text)
        }
        return res
    }

    suspend fun comment(mediaId: Int, parentCommentId: Int?, content: String): Comment? {
        val url = "$address/comments"
        val body = FormBody.Builder()
            .add("user_id", userId ?: return null)
            .add("media_id", mediaId.toString())
            .add("content", content)
        parentCommentId?.let {
            body.add("parent_comment_id", it.toString())
        }
        val request = requestBuilder()
        val json = request.post(url, requestBody = body.build())
        val res = json.code == 200
        if (!res) {
            errorReason(json.code, json.text)
            return null
        }
        val parsed = try {
            Json.decodeFromString<ReturnedComment>(json.text)
        } catch (e: Exception) {
            println("comment: $e")
            snackString("Failed to parse comment")
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
            0,
            0,
            null,
            Anilist.username ?: "",
            Anilist.avatar,
            totalVotes = totalVotes
        )
    }

    suspend fun deleteComment(commentId: Int): Boolean {
        val url = "$address/comments/$commentId"
        val request = requestBuilder()
        val json = request.delete(url)
        val res = json.code == 200
        if (!res) {
            errorReason(json.code, json.text)
        }
        return res
    }

    suspend fun editComment(commentId: Int, content: String): Boolean {
        val url = "$address/comments/$commentId"
        val body = FormBody.Builder()
            .add("content", content)
            .build()
        val request = requestBuilder()
        val json = request.put(url, requestBody = body)
        val res = json.code == 200
        if (!res) {
            errorReason(json.code, json.text)
        }
        return res
    }

    suspend fun banUser(userId: String): Boolean {
        val url = "$address/ban/$userId"
        val request = requestBuilder()
        val json = request.post(url)
        val res = json.code == 200
        if (!res) {
            errorReason(json.code, json.text)
        }
        return res
    }

    suspend fun fetchAuthToken() {
        val url = "$address/authenticate"
        val token = PrefManager.getVal(PrefName.AnilistToken, null as String?) ?: return
        val body: FormBody = FormBody.Builder()
            .add("token", token)
            .build()
        val request = requestBuilder()
        val json = request.post(url, requestBody = body)
        if (!json.text.startsWith("{")) return
        val parsed = try {
            Json.decodeFromString<AuthResponse>(json.text)
        } catch (e: Exception) {
            snackString("Failed to login to comments API: ${e.printStackTrace()}")
            return
        }
        authToken = parsed.authToken
        userId = parsed.user.id
        isBanned = parsed.user.isBanned ?: false
        isAdmin = parsed.user.isAdmin ?: false
        isMod = parsed.user.isMod ?: false
        totalVotes = parsed.user.totalVotes
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

    private fun requestBuilder(): Requests {
        return Requests(
            Injekt.get<NetworkHelper>().client,
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

        snackString("Error $code: $message")
    }
}

@Serializable
data class ErrorResponse(
    @SerialName("message")
    val message: String
)

@Serializable
data class AuthResponse(
    @SerialName("authToken")
    val authToken: String,
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
)

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