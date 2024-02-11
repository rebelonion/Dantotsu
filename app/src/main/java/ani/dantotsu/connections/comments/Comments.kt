package ani.dantotsu.connections.comments

import android.security.keystore.KeyProperties
import android.util.Base64
import ani.dantotsu.BuildConfig
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import com.lagradost.nicehttp.Requests
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class Comments {
    val address: String = "http://10.0.2.2:8081"
    val appSecret = BuildConfig.APP_SECRET
    val requestClient = Injekt.get<NetworkHelper>().client
    var authToken: String? = null
    fun run() {
        runBlocking {
            val request = Requests(
                requestClient,
                headerBuilder()
            )
                .get(address)
            println("comments: $request")
        }
    }

    fun getCommentsForId(id: Int) {
        val url = "$address/comments/$id"
        runBlocking {
            val request = Requests(
                requestClient,
                headerBuilder()
            )
                .get(url)
            println("comments: $request")
        }
    }

    fun fetchAuthToken() {
        val url = "$address/authenticate"
        //test user id = asdf
        //test username = test
        val user = User(generateUserId() ?: return, "rebel onion")
        val body: FormBody = FormBody.Builder()
            .add("user_id", user.id)
            .add("username", user.username)
            .build()
        runBlocking {
            val request = Requests(
                requestClient,
                headerBuilder()
            )
            val json = request.post(url, requestBody = body)
            if (!json.text.startsWith("{")) return@runBlocking
            val parsed = try {
                Json.decodeFromString<Auth>(json.text)
            } catch (e: Exception) {
                return@runBlocking
            }
            authToken = parsed.authToken

            println("comments: $json")
            println("comments: $authToken")
        }
    }

    private fun headerBuilder(): Map<String, String> {
        return if (authToken != null) {
            mapOf(
                "appauth" to appSecret,
                "Authorization" to authToken!!
            )
        } else {
            mapOf(
                "appauth" to appSecret,
            )
        }
    }

    private fun generateUserId(): String? {
        val anilistId = PrefManager.getVal(PrefName.AnilistToken, null as String?) ?: return null
        val userIdEncryptKey = BuildConfig.USER_ID_ENCRYPT_KEY
        val keySpec = SecretKeySpec(userIdEncryptKey.toByteArray(), KeyProperties.KEY_ALGORITHM_AES)
        val cipher =  Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypted = cipher.doFinal(anilistId.toByteArray())
        val base = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        val bytes = MessageDigest.getInstance("SHA-256").digest(base.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }

    }
}

@Serializable
data class Auth(
    @SerialName("authToken")
    val authToken: String
)

@Serializable
data class User(
    @SerialName("user_id")
    val id: String,
    @SerialName("username")
    val username: String
)