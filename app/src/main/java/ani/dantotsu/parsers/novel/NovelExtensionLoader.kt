package ani.dantotsu.parsers.novel

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.os.Build
import android.util.Log
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.util.Logger
import ani.dantotsu.parsers.NovelInterface
import ani.dantotsu.snackString
import dalvik.system.PathClassLoader
import eu.kanade.tachiyomi.util.lang.Hash
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Locale

internal object NovelExtensionLoader {

    private const val officialSignature =
        "a3061edb369278749b8e8de810d440d38e96417bbd67bbdfc5d9d9ed475ce4a5"  //dan's key

    fun loadExtensions(context: Context): List<NovelLoadResult> {
        val installDir = context.getExternalFilesDir(null)?.absolutePath + "/extensions/novel/"
        val results = mutableListOf<NovelLoadResult>()
        //the number of files
        Logger.log("Loading extensions from $installDir")
        Logger.log(
            "Loading extensions from ${File(installDir).listFiles()?.size}"
        )
        File(installDir).setWritable(false)
        File(installDir).listFiles()?.forEach {
            //set the file to read only
            it.setWritable(false)
            Logger.log("Loading extension ${it.name}")
            val extension = loadExtension(context, it)
            if (extension is NovelLoadResult.Success) {
                results.add(extension)
            } else {
                Logger.log("Failed to load extension ${it.name}")
            }
        }
        return results
    }

    /**
     * Attempts to load an extension from the given package name. It checks if the extension
     * contains the required feature flag before trying to load it.
     */
    fun loadExtensionFromPkgName(context: Context, pkgName: String): NovelLoadResult {
        val path =
            context.getExternalFilesDir(null)?.absolutePath + "/extensions/novel/$pkgName.apk"
        //make /extensions/novel read only
        context.getExternalFilesDir(null)?.absolutePath + "/extensions/novel/".let {
            File(it).setWritable(false)
            File(it).setReadable(true)
        }
        val pkgInfo = try {
            context.packageManager.getPackageArchiveInfo(path, 0)
        } catch (error: Exception) {
            // Unlikely, but the package may have been uninstalled at this point
            Logger.log("Failed to load extension $pkgName")
            return NovelLoadResult.Error(Exception("Failed to load extension"))
        }
        return loadExtension(context, File(path))
    }

    @Suppress("DEPRECATION")
    fun loadExtension(context: Context, file: File): NovelLoadResult {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageArchiveInfo(
                file.absolutePath,
                GET_SIGNATURES or GET_SIGNING_CERTIFICATES
            )
                ?: return NovelLoadResult.Error(Exception("Failed to load extension"))
        } else {
            context.packageManager.getPackageArchiveInfo(file.absolutePath, GET_SIGNATURES)
                ?: return NovelLoadResult.Error(Exception("Failed to load extension"))
        }
        val appInfo = packageInfo.applicationInfo
            ?: return NovelLoadResult.Error(Exception("Failed to load Extension Info"))
        appInfo.sourceDir = file.absolutePath
        appInfo.publicSourceDir = file.absolutePath

        val signatureHash = getSignatureHash(packageInfo)

        if ((signatureHash == null) || !signatureHash.contains(officialSignature)) {
            Logger.log("Package ${packageInfo.packageName} isn't signed")
            Logger.log("signatureHash: $signatureHash")
            snackString("Package ${packageInfo.packageName} isn't signed")
            //return NovelLoadResult.Error(Exception("Extension not signed"))
        }

        val extension = NovelExtension.Installed(
            packageInfo.applicationInfo?.loadLabel(context.packageManager)?.toString()
                ?: return NovelLoadResult.Error(Exception("Failed to load Extension Info")),
            packageInfo.packageName
                ?: return NovelLoadResult.Error(Exception("Failed to load Extension Info")),
            packageInfo.versionName ?: "",
            packageInfo.versionCode.toLong(),
            loadSources(
                context, file,
                packageInfo.applicationInfo?.loadLabel(context.packageManager)?.toString()!!
            ),
            packageInfo.applicationInfo?.loadIcon(context.packageManager)
        )

        return NovelLoadResult.Success(extension)
    }

    @Suppress("DEPRECATION")
    private fun getSignatureHash(pkgInfo: PackageInfo): List<String>? {
        val signatures =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && pkgInfo.signingInfo != null) {
                pkgInfo.signingInfo.apkContentsSigners
            } else {
                pkgInfo.signatures
            }
        return if (!signatures.isNullOrEmpty()) {
            signatures.map { Hash.sha256(it.toByteArray()) }
        } else {
            null
        }
    }

    private fun loadSources(context: Context, file: File, className: String): List<NovelInterface> {
        return try {
            Logger.log("isFileWritable: ${file.canWrite()}")
            if (file.canWrite()) {
                val a = file.setWritable(false)
                Logger.log("success: $a")
            }
            Logger.log("isFileWritable: ${file.canWrite()}")
            val classLoader = PathClassLoader(file.absolutePath, null, context.classLoader)
            val className =
                "some.random.novelextensions.${className.lowercase(Locale.getDefault())}.$className"
            val loadedClass = classLoader.loadClass(className)
            val instance = loadedClass.newInstance()
            val novelInterfaceInstance = instance as? NovelInterface
            listOfNotNull(novelInterfaceInstance)
        } catch (e: Exception) {
            e.printStackTrace()
            Injekt.get<CrashlyticsInterface>().logException(e)
            emptyList()
        }
    }
}

sealed class NovelLoadResult {
    data class Success(val extension: NovelExtension.Installed) : NovelLoadResult()
    data class Error(val error: Exception) : NovelLoadResult()
}