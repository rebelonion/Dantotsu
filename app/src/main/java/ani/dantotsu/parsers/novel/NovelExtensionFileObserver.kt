package ani.dantotsu.parsers.novel

import android.os.FileObserver
import ani.dantotsu.parsers.novel.FileObserver.fileObserver
import ani.dantotsu.util.Logger
import java.io.File


class NovelExtensionFileObserver(private val listener: Listener, private val path: String) :
    FileObserver(path, CREATE or DELETE or MOVED_FROM or MOVED_TO or MODIFY) {

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
        Logger.log("Event: $event")
        if (file == null) return

        val fullPath = File(path, file)

        when (event) {
            CREATE -> {
                Logger.log("File created: $fullPath")
                listener.onExtensionFileCreated(fullPath)
            }

            DELETE -> {
                Logger.log("File deleted: $fullPath")
                listener.onExtensionFileDeleted(fullPath)
            }

            MODIFY -> {
                Logger.log("File modified: $fullPath")
                listener.onExtensionFileModified(fullPath)
            }
        }
    }

    interface Listener {
        fun onExtensionFileCreated(file: File)
        fun onExtensionFileDeleted(file: File)
        fun onExtensionFileModified(file: File)
    }
}

object FileObserver {
    var fileObserver: FileObserver? = null
}