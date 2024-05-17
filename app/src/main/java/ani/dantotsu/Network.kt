package ani.dantotsu

import android.os.Build
import androidx.fragment.app.FragmentActivity
import ani.dantotsu.others.webview.CloudFlare
import ani.dantotsu.others.webview.WebViewBottomDialog
import ani.dantotsu.util.Logger
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.ResponseParser
import com.lagradost.nicehttp.addGenericDns
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkHelper.Companion.defaultUserAgentProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.OkHttpClient
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.PrintWriter
import java.io.Serializable
import java.io.StringWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

lateinit var defaultHeaders: Map<String, String>

lateinit var okHttpClient: OkHttpClient
lateinit var client: Requests

fun initializeNetwork() {

    val networkHelper = Injekt.get<NetworkHelper>()

    defaultHeaders = mapOf(
        "User-Agent" to
                defaultUserAgentProvider()
                    .format(Build.VERSION.RELEASE, Build.MODEL)
    )

    okHttpClient = networkHelper.client
    client = Requests(
        networkHelper.client,
        defaultHeaders,
        defaultCacheTime = 6,
        defaultCacheTimeUnit = TimeUnit.HOURS,
        responseParser = Mapper
    )

}

object Mapper : ResponseParser {

    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @OptIn(InternalSerializationApi::class)
    override fun <T : Any> parse(text: String, kClass: KClass<T>): T {
        return json.decodeFromString(kClass.serializer(), text)
    }

    override fun <T : Any> parseSafe(text: String, kClass: KClass<T>): T? {
        return tryWith {
            parse(text, kClass)
        }
    }

    override fun writeValueAsString(obj: Any): String {
        return json.encodeToString(obj)
    }

    inline fun <reified T> parse(text: String): T {
        return json.decodeFromString(text)
    }
}

fun <A, B> Collection<A>.asyncMap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async { f(it) } }.map { it.await() }
}

fun <A, B> Collection<A>.asyncMapNotNull(f: suspend (A) -> B?): List<B> = runBlocking {
    map { async { f(it) } }.mapNotNull { it.await() }
}

fun logError(e: Throwable, post: Boolean = true, snackbar: Boolean = true) {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    e.printStackTrace(pw)
    val stackTrace: String = sw.toString()
    if (post) {
        if (snackbar)
            snackString(e.localizedMessage, null, stackTrace)
        else
            toast(e.localizedMessage)
    }
    e.printStackTrace()
    Logger.log(e)
}

fun <T> tryWith(post: Boolean = false, snackbar: Boolean = true, call: () -> T): T? {
    return try {
        call.invoke()
    } catch (e: Throwable) {
        logError(e, post, snackbar)
        null
    }
}

suspend fun <T> tryWithSuspend(
    post: Boolean = false,
    snackbar: Boolean = true,
    call: suspend () -> T
): T? {
    return try {
        call.invoke()
    } catch (e: Throwable) {
        logError(e, post, snackbar)
        null
    } catch (e: CancellationException) {
        null
    }
}

/**
 * A url, which can also have headers
 * **/
data class FileUrl(
    var url: String,
    var headers: Map<String, String> = mapOf()
) : Serializable {
    companion object {
        operator fun get(url: String?, headers: Map<String, String> = mapOf()): FileUrl? {
            return FileUrl(url ?: return null, headers)
        }

        private const val serialVersionUID = 1L
    }
}

//Credits to leg
data class Lazier<T>(
    val factory: () -> T,
    val name: String,
    val lClass: KFunction<T>? = null
) {
    val get = lazy { factory() ?: lClass?.call() }
}


fun <T> lazyList(vararg objects: Pair<String, () -> T>): List<Lazier<T>> {
    return objects.map {
        Lazier(it.second, it.first)
    }
}


fun <T> T.printIt(pre: String = ""): T {
    println("$pre$this")
    return this
}


fun OkHttpClient.Builder.addGoogleDns() = (
        addGenericDns(
            "https://dns.google/dns-query",
            listOf(
                "8.8.4.4",
                "8.8.8.8"
            )
        ))

fun OkHttpClient.Builder.addCloudFlareDns() = (
        addGenericDns(
            "https://cloudflare-dns.com/dns-query",
            listOf(
                "1.1.1.1",
                "1.0.0.1",
                "2606:4700:4700::1111",
                "2606:4700:4700::1001"
            )
        ))

fun OkHttpClient.Builder.addAdGuardDns() = (
        addGenericDns(
            "https://dns.adguard.com/dns-query",
            listOf(
                // "Non-filtering"
                "94.140.14.140",
                "94.140.14.141",
            )
        ))

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun webViewInterface(webViewDialog: WebViewBottomDialog): Map<String, String>? {
    var map: Map<String, String>? = null

    val latch = CountDownLatch(1)
    webViewDialog.callback = {
        map = it
        latch.countDown()
    }
    val fragmentManager =
        (currContext() as FragmentActivity?)?.supportFragmentManager ?: return null
    webViewDialog.show(fragmentManager, "web-view")
    delay(0)
    latch.await(2, TimeUnit.MINUTES)
    return map
}

suspend fun webViewInterface(type: String, url: FileUrl): Map<String, String>? {
    val webViewDialog: WebViewBottomDialog = when (type) {
        "Cloudflare" -> CloudFlare.newInstance(url)
        else -> return null
    }
    return webViewInterface(webViewDialog)
}

suspend fun webViewInterface(type: String, url: String): Map<String, String>? {
    return webViewInterface(type, FileUrl(url))
}