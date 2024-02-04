package eu.kanade.tachiyomi.network

import android.content.Context
import android.os.Build
import ani.dantotsu.Mapper
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.settings.saving.PrefManager
import com.lagradost.nicehttp.Requests
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor
import eu.kanade.tachiyomi.network.interceptor.UserAgentInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

class NetworkHelper(
    context: Context
) {

    private val cacheDir = File(context.cacheDir, "network_cache")
    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieJar = AndroidCookieJar()

    private val userAgentInterceptor by lazy {
        UserAgentInterceptor(::defaultUserAgentProvider)
    }
    private val cloudflareInterceptor by lazy {
        CloudflareInterceptor(context, cookieJar, ::defaultUserAgentProvider)
    }

    private fun baseClientBuilder(callTimeout: Int = 2): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(callTimeout.toLong(), TimeUnit.MINUTES)
            .addInterceptor(UncaughtExceptionInterceptor())
            .addInterceptor(userAgentInterceptor)

        if (PrefManager.getVal(PrefName.VerboseLogging)) {
            val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }
            builder.addNetworkInterceptor(httpLoggingInterceptor)
        }

        when (PrefManager.getVal<Int>(PrefName.DohProvider)) {
            PREF_DOH_CLOUDFLARE -> builder.dohCloudflare()
            PREF_DOH_GOOGLE -> builder.dohGoogle()
            PREF_DOH_ADGUARD -> builder.dohAdGuard()
            PREF_DOH_QUAD9 -> builder.dohQuad9()
            PREF_DOH_ALIDNS -> builder.dohAliDNS()
            PREF_DOH_DNSPOD -> builder.dohDNSPod()
            PREF_DOH_360 -> builder.doh360()
            PREF_DOH_QUAD101 -> builder.dohQuad101()
            PREF_DOH_MULLVAD -> builder.dohMullvad()
            PREF_DOH_CONTROLD -> builder.dohControlD()
            PREF_DOH_NJALLA -> builder.dohNajalla()
            PREF_DOH_SHECAN -> builder.dohShecan()
            PREF_DOH_LIBREDNS -> builder.dohLibreDNS()
        }

        return builder
    }


    val client by lazy { baseClientBuilder().cache(Cache(cacheDir, cacheSize)).build() }
    val downloadClient by lazy { baseClientBuilder(20).build() }

    @Suppress("UNUSED")
    val cloudflareClient by lazy {
        client.newBuilder()
            .addInterceptor(cloudflareInterceptor)
            .build()
    }

    val requestClient = Requests(
        client,
        mapOf(
            "User-Agent" to
                    defaultUserAgentProvider()
                        .format(Build.VERSION.RELEASE, Build.MODEL)
        ),
        defaultCacheTime = 6,
        defaultCacheTimeUnit = TimeUnit.HOURS,
        responseParser = Mapper
    )

    fun defaultUserAgentProvider() = PrefManager.getVal<String>(PrefName.DefaultUserAgent)
}
