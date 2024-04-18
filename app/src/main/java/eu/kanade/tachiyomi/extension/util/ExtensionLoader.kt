package eu.kanade.tachiyomi.extension.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import ani.dantotsu.media.MediaType
import ani.dantotsu.util.Logger
import dalvik.system.PathClassLoader
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.extension.anime.model.AnimeLoadResult
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.extension.manga.model.MangaLoadResult
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.MangaSource
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.util.lang.Hash
import eu.kanade.tachiyomi.util.system.getApplicationIcon
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.injectLazy

/**
 * Class that handles the loading of the extensions. Supports two kinds of extensions:
 *
 * 1. Shared extension: This extension is installed to the system with package
 * installer, so other variants of Tachiyomi and its forks can also use this extension.
 *
 * 2. Private extension: This extension is put inside private data directory of the
 * running app, so this extension can only be used by the running app and not shared
 * with other apps.
 *
 * When both kinds of extensions are installed with a same package name, shared
 * extension will be used unless the version codes are different. In that case the
 * one with higher version code will be used.
 */
internal object ExtensionLoader {

    private val preferences: SourcePreferences by injectLazy()
    private val loadNsfwSource by lazy {
        preferences.showNsfwSource().get()
    }

    private const val ANIME_PACKAGE = "tachiyomi.animeextension"
    private const val MANGA_PACKAGE = "tachiyomi.extension"

    private const val XX_METADATA_SOURCE_CLASS = ".class"
    private const val XX_METADATA_SOURCE_FACTORY = ".factory"
    private const val XX_METADATA_NSFW = "n.nsfw"
    private const val XX_METADATA_HAS_README = ".hasReadme"
    private const val XX_METADATA_HAS_CHANGELOG = ".hasChangelog"
    const val ANIME_LIB_VERSION_MIN = 12
    const val ANIME_LIB_VERSION_MAX = 15

    const val MANGA_LIB_VERSION_MIN = 1.2
    const val MANGA_LIB_VERSION_MAX = 1.5

    val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
            PackageManager.GET_META_DATA or
            @Suppress ("DEPRECATION") PackageManager.GET_SIGNATURES or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                PackageManager.GET_SIGNING_CERTIFICATES else 0)

    // jmir1's key
    private const val officialSignatureAnime =
        "50ab1d1e3a20d204d0ad6d334c7691c632e41b98dfa132bf385695fdfa63839c"

    var trustedSignaturesAnime =
        mutableSetOf<String>() + preferences.trustedSignatures().get() + officialSignatureAnime

    // inorichi's key
    private const val officialSignatureManga =
        "7ce04da7773d41b489f4693a366c36bcd0a11fc39b547168553c285bd7348e23"

    /**
     * List of the trusted signatures.
     */
    var trustedSignaturesManga =
        mutableSetOf<String>() + preferences.trustedSignatures().get() + officialSignatureManga

    /**
     * Return a list of all the installed extensions initialized concurrently.
     *
     * @param context The application context.
     */
    fun loadAnimeExtensions(context: Context): List<AnimeLoadResult> {
        val pkgManager = context.packageManager


        val installedPkgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pkgManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pkgManager.getInstalledPackages(PACKAGE_FLAGS)
        }

        val extPkgs = installedPkgs.filter { isPackageAnExtension(MediaType.ANIME, it) }

        if (extPkgs.isEmpty()) return emptyList()

        // Load each extension concurrently and wait for completion
        return runBlocking {
            val deferred = extPkgs.map {
                async { loadAnimeExtension(context, it.packageName, it) }
            }
            deferred.map { it.await() }
        }
    }

    fun loadMangaExtensions(context: Context): List<MangaLoadResult> {
        val pkgManager = context.packageManager

        val installedPkgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pkgManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pkgManager.getInstalledPackages(PACKAGE_FLAGS)
        }

        val extPkgs = installedPkgs.filter { isPackageAnExtension(MediaType.MANGA, it) }

        if (extPkgs.isEmpty()) return emptyList()

        // Load each extension concurrently and wait for completion
        return runBlocking {
            val deferred = extPkgs.map {
                async { loadMangaExtension(context, it.packageName, it) }
            }
            deferred.map { it.await() }
        }
    }

    /**
     * Attempts to load an extension from the given package name. It checks if the extension
     * contains the required feature flag before trying to load it.
     */
    fun loadAnimeExtensionFromPkgName(context: Context, pkgName: String): AnimeLoadResult {
        val pkgInfo = try {
            context.packageManager.getPackageInfo(pkgName, PACKAGE_FLAGS)
        } catch (error: PackageManager.NameNotFoundException) {
            // Unlikely, but the package may have been uninstalled at this point
            Logger.log(error)
            return AnimeLoadResult.Error
        }
        if (!isPackageAnExtension(MediaType.ANIME,pkgInfo)) {
            Logger.log("Tried to load a package that wasn't a extension ($pkgName)")
            return AnimeLoadResult.Error
        }
        return loadAnimeExtension(context, pkgName, pkgInfo)
    }

    fun loadMangaExtensionFromPkgName(context: Context, pkgName: String): MangaLoadResult {
        val pkgInfo = try {
            context.packageManager.getPackageInfo(pkgName, PACKAGE_FLAGS)
        } catch (error: PackageManager.NameNotFoundException) {
            // Unlikely, but the package may have been uninstalled at this point
            Logger.log(error)
            return MangaLoadResult.Error
        }
        if (!isPackageAnExtension(MediaType.MANGA, pkgInfo)) {
            Logger.log("Tried to load a package that wasn't a extension ($pkgName)")
            return MangaLoadResult.Error
        }
        return loadMangaExtension(context, pkgName, pkgInfo)
    }

    /**
     * Loads an extension given its package name.
     *
     * @param context The application context.
     * @param pkgName The package name of the extension to load.
     * @param pkgInfo The package info of the extension.
     */
    private fun loadAnimeExtension(
        context: Context,
        pkgName: String,
        pkgInfo: PackageInfo
    ): AnimeLoadResult {
        val pkgManager = context.packageManager

        val appInfo = try {
            pkgManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
        } catch (error: PackageManager.NameNotFoundException) {
            // Unlikely, but the package may have been uninstalled at this point
            Logger.log(error)
            return AnimeLoadResult.Error
        }

        val extName = pkgManager.getApplicationLabel(appInfo).toString().substringAfter("Aniyomi: ")
        val versionName = pkgInfo.versionName
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)

        if (versionName.isNullOrEmpty()) {
            Logger.log("Missing versionName for extension $extName")
            return AnimeLoadResult.Error
        }

        // Validate lib version
        val libVersion = versionName.substringBeforeLast('.').toDoubleOrNull()
        if (libVersion == null || libVersion < ANIME_LIB_VERSION_MIN || libVersion > ANIME_LIB_VERSION_MAX) {
            Logger.log("Lib version is $libVersion, while only versions " +
                    "$ANIME_LIB_VERSION_MIN to $ANIME_LIB_VERSION_MAX are allowed"
            )
            return AnimeLoadResult.Error
        }

        val signatureHash = getSignatureHash(pkgInfo)

        if (signatureHash == null) {
            Logger.log("Package $pkgName isn't signed")
            return AnimeLoadResult.Error
        } else if (signatureHash !in trustedSignaturesAnime) {
            val extension = AnimeExtension.Untrusted(
                extName,
                pkgName,
                versionName,
                versionCode,
                libVersion,
                signatureHash
            )
            Logger.log("Extension $pkgName isn't trusted")
            return AnimeLoadResult.Untrusted(extension)
        }

        val isNsfw = appInfo.metaData.getInt("$ANIME_PACKAGE$XX_METADATA_NSFW") == 1
        if (!loadNsfwSource && isNsfw) {
            Logger.log("NSFW extension $pkgName not allowed")
            return AnimeLoadResult.Error
        }

        val hasReadme = appInfo.metaData.getInt("$ANIME_PACKAGE$XX_METADATA_HAS_README", 0) == 1
        val hasChangelog = appInfo.metaData.getInt("$ANIME_PACKAGE$XX_METADATA_HAS_CHANGELOG", 0) == 1

        val classLoader = PathClassLoader(appInfo.sourceDir, null, context.classLoader)

        val sources = appInfo.metaData.getString("$ANIME_PACKAGE$XX_METADATA_SOURCE_CLASS")!!
            .split(";")
            .map {
                val sourceClass = it.trim()
                if (sourceClass.startsWith(".")) {
                    pkgInfo.packageName + sourceClass
                } else {
                    sourceClass
                }
            }
            .flatMap {
                try {
                    when (val obj = Class.forName(it, false, classLoader).getDeclaredConstructor().newInstance()) {
                        is AnimeSource -> listOf(obj)
                        is AnimeSourceFactory -> obj.createSources()
                        else -> throw Exception("Unknown source class type! ${obj.javaClass}")
                    }
                } catch (e: Throwable) {
                    Logger.log("Extension load error: $extName ($it)")
                    return AnimeLoadResult.Error
                }
            }

        val langs = sources.filterIsInstance<AnimeCatalogueSource>()
            .map { it.lang }
            .toSet()
        val lang = when (langs.size) {
            0 -> ""
            1 -> langs.first()
            else -> "all"
        }

        val extension = AnimeExtension.Installed(
            name = extName,
            pkgName = pkgName,
            versionName = versionName,
            versionCode = versionCode,
            libVersion = libVersion,
            lang = lang,
            isNsfw = isNsfw,
            hasReadme = hasReadme,
            hasChangelog = hasChangelog,
            sources = sources,
            pkgFactory = appInfo.metaData.getString("$ANIME_PACKAGE$XX_METADATA_SOURCE_FACTORY"),
            isUnofficial = signatureHash != officialSignatureAnime,
            icon = context.getApplicationIcon(pkgName),
        )
        return AnimeLoadResult.Success(extension)
    }

    private fun loadMangaExtension(
        context: Context,
        pkgName: String,
        pkgInfo: PackageInfo
    ): MangaLoadResult {
        val pkgManager = context.packageManager

        val appInfo = try {
            pkgManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
        } catch (error: PackageManager.NameNotFoundException) {
            // Unlikely, but the package may have been uninstalled at this point
            Logger.log(error)
            return MangaLoadResult.Error
        }

        val extName =
            pkgManager.getApplicationLabel(appInfo).toString().substringAfter("Tachiyomi: ")
        val versionName = pkgInfo.versionName
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)

        if (versionName.isNullOrEmpty()) {
            Logger.log("Missing versionName for extension $extName")
            return MangaLoadResult.Error
        }

        // Validate lib version
        val libVersion = versionName.substringBeforeLast('.').toDoubleOrNull()
        if (libVersion == null || libVersion < MANGA_LIB_VERSION_MIN || libVersion > MANGA_LIB_VERSION_MAX) {
            Logger.log("Lib version is $libVersion, while only versions " +
                    "$MANGA_LIB_VERSION_MIN to $MANGA_LIB_VERSION_MAX are allowed"
            )
            return MangaLoadResult.Error
        }

        val signatureHash = getSignatureHash(pkgInfo)

        /*  temporarily disabling signature check TODO: remove?
        if (signatureHash == null) {
            Logger.log("Package $pkgName isn't signed")
            return MangaLoadResult.Error
        } else if (signatureHash !in trustedSignatures) {
            val extension = MangaExtension.Untrusted(extName, pkgName, versionName, versionCode, libVersion, signatureHash)
            Logger.log("Extension $pkgName isn't trusted")
            return MangaLoadResult.Untrusted(extension)
        }
        */

        val isNsfw = appInfo.metaData.getInt("$MANGA_PACKAGE$XX_METADATA_NSFW") == 1
        if (!loadNsfwSource && isNsfw) {
            Logger.log("NSFW extension $pkgName not allowed")
            return MangaLoadResult.Error
        }

        val hasReadme = appInfo.metaData.getInt("$MANGA_PACKAGE$XX_METADATA_HAS_README", 0) == 1
        val hasChangelog = appInfo.metaData.getInt("$MANGA_PACKAGE$XX_METADATA_HAS_CHANGELOG", 0) == 1

        val classLoader = PathClassLoader(appInfo.sourceDir, null, context.classLoader)

        val sources = appInfo.metaData.getString("$MANGA_PACKAGE$XX_METADATA_SOURCE_CLASS")!!
            .split(";")
            .map {
                val sourceClass = it.trim()
                if (sourceClass.startsWith(".")) {
                    pkgInfo.packageName + sourceClass
                } else {
                    sourceClass
                }
            }
            .flatMap {
                try {
                    when (val obj = Class.forName(it, false, classLoader)
                        .getDeclaredConstructor().newInstance()) {
                        is MangaSource -> listOf(obj)
                        is SourceFactory -> obj.createSources()
                        else -> throw Exception("Unknown source class type! ${obj.javaClass}")
                    }
                } catch (e: Throwable) {
                    Logger.log("Extension load error: $extName ($it)")
                    return MangaLoadResult.Error
                }
            }

        val langs = sources.filterIsInstance<CatalogueSource>()
            .map { it.lang }
            .toSet()
        val lang = when (langs.size) {
            0 -> ""
            1 -> langs.first()
            else -> "all"
        }

        val extension = MangaExtension.Installed(
            name = extName,
            pkgName = pkgName,
            versionName = versionName,
            versionCode = versionCode,
            libVersion = libVersion,
            lang = lang,
            isNsfw = isNsfw,
            hasReadme = hasReadme,
            hasChangelog = hasChangelog,
            sources = sources,
            pkgFactory = appInfo.metaData.getString("$MANGA_PACKAGE$XX_METADATA_SOURCE_FACTORY"),
            isUnofficial = signatureHash != officialSignatureManga,
            icon = context.getApplicationIcon(pkgName),
        )
        return MangaLoadResult.Success(extension)
    }

    /**
     * Returns true if the given package is an extension.
     *
     * @param pkgInfo The package info of the application.
     */
    private fun isPackageAnExtension(type: MediaType, pkgInfo: PackageInfo): Boolean {
        return pkgInfo.reqFeatures.orEmpty().any { it.name == when (type) {
            MediaType.ANIME -> ANIME_PACKAGE
            MediaType.MANGA -> MANGA_PACKAGE
            else -> ""
        } }
    }

    /**
     * Returns the signature hash of the package or null if it's not signed.
     *
     * @param pkgInfo The package info of the application.
     */
    private fun getSignatureHash(pkgInfo: PackageInfo): String? {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            pkgInfo.signingInfo.signingCertificateHistory
        else
            @Suppress ("DEPRECATION") pkgInfo.signatures
        return if (signatures != null && signatures.isNotEmpty()) {
            Hash.sha256(signatures.first().toByteArray())
        } else {
            null
        }
    }
}
