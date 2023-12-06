package ani.dantotsu.download

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.Serializable

class DownloadsManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("downloads_pref", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val downloadsList = loadDownloads().toMutableList()

    val mangaDownloads: List<Download>
        get() = downloadsList.filter { it.type == Download.Type.MANGA }
    val animeDownloads: List<Download>
        get() = downloadsList.filter { it.type == Download.Type.ANIME }
    val novelDownloads: List<Download>
        get() = downloadsList.filter { it.type == Download.Type.NOVEL }

    private fun saveDownloads() {
        val jsonString = gson.toJson(downloadsList)
        prefs.edit().putString("downloads_key", jsonString).apply()
    }

    private fun loadDownloads(): List<Download> {
        val jsonString = prefs.getString("downloads_key", null)
        return if (jsonString != null) {
            val type = object : TypeToken<List<Download>>() {}.type
            gson.fromJson(jsonString, type)
        } else {
            emptyList()
        }
    }

    fun addDownload(download: Download) {
        downloadsList.add(download)
        saveDownloads()
    }

    fun removeDownload(download: Download) {
        downloadsList.remove(download)
        removeDirectory(download)
        saveDownloads()
    }

    fun removeMedia(title: String, type: Download.Type) {
        val subDirectory = if (type == Download.Type.MANGA) {
            "Manga"
        } else if (type == Download.Type.ANIME) {
            "Anime"
        } else {
            "Novel"
        }
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
        downloadsList.removeAll { it.title == title }
        saveDownloads()
    }

    private fun cleanDownloads() {
        cleanDownload(Download.Type.MANGA)
        cleanDownload(Download.Type.ANIME)
        cleanDownload(Download.Type.NOVEL)
    }

    private fun cleanDownload(type: Download.Type) {
        // remove all folders that are not in the downloads list
        val subDirectory = if (type == Download.Type.MANGA) {
            "Manga"
        } else if (type == Download.Type.ANIME) {
            "Anime"
        } else {
            "Novel"
        }
        val directory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/$subDirectory"
        )
        val downloadsSubList = if (type == Download.Type.MANGA) {
            mangaDownloads
        } else if (type == Download.Type.ANIME) {
            animeDownloads
        } else {
            novelDownloads
        }
        if (directory.exists()) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (!downloadsSubList.any { it.title == file.name }) {
                        val deleted = file.deleteRecursively()
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

    fun saveDownloadsListToJSONFileInDownloadsFolder(downloadsList: List<Download>)  //for debugging
    {
        val jsonString = gson.toJson(downloadsList)
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
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

    fun queryDownload(download: Download): Boolean {
        return downloadsList.contains(download)
    }

    private fun removeDirectory(download: Download) {
        val directory = if (download.type == Download.Type.MANGA) {
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/Manga/${download.title}/${download.chapter}"
            )
        } else if (download.type == Download.Type.ANIME) {
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/Anime/${download.title}/${download.chapter}"
            )
        } else {
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/Novel/${download.title}/${download.chapter}"
            )
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

    fun exportDownloads(download: Download) { //copies to the downloads folder available to the user
        val directory = if (download.type == Download.Type.MANGA) {
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/Manga/${download.title}/${download.chapter}"
            )
        } else if (download.type == Download.Type.ANIME) {
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/Anime/${download.title}/${download.chapter}"
            )
        } else {
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/Novel/${download.title}/${download.chapter}"
            )
        }
        val destination = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Dantotsu/${download.title}/${download.chapter}"
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

    fun purgeDownloads(type: Download.Type) {
        val directory = if (type == Download.Type.MANGA) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Dantotsu/Manga")
        } else if (type == Download.Type.ANIME) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Dantotsu/Anime")
        } else {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Dantotsu/Novel")
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
    }

}

data class Download(val title: String, val chapter: String, val type: Type) : Serializable {
    enum class Type {
        MANGA,
        ANIME,
        NOVEL
    }
}
