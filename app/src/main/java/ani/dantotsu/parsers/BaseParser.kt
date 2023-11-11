package ani.dantotsu.parsers

import ani.dantotsu.*
import ani.dantotsu.media.Media
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.source.model.SManga
import java.io.Serializable
import java.net.URLDecoder
import java.net.URLEncoder
import me.xdrop.fuzzywuzzy.FuzzySearch


abstract class BaseParser {

    /**
     * Name that will be shown in Source Selection
     * **/
    open val name: String = ""

    /**
     * Name used to save the ShowResponse selected by user or by autoSearch
     * **/
    open val saveName: String = ""

    /**
     * The main URL of the Site
     * **/
    open val hostUrl: String = ""

    /**
     * override as `true` if the site **only** has NSFW media
     * **/
    open val isNSFW = false

    /**
     * mostly redundant for official app, But override if you want to add different languages
     * **/
    open val language = "English"

    /**
     *  Search for Anime/Manga/Novel, returns a List of Responses
     *
     *  use `encode(query)` to encode the query for making requests
     * **/
    abstract suspend fun search(query: String): List<ShowResponse>

    /**
     * The function app uses to auto find the anime/manga using Media data provided by anilist
     *
     * Isn't necessary to override, but recommended, if you want to improve auto search results
     * **/
    open suspend fun autoSearch(mediaObj: Media): ShowResponse? {
        var response: ShowResponse? = null//loadSavedShowResponse(mediaObj.id)
        if (response != null) {
            saveShowResponse(mediaObj.id, response, true)
        } else {
            setUserText("Searching : ${mediaObj.mainName()}")
            val results = search(mediaObj.mainName())
            val sortedResults = if (results.isNotEmpty()) {
                results.sortedByDescending { FuzzySearch.ratio(it.name.lowercase(), mediaObj.mainName().lowercase()) }
            } else {
                emptyList()
            }
            response = sortedResults.firstOrNull()

            if (response == null || FuzzySearch.ratio(response.name.lowercase(), mediaObj.mainName().lowercase()) < 100) {
                setUserText("Searching : ${mediaObj.nameRomaji}")
                val romajiResults = search(mediaObj.nameRomaji)
                val sortedRomajiResults = if (romajiResults.isNotEmpty()) {
                    romajiResults.sortedByDescending { FuzzySearch.ratio(it.name.lowercase(), mediaObj.nameRomaji.lowercase()) }
                } else {
                    emptyList()
                }
                val closestRomaji = sortedRomajiResults.firstOrNull()
                logger("Closest match from RomajiResults: ${closestRomaji?.name ?: "None"}")

                response = if (response == null) {
                    logger("No exact match found in results. Using closest match from RomajiResults.")
                    closestRomaji
                } else {
                    val romajiRatio = FuzzySearch.ratio(closestRomaji?.name?.lowercase() ?: "", mediaObj.nameRomaji.lowercase())
                    val mainNameRatio = FuzzySearch.ratio(response.name.lowercase(), mediaObj.mainName().lowercase())
                    logger("Fuzzy ratio for closest match in results: $mainNameRatio for ${response.name.lowercase()}")
                    logger("Fuzzy ratio for closest match in RomajiResults: $romajiRatio for ${closestRomaji?.name?.lowercase() ?: "None"}")

                    if (romajiRatio > mainNameRatio) {
                        logger("RomajiResults has a closer match. Replacing response.")
                        closestRomaji
                    } else {
                        logger("Results has a closer or equal match. Keeping existing response.")
                        response
                    }
                }

            }
            saveShowResponse(mediaObj.id, response)
        }
        return response
    }


    /**
     * Used to get an existing Search Response which was selected by the user.
     * **/
    open suspend fun loadSavedShowResponse(mediaId: Int): ShowResponse? {
        checkIfVariablesAreEmpty()
        return loadData("${saveName}_$mediaId")
    }

    /**
     * Used to save Shows Response using `saveName`.
     * **/
    open fun saveShowResponse(mediaId: Int, response: ShowResponse?, selected: Boolean = false) {
        if (response != null) {
            checkIfVariablesAreEmpty()
            setUserText("${if (selected) currContext()!!.getString(R.string.selected) else currContext()!!.getString(R.string.found)} : ${response.name}")
            saveData("${saveName}_$mediaId", response)
        }
    }

    fun checkIfVariablesAreEmpty() {
        if (hostUrl.isEmpty()) throw UninitializedPropertyAccessException("Please provide a `hostUrl` for the Parser")
        if (name.isEmpty()) throw UninitializedPropertyAccessException("Please provide a `name` for the Parser")
        if (saveName.isEmpty()) throw UninitializedPropertyAccessException("Please provide a `saveName` for the Parser")
    }

    open var showUserText = ""
    open var showUserTextListener: ((String) -> Unit)? = null

    /**
     * Used to show messages & errors to the User, a useful way to convey what's currently happening or what was done.
     * **/
    fun setUserText(string: String) {
        showUserText = string
        showUserTextListener?.invoke(showUserText)
    }

    fun encode(input: String): String = URLEncoder.encode(input, "utf-8").replace("+", "%20")
    fun decode(input: String): String = URLDecoder.decode(input, "utf-8")

    val defaultImage = "https://s4.anilist.co/file/anilistcdn/media/manga/cover/medium/default.jpg"
}


/**
 * A single show which contains some episodes/chapters which is sent by the site using their search function.
 *
 * You might wanna include `otherNames` & `total` too, to further improve user experience.
 *
 * You can also store a Map of Strings if you want to save some extra data.
 * **/
data class ShowResponse(
    val name: String,
    val link: String,
    val coverUrl: FileUrl,

    //would be Useful for custom search, ig
    val otherNames: List<String> = listOf(),

    //Total number of Episodes/Chapters in the show.
    val total: Int? = null,

    //In case you want to sent some extra data
    val extra : Map<String,String>?=null,

    //SAnime object from Aniyomi
    val sAnime: SAnime? = null,

    //SManga object from Aniyomi
    val sManga: SManga? = null
) : Serializable {
    constructor(name: String, link: String, coverUrl: String, otherNames: List<String> = listOf(), total: Int? = null, extra: Map<String, String>?=null)
            : this(name, link, FileUrl(coverUrl), otherNames, total, extra)

    constructor(name: String, link: String, coverUrl: String, otherNames: List<String> = listOf(), total: Int? = null)
            : this(name, link, FileUrl(coverUrl), otherNames, total)

    constructor(name: String, link: String, coverUrl: String, otherNames: List<String> = listOf())
            : this(name, link, FileUrl(coverUrl), otherNames)

    constructor(name: String, link: String, coverUrl: String)
            : this(name, link, FileUrl(coverUrl))

    constructor(name: String, link: String, coverUrl: String, sAnime: SAnime)
            : this(name, link, FileUrl(coverUrl), sAnime = sAnime)

    constructor(name: String, link: String, coverUrl: String, sManga: SManga)
            : this(name, link, FileUrl(coverUrl), sManga = sManga)
}


