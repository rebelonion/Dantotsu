package eu.kanade.tachiyomi.animesource.model

import android.net.Uri
import eu.kanade.tachiyomi.network.ProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Headers
import rx.subjects.Subject
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

data class Track(val url: String, val lang: String) : Serializable

open class Video(
    val url: String = "",
    val quality: String = "",
    var videoUrl: String? = null,
    headers: Headers? = null,
    // "url", "language-label-2", "url2", "language-label-2"
    val subtitleTracks: List<Track> = emptyList(),
    val audioTracks: List<Track> = emptyList(),
) : Serializable, ProgressListener {

    @Transient
    var headers: Headers? = headers

    @Suppress("UNUSED_PARAMETER")
    constructor(
        url: String,
        quality: String,
        videoUrl: String?,
        uri: Uri? = null,
        headers: Headers? = null,
    ) : this(url, quality, videoUrl, headers)

    @Transient
    @Volatile
    var status: State = State.QUEUE

    @Transient
    private val _progressFlow = MutableStateFlow(0)

    @Transient
    val progressFlow = _progressFlow.asStateFlow()
    var progress: Int
        get() = _progressFlow.value
        set(value) {
            _progressFlow.value = value
        }

    @Transient
    @Volatile
    var totalBytesDownloaded: Long = 0L

    @Transient
    @Volatile
    var totalContentLength: Long = 0L

    @Transient
    @Volatile
    var bytesDownloaded: Long = 0L
        set(value) {
            totalBytesDownloaded += if (value < field) {
                value
            } else {
                value - field
            }
            field = value
        }

    @Transient
    var progressSubject: Subject<State, State>? = null

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        bytesDownloaded = bytesRead
        if (contentLength > totalContentLength) {
            totalContentLength = contentLength
        }
        val newProgress = if (totalContentLength > 0) {
            (100 * totalBytesDownloaded / totalContentLength).toInt()
        } else {
            -1
        }
        if (progress != newProgress) progress = newProgress
    }

    enum class State {
        QUEUE,
        LOAD_VIDEO,
        DOWNLOAD_IMAGE,
        READY,
        ERROR,
    }

    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.defaultWriteObject()
        val headersMap: Map<String, List<String>> = headers?.toMultimap() ?: emptyMap()
        out.writeObject(headersMap)
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()
        val headersMap = input.readObject() as? Map<String, List<String>>
        headers = headersMap?.let { map ->
            val builder = Headers.Builder()
            for ((key, values) in map) {
                for (value in values) {
                    builder.add(key, value)
                }
            }
            builder.build()
        }

    }
}
