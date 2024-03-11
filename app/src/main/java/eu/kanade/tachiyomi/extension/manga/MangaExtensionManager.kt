package eu.kanade.tachiyomi.extension.manga

import android.content.Context
import android.graphics.drawable.Drawable
import ani.dantotsu.snackString
import ani.dantotsu.util.Logger
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.extension.manga.api.MangaExtensionGithubApi
import eu.kanade.tachiyomi.extension.manga.model.AvailableMangaSources
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.extension.manga.model.MangaLoadResult
import eu.kanade.tachiyomi.extension.manga.util.MangaExtensionInstallReceiver
import eu.kanade.tachiyomi.extension.manga.util.MangaExtensionInstaller
import eu.kanade.tachiyomi.extension.manga.util.MangaExtensionLoader
import eu.kanade.tachiyomi.util.preference.plusAssign
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import rx.Observable
import tachiyomi.core.util.lang.launchNow
import tachiyomi.core.util.lang.withUIContext
import tachiyomi.domain.source.manga.model.MangaSourceData
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

/**
 * The manager of extensions installed as another apk which extend the available sources. It handles
 * the retrieval of remotely available extensions as well as installing, updating and removing them.
 * To avoid malicious distribution, every extension must be signed and it will only be loaded if its
 * signature is trusted, otherwise the user will be prompted with a warning to trust it before being
 * loaded.
 *
 * @param context The application context.
 * @param preferences The application preferences.
 */
class MangaExtensionManager(
    private val context: Context,
    private val preferences: SourcePreferences = Injekt.get(),
) {

    var isInitialized = false
        private set

    /**
     * API where all the available extensions can be found.
     */
    private val api = MangaExtensionGithubApi()

    /**
     * The installer which installs, updates and uninstalls the extensions.
     */
    private val installer by lazy { MangaExtensionInstaller(context) }

    private val iconMap = mutableMapOf<String, Drawable>()

    private val _installedExtensionsFlow = MutableStateFlow(emptyList<MangaExtension.Installed>())
    val installedExtensionsFlow = _installedExtensionsFlow.asStateFlow()

    private var subLanguagesEnabledOnFirstRun = preferences.enabledLanguages().isSet()

    fun getAppIconForSource(sourceId: Long): Drawable? {
        val pkgName =
            _installedExtensionsFlow.value.find { ext -> ext.sources.any { it.id == sourceId } }?.pkgName
        if (pkgName != null) {
            return iconMap[pkgName]
                ?: iconMap.getOrPut(pkgName) { context.packageManager.getApplicationIcon(pkgName) }
        }
        return null
    }

    private val _availableExtensionsFlow = MutableStateFlow(emptyList<MangaExtension.Available>())
    val availableExtensionsFlow = _availableExtensionsFlow.asStateFlow()

    private var availableExtensionsSourcesData: Map<Long, MangaSourceData> = emptyMap()

    private fun setupAvailableExtensionsSourcesDataMap(extensions: List<MangaExtension.Available>) {
        if (extensions.isEmpty()) return
        availableExtensionsSourcesData = extensions
            .flatMap { ext -> ext.sources.map { it.toSourceData() } }
            .associateBy { it.id }
    }

    fun getSourceData(id: Long) = availableExtensionsSourcesData[id]

    private val _untrustedExtensionsFlow = MutableStateFlow(emptyList<MangaExtension.Untrusted>())
    val untrustedExtensionsFlow = _untrustedExtensionsFlow.asStateFlow()

    init {
        initExtensions()
        MangaExtensionInstallReceiver(InstallationListener()).register(context)
    }

    /**
     * Loads and registers the installed extensions.
     */
    private fun initExtensions() {
        val extensions = MangaExtensionLoader.loadMangaExtensions(context)

        _installedExtensionsFlow.value = extensions
            .filterIsInstance<MangaLoadResult.Success>()
            .map { it.extension }

        _untrustedExtensionsFlow.value = extensions
            .filterIsInstance<MangaLoadResult.Untrusted>()
            .map { it.extension }

        isInitialized = true
    }

    /**
     * Finds the available extensions in the [api] and updates [availableExtensions].
     */
    suspend fun findAvailableExtensions() {
        val extensions: List<MangaExtension.Available> = try {
            api.findExtensions()
        } catch (e: Exception) {
            Logger.log(e)
            withUIContext { snackString("Failed to get manga extensions") }
            emptyList()
        }

        enableAdditionalSubLanguages(extensions)

        _availableExtensionsFlow.value = extensions
        updatedInstalledExtensionsStatuses(extensions)
        setupAvailableExtensionsSourcesDataMap(extensions)
    }

    /**
     * Enables the additional sub-languages in the app first run. This addresses
     * the issue where users still need to enable some specific languages even when
     * the device language is inside that major group. As an example, if a user
     * has a zh device language, the app will also enable zh-Hans and zh-Hant.
     *
     * If the user have already changed the enabledLanguages preference value once,
     * the new languages will not be added to respect the user enabled choices.
     */
    private fun enableAdditionalSubLanguages(extensions: List<MangaExtension.Available>) {
        if (subLanguagesEnabledOnFirstRun || extensions.isEmpty()) {
            return
        }

        // Use the source lang as some aren't present on the extension level.
        val availableLanguages = extensions
            .flatMap(MangaExtension.Available::sources)
            .distinctBy(AvailableMangaSources::lang)
            .map(AvailableMangaSources::lang)

        val deviceLanguage = Locale.getDefault().language
        val defaultLanguages = preferences.enabledLanguages().defaultValue()
        val languagesToEnable = availableLanguages.filter {
            it != deviceLanguage && it.startsWith(deviceLanguage)
        }

        preferences.enabledLanguages().set(defaultLanguages + languagesToEnable)
        subLanguagesEnabledOnFirstRun = true
    }

    /**
     * Sets the update field of the installed extensions with the given [availableExtensions].
     *
     * @param availableExtensions The list of extensions given by the [api].
     */
    private fun updatedInstalledExtensionsStatuses(availableExtensions: List<MangaExtension.Available>) {
        if (availableExtensions.isEmpty()) {
            preferences.mangaExtensionUpdatesCount().set(0)
            return
        }

        val mutInstalledExtensions = _installedExtensionsFlow.value.toMutableList()
        var changed = false

        for ((index, installedExt) in mutInstalledExtensions.withIndex()) {
            val pkgName = installedExt.pkgName
            val availableExt = availableExtensions.find { it.pkgName == pkgName }

            if (!installedExt.isUnofficial && availableExt == null && !installedExt.isObsolete) {
                mutInstalledExtensions[index] = installedExt.copy(isObsolete = true)
                changed = true
            } else if (availableExt != null) {
                val hasUpdate = installedExt.updateExists(availableExt)

                if (installedExt.hasUpdate != hasUpdate) {
                    mutInstalledExtensions[index] = installedExt.copy(hasUpdate = hasUpdate)
                    changed = true
                }
            }
        }
        if (changed) {
            _installedExtensionsFlow.value = mutInstalledExtensions
        }
        updatePendingUpdatesCount()
    }

    /**
     * Returns an observable of the installation process for the given extension. It will complete
     * once the extension is installed or throws an error. The process will be canceled if
     * unsubscribed before its completion.
     *
     * @param extension The extension to be installed.
     */
    fun installExtension(extension: MangaExtension.Available): Observable<InstallStep> {
        return installer.downloadAndInstall(api.getApkUrl(extension), extension)
    }

    /**
     * Returns an observable of the installation process for the given extension. It will complete
     * once the extension is updated or throws an error. The process will be canceled if
     * unsubscribed before its completion.
     *
     * @param extension The extension to be updated.
     */
    fun updateExtension(extension: MangaExtension.Installed): Observable<InstallStep> {
        val availableExt = _availableExtensionsFlow.value.find { it.pkgName == extension.pkgName }
            ?: return Observable.empty()
        return installExtension(availableExt)
    }

    fun cancelInstallUpdateExtension(extension: MangaExtension) {
        installer.cancelInstall(extension.pkgName)
    }

    /**
     * Sets to "installing" status of an extension installation.
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
     * Uninstalls the extension that matches the given package name.
     *
     * @param pkgName The package name of the application to uninstall.
     */
    fun uninstallExtension(pkgName: String) {
        installer.uninstallApk(pkgName)
    }

    /**
     * Adds the given signature to the list of trusted signatures. It also loads in background the
     * extensions that match this signature.
     *
     * @param signature The signature to whitelist.
     */
    fun trustSignature(signature: String) {
        val untrustedSignatures = _untrustedExtensionsFlow.value.map { it.signatureHash }.toSet()
        if (signature !in untrustedSignatures) return

        MangaExtensionLoader.trustedSignatures += signature
        preferences.trustedSignatures() += signature

        val nowTrustedExtensions =
            _untrustedExtensionsFlow.value.filter { it.signatureHash == signature }
        _untrustedExtensionsFlow.value -= nowTrustedExtensions

        val ctx = context
        launchNow {
            nowTrustedExtensions
                .map { extension ->
                    async {
                        MangaExtensionLoader.loadMangaExtensionFromPkgName(
                            ctx,
                            extension.pkgName
                        )
                    }
                }
                .map { it.await() }
                .forEach { result ->
                    if (result is MangaLoadResult.Success) {
                        registerNewExtension(result.extension)
                    }
                }
        }
    }

    /**
     * Registers the given extension in this and the source managers.
     *
     * @param extension The extension to be registered.
     */
    private fun registerNewExtension(extension: MangaExtension.Installed) {
        _installedExtensionsFlow.value += extension
    }

    /**
     * Registers the given updated extension in this and the source managers previously removing
     * the outdated ones.
     *
     * @param extension The extension to be registered.
     */
    private fun registerUpdatedExtension(extension: MangaExtension.Installed) {
        val mutInstalledExtensions = _installedExtensionsFlow.value.toMutableList()
        val oldExtension = mutInstalledExtensions.find { it.pkgName == extension.pkgName }
        if (oldExtension != null) {
            mutInstalledExtensions -= oldExtension
        }
        mutInstalledExtensions += extension
        _installedExtensionsFlow.value = mutInstalledExtensions
    }

    /**
     * Unregisters the extension in this and the source managers given its package name. Note this
     * method is called for every uninstalled application in the system.
     *
     * @param pkgName The package name of the uninstalled application.
     */
    private fun unregisterExtension(pkgName: String) {
        val installedExtension = _installedExtensionsFlow.value.find { it.pkgName == pkgName }
        if (installedExtension != null) {
            _installedExtensionsFlow.value -= installedExtension
        }
        val untrustedExtension = _untrustedExtensionsFlow.value.find { it.pkgName == pkgName }
        if (untrustedExtension != null) {
            _untrustedExtensionsFlow.value -= untrustedExtension
        }
    }

    /**
     * Listener which receives events of the extensions being installed, updated or removed.
     */
    private inner class InstallationListener : MangaExtensionInstallReceiver.Listener {

        override fun onExtensionInstalled(extension: MangaExtension.Installed) {
            registerNewExtension(extension.withUpdateCheck())
            updatePendingUpdatesCount()
        }

        override fun onExtensionUpdated(extension: MangaExtension.Installed) {
            registerUpdatedExtension(extension.withUpdateCheck())
            updatePendingUpdatesCount()
        }

        override fun onExtensionUntrusted(extension: MangaExtension.Untrusted) {
            _untrustedExtensionsFlow.value += extension
        }

        override fun onPackageUninstalled(pkgName: String) {
            unregisterExtension(pkgName)
            updatePendingUpdatesCount()
        }
    }

    /**
     * Extension method to set the update field of an installed extension.
     */
    private fun MangaExtension.Installed.withUpdateCheck(): MangaExtension.Installed {
        return if (updateExists()) {
            copy(hasUpdate = true)
        } else {
            this
        }
    }

    private fun MangaExtension.Installed.updateExists(availableExtension: MangaExtension.Available? = null): Boolean {
        val availableExt =
            availableExtension ?: _availableExtensionsFlow.value.find { it.pkgName == pkgName }
        if (isUnofficial || availableExt == null) return false

        return (availableExt.versionCode > versionCode || availableExt.libVersion > libVersion)
    }

    private fun updatePendingUpdatesCount() {
        preferences.mangaExtensionUpdatesCount()
            .set(_installedExtensionsFlow.value.count { it.hasUpdate })
    }
}
