package ani.dantotsu.parsers

import ani.dantotsu.FileUrl
import eu.kanade.tachiyomi.animesource.model.Track
import java.io.Serializable

/**
 * Used to extract videos from a specific video host,
 *
 * A new instance is created for every embeds/iframes of that Episode
 * **/
abstract class VideoExtractor : Serializable {
    abstract val server: VideoServer
    var videos: List<Video> = listOf()
    var subtitles: List<Subtitle> = listOf()
    var audioTracks: List<Track> = listOf()

    /**
     * Extracts videos & subtitles from the `embed`
     *
     * returns a container containing both videos & subtitles (optional)
     * **/
    abstract suspend fun extract(): VideoContainer

    /**
     * Loads videos & subtitles from a given Url
     *
     * & returns itself with the data loaded
     * **/
    open suspend fun load(): VideoExtractor {
        extract().also {
            videos = it.videos
            subtitles = it.subtitles
            audioTracks = it.audioTracks
            return this
        }
    }

    /**
     * Gets called when a Video from this extractor starts playing
     *
     * Useful for Extractor that require Polling
     * **/
    open suspend fun onVideoPlayed(video: Video?) {}

    /**
     * Called when a particular video has been stopped playing
     **/
    open suspend fun onVideoStopped(video: Video?) {}
}

/**
 * A simple class containing name, link & extraData(in case you want to give some to it) of the embed which shows the video present on the site
 *
 * `name` variable is used when checking if there was a Default Server Selected with the same name
 *
 *
 * **/
data class VideoServer(
    val name: String,
    val embed: FileUrl,
    val extraData: Map<String, String>? = null,
    val video: eu.kanade.tachiyomi.animesource.model.Video? = null,
    val offline: Boolean = false
) : Serializable {
    constructor(name: String, embedUrl: String, extraData: Map<String, String>? = null)
            : this(name, FileUrl(embedUrl), extraData)

    constructor(name: String, offline: Boolean, extraData: Map<String, String>?)
            : this(name, FileUrl(""), extraData, null, offline)

    constructor(
        name: String,
        embedUrl: String,
        extraData: Map<String, String>? = null,
        video: eu.kanade.tachiyomi.animesource.model.Video?
    )
            : this(name, FileUrl(embedUrl), extraData, video)
}

/**
 * A Container for keeping video & subtitles, so you dont need to check backend
 * **/
data class VideoContainer(
    val videos: List<Video>,
    val subtitles: List<Subtitle> = listOf(),
    val audioTracks: List<Track> = listOf(),
) : Serializable

/**
 * The Class which contains all the information about a Video
 * **/
data class Video(
    /**
     * Will represent quality to user in form of `"${quality}p"` (1080p)
     *
     * If quality is null, shows "Unknown Quality"
     *
     * If isM3U8 is true, shows "Multi Quality"
     * **/
    val quality: Int?,

    /**
     * Mime type / Format of the video,
     *
     * If not a "CONTAINER" format, the app show video as a "Multi Quality" Link
     * "CONTAINER" formats are Mp4 & Mkv
     * **/
    val format: VideoType,

    /**
     * The direct url to the Video
     *
     * Supports mp4, mkv, dash & m3u8, afaik
     * **/
    val file: FileUrl,

    /**
     * use getSize(url) to get this size,
     *
     * no need to set it on M3U8 links
     * **/
    val size: Double? = null,

    /**
     * In case, you want to show some extra notes to the User
     *
     * Ex: "Backup" which could be used if the site provides some
     * **/
    val extraNote: String? = null,
) : Serializable {

    constructor(
        quality: Int? = null,
        videoType: VideoType,
        url: String,
        size: Double?,
        extraNote: String? = null
    )
            : this(quality, videoType, FileUrl(url), size, extraNote)

    constructor(quality: Int? = null, videoType: VideoType, url: String, size: Double?)
            : this(quality, videoType, FileUrl(url), size)

    constructor(quality: Int? = null, videoType: VideoType, url: String)
            : this(quality, videoType, FileUrl(url))
}

/**
 * The Class which contains the link to a subtitle file of a specific language
 * **/
data class Subtitle(
    /**
     * Language of the Subtitle
     *
     * for now app will directly try to select "English".
     * Probably in rework we can add more subtitles support
     * **/
    val language: String,

    /**
     * The direct url to the Subtitle
     * **/
    val file: FileUrl,

    /**
     * format of the Subtitle
     *
     * Supports VTT, SRT & ASS
     * **/
    val type: SubtitleType = SubtitleType.VTT,
) : Serializable {
    constructor(language: String, url: String, type: SubtitleType = SubtitleType.VTT) : this(
        language,
        FileUrl(url),
        type
    )
}

enum class VideoType {
    CONTAINER, M3U8, DASH
}

enum class SubtitleType {
    VTT, ASS, SRT, UNKNOWN
}