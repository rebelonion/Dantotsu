package ani.dantotsu.others.imagesearch

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ani.dantotsu.client
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream

class ImageSearchViewModel : ViewModel() {
    val searchResultLiveData: MutableLiveData<SearchResult> = MutableLiveData()

    private val url = "https://api.trace.moe/search?cutBorders&anilistInfo"

    suspend fun analyzeImage(inputStream: InputStream) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "image.jpg",
                inputStream.readBytes().toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val res = try {
            client.post(url, requestBody = requestBody).parsed<SearchResult>()
        } catch (e: Exception) {
            SearchResult(error = e.message)
        }
        searchResultLiveData.postValue(res)
    }

    fun clearResults() {
        searchResultLiveData.postValue(SearchResult())
    }

    @Serializable
    data class SearchResult(
        val frameCount: Long? = null,
        val error: String? = null,
        val result: List<ImageResult>? = null
    )

    @Serializable
    data class ImageResult(
        val anilist: AnilistData? = null,
        val filename: String? = null,
        @SerialName("episode") val rawEpisode: JsonElement? = null,
        val from: Double? = null,
        val to: Double? = null,
        val similarity: Double? = null,
        val video: String? = null,
        val image: String? = null
    ) {
        val episode: String?
            get() = rawEpisode?.toString()

        override fun toString(): String {
            return "$image & $video"
        }
    }

    @Serializable
    data class AnilistData(
        val id: Long? = null,
        val idMal: Long? = null,
        val title: Title? = null,
        val synonyms: List<String>? = null,
        val isAdult: Boolean? = null
    )

    @Serializable
    data class Title(
        val native: String? = null,
        val romaji: String? = null,
        val english: String? = null
    )
}