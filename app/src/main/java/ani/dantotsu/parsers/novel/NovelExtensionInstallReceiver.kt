package ani.dantotsu.parsers.novel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.FileObserver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import ani.dantotsu.parsers.novel.FileObserver.fileObserver
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.extension.anime.model.AnimeLoadResult
import eu.kanade.tachiyomi.extension.anime.util.AnimeExtensionLoader
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import logcat.LogPriority
import tachiyomi.core.util.lang.launchNow
import tachiyomi.core.util.system.logcat
import java.io.File
import java.lang.Exception


class NovelExtensionFileObserver(private val listener: Listener, private val path: String) : FileObserver(path, CREATE or DELETE or MOVED_FROM or MOVED_TO or MODIFY) {

    init {
        fileObserver = this
    }
    /**
     * Starts observing the file changes in the directory.
     */
    fun register() {
        startWatching()
    }


    override fun onEvent(event: Int, file: String?) {
        Log.e("NovelExtensionFileObserver", "Event: $event")
        if (file == null) return

        val fullPath = File(path, file)

        when (event) {
            CREATE -> {
                Log.e("NovelExtensionFileObserver", "File created: $fullPath")
                listener.onExtensionFileCreated(fullPath)
            }
            DELETE -> {
                Log.e("NovelExtensionFileObserver", "File deleted: $fullPath")
                listener.onExtensionFileDeleted(fullPath)
            }
            MODIFY -> {
                Log.e("NovelExtensionFileObserver", "File modified: $fullPath")
                listener.onExtensionFileModified(fullPath)
            }
        }
    }

    /**
     * Loads the extension from the file.
     *
     * @param file The file name of the extension.
     */
    //private suspend fun loadExtensionFromFile(file: String): String {
    //    return file
    //}

    interface Listener {
        fun onExtensionFileCreated(file: File)
        fun onExtensionFileDeleted(file: File)
        fun onExtensionFileModified(file: File)
    }
}

object FileObserver {
    var fileObserver: FileObserver? = null
}