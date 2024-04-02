package ani.dantotsu.download

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import ani.dantotsu.download.DownloadsManager.Companion.findValidName
import ani.dantotsu.media.MediaType
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import com.anggrayudi.storage.file.deleteRecursively
import com.anggrayudi.storage.file.findFolder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

class DownloadsManager(private val context: Context) {
    private val gson = Gson()
    private val downloadsList = loadDownloads().toMutableList()

    val mangaDownloadedTypes: List<DownloadedType>
        get() = downloadsList.filter { it.type == MediaType.MANGA }
    val animeDownloadedTypes: List<DownloadedType>
        get() = downloadsList.filter { it.type == MediaType.ANIME }
    val novelDownloadedTypes: List<DownloadedType>
        get() = downloadsList.filter { it.type == MediaType.NOVEL }

    private fun saveDownloads() {
        val jsonString = gson.toJson(downloadsList)
        PrefManager.setVal(PrefName.DownloadsKeys, jsonString)
    }

    private fun loadDownloads(): List<DownloadedType> {
        val jsonString = PrefManager.getVal(PrefName.DownloadsKeys, null as String?)
        return if (jsonString != null) {
            val type = object : TypeToken<List<DownloadedType>>() {}.type
            gson.fromJson(jsonString, type)
        } else {
            emptyList()
        }
    }

    fun addDownload(downloadedType: DownloadedType) {
        downloadsList.add(downloadedType)
        saveDownloads()
    }

    fun removeDownload(downloadedType: DownloadedType, onFinished: () -> Unit) {
        downloadsList.remove(downloadedType)
        CoroutineScope(Dispatchers.IO).launch {
            removeDirectory(downloadedType)
            withContext(Dispatchers.Main) {
                onFinished()
            }
        }
        saveDownloads()
    }

    fun removeMedia(title: String, type: MediaType) {
        val baseDirectory = getBaseDirectory(context, type)
        val directory = baseDirectory?.findFolder(title)
        if (directory?.exists() == true) {
            val deleted = directory.deleteRecursively(context, false)
            if (deleted) {
                snackString("Successfully deleted")
            } else {
                snackString("Failed to delete directory")
            }
        } else {
            snackString("Directory does not exist")
            cleanDownloads()
        }
        when (type) {
            MediaType.MANGA -> {
                downloadsList.removeAll { it.title == title && it.type == MediaType.MANGA }
            }

            MediaType.ANIME -> {
                downloadsList.removeAll { it.title == title && it.type == MediaType.ANIME }
            }

            MediaType.NOVEL -> {
                downloadsList.removeAll { it.title == title && it.type == MediaType.NOVEL }
            }
        }
        saveDownloads()
    }

    private fun cleanDownloads() {
        cleanDownload(MediaType.MANGA)
        cleanDownload(MediaType.ANIME)
        cleanDownload(MediaType.NOVEL)
    }

    private fun cleanDownload(type: MediaType) {
        // remove all folders that are not in the downloads list
        val directory = getBaseDirectory(context, type)
        val downloadsSubLists = when (type) {
            MediaType.MANGA -> mangaDownloadedTypes
            MediaType.ANIME -> animeDownloadedTypes
            else -> novelDownloadedTypes
        }
        if (directory?.exists() == true && directory.isDirectory) {
            val files = directory.listFiles()
            for (file in files) {
                if (!downloadsSubLists.any { it.title == file.name }) {
                    file.deleteRecursively(context, false)
                }
            }
        }
        //now remove all downloads that do not have a folder
        val iterator = downloadsList.iterator()
        while (iterator.hasNext()) {
            val download = iterator.next()
            val downloadDir = directory?.findFolder(download.title)
            if ((downloadDir?.exists() == false && download.type == type) || download.title.isBlank()) {
                iterator.remove()
            }
        }
    }

    fun queryDownload(downloadedType: DownloadedType): Boolean {
        return downloadsList.contains(downloadedType)
    }

    fun queryDownload(title: String, chapter: String, type: MediaType? = null): Boolean {
        return if (type == null) {
            downloadsList.any { it.title == title && it.chapter == chapter }
        } else {
            downloadsList.any { it.title == title && it.chapter == chapter && it.type == type }
        }
    }

