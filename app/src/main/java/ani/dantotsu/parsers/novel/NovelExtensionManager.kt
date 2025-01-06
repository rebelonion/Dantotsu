package ani.dantotsu.parsers.novel

import android.content.Context
import android.graphics.drawable.Drawable
import ani.dantotsu.media.MediaType
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.extension.api.ExtensionGithubApi
import eu.kanade.tachiyomi.extension.util.ExtensionInstallReceiver
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import rx.Observable
import tachiyomi.core.util.lang.withUIContext

class NovelExtensionManager(private val context: Context) {
    var isInitialized = false
        private set


    /**
     * API where all the available Novel extensions can be found.
     */
    private val api = ExtensionGithubApi()

    /**
     * The installer which installs, updates and uninstalls the Novel extensions.
     */
    private val installer by lazy { ExtensionInstaller(context) }

    private val iconMap = mutableMapOf<String, Drawable>()

    private val _installedNovelExtensionsFlow =
        MutableStateFlow(emptyList<NovelExtension.Installed>())
    val installedExtensionsFlow = _installedNovelExtensionsFlow.asStateFlow()

    private val _availableNovelExtensionsFlow =
        MutableStateFlow(emptyList<NovelExtension.Available>())
    val availableExtensionsFlow = _availableNovelExtensionsFlow.asStateFlow()

    private var availableNovelExtensionsSourcesData: Map<Long, NovelSourceData> = emptyMap()

    private fun setupAvailableNovelExtensionsSourcesDataMap(novelExtensions: List<NovelExtension.Available>) {
        if (novelExtensions.isEmpty()) return
        availableNovelExtensionsSourcesData = novelExtensions
            .flatMap { ext -> ext.sources.map { it.toNovelSourceData() } }
            .associateBy { it.id }
    }

    fun getSourceData(id: Long) = availableNovelExtensionsSourcesData[id]

    init {
        initNovelExtensions()
        ExtensionInstallReceiver().setNovelListener(NovelInstallationListener()).register(context)
    }

    private fun initNovelExtensions() {
        val novelExtensions = ExtensionLoader.loadNovelExtensions(context)

        _installedNovelExtensionsFlow.value = novelExtensions
            .filterIsInstance<NovelLoadResult.Success>()
            .map { it.extension }

        isInitialized = true
    }

    /**
     * Finds the available manga extensions in the [api] and updates [availableExtensions].
     */
    suspend fun findAvailableExtensions() {
        val extensions: List<NovelExtension.Available> = try {
            api.findNovelExtensions()
        } catch (e: Exception) {
            Logger.log("Error finding extensions: ${e.message}")
            withUIContext { snackString("Failed to get Novel extensions list") }
            emptyList()
        }

        _availableNovelExtensionsFlow.value = extensions
        updatedInstalledNovelExtensionsStatuses(extensions)
        setupAvailableNovelExtensionsSourcesDataMap(extensions)
    }

    private fun updatedInstalledNovelExtensionsStatuses(availableNovelExtensions: List<NovelExtension.Available>) {
        if (availableNovelExtensions.isEmpty()) {
            return
        }

        val mutInstalledNovelExtensions = _installedNovelExtensionsFlow.value.toMutableList()
        var hasChanges = false

        for ((index, installedExt) in mutInstalledNovelExtensions.withIndex()) {
            val pkgName = installedExt.pkgName
            val availableExt = availableNovelExtensions.find { it.pkgName == pkgName }

            if (availableExt == null && !installedExt.isObsolete) {
                mutInstalledNovelExtensions[index] = installedExt.copy(isObsolete = true)
                hasChanges = true
            } else if (availableExt != null) {
                val hasUpdate = installedExt.updateExists(availableExt)

                if (installedExt.hasUpdate != hasUpdate) {
                    mutInstalledNovelExtensions[index] = installedExt.copy(hasUpdate = hasUpdate)
                    hasChanges = true
                }
            }
        }
        if (hasChanges) {
            _installedNovelExtensionsFlow.value = mutInstalledNovelExtensions
        }
    }

    /**
     * Returns an observable of the installation process for the given novel extension. It will complete
     * once the novel extension is installed or throws an error. The process will be canceled if
     * unsubscribed before its completion.
     *
     * @param extension The anime extension to be installed.
     */
    fun installExtension(extension: NovelExtension.Available): Observable<InstallStep> {
        return installer.downloadAndInstall(
            api.getNovelApkUrl(extension), extension.pkgName,
            extension.name, MediaType.NOVEL
        )
    }

    /**
     * Returns an observable of the installation process for the given anime extension. It will complete
     * once the anime extension is updated or throws an error. The process will be canceled if
     * unsubscribed before its completion.
     *
     * @param extension The anime extension to be updated.
     */
    fun updateExtension(extension: NovelExtension.Installed): Observable<InstallStep> {
        val availableExt =
            _availableNovelExtensionsFlow.value.find { it.pkgName == extension.pkgName }
                ?: return Observable.empty()
        return installExtension(availableExt)
    }

    fun cancelInstallUpdateExtension(extension: NovelExtension) {
        installer.cancelInstall(extension.pkgName)
    }

    /**
     * Sets to "installing" status of an novel extension installation.
     *
     * @param downloadId The id of the download.
     */
    fun setInstalling(downloadId: Long) {
        installer.updateInstallStep(downloadId, InstallStep.Installing)
    }

    fun updateInstallStep(downloadId: Long, step: InstallStep) {
        installer.updateInstallStep(downloadId, step)
    }

    /**
     * Uninstalls the novel extension that matches the given package name.
     *
     * @param pkgName The package name of the application to uninstall.
     */
    fun uninstallExtension(pkgName: String) {
        installer.uninstallApk(pkgName)
    }

    /**
     * Registers the given novel extension in this and the source managers.
     *
     * @param extension The anime extension to be registered.
     */
    private fun registerNewExtension(extension: NovelExtension.Installed) {
        _installedNovelExtensionsFlow.value += extension
    }

    /**
     * Registers the given updated novel extension in this and the source managers previously removing
     * the outdated ones.
     *
     * @param extension The anime extension to be registered.
     */
    private fun registerUpdatedExtension(extension: NovelExtension.Installed) {
        val mutInstalledNovelExtensions = _installedNovelExtensionsFlow.value.toMutableList()
        val oldNovelExtension = mutInstalledNovelExtensions.find { it.pkgName == extension.pkgName }
        if (oldNovelExtension != null) {
            mutInstalledNovelExtensions -= oldNovelExtension
        }
        mutInstalledNovelExtensions += extension
        _installedNovelExtensionsFlow.value = mutInstalledNovelExtensions
    }

    /**
     * Unregisters the novel extension in this and the source managers given its package name. Note this
     * method is called for every uninstalled application in the system.
     *
     * @param pkgName The package name of the uninstalled application.
     */
    private fun unregisterNovelExtension(pkgName: String) {
        val installedNovelExtension =
            _installedNovelExtensionsFlow.value.find { it.pkgName == pkgName }
        if (installedNovelExtension != null) {
            _installedNovelExtensionsFlow.value -= installedNovelExtension
        }
    }

    /**
     * Listener which receives events of the novel extensions being installed, updated or removed.
     */
    private inner class NovelInstallationListener : ExtensionInstallReceiver.NovelListener {
        override fun onExtensionInstalled(extension: NovelExtension.Installed) {
            registerNewExtension(extension.withUpdateCheck())
        }

        override fun onExtensionUpdated(extension: NovelExtension.Installed) {
            registerUpdatedExtension(extension.withUpdateCheck())
        }

        override fun onPackageUninstalled(pkgName: String) {
            unregisterNovelExtension(pkgName)
        }
    }

    /**
     * AnimeExtension method to set the update field of an installed anime extension.
     */
    private fun NovelExtension.Installed.withUpdateCheck(): NovelExtension.Installed {
        return if (updateExists()) {
            copy(hasUpdate = true)
        } else {
            this
        }
    }

    private fun NovelExtension.Installed.updateExists(availableNovelExtension: NovelExtension.Available? = null): Boolean {
        val availableExt = availableNovelExtension
            ?: _availableNovelExtensionsFlow.value.find { it.pkgName == pkgName }
        if (availableExt == null) return false

        return (availableExt.versionCode > versionCode)
    }
}