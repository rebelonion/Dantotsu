package ani.dantotsu.media.manga

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImageData(
    val page: Page,
    val source: HttpSource,
){
    suspend fun fetchAndProcessImage(page: Page, httpSource: HttpSource): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch the image
                val response = httpSource.getImage(page)

                // Convert the Response to an InputStream
                val inputStream = response.body?.byteStream()

                // Convert InputStream to Bitmap
                val bitmap = BitmapFactory.decodeStream(inputStream)

                inputStream?.close()

                return@withContext bitmap
            } catch (e: Exception) {
                // Handle any exceptions
                println("An error occurred: ${e.message}")
                return@withContext null
            }
        }
    }
}

class MangaCache() {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024 / 2).toInt()
    private val cache = LruCache<String, ImageData>(maxMemory)

    @Synchronized
    fun put(key: String, imageDate: ImageData) {
        cache.put(key, imageDate)
    }

    @Synchronized
    fun get(key: String): ImageData? = cache.get(key)

    @Synchronized
    fun remove(key: String) {
        cache.remove(key)
    }

    @Synchronized
    fun clear() {
        cache.evictAll()
    }

    fun size(): Int = cache.size()


}