    private fun removeDirectory(downloadedType: DownloadedType) {
        val baseDirectory = getBaseDirectory(context, downloadedType.type)
        val directory =
            baseDirectory?.findFolder(downloadedType.title)?.findFolder(downloadedType.chapter)

        // Check if the directory exists and delete it recursively
        if (directory?.exists() == true) {
            val deleted = directory.deleteRecursively(context, false)
            if (deleted) {
                snackString("Successfully deleted")

            } else {
                snackString("Failed to delete directory")
            }
        } else {
            snackString("Directory does not exist")
        }
    }

    fun purgeDownloads(type: MediaType) {
        val directory = getBaseDirectory(context, type)
        if (directory?.exists() == true) {
            val deleted = directory.deleteRecursively(context, false)
            if (deleted) {
                snackString("Successfully deleted")
            } else {
                snackString("Failed to delete directory")
            }
        } else {
            snackString("Directory does not exist")
        }

        downloadsList.removeAll { it.type == type }
        saveDownloads()
    }

    companion object {
        private const val BASE_LOCATION = "Dantotsu"
        private const val MANGA_SUB_LOCATION = "Manga"
        private const val ANIME_SUB_LOCATION = "Anime"
        private const val NOVEL_SUB_LOCATION = "Novel"
        private const val RESERVED_CHARS = "|\\?*<\":>+[]/'"

        fun String?.findValidName(): String {
            return this?.filterNot { RESERVED_CHARS.contains(it) } ?: ""
        }

        /**
         * Get and create a base directory for the given type
         * @param context the context
         * @param type the type of media
         * @return the base directory
         */

        private fun getBaseDirectory(context: Context, type: MediaType): DocumentFile? {
            val baseDirectory = Uri.parse(PrefManager.getVal<String>(PrefName.DownloadsDir))
            if (baseDirectory == Uri.EMPTY) return null
            var base = DocumentFile.fromTreeUri(context, baseDirectory) ?: return null
            base = base.findOrCreateFolder(BASE_LOCATION, false) ?: return null
            return when (type) {
                MediaType.MANGA -> {
                    base.findOrCreateFolder(MANGA_SUB_LOCATION, false)
                }

                MediaType.ANIME -> {
                    base.findOrCreateFolder(ANIME_SUB_LOCATION, false)
                }

                else -> {
                    base.findOrCreateFolder(NOVEL_SUB_LOCATION, false)
                }
            }
        }

        /**
         * Get and create a subdirectory for the given type
         * @param context the context
         * @param type the type of media
         * @param title the title of the media
         * @param chapter the chapter of the media
         * @return the subdirectory
         */
        fun getSubDirectory(
            context: Context,
            type: MediaType,
            overwrite: Boolean,
            title: String,
            chapter: String? = null
        ): DocumentFile? {
            val baseDirectory = getBaseDirectory(context, type) ?: return null
            return if (chapter != null) {
                baseDirectory.findOrCreateFolder(title, false)
                    ?.findOrCreateFolder(chapter, overwrite)
            } else {
                baseDirectory.findOrCreateFolder(title, overwrite)
            }
        }

        fun getDirSize(context: Context, type: MediaType, title: String, chapter: String? = null): Long {
            val directory = getSubDirectory(context, type, false, title, chapter) ?: return 0
            var size = 0L
            directory.listFiles().forEach {
                size += it.length()
            }
            return size
        }

        private fun DocumentFile.findOrCreateFolder(
            name: String, overwrite: Boolean
        ): DocumentFile? {
            return if (overwrite) {
                findFolder(name.findValidName())?.delete()
                createDirectory(name.findValidName())
            } else {
                findFolder(name.findValidName()) ?: createDirectory(name.findValidName())
            }
        }

    }
}

data class DownloadedType(
    val pTitle: String, val pChapter: String, val type: MediaType
) : Serializable {
    val title: String
        get() = pTitle.findValidName()
    val chapter: String
        get() = pChapter.findValidName()
}
