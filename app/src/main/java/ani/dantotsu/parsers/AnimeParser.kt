package ani.dantotsu.parsers

import android.net.Uri
import ani.dantotsu.FileUrl
import ani.dantotsu.R
import ani.dantotsu.asyncMap
import ani.dantotsu.currContext
import ani.dantotsu.others.MalSyncBackup
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.tryWithSuspend
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode

/**
 * An abstract class for creating a new Source
 *
 * Most of the functions & variables that need to be overridden are abstract
 * **/
abstract class AnimeParser : BaseParser() {

    /**
     * Takes ShowResponse.link & ShowResponse.extra (if you added any) as arguments & gives a list of total episodes present on the site.
     * **/
    abstract suspend fun loadEpisodes(
        animeLink: String,
        extra: Map<String, String>?,
        sAnime: SAnime
    ): List<Episode>

    /**
     * Takes ShowResponse.link, ShowResponse.extra & the Last Largest Episode Number known by app as arguments
     *
     * Returns the latest episode (If overriding, Make sure the episode is actually the latest episode)
     * Returns null, if no latest episode is found.
     * **/
    open suspend fun getLatestEpisode(
        animeLink: String,
        extra: Map<String, String>?,
        sAnime: SAnime,
        latest: Float
    ): Episode? {
        val episodes = loadEpisodes(animeLink, extra, sAnime)
        val max = episodes
            .maxByOrNull { it.number.toFloatOrNull() ?: 0f }
        return max
            ?.takeIf { latest < (it.number.toFloatOrNull() ?: 0.001f) }
    }

    /**
     * Takes Episode.link as a parameter
     *
     * This returns a Map of "Video Server's Name" & "Link/Data" of all the Video Servers present on the site, which can be further used by loadVideoServers() & loadSingleVideoServer()
     * **/
    abstract suspend fun loadVideoServers(
        episodeLink: String,
        extra: Map<String, String>?,
        sEpisode: SEpisode
    ): List<VideoServer>


    /**
     * This function will receive **url of the embed** & **name** of a Video Server present on the site to host the episode.
     *
     *
     * Create a new VideoExtractor for the video server you are trying to scrape, if there's not one already.
     *
     *
     * (Some sites might not have separate video hosts. In that case, just create a new VideoExtractor for that particular site)
     *
     *
     * returns a **VideoExtractor** containing **`server`**, the app will further load the videos using `extract()` function inside it
     *
     * **Example for Site with multiple Video Servers**
     * ```
    val domain = Uri.parse(server.embed.url).host ?: ""
    val extractor: VideoExtractor? = when {
    "fembed" in domain   -> FPlayer(server)
    "sb" in domain       -> StreamSB(server)
    "streamta" in domain -> StreamTape(server)
    else                 -> null
    }
    return extractor
    ```
     * You can use your own way to get the Extractor for reliability.
     * if there's only extractor, you can directly return it.
     * **/
    open suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        var domain = Uri.parse(server.embed.url).host ?: return null
        if (domain.startsWith("www.")) {
            domain = domain.substring(4)
        }

