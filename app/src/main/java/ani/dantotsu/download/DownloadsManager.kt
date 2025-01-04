package ani.dantotsu.download

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import ani.dantotsu.download.DownloadCompat.Companion.removeDownloadCompat
import ani.dantotsu.download.DownloadCompat.Companion.removeMediaCompat
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaType
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import com.anggrayudi.storage.callback.FolderCallback
import com.anggrayudi.storage.file.deleteRecursively
import com.anggrayudi.storage.file.moveFolderTo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.xdrop.fuzzywuzzy.FuzzySearch
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

    fun removeDownload(
        downloadedType: DownloadedType,
        toast: Boolean = true,
        onFinished: () -> Unit
    ) {
        removeDownloadCompat(context, downloadedType, toast)
        downloadsList.removeAll { it.titleName == downloadedType.titleName && it.chapterName == downloadedType.chapterName }
        CoroutineScope(Dispatchers.IO).launch {
            removeDirectory(downloadedType, toast)
            withContext(Dispatchers.Main) {
                onFinished()
            }
        }
        saveDownloads()
    }

    fun removeMedia(title: String, type: MediaType) {
        removeMediaCompat(context, title, type)
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
                downloadsList.removeAll { it.titleName == title && it.type == MediaType.MANGA }
            }

            MediaType.ANIME -> {
                downloadsList.removeAll { it.titleName == title && it.type == MediaType.ANIME }
            }

            MediaType.NOVEL -> {
                downloadsList.removeAll { it.titleName == title && it.type == MediaType.NOVEL }
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
                if (!downloadsSubLists.any { it.titleName == file.name }) {
                    file.deleteRecursively(context, false)
                }
            }
        }
        //now remove all downloads that do not have a folder
        val iterator = downloadsList.iterator()
        while (iterator.hasNext()) {
            val download = iterator.next()
            val downloadDir = directory?.findFolder(download.titleName)
            if ((downloadDir?.exists() == false && download.type == type) || download.titleName.isBlank()) {
                iterator.remove()
            }
        }
    }

    fun moveDownloadsDir(
        context: Context,
        oldUri: Uri,
        newUri: Uri,
        finished: (Boolean, String) -> Unit
    ) {
        if (oldUri == newUri) {
            Logger.log("Source and destination are the same")
            finished(false, "Source and destination are the same")
            return
        }
        if (oldUri == Uri.EMPTY) {
            Logger.log("Old Uri is empty")
            finished(true, "Old Uri is empty")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val oldBase =
                    DocumentFile.fromTreeUri(context, oldUri) ?: throw Exception("Old base is null")
                val newBase =
                    DocumentFile.fromTreeUri(context, newUri) ?: throw Exception("New base is null")
                val folder =
                    oldBase.findFolder(BASE_LOCATION) ?: throw Exception("Base folder not found")
                folder.moveFolderTo(context, newBase, false, BASE_LOCATION, object :
                    FolderCallback() {
                    override fun onFailed(errorCode: ErrorCode) {
                        when (errorCode) {
                            ErrorCode.CANCELED -> finished(false, "Move canceled")
                            ErrorCode.CANNOT_CREATE_FILE_IN_TARGET -> finished(
                                false,
                                "Cannot create file in target"
                            )

                            ErrorCode.INVALID_TARGET_FOLDER -> finished(
                                true,
                                "Invalid target folder"
                            ) // seems to still work
                            ErrorCode.NO_SPACE_LEFT_ON_TARGET_PATH -> finished(
                                false,
                                "No space left on target path"
                            )

                            ErrorCode.UNKNOWN_IO_ERROR -> finished(false, "Unknown IO error")
                            ErrorCode.SOURCE_FOLDER_NOT_FOUND -> finished(
                                false,
                                "Source folder not found"
                            )

                            ErrorCode.STORAGE_PERMISSION_DENIED -> finished(
                                false,
                                "Storage permission denied"
                            )

                            ErrorCode.TARGET_FOLDER_CANNOT_HAVE_SAME_PATH_WITH_SOURCE_FOLDER -> finished(
                                false,
                                "Target folder cannot have same path with source folder"
                            )

                            else -> finished(false, "Failed to move downloads: $errorCode")
                        }
                        Logger.log("Failed to move downloads: $errorCode")
                        super.onFailed(errorCode)
                    }

                    override fun onCompleted(result: Result) {
                        finished(true, "Successfully moved downloads")
                        super.onCompleted(result)
                    }

                })

            } catch (e: Exception) {
                snackString("Error: ${e.message}")
                Logger.log("Failed to move downloads: ${e.message}")
                Logger.log(e)
                Logger.log("oldUri: $oldUri, newUri: $newUri")
                finished(false, "Failed to move downloads: ${e.message}")
                return@launch
            }
        }
    }

    fun queryDownload(downloadedType: DownloadedType): Boolean {
        return downloadsList.contains(downloadedType)
    }

    fun queryDownload(title: String, chapter: String, type: MediaType? = null): Boolean {
        return if (type == null) {
            downloadsList.any { it.titleName == title && it.chapterName == chapter }
        } else {
            downloadsList.any { it.titleName == title && it.chapterName == chapter && it.type == type }
        }
    }

    private fun removeDirectory(downloadedType: DownloadedType, toast: Boolean) {
        val baseDirectory = getBaseDirectory(context, downloadedType.type)
        val directory =
            baseDirectory?.findFolder(downloadedType.titleName)
                ?.findFolder(downloadedType.chapterName)
        downloadsList.removeAll { it.titleName == downloadedType.titleName && it.chapterName == downloadedType.chapterName }
        // Check if the directory exists and delete it recursively
        if (directory?.exists() == true) {
            val deleted = directory.deleteRecursively(context, false)
            if (deleted) {
                if (toast) snackString("Successfully deleted")
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


        /**
         * Get and create a base directory for the given type
         * @param context the context
         * @param type the type of media
         * @return the base directory
         */
        @Synchronized
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
        @Synchronized
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

        fun getDirSize(
            context: Context,
            type: MediaType,
            title: String,
            chapter: String? = null
        ): Long {
            val directory = getSubDirectory(context, type, false, title, chapter) ?: return 0
            var size = 0L
            directory.listFiles().forEach {
                size += it.length()
            }
            return size
        }

        fun addNoMedia(context: Context) {
            val baseDirectory = getBaseDirectory(context) ?: return
            if (baseDirectory.findFile(".nomedia") == null) {
                baseDirectory.createFile("application/octet-stream", ".nomedia")
            }
        }

        @Synchronized
        private fun getBaseDirectory(context: Context): DocumentFile? {
            val baseDirectory = Uri.parse(PrefManager.getVal<String>(PrefName.DownloadsDir))
            if (baseDirectory == Uri.EMPTY) return null
            val base = DocumentFile.fromTreeUri(context, baseDirectory) ?: return null
            return base.findOrCreateFolder(BASE_LOCATION, false)
        }

        private val lock = Any()

        private fun DocumentFile.findOrCreateFolder(
            name: String, overwrite: Boolean
        ): DocumentFile? {
            val validName = name.findValidName()
            synchronized(lock) {
                return if (overwrite) {
                    findFolder(validName)?.delete()
                    createDirectory(validName)
                } else {
                    val folder = findFolder(validName)
                    folder ?: createDirectory(validName)
                }
            }
        }

        private fun DocumentFile.findFolder(name: String): DocumentFile? =
            listFiles().find { it.name == name && it.isDirectory }

        private const val RATIO_THRESHOLD = 95
        fun Media.compareName(name: String): Boolean {
            val mainName = mainName().findValidName().lowercase()
            val ratio = FuzzySearch.ratio(mainName, name.lowercase())
            return ratio > RATIO_THRESHOLD
        }

        fun String.compareName(name: String): Boolean {
            val mainName = findValidName().lowercase()
            val compareName = name.findValidName().lowercase()
            val ratio = FuzzySearch.ratio(mainName, compareName)
            return ratio > RATIO_THRESHOLD
        }
    }
}

private const val RESERVED_CHARS = "|\\?*<\":>+[]/'"
fun String?.findValidName(): String {
    return this?.replace("/", "_")?.filterNot { RESERVED_CHARS.contains(it) } ?: ""
}

data class DownloadedType(
    private val pTitle: String?,
    private val pChapter: String?,
    val type: MediaType,
    @Deprecated("use pTitle instead")
    private val title: String? = null,
    @Deprecated("use pChapter instead")
    private val chapter: String? = null,
    val scanlator: String = "Unknown"
) : Serializable {
    val titleName: String
        get() = title ?: pTitle.findValidName()
    val chapterName: String
        get() = chapter ?: pChapter.findValidName()
    val uniqueName: String
        get() = "$chapterName-${scanlator}"
}
