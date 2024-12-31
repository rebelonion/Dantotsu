package eu.kanade.tachiyomi.network.interceptor

import ani.dantotsu.util.Logger
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Catches any uncaught exceptions from later in the chain and rethrows as a non-fatal
 * IOException to avoid catastrophic failure.
 *
 * This should be the first interceptor in the client.
 *
 * See https://square.github.io/okhttp/4.x/okhttp/okhttp3/-interceptor/
 */
class UncaughtExceptionInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            chain.proceed(chain.request())
        } catch (e: SocketTimeoutException) {
            Logger.log(e)
            throw IOException("Request timed out")  // there's some odd behavior throwing a SocketTimeoutException
        } catch (e: Exception) {
            Logger.log(e)
            if (e is IOException) {
                throw e
            } else {
                throw IOException(e)
            }
        }
    }
}
