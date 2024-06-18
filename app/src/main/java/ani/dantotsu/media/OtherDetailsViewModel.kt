package ani.dantotsu.media

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ani.dantotsu.connections.anilist.Anilist
import java.text.DateFormat
import java.util.Date

class OtherDetailsViewModel : ViewModel() {
    private val character: MutableLiveData<Character> = MutableLiveData(null)
    fun getCharacter(): LiveData<Character> = character
    suspend fun loadCharacter(m: Character) {
        if (character.value == null) character.postValue(Anilist.query.getCharacterDetails(m))
    }

    private val studio: MutableLiveData<Studio> = MutableLiveData(null)
    fun getStudio(): LiveData<Studio> = studio
    suspend fun loadStudio(m: Studio) {
        if (studio.value == null) studio.postValue(Anilist.query.getStudioDetails(m))
    }

    private val author: MutableLiveData<Author> = MutableLiveData(null)
    fun getAuthor(): LiveData<Author> = author
    suspend fun loadAuthor(m: Author) {
        if (author.value == null) author.postValue(Anilist.query.getAuthorDetails(m))
    }

    private var cachedAllCalendarData: Map<String, MutableList<Media>>? = null
    private var cachedLibraryCalendarData: Map<String, MutableList<Media>>? = null
    private val calendar: MutableLiveData<Map<String, MutableList<Media>>> = MutableLiveData(null)
    fun getCalendar(): LiveData<Map<String, MutableList<Media>>> = calendar
    suspend fun loadCalendar(showOnlyLibrary: Boolean = false) {
        if (cachedAllCalendarData == null || cachedLibraryCalendarData == null) {
            val curr = System.currentTimeMillis() / 1000
            val res = Anilist.query.recentlyUpdated(curr - 86400, curr + (86400 * 6))
            val df = DateFormat.getDateInstance(DateFormat.FULL)
            val allMap = mutableMapOf<String, MutableList<Media>>()
            val libraryMap = mutableMapOf<String, MutableList<Media>>()
            val idMap = mutableMapOf<String, MutableList<Int>>()

            val userId = Anilist.userid ?: 0
            val userLibrary = Anilist.query.getMediaLists(true, userId)
            val libraryMediaIds = userLibrary.flatMap { it.value }.map { it.id }

            res.forEach {
                val v = it.relation?.split(",")?.map { i -> i.toLong() }!!
                val dateInfo = df.format(Date(v[1] * 1000))
                val list = allMap.getOrPut(dateInfo) { mutableListOf() }
                val libraryList = if (libraryMediaIds.contains(it.id)) {
                    libraryMap.getOrPut(dateInfo) { mutableListOf() }
                } else {
                    null
                }
                val idList = idMap.getOrPut(dateInfo) { mutableListOf() }
                it.relation = "Episode ${v[0]}"
                if (!idList.contains(it.id)) {
                    idList.add(it.id)
                    list.add(it)
                    libraryList?.add(it)
                }
            }

            cachedAllCalendarData = allMap
            cachedLibraryCalendarData = libraryMap
        }

        val cacheToUse: Map<String, MutableList<Media>> = if (showOnlyLibrary) {
            cachedLibraryCalendarData ?: emptyMap()
        } else {
            cachedAllCalendarData ?: emptyMap()
        }
        calendar.postValue(cacheToUse)
    }
}