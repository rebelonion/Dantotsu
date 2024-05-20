package ani.dantotsu.addons.download

import android.graphics.drawable.Drawable
import ani.dantotsu.addons.Addon

sealed class DownloadAddon : Addon() {

    data class Installed(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        val extension: DownloadAddonApiV2,
        val icon: Drawable?,
        val hasUpdate: Boolean = false,
    ) : Addon.Installed(name, pkgName, versionName, versionCode)

}