        return when (domain) {
            else -> {
                println("$name : No extractor found for: $domain | ${server.embed.url}")
                null
            }
        }
    }

    /**
     * If the Video Servers support preloading links for the videos
     * typically depends on what Video Extractor is being used
     * **/
    open val allowsPreloading = true

    /**
     * This Function used when there "isn't" a default Server set by the user, or when user wants to switch the Server
     *
     * Doesn't need to be overridden, if the parser is following the norm.
     * **/
    open suspend fun loadByVideoServers(
        episodeUrl: String,
        extra: Map<String, String>?,
        sEpisode: SEpisode,
        callback: (VideoExtractor) -> Unit
    ) {
        tryWithSuspend(true) {
            loadVideoServers(episodeUrl, extra, sEpisode).asyncMap {
                getVideoExtractor(it)?.apply {
                    tryWithSuspend(true) {
                        load()
                    }
                    callback.invoke(this)
                }
            }
        }
    }

    /**
     * This Function used when there "is" a default Server set by the user, only loads a Single Server for faster response.
     *
     * Doesn't need to be overridden, if the parser is following the norm.
     * **/
    open suspend fun loadSingleVideoServer(
        serverName: String,
        episodeUrl: String,
        extra: Map<String, String>?,
        sEpisode: SEpisode,
        post: Boolean
    ): VideoExtractor? {
        return tryWithSuspend(post) {
            loadVideoServers(episodeUrl, extra, sEpisode).apply {
                find { it.name == serverName }?.also {
                    return@tryWithSuspend getVideoExtractor(it)?.apply {
                        load()
                    }
                }
            }
            null
        }
    }


    /**
     * Many sites have Dub & Sub anime as separate Shows
     *
     * make this `true`, if they are separated else `false`
     *
     * **NOTE : do not forget to override `search` if the site does not support only dub search**
     * **/
    open fun isDubAvailableSeparately(sourceLang: Int? = null): Boolean = false

    /**
     * The app changes this, depending on user's choice.
     * **/
    open var selectDub = false

    /**
     * Name used to get Shows Directly from MALSyncBackup's github dump
     *
     * Do not override if the site is not present on it.
     * **/
    open val malSyncBackupName = ""

    /**
     * Overridden to add MalSyncBackup support for Anime Sites
     * **/
    override suspend fun loadSavedShowResponse(mediaId: Int): ShowResponse? {
        checkIfVariablesAreEmpty()
        val dub = if (isDubAvailableSeparately()) "_${if (selectDub) "dub" else "sub"}" else ""
        var loaded = PrefManager.getNullableCustomVal(
            "${saveName}${dub}_$mediaId",
            null,
            ShowResponse::class.java
        )
        if (loaded == null && malSyncBackupName.isNotEmpty())
            loaded = MalSyncBackup.get(mediaId, malSyncBackupName, selectDub)
                ?.also { saveShowResponse(mediaId, it, true) }
        return loaded
    }

    override fun saveShowResponse(mediaId: Int, response: ShowResponse?, selected: Boolean) {
        if (response != null) {
            checkIfVariablesAreEmpty()
            setUserText(
                "${
                    if (selected) currContext()!!.getString(R.string.selected) else currContext()!!.getString(
                        R.string.found
                    )
                } : ${response.name}"
            )
            val dub = if (isDubAvailableSeparately()) "_${if (selectDub) "dub" else "sub"}" else ""
            PrefManager.setCustomVal("${saveName}${dub}_$mediaId", response)
        }
    }
}

class EmptyAnimeParser : AnimeParser() {
    override val name: String = "None"
    override val saveName: String = "None"
    override suspend fun loadEpisodes(
        animeLink: String,
        extra: Map<String, String>?,
        sAnime: SAnime
    ): List<Episode> = emptyList()

    override suspend fun loadVideoServers(
        episodeLink: String,
        extra: Map<String, String>?,
        sEpisode: SEpisode
    ): List<VideoServer> = emptyList()

    override suspend fun search(query: String): List<ShowResponse> = emptyList()
}

/**
 * A class for containing Episode data of a particular parser
 * **/
data class Episode(
    /**
     * Number of the Episode in "String",
     *
     * useful in cases where episode is not a number
     * **/
    val number: String,

    /**
     * Link that links to the episode page containing videos
     * **/
    val link: String,

    //Self-Descriptive
    val title: String? = null,
    val thumbnail: FileUrl? = null,
    val description: String? = null,
    val isFiller: Boolean = false,

    /**
     * In case, you want to pass extra data
     * **/
    val extra: Map<String, String>? = null,

    //SEpisode from Aniyomi
    val sEpisode: SEpisode? = null
) {
    constructor(
        number: String,
        link: String,
        title: String? = null,
        thumbnail: String,
        description: String? = null,
        isFiller: Boolean = false,
        extra: Map<String, String>? = null
    ) : this(number, link, title, FileUrl(thumbnail), description, isFiller, extra)

    constructor(
        number: String,
        link: String,
        title: String? = null,
        thumbnail: String,
        description: String? = null,
        isFiller: Boolean = false,
        extra: Map<String, String>? = null,
        sEpisode: SEpisode? = null
    ) : this(number, link, title, FileUrl(thumbnail), description, isFiller, extra, sEpisode)
}
