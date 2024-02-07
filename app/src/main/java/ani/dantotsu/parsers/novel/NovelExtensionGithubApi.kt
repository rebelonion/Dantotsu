package ani.dantotsu.parsers.novel


import android.content.Context
import ani.dantotsu.logger
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import eu.kanade.tachiyomi.extension.ExtensionUpdateNotifier
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.extension.anime.model.AnimeLoadResult
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logcat.LogPriority
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.core.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.util.Date
import kotlin.time.Duration.Companion.days

class NovelExtensionGithubApi {

    private val networkService: NetworkHelper by injectLazy()
    private val novelExtensionManager: NovelExtensionManager by injectLazy()
    private val json: Json by injectLazy()

    private val lastExtCheck: Long = PrefManager.getVal(PrefName.NovelLastExtCheck)

    private var requiresFallbackSource = false

    suspend fun findExtensions(): List<NovelExtension.Available> {
        return withIOContext {
            val githubResponse = if (requiresFallbackSource) {
                null
            } else {
                try {
                    networkService.client
                        .newCall(GET("${REPO_URL_PREFIX}index.min.json"))
                        .awaitSuccess()
                } catch (e: Throwable) {
                    logcat(LogPriority.ERROR, e) { "Failed to get extensions from GitHub" }
                    requiresFallbackSource = true
                    null
                }
            }

            val response = githubResponse ?: run {
                logger("using fallback source")
                networkService.client
                    .newCall(GET("${FALLBACK_REPO_URL_PREFIX}index.min.json"))
                    .awaitSuccess()
            }

            logger("response: $response")

            val extensions = with(json) {
                response
                    .parseAs<List<NovelExtensionJsonObject>>()
                    .toExtensions()
            }

            // Sanity check - a small number of extensions probably means something broke
            // with the repo generator
            /*if (extensions.size < 10) {  //TODO: uncomment when more extensions are added
                throw Exception()
            }*/
            logger("extensions: $extensions")
            extensions
        }
    }

    suspend fun checkForUpdates(
        context: Context,
        fromAvailableExtensionList: Boolean = false
    ): List<AnimeExtension.Installed>? {
        // Limit checks to once a day at most
        if (fromAvailableExtensionList && Date().time < lastExtCheck + 1.days.inWholeMilliseconds) {
            return null
        }

        val extensions = if (fromAvailableExtensionList) {
            novelExtensionManager.availableExtensionsFlow.value
        } else {
            findExtensions().also {
                PrefManager.setVal(PrefName.NovelLastExtCheck, Date().time)
            }
        }

        val installedExtensions = NovelExtensionLoader.loadExtensions(context)
            .filterIsInstance<AnimeLoadResult.Success>()
            .map { it.extension }

        val extensionsWithUpdate = mutableListOf<AnimeExtension.Installed>()
        for (installedExt in installedExtensions) {
            val pkgName = installedExt.pkgName
            val availableExt = extensions.find { it.pkgName == pkgName } ?: continue

            val hasUpdatedVer = availableExt.versionCode > installedExt.versionCode
            val hasUpdate = installedExt.isUnofficial.not() && (hasUpdatedVer)
            if (hasUpdate) {
                extensionsWithUpdate.add(installedExt)
            }
        }

        if (extensionsWithUpdate.isNotEmpty()) {
            ExtensionUpdateNotifier(context).promptUpdates(extensionsWithUpdate.map { it.name })
        }

        return extensionsWithUpdate
    }

    private fun List<NovelExtensionJsonObject>.toExtensions(): List<NovelExtension.Available> {
        return mapNotNull { extension ->
            val sources = extension.sources?.map { source ->
                NovelExtensionSourceJsonObject(
                    source.id,
                    source.lang,
                    source.name,
                    source.baseUrl,
                )
            }
            val iconUrl = "${REPO_URL_PREFIX}icon/${extension.pkg}.png"
            NovelExtension.Available(
                extension.name,
                extension.pkg,
                extension.apk,
                extension.code,
                sources?.toSources() ?: emptyList(),
                iconUrl,
            )
        }
    }

    private fun List<NovelExtensionSourceJsonObject>.toSources(): List<AvailableNovelSources> {
        return map { source ->
            AvailableNovelSources(
                source.id,
                source.lang,
                source.name,
                source.baseUrl,
            )
        }
    }

    fun getApkUrl(extension: NovelExtension.Available): String {
        return "${getUrlPrefix()}apk/${extension.pkgName}.apk"
    }

    private fun getUrlPrefix(): String {
        return if (requiresFallbackSource) {
            FALLBACK_REPO_URL_PREFIX
        } else {
            REPO_URL_PREFIX
        }
    }
}

private const val REPO_URL_PREFIX =
    "https://raw.githubusercontent.com/dannovels/novel-extensions/main/"
private const val FALLBACK_REPO_URL_PREFIX =
    "https://gcore.jsdelivr.net/gh/dannovels/novel-extensions@latest/"

@Serializable
private data class NovelExtensionJsonObject(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int,
    val hasReadme: Int = 0,
    val hasChangelog: Int = 0,
    val sources: List<NovelExtensionSourceJsonObject>?,
)

@Serializable
private data class NovelExtensionSourceJsonObject(
    val id: Long,
    val lang: String,
    val name: String,
    val baseUrl: String,
)

