package ani.dantotsu.addons

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import ani.dantotsu.addons.torrent.TorrentAddonApi
import ani.dantotsu.addons.torrent.TorrentAddon
import ani.dantotsu.addons.torrent.TorrentLoadResult
import ani.dantotsu.util.Logger
import dalvik.system.PathClassLoader
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.util.system.getApplicationIcon

class AddonLoader {
    companion object {
        fun loadExtension(
            context: Context,
            packageName: String,
            className: String,
            type: AddonType
        ): LoadResult? {
            val pkgManager = context.packageManager

            val installedPkgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pkgManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(ExtensionLoader.PACKAGE_FLAGS.toLong()))
            } else {
                pkgManager.getInstalledPackages(ExtensionLoader.PACKAGE_FLAGS)
            }

            val extPkgs = installedPkgs.filter {
                isPackageAnExtension(
                    packageName,
                    it
                )
            }

            if (extPkgs.isEmpty() || extPkgs.size > 1) return null

            val pkgName = extPkgs.first().packageName
            val pkgInfo = extPkgs.first()

            val appInfo = try {
                pkgManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
            } catch (error: PackageManager.NameNotFoundException) {
                // Unlikely, but the package may have been uninstalled at this point
                Logger.log(error)
                return null
            }

            val extName = pkgManager.getApplicationLabel(appInfo).toString().substringAfter("Dantotsu: ")
            val versionName = pkgInfo.versionName
            val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)

            if (versionName.isNullOrEmpty()) {
                Logger.log("Missing versionName for extension $extName")
                return null
            }
            val classLoader = PathClassLoader(appInfo.sourceDir, appInfo.nativeLibraryDir, context.classLoader)
            val loadedClass = try {
                Class.forName(className, false, classLoader)
            } catch (e: Throwable) {
                Logger.log("Extension load error: $extName ($className)")
                return null
            }
            val instance = loadedClass.getDeclaredConstructor().newInstance()

            return when (type) {
                AddonType.TORRENT -> {
                    val extension = instance as? TorrentAddonApi ?: return null
                    TorrentLoadResult.Success(
                        TorrentAddon.Installed(
                            name = extName,
                            pkgName = pkgName,
                            versionName = versionName,
                            versionCode = versionCode,
                            extension = extension,
                            icon = context.getApplicationIcon(pkgName),
                        )
                    )
                }
                AddonType.DOWNLOAD -> null
            }

        }

        private fun isPackageAnExtension(type: String, pkgInfo: PackageInfo): Boolean {
            return pkgInfo.packageName.equals(type)
        }

        enum class AddonType {
            TORRENT,
            DOWNLOAD,
        }
    }

}