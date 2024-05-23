package eu.kanade.tachiyomi.extension.util

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import ani.dantotsu.addons.download.DownloadAddonManager
import ani.dantotsu.addons.torrent.TorrentAddonManager
import ani.dantotsu.media.AddonType
import ani.dantotsu.media.MediaType
import ani.dantotsu.parsers.novel.NovelExtensionManager
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.util.system.getSerializableExtraCompat
import eu.kanade.tachiyomi.util.system.hasMiuiPackageInstaller
import eu.kanade.tachiyomi.util.system.toast
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

/**
 * Activity used to install extensions, because we can only receive the result of the installation
 * with [startActivityForResult], which we need to update the UI.
 */
class ExtensionInstallActivity : AppCompatActivity() {

    // MIUI package installer bug workaround
    private var ignoreUntil = 0L
    private var ignoreResult = false
    private var hasIgnoredResult = false

    private var mediaType: MediaType? = null
    private var addonType: AddonType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.hasExtra(ExtensionInstaller.EXTRA_EXTENSION_TYPE))
            mediaType =
                intent.getSerializableExtraCompat<MediaType>(ExtensionInstaller.EXTRA_EXTENSION_TYPE)
        if (intent.hasExtra(ExtensionInstaller.EXTRA_ADDON_TYPE))
            addonType =
                intent.getSerializableExtraCompat<AddonType>(ExtensionInstaller.EXTRA_ADDON_TYPE)

        @Suppress("DEPRECATION")
        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            .setDataAndType(intent.data, intent.type)
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (hasMiuiPackageInstaller) {
            ignoreResult = true
            ignoreUntil = System.nanoTime() + 1.seconds.inWholeNanoseconds
        }

        val onInstallResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (ignoreResult && System.nanoTime() < ignoreUntil) {
                hasIgnoredResult = true
                return@registerForActivityResult
            }
            checkInstallationResult(result.resultCode)
            finish()
        }

        try {
            onInstallResult.launch(installIntent)
        } catch (error: Exception) {
            // Either install package can't be found (probably bots) or there's a security exception
            // with the download manager. Nothing we can workaround.
            toast(error.message)
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasIgnoredResult) {
            checkInstallationResult(RESULT_CANCELED)
            finish()
        }
    }

    @Suppress("all")
    private fun checkInstallationResult(resultCode: Int) {
        val downloadId = intent.extras!!.getLong(ExtensionInstaller.EXTRA_DOWNLOAD_ID)
        val newStep = when (resultCode) {
            RESULT_OK -> InstallStep.Installed
            RESULT_CANCELED -> InstallStep.Idle
            else -> InstallStep.Error
        }
        if (mediaType != null) {
            when (mediaType) {
                MediaType.ANIME -> {
                    Injekt.get<AnimeExtensionManager>().updateInstallStep(downloadId, newStep)
                }

                MediaType.MANGA -> {
                    Injekt.get<MangaExtensionManager>().updateInstallStep(downloadId, newStep)
                }

                else -> {
                    Injekt.get<NovelExtensionManager>().updateInstallStep(downloadId, newStep)
                }
            }
        } else {
            when (addonType) {
                AddonType.TORRENT -> {
                    Injekt.get<TorrentAddonManager>().updateInstallStep(downloadId, newStep)
                }

                AddonType.DOWNLOAD -> {
                    Injekt.get<DownloadAddonManager>().updateInstallStep(downloadId, newStep)
                }

                null -> {}
            }
        }
    }
}
