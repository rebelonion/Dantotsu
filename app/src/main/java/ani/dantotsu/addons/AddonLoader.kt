package ani.dantotsu.addons

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import ani.dantotsu.addons.download.DownloadAddon
import ani.dantotsu.addons.download.DownloadAddonApi
import ani.dantotsu.addons.download.DownloadLoadResult
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

            if (extPkgs.isEmpty()) return null
            if (extPkgs.size > 1) throw IllegalStateException("Multiple extensions with the same package name found")

            val pkgName = extPkgs.first().packageName
            val pkgInfo = extPkgs.first()

            val appInfo = try {
                pkgManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
            } catch (error: PackageManager.NameNotFoundException) {
                // Unlikely, but the package may have been uninstalled at this point
                Logger.log(error)
                throw error
            }

            val extName = pkgManager.getApplicationLabel(appInfo).toString().substringAfter("Dantotsu: ")
            val versionName = pkgInfo.versionName
            val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)

            if (versionName.isNullOrEmpty()) {
                Logger.log("Missing versionName for extension $extName")
                throw IllegalStateException("Missing versionName for extension $extName")
            }
            val classLoader = PathClassLoader(appInfo.sourceDir, appInfo.nativeLibraryDir, context.classLoader)
            val loadedClass = try {
                Class.forName(className, false, classLoader)
            } catch (e: Throwable) {
                Logger.log("Extension load error: $extName ($className)")
                Logger.log(e)
                throw e
            }
            val instance = loadedClass.getDeclaredConstructor().newInstance()

            return when (type) {
                AddonType.TORRENT -> {
                    val extension = instance as? TorrentAddonApi ?: throw IllegalStateException("Extension is not a TorrentAddonApi")
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
                AddonType.DOWNLOAD -> {
                    val extension = instance as? DownloadAddonApi ?: throw IllegalStateException("Extension is not a DownloadAddonApi")
                    DownloadLoadResult.Success(
                        DownloadAddon.Installed(
                            name = extName,
                            pkgName = pkgName,
                            versionName = versionName,
                            versionCode = versionCode,
                            extension = extension,
                            icon = context.getApplicationIcon(pkgName),
                        )
                    )
                }
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