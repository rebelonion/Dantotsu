package ani.dantotsu.connections.mal

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.FragmentActivity
import ani.dantotsu.R
import ani.dantotsu.client
import ani.dantotsu.currContext
import ani.dantotsu.loadData
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.saveData
import ani.dantotsu.tryWithSuspend
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.security.SecureRandom

object MAL {
    val query: MALQueries = MALQueries()
    const val clientId = "86b35cf02205a0303da3aaea1c9e33f3"
    var username: String? = null
    var avatar: String? = null
    var token: String? = null
    var userid: Int? = null

    fun loginIntent(context: Context) {
        val codeVerifierBytes = ByteArray(96)
        SecureRandom().nextBytes(codeVerifierBytes)
        val codeChallenge = Base64.encodeToString(codeVerifierBytes, Base64.DEFAULT).trimEnd('=')
            .replace("+", "-")
            .replace("/", "_")
            .replace("\n", "")

        saveData("malCodeChallenge", codeChallenge, context)
        val request =
            "https://myanimelist.net/v1/oauth2/authorize?response_type=code&client_id=$clientId&code_challenge=$codeChallenge"
        try {
            CustomTabsIntent.Builder().build().launchUrl(
                context,
                Uri.parse(request)
            )
        } catch (e: ActivityNotFoundException) {
            openLinkInBrowser(request)
        }
    }

    private const val MAL_TOKEN = "malToken"

    private suspend fun refreshToken(): ResponseToken? {
        return tryWithSuspend {
            val token = loadData<ResponseToken>(MAL_TOKEN)
                ?: throw Exception(currContext()?.getString(R.string.refresh_token_load_failed))
            val res = client.post(
                "https://myanimelist.net/v1/oauth2/token",
                data = mapOf(
                    "client_id" to clientId,
                    "grant_type" to "refresh_token",
                    "refresh_token" to token.refreshToken
                )
            ).parsed<ResponseToken>()
            saveResponse(res)
            return@tryWithSuspend res
        }
    }


    suspend fun getSavedToken(context: FragmentActivity): Boolean {
        return tryWithSuspend(false) {
            var res: ResponseToken = loadData(MAL_TOKEN, context)
                ?: return@tryWithSuspend false
            if (System.currentTimeMillis() > res.expiresIn)
                res = refreshToken()
                    ?: throw Exception(currContext()?.getString(R.string.refreshing_token_failed))
            token = res.accessToken
            return@tryWithSuspend true
        } ?: false
    }

    fun removeSavedToken(context: Context) {
        token = null
        username = null
        userid = null
        avatar = null
        if (MAL_TOKEN in context.fileList()) {
            File(context.filesDir, MAL_TOKEN).delete()
        }
    }

    fun saveResponse(res: ResponseToken) {
        res.expiresIn += System.currentTimeMillis()
        saveData(MAL_TOKEN, res)
    }

    @Serializable
    data class ResponseToken(
        @SerialName("token_type") val tokenType: String,
        @SerialName("expires_in") var expiresIn: Long,
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String,
    ) : java.io.Serializable

}
