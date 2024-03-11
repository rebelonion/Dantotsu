package eu.kanade.tachiyomi.extension.manga.api

import android.content.Context
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.extension.ExtensionUpdateNotifier
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.extension.manga.model.AvailableMangaSources
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.extension.manga.model.MangaLoadResult
import eu.kanade.tachiyomi.extension.manga.util.MangaExtensionLoader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import java.util.Date
import kotlin.time.Duration.Companion.days

internal class MangaExtensionGithubApi {

    private val networkService: NetworkHelper by injectLazy()
    private val preferenceStore: PreferenceStore by injectLazy()
    private val extensionManager: MangaExtensionManager by injectLazy()
    private val json: Json by injectLazy()

    private val lastExtCheck: Preference<Long> by lazy {
        preferenceStore.getLong("last_ext_check", 0)
    }

    private var requiresFallbackSource = false

    suspend fun findExtensions(): List<MangaExtension.Available> {
        return withIOContext {
            val githubResponse = if (requiresFallbackSource) {
                null
            } else {
                try {
                    networkService.client
                        .newCall(GET("${REPO_URL_PREFIX}index.min.json"))
                        .awaitSuccess()
                } catch (e: Throwable) {
                    Logger.log("Failed to get extensions from GitHub")
                    requiresFallbackSource = true
                    null
                }
            }

            val response = githubResponse ?: run {
                networkService.client
                    .newCall(GET("${FALLBACK_REPO_URL_PREFIX}index.min.json"))
                    .awaitSuccess()
            }

            val extensions = with(json) {
                response
                    .parseAs<List<ExtensionJsonObject>>()
                    .toExtensions()
            }

            // Sanity check - a small number of extensions probably means something broke
            // with the repo generator
            if (extensions.size < 100) {
                throw Exception()
            }

            extensions
        }
    }

    suspend fun checkForUpdates(
        context: Context,
        fromAvailableExtensionList: Boolean = false
    ): List<MangaExtension.Installed>? {
        // Limit checks to once a day at most
        if (fromAvailableExtensionList && Date().time < lastExtCheck.get() + 1.days.inWholeMilliseconds) {
            return null
        }

        val extensions = if (fromAvailableExtensionList) {
            extensionManager.availableExtensionsFlow.value
        } else {
            findExtensions().also { lastExtCheck.set(Date().time) }
        }

        val installedExtensions = MangaExtensionLoader.loadMangaExtensions(context)
            .filterIsInstance<MangaLoadResult.Success>()
            .map { it.extension }

        val extensionsWithUpdate = mutableListOf<MangaExtension.Installed>()
        for (installedExt in installedExtensions) {
            val pkgName = installedExt.pkgName
            val availableExt = extensions.find { it.pkgName == pkgName } ?: continue
            val hasUpdatedVer = availableExt.versionCode > installedExt.versionCode
            val hasUpdatedLib = availableExt.libVersion > installedExt.libVersion
            val hasUpdate = installedExt.isUnofficial.not() && (hasUpdatedVer || hasUpdatedLib)
            if (hasUpdate) {
                extensionsWithUpdate.add(installedExt)
            }
        }

        if (extensionsWithUpdate.isNotEmpty()) {
            ExtensionUpdateNotifier(context).promptUpdates(extensionsWithUpdate.map { it.name })
        }

        return extensionsWithUpdate
    }

    private fun List<ExtensionJsonObject>.toExtensions(): List<MangaExtension.Available> {
        return this
            .filter {
                val libVersion = it.extractLibVersion()
                libVersion >= MangaExtensionLoader.LIB_VERSION_MIN && libVersion <= MangaExtensionLoader.LIB_VERSION_MAX
            }
            .map {
                MangaExtension.Available(
                    name = it.name.substringAfter("Tachiyomi: "),
                    pkgName = it.pkg,
                    versionName = it.version,
                    versionCode = it.code,
                    libVersion = it.extractLibVersion(),
                    lang = it.lang,
                    isNsfw = it.nsfw == 1,
                    hasReadme = it.hasReadme == 1,
                    hasChangelog = it.hasChangelog == 1,
                    sources = it.sources?.toExtensionSources().orEmpty(),
                    apkName = it.apk,
                    iconUrl = "${getUrlPrefix()}icon/${it.pkg}.png",
                )
            }
    }

    private fun List<ExtensionSourceJsonObject>.toExtensionSources(): List<AvailableMangaSources> {
        return this.map {
            AvailableMangaSources(
                id = it.id,
                lang = it.lang,
                name = it.name,
                baseUrl = it.baseUrl,
            )
        }
    }

    fun getApkUrl(extension: MangaExtension.Available): String {
        return "${getUrlPrefix()}apk/${extension.apkName}"
    }

    private fun getUrlPrefix(): String {
        return if (requiresFallbackSource) {
            FALLBACK_REPO_URL_PREFIX
        } else {
            REPO_URL_PREFIX
        }
    }

    private fun ExtensionJsonObject.extractLibVersion(): Double {
        return version.substringBeforeLast('.').toDouble()
    }
}

private const val REPO_URL_PREFIX = "https://raw.githubusercontent.com/keiyoushi/extensions/main/"
private const val FALLBACK_REPO_URL_PREFIX =
    "https://gcore.jsdelivr.net/gh/keiyoushi/extensions@main/"

@Serializable
private data class ExtensionJsonObject(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int,
    val hasReadme: Int = 0,
    val hasChangelog: Int = 0,
    val sources: List<ExtensionSourceJsonObject>?,
)

@Serializable
private data class ExtensionSourceJsonObject(
    val id: Long,
    val lang: String,
    val name: String,
    val baseUrl: String,
)
