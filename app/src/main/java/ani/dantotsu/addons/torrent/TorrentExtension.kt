package ani.dantotsu.addons.torrent

import android.graphics.drawable.Drawable

sealed class TorrentExtension {
    abstract val name: String
    abstract val pkgName: String
    abstract val versionName: String
    abstract val versionCode: Long

    data class Installed(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        val extension: TorrentExtensionApi,
        val icon: Drawable?,
        val hasUpdate: Boolean = false,
    ) : TorrentExtension()

}