package ani.dantotsu.download

import android.content.Context
import android.os.Environment
import android.widget.Toast
import ani.dantotsu.media.MediaType
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
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

    fun removeDownload(downloadedType: DownloadedType) {
        downloadsList.remove(downloadedType)
        removeDirectory(downloadedType)
        saveDownloads()
    }

    fun removeMedia(title: String, type: MediaType) {
        val subDirectory = type.asText()
        val directory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/$subDirectory/$title"
        )
        if (directory.exists()) {
            val deleted = directory.deleteRecursively()
            if (deleted) {
                Toast.makeText(context, "Successfully deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to delete directory", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Directory does not exist", Toast.LENGTH_SHORT).show()
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
        val subDirectory = type.asText()
        val directory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/$subDirectory"
        )
        val downloadsSubLists = when (type) {
            MediaType.MANGA -> mangaDownloadedTypes
            MediaType.ANIME -> animeDownloadedTypes
            else -> novelDownloadedTypes
        }
        if (directory.exists()) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (!downloadsSubLists.any { it.title == file.name }) {
                        file.deleteRecursively()
                    }
                }
            }
        }
        //now remove all downloads that do not have a folder
        val iterator = downloadsList.iterator()
        while (iterator.hasNext()) {
            val download = iterator.next()
            val downloadDir = File(directory, download.title)
            if ((!downloadDir.exists() && download.type == type) || download.title.isBlank()) {
                iterator.remove()
            }
        }
    }

    fun saveDownloadsListToJSONFileInDownloadsFolder(downloadsList: List<DownloadedType>)  //for debugging
    {
        val jsonString = gson.toJson(downloadsList)
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/downloads.json"
        )
        if (file.parentFile?.exists() == false) {
            file.parentFile?.mkdirs()
        }
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeText(jsonString)
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
        val directory = when (downloadedType.type) {
            MediaType.MANGA -> {
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "Dantotsu/Manga/${downloadedType.title}/${downloadedType.chapter}"
                )
            }
            MediaType.ANIME -> {
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "Dantotsu/Anime/${downloadedType.title}/${downloadedType.chapter}"
                )
            }
            else -> {
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "Dantotsu/Novel/${downloadedType.title}/${downloadedType.chapter}"
                )
            }
        }

        // Check if the directory exists and delete it recursively
        if (directory.exists()) {
            val deleted = directory.deleteRecursively()
            if (deleted) {
                Toast.makeText(context, "Successfully deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to delete directory", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Directory does not exist", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportDownloads(downloadedType: DownloadedType) { //copies to the downloads folder available to the user
        val directory = when (downloadedType.type) {
            MediaType.MANGA -> {
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "Dantotsu/Manga/${downloadedType.title}/${downloadedType.chapter}"
                )
            }
            MediaType.ANIME -> {
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "Dantotsu/Anime/${downloadedType.title}/${downloadedType.chapter}"
                )
            }
            else -> {
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "Dantotsu/Novel/${downloadedType.title}/${downloadedType.chapter}"
                )
            }
        }
        val destination = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/${downloadedType.title}/${downloadedType.chapter}"
        )
        if (directory.exists()) {
            val copied = directory.copyRecursively(destination, true)
            if (copied) {
                Toast.makeText(context, "Successfully copied", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to copy directory", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Directory does not exist", Toast.LENGTH_SHORT).show()
        }
    }

    fun purgeDownloads(type: MediaType) {
        val directory = when (type) {
            MediaType.MANGA -> {
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Dantotsu/Manga")
            }
            MediaType.ANIME -> {
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Dantotsu/Anime")
            }
            else -> {
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Dantotsu/Novel")
            }
        }
        if (directory.exists()) {
            val deleted = directory.deleteRecursively()
            if (deleted) {
                Toast.makeText(context, "Successfully deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to delete directory", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Directory does not exist", Toast.LENGTH_SHORT).show()
        }

        downloadsList.removeAll { it.type == type }
        saveDownloads()
    }

    companion object {
        const val novelLocation = "Dantotsu/Novel"
        const val mangaLocation = "Dantotsu/Manga"
        const val animeLocation = "Dantotsu/Anime"

        fun getDirectory(
            context: Context,
            type: MediaType,
            title: String,
            chapter: String? = null
        ): File {
            return when (type) {
                MediaType.MANGA -> {
                    if (chapter != null) {
                        File(
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                            "$mangaLocation/$title/$chapter"
                        )
                    } else {
                        File(
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                            "$mangaLocation/$title"
                        )
                    }
                }
                MediaType.ANIME -> {
                    if (chapter != null) {
                        File(
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                            "$animeLocation/$title/$chapter"
                        )
                    } else {
                        File(
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                            "$animeLocation/$title"
                        )
                    }
                }
                else -> {
                    if (chapter != null) {
                        File(
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                            "$novelLocation/$title/$chapter"
                        )
                    } else {
                        File(
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                            "$novelLocation/$title"
                        )
                    }
                }
            }
        }
    }
}

data class DownloadedType(val title: String, val chapter: String, val type: MediaType) : Serializable
