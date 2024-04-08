package ani.dantotsu.settings

import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Build
import android.os.Build.BRAND
import android.os.Build.DEVICE
import android.os.Build.SUPPORTED_ABIS
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.RELEASE
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.NotificationType
import ani.dantotsu.connections.discord.Discord
import ani.dantotsu.connections.mal.MAL
import ani.dantotsu.copyToClipboard
import ani.dantotsu.currContext
import ani.dantotsu.databinding.ActivitySettingsAboutBinding
import ani.dantotsu.databinding.ActivitySettingsAccountsBinding
import ani.dantotsu.databinding.ActivitySettingsAnimeBinding
import ani.dantotsu.databinding.ActivitySettingsBinding
import ani.dantotsu.databinding.ActivitySettingsCommonBinding
import ani.dantotsu.databinding.ActivitySettingsExtensionsBinding
import ani.dantotsu.databinding.ActivitySettingsMangaBinding
import ani.dantotsu.databinding.ActivitySettingsNotificationsBinding
import ani.dantotsu.databinding.ActivitySettingsThemeBinding
import ani.dantotsu.databinding.ItemRepositoryBinding
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.initActivity
import ani.dantotsu.loadImage
import ani.dantotsu.media.MediaType
import ani.dantotsu.navBarHeight
import ani.dantotsu.notifications.TaskScheduler
import ani.dantotsu.notifications.anilist.AnilistNotificationWorker
import ani.dantotsu.notifications.comment.CommentNotificationWorker
import ani.dantotsu.notifications.subscription.SubscriptionNotificationWorker.Companion.checkIntervals
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.openLinkInYouTube
import ani.dantotsu.openSettings
import ani.dantotsu.others.AppUpdater
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.pop
import ani.dantotsu.reloadActivity
import ani.dantotsu.restartApp
import ani.dantotsu.savePrefsToDownloads
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.settings.saving.internal.Location
import ani.dantotsu.settings.saving.internal.PreferenceKeystore
import ani.dantotsu.settings.saving.internal.PreferencePackager
import ani.dantotsu.snackString
import ani.dantotsu.startMainActivity
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import ani.dantotsu.util.LauncherWrapper
import ani.dantotsu.util.Logger
import ani.dantotsu.util.StoragePermissions.Companion.downloadsPermission
import com.google.android.material.textfield.TextInputEditText
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE
import eltos.simpledialogfragment.color.SimpleColorDialog
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.random.Random


class SettingsActivity : AppCompatActivity(), SimpleDialog.OnDialogResultListener {
    private val restartMainActivity = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = startMainActivity(this@SettingsActivity)
    }
    lateinit var binding: ActivitySettingsBinding
    lateinit var launcher: LauncherWrapper
    private lateinit var bindingAccounts: ActivitySettingsAccountsBinding
    private lateinit var bindingTheme: ActivitySettingsThemeBinding
    private lateinit var bindingExtensions: ActivitySettingsExtensionsBinding
    private lateinit var bindingCommon: ActivitySettingsCommonBinding
    private lateinit var bindingAnime: ActivitySettingsAnimeBinding
    private lateinit var bindingManga: ActivitySettingsMangaBinding
    private lateinit var bindingNotifications: ActivitySettingsNotificationsBinding
    private lateinit var bindingAbout: ActivitySettingsAboutBinding
    private val extensionInstaller = Injekt.get<BasePreferences>().extensionInstaller()
    private var cursedCounter = 0
    private val animeExtensionManager: AnimeExtensionManager by injectLazy()
    private val mangaExtensionManager: MangaExtensionManager by injectLazy()

    @kotlin.OptIn(DelicateCoroutinesApi::class)
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)

        val openDocumentLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    try {
                        val jsonString = contentResolver.openInputStream(uri)?.readBytes()
                            ?: throw Exception("Error reading file")
                        val name = DocumentFile.fromSingleUri(this, uri)?.name ?: "settings"
                        //.sani is encrypted, .ani is not
                        if (name.endsWith(".sani")) {
                            passwordAlertDialog(false) { password ->
                                if (password != null) {
                                    val salt = jsonString.copyOfRange(0, 16)
                                    val encrypted = jsonString.copyOfRange(16, jsonString.size)
                                    val decryptedJson = try {
                                        PreferenceKeystore.decryptWithPassword(
                                            password,
                                            encrypted,
                                            salt
                                        )
                                    } catch (e: Exception) {
                                        toast(getString(R.string.incorrect_password))
                                        return@passwordAlertDialog
                                    }
                                    if (PreferencePackager.unpack(decryptedJson))
                                        restartApp(binding.root)
                                } else {
                                    toast(getString(R.string.password_cannot_be_empty))
                                }
                            }
                        } else if (name.endsWith(".ani")) {
                            val decryptedJson = jsonString.toString(Charsets.UTF_8)
                            if (PreferencePackager.unpack(decryptedJson))
                                restartApp(binding.root)
                        } else {
                            toast(getString(R.string.unknown_file_type))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        toast(getString(R.string.error_importing_settings))
                    }
                }
            }
        val contract = ActivityResultContracts.OpenDocumentTree()
        launcher = LauncherWrapper(this, contract)

        binding.settingsVersion.text = getString(R.string.version_current, BuildConfig.VERSION_NAME)
        binding.settingsVersion.setOnLongClickListener {
            copyToClipboard(getDeviceInfo(), false)
            toast(getString(R.string.copied_device_info))
            return@setOnLongClickListener true
        }

        binding.settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }

        onBackPressedDispatcher.addCallback(this, restartMainActivity)

        binding.settingsBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        bindingAccounts = ActivitySettingsAccountsBinding.bind(binding.root).apply {
            settingsAccountHelp.setOnClickListener {
                val title = getString(R.string.account_help)
                val full = getString(R.string.full_account_help)
                CustomBottomDialog.newInstance().apply {
                    setTitleText(title)
                    addView(
                        TextView(it.context).apply {
                            val markWon = Markwon.builder(it.context)
                                .usePlugin(SoftBreakAddsNewLinePlugin.create()).build()
                            markWon.setMarkdown(this, full)
                        }
                    )
                }.show(supportFragmentManager, "dialog")
            }

            fun reload() {
                if (Anilist.token != null) {
                    settingsAnilistLogin.setText(R.string.logout)
                    settingsAnilistLogin.setOnClickListener {
                        Anilist.removeSavedToken()
                        restartMainActivity.isEnabled = true
                        reload()
                    }
                    settingsAnilistUsername.visibility = View.VISIBLE
                    settingsAnilistUsername.text = Anilist.username
                    settingsAnilistAvatar.loadImage(Anilist.avatar)
                    settingsAnilistAvatar.setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        val anilistLink = getString(
                            R.string.anilist_link,
                            PrefManager.getVal<String>(PrefName.AnilistUserName)
                        )
                        openLinkInBrowser(anilistLink)
                        true
                    }

                    settingsMALLoginRequired.visibility = View.GONE
                    settingsMALLogin.visibility = View.VISIBLE
                    settingsMALUsername.visibility = View.VISIBLE

                    if (MAL.token != null) {
                        settingsMALLogin.setText(R.string.logout)
                        settingsMALLogin.setOnClickListener {
                            MAL.removeSavedToken()
                            restartMainActivity.isEnabled = true
                            reload()
                        }
                        settingsMALUsername.visibility = View.VISIBLE
                        settingsMALUsername.text = MAL.username
                        settingsMALAvatar.loadImage(MAL.avatar)
                        settingsMALAvatar.setOnClickListener {
                            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            val myanilistLink = getString(R.string.myanilist_link, MAL.username)
                            openLinkInBrowser(myanilistLink)
                            true
                        }
                    } else {
                        settingsMALAvatar.setImageResource(R.drawable.ic_round_person_24)
                        settingsMALUsername.visibility = View.GONE
                        settingsMALLogin.setText(R.string.login)
                        settingsMALLogin.setOnClickListener {
                            MAL.loginIntent(this@SettingsActivity)
                        }
                    }
                } else {
                    settingsAnilistAvatar.setImageResource(R.drawable.ic_round_person_24)
                    settingsAnilistUsername.visibility = View.GONE
                    settingsAnilistLogin.setText(R.string.login)
                    settingsAnilistLogin.setOnClickListener {
                        Anilist.loginIntent(this@SettingsActivity)
                    }
                    settingsMALLoginRequired.visibility = View.VISIBLE
                    settingsMALLogin.visibility = View.GONE
                    settingsMALUsername.visibility = View.GONE
                }

                if (Discord.token != null) {
                    val id = PrefManager.getVal(PrefName.DiscordId, null as String?)
                    val avatar = PrefManager.getVal(PrefName.DiscordAvatar, null as String?)
                    val username = PrefManager.getVal(PrefName.DiscordUserName, null as String?)
                    if (id != null && avatar != null) {
                        settingsDiscordAvatar.loadImage("https://cdn.discordapp.com/avatars/$id/$avatar.png")
                        settingsDiscordAvatar.setOnClickListener {
                            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            val discordLink = getString(R.string.discord_link, id)
                            openLinkInBrowser(discordLink)
                            true
                        }
                    }
                    settingsDiscordUsername.visibility = View.VISIBLE
                    settingsDiscordUsername.text =
                        username ?: Discord.token?.replace(Regex("."), "*")
                    settingsDiscordLogin.setText(R.string.logout)
                    settingsDiscordLogin.setOnClickListener {
                        Discord.removeSavedToken(this@SettingsActivity)
                        restartMainActivity.isEnabled = true
                        reload()
                    }

                    settingsImageSwitcher.visibility = View.VISIBLE
                    var initialStatus = when (PrefManager.getVal<String>(PrefName.DiscordStatus)) {
                        "online" -> R.drawable.discord_status_online
                        "idle" -> R.drawable.discord_status_idle
                        "dnd" -> R.drawable.discord_status_dnd
                        else -> R.drawable.discord_status_online
                    }
                    settingsImageSwitcher.setImageResource(initialStatus)

                    val zoomInAnimation =
                        AnimationUtils.loadAnimation(this@SettingsActivity, R.anim.bounce_zoom)
                    settingsImageSwitcher.setOnClickListener {
                        var status = "online"
                        initialStatus = when (initialStatus) {
                            R.drawable.discord_status_online -> {
                                status = "idle"
                                R.drawable.discord_status_idle
                            }

                            R.drawable.discord_status_idle -> {
                                status = "dnd"
                                R.drawable.discord_status_dnd
                            }

                            R.drawable.discord_status_dnd -> {
                                status = "online"
                                R.drawable.discord_status_online
                            }

                            else -> R.drawable.discord_status_online
                        }

                        PrefManager.setVal(PrefName.DiscordStatus, status)
                        settingsImageSwitcher.setImageResource(initialStatus)
                        settingsImageSwitcher.startAnimation(zoomInAnimation)
                    }
                    settingsImageSwitcher.setOnLongClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        DiscordDialogFragment().show(supportFragmentManager, "dialog")
                        true
                    }
                } else {
                    settingsImageSwitcher.visibility = View.GONE
                    settingsDiscordAvatar.setImageResource(R.drawable.ic_round_person_24)
                    settingsDiscordUsername.visibility = View.GONE
                    settingsDiscordLogin.setText(R.string.login)
                    settingsDiscordLogin.setOnClickListener {
                        Discord.warning(this@SettingsActivity)
                            .show(supportFragmentManager, "dialog")
                    }
                }
            }
            reload()
        }

        bindingTheme = ActivitySettingsThemeBinding.bind(binding.root).apply {
            settingsUseMaterialYou.isChecked =
                PrefManager.getVal(PrefName.UseMaterialYou)
            settingsUseMaterialYou.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.UseMaterialYou, isChecked)
                if (isChecked) settingsUseCustomTheme.isChecked = false
                restartApp(binding.root)
            }

            settingsUseCustomTheme.isChecked =
                PrefManager.getVal(PrefName.UseCustomTheme)
            settingsUseCustomTheme.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.UseCustomTheme, isChecked)
                if (isChecked) {
                    settingsUseMaterialYou.isChecked = false
                }

                restartApp(binding.root)
            }

            settingsUseSourceTheme.isChecked =
                PrefManager.getVal(PrefName.UseSourceTheme)
            settingsUseSourceTheme.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.UseSourceTheme, isChecked)
                restartApp(binding.root)
            }

            settingsUseOLED.isChecked = PrefManager.getVal(PrefName.UseOLED)
            settingsUseOLED.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.UseOLED, isChecked)
                restartApp(binding.root)
            }

            val themeString: String = PrefManager.getVal(PrefName.Theme)
            val themeText = themeString.substring(0, 1) + themeString.substring(1).lowercase()
            themeSwitcher.setText(themeText)

            themeSwitcher.setAdapter(
                ArrayAdapter(
                    this@SettingsActivity,
                    R.layout.item_dropdown,
                    ThemeManager.Companion.Theme.entries
                        .map { it.theme.substring(0, 1) + it.theme.substring(1).lowercase() })
            )

            themeSwitcher.setOnItemClickListener { _, _, i, _ ->
                PrefManager.setVal(PrefName.Theme, ThemeManager.Companion.Theme.entries[i].theme)
                //ActivityHelper.shouldRefreshMainActivity = true
                themeSwitcher.clearFocus()
                restartApp(binding.root)
            }

            customTheme.setOnClickListener {
                val originalColor: Int = PrefManager.getVal(PrefName.CustomThemeInt)

                class CustomColorDialog : SimpleColorDialog() { //idk where to put it
                    override fun onPositiveButtonClick() {
                        restartApp(binding.root)
                        super.onPositiveButtonClick()
                    }
                }

                val tag = "colorPicker"
                CustomColorDialog().title(R.string.custom_theme)
                    .colorPreset(originalColor)
                    .colors(this@SettingsActivity, SimpleColorDialog.MATERIAL_COLOR_PALLET)
                    .allowCustom(true)
                    .showOutline(0x46000000)
                    .gridNumColumn(5)
                    .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                    .neg()
                    .show(this@SettingsActivity, tag)
            }

            var previous: View = when (PrefManager.getVal<Int>(PrefName.DarkMode)) {
                0 -> settingsUiAuto
                1 -> settingsUiLight
                2 -> settingsUiDark
                else -> settingsUiAuto
            }
            previous.alpha = 1f
            fun uiTheme(mode: Int, current: View) {
                previous.alpha = 0.33f
                previous = current
                current.alpha = 1f
                PrefManager.setVal(PrefName.DarkMode, mode)
                reloadActivity()
            }

            settingsUiAuto.setOnClickListener {
                uiTheme(0, it)
            }

            settingsUiLight.setOnClickListener {
                settingsUseOLED.isChecked = false
                uiTheme(1, it)
            }

            settingsUiDark.setOnClickListener {
                uiTheme(2, it)
            }
        }

        val managers = arrayOf("Default", "1DM", "ADM")
        val downloadManagerDialog =
            AlertDialog.Builder(this, R.style.MyPopup).setTitle(R.string.download_manager)
        var downloadManager: Int = PrefManager.getVal(PrefName.DownloadManager)

        bindingAnime = ActivitySettingsAnimeBinding.bind(binding.root).apply {
            settingsPlayer.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, PlayerSettingsActivity::class.java))
            }

            purgeAnimeDownloads.setOnClickListener {
                val dialog = AlertDialog.Builder(this@SettingsActivity, R.style.MyPopup)
                    .setTitle(R.string.purge_anime_downloads)
                    .setMessage(getString(R.string.purge_confirm, getString(R.string.anime)))
                    .setPositiveButton(R.string.yes) { dialog, _ ->
                        val downloadsManager = Injekt.get<DownloadsManager>()
                        downloadsManager.purgeDownloads(MediaType.ANIME)
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.no) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            settingsPreferDub.isChecked = PrefManager.getVal(PrefName.SettingsPreferDub)
            settingsPreferDub.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.SettingsPreferDub, isChecked)
            }


            settingsShowYt.isChecked = PrefManager.getVal(PrefName.ShowYtButton)
            settingsShowYt.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.ShowYtButton, isChecked)
            }
            settingsIncludeAnimeList.isChecked = PrefManager.getVal(PrefName.IncludeAnimeList)
            settingsIncludeAnimeList.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.IncludeAnimeList, isChecked)
                restartApp(binding.root)
            }

            var previousEp: View = when (PrefManager.getVal<Int>(PrefName.AnimeDefaultView)) {
                0 -> settingsEpList
                1 -> settingsEpGrid
                2 -> settingsEpCompact
                else -> settingsEpList
            }
            previousEp.alpha = 1f
            fun uiEp(mode: Int, current: View) {
                previousEp.alpha = 0.33f
                previousEp = current
                current.alpha = 1f
                PrefManager.setVal(PrefName.AnimeDefaultView, mode)
            }

            settingsEpList.setOnClickListener {
                uiEp(0, it)
            }

            settingsEpGrid.setOnClickListener {
                uiEp(1, it)
            }

            settingsEpCompact.setOnClickListener {
                uiEp(2, it)
            }
        }

        bindingManga = ActivitySettingsMangaBinding.bind(binding.root).apply {
            purgeMangaDownloads.setOnClickListener {
                val dialog = AlertDialog.Builder(this@SettingsActivity, R.style.MyPopup)
                    .setTitle(R.string.purge_manga_downloads)
                    .setMessage(getString(R.string.purge_confirm, getString(R.string.manga)))
                    .setPositiveButton(R.string.yes) { dialog, _ ->
                        val downloadsManager = Injekt.get<DownloadsManager>()
                        downloadsManager.purgeDownloads(MediaType.MANGA)
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.no) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            purgeNovelDownloads.setOnClickListener {
                val dialog = AlertDialog.Builder(this@SettingsActivity, R.style.MyPopup)
                    .setTitle(R.string.purge_novel_downloads)
                    .setMessage(getString(R.string.purge_confirm, getString(R.string.novels)))
                    .setPositiveButton(R.string.yes) { dialog, _ ->
                        val downloadsManager = Injekt.get<DownloadsManager>()
                        downloadsManager.purgeDownloads(MediaType.NOVEL)
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.no) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            settingsReader.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, ReaderSettingsActivity::class.java))
            }

            var previousChp: View = when (PrefManager.getVal<Int>(PrefName.MangaDefaultView)) {
                0 -> settingsChpList
                1 -> settingsChpCompact
                else -> settingsChpList
            }
            previousChp.alpha = 1f
            fun uiChp(mode: Int, current: View) {
                previousChp.alpha = 0.33f
                previousChp = current
                current.alpha = 1f
                PrefManager.setVal(PrefName.MangaDefaultView, mode)
            }

            settingsChpList.setOnClickListener {
                uiChp(0, it)
            }

            settingsChpCompact.setOnClickListener {
                uiChp(1, it)
            }

            settingsIncludeMangaList.isChecked = PrefManager.getVal(PrefName.IncludeMangaList)
            settingsIncludeMangaList.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.IncludeMangaList, isChecked)
                restartApp(binding.root)
            }
        }

        bindingExtensions = ActivitySettingsExtensionsBinding.bind(binding.root).apply {

            fun setExtensionOutput(repoInventory: ViewGroup, type: MediaType) {
                repoInventory.removeAllViews()
                val prefName: PrefName? = when (type) {
                    MediaType.ANIME -> { PrefName.AnimeExtensionRepos }
                    MediaType.MANGA -> { PrefName.MangaExtensionRepos }
                    else -> { null }
                }
                prefName?.let { repoList ->
                    PrefManager.getVal<Set<String>>(repoList).forEach { item ->
                        val view = ItemRepositoryBinding.inflate(
                            LayoutInflater.from(repoInventory.context), repoInventory, true
                        )
                        view.repositoryItem.text = item.removePrefix("https://raw.githubusercontent.com")
                        view.repositoryItem.setOnClickListener {
                            AlertDialog.Builder(this@SettingsActivity, R.style.MyPopup)
                                .setTitle(R.string.rem_repository)
                                .setMessage(item)
                                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                                    val repos = PrefManager.getVal<Set<String>>(repoList).minus(item)
                                    PrefManager.setVal(repoList, repos)
                                    setExtensionOutput(repoInventory, type)
                                    CoroutineScope(Dispatchers.IO).launch {
                                        when (type) {
                                            MediaType.ANIME -> { animeExtensionManager.findAvailableExtensions() }
                                            MediaType.MANGA -> { mangaExtensionManager.findAvailableExtensions() }
                                            else -> { }
                                        }
                                    }
                                    dialog.dismiss()
                                }
                                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .create()
                                .show()
                        }
                        view.repositoryItem.setOnLongClickListener {
                            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            copyToClipboard(item, true)
                            true
                        }
                    }
                    repoInventory.isVisible = repoInventory.childCount > 0
                }
            }

            fun processUserInput(input: String, mediaType: MediaType) {
                val entry = if (input.endsWith("/") || input.endsWith("index.min.json"))
                    input.substring(0, input.lastIndexOf("/")) else input
                if (mediaType == MediaType.ANIME) {
                    val anime =
                        PrefManager.getVal<Set<String>>(PrefName.AnimeExtensionRepos).plus(entry)
                    PrefManager.setVal(PrefName.AnimeExtensionRepos, anime)
                    CoroutineScope(Dispatchers.IO).launch {
                        animeExtensionManager.findAvailableExtensions()
                    }
                    setExtensionOutput(animeRepoInventory, MediaType.ANIME)
                }
                if (mediaType == MediaType.MANGA) {
                    val manga =
                        PrefManager.getVal<Set<String>>(PrefName.MangaExtensionRepos).plus(entry)
                    PrefManager.setVal(PrefName.MangaExtensionRepos, manga)
                    CoroutineScope(Dispatchers.IO).launch {
                        mangaExtensionManager.findAvailableExtensions()
                    }
                    setExtensionOutput(mangaRepoInventory, MediaType.MANGA)
                }
            }

            fun processEditorAction(dialog: AlertDialog, editText: EditText, mediaType: MediaType) {
                editText.setOnEditorActionListener { textView, action, keyEvent ->
                    if (action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_DONE ||
                        (keyEvent?.action == KeyEvent.ACTION_UP
                                && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)
                    ) {
                        return@setOnEditorActionListener if (textView.text.isNullOrBlank()) {
                            false
                        } else {
                            processUserInput(textView.text.toString(), mediaType)
                            dialog.dismiss()
                            true
                        }
                    }
                    false
                }
            }

            setExtensionOutput(animeRepoInventory, MediaType.ANIME)
            setExtensionOutput(mangaRepoInventory, MediaType.MANGA)
            animeAddRepository.setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.dialog_user_agent, null)
                val editText =
                    dialogView.findViewById<TextInputEditText>(R.id.userAgentTextBox).apply {
                        hint = getString(R.string.anime_add_repository)
                    }
                val alertDialog = AlertDialog.Builder(this@SettingsActivity, R.style.MyPopup)
                    .setTitle(R.string.anime_add_repository)
                    .setView(dialogView)
                    .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                        if (!editText.text.isNullOrBlank())
                            processUserInput(editText.text.toString(), MediaType.ANIME)
                        dialog.dismiss()
                    }
                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()

                processEditorAction(alertDialog, editText, MediaType.ANIME)
                alertDialog.show()
                alertDialog.window?.setDimAmount(0.8f)
            }

            mangaAddRepository.setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.dialog_user_agent, null)
                val editText =
                    dialogView.findViewById<TextInputEditText>(R.id.userAgentTextBox).apply {
                        hint = getString(R.string.manga_add_repository)
                    }
                val alertDialog = AlertDialog.Builder(this@SettingsActivity, R.style.MyPopup)
                    .setTitle(R.string.manga_add_repository)
                    .setView(dialogView)
                    .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                        if (!editText.text.isNullOrBlank())
                            processUserInput(editText.text.toString(), MediaType.MANGA)
                        dialog.dismiss()
                    }
                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()

                processEditorAction(alertDialog, editText, MediaType.MANGA)
                alertDialog.show()
                alertDialog.window?.setDimAmount(0.8f)
            }

            settingsForceLegacyInstall.isChecked =
                extensionInstaller.get() == BasePreferences.ExtensionInstaller.LEGACY
            settingsForceLegacyInstall.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    extensionInstaller.set(BasePreferences.ExtensionInstaller.LEGACY)
                } else {
                    extensionInstaller.set(BasePreferences.ExtensionInstaller.PACKAGEINSTALLER)
                }
            }

            skipExtensionIcons.isChecked =
                PrefManager.getVal(PrefName.SkipExtensionIcons)
            skipExtensionIcons.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.getVal(PrefName.SkipExtensionIcons, isChecked)
            }
            NSFWExtension.isChecked = PrefManager.getVal(PrefName.NSFWExtension)
            NSFWExtension.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.NSFWExtension, isChecked)

            }

            userAgent.setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.dialog_user_agent, null)
                val editText = dialogView.findViewById<TextInputEditText>(R.id.userAgentTextBox)
                editText.setText(PrefManager.getVal<String>(PrefName.DefaultUserAgent))
                val alertDialog = AlertDialog.Builder(this@SettingsActivity, R.style.MyPopup)
                    .setTitle(R.string.user_agent)
                    .setView(dialogView)
                    .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                        PrefManager.setVal(PrefName.DefaultUserAgent, editText.text.toString())
                        dialog.dismiss()
                    }
                    .setNeutralButton(getString(R.string.reset)) { dialog, _ ->
                        PrefManager.removeVal(PrefName.DefaultUserAgent)
                        editText.setText("")
                        dialog.dismiss()
                    }
                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()

                alertDialog.show()
                alertDialog.window?.setDimAmount(0.8f)
            }
        }


        val exDns = listOf(
            "None",
            "Cloudflare",
            "Google",
            "AdGuard",
            "Quad9",
            "AliDNS",
            "DNSPod",
            "360",
            "Quad101",
            "Mullvad",
            "Controld",
            "Njalla",
            "Shecan",
            "Libre"
        )

        bindingCommon = ActivitySettingsCommonBinding.bind(binding.root).apply {
            settingsDownloadManager.setOnClickListener {
                val dialog = downloadManagerDialog.setSingleChoiceItems(
                    managers,
                    downloadManager
                ) { dialog, count ->
                    downloadManager = count
                    PrefManager.setVal(PrefName.DownloadManager, downloadManager)
                    dialog.dismiss()
                }.show()
                dialog.window?.setDimAmount(0.8f)
            }

            importExportSettings.setOnClickListener {
                downloadsPermission(this@SettingsActivity)
                val selectedArray = mutableListOf(false)
                val filteredLocations = Location.entries.filter { it.exportable }
                selectedArray.addAll(List(filteredLocations.size - 1) { false })
                val dialog = AlertDialog.Builder(this@SettingsActivity, R.style.MyPopup)
                    .setTitle(R.string.backup_restore)
                    .setMultiChoiceItems(
                        filteredLocations.map { it.name }.toTypedArray(),
                        selectedArray.toBooleanArray()
                    ) { _, which, isChecked ->
                        selectedArray[which] = isChecked
                    }
                    .setPositiveButton(R.string.button_restore) { dialog, _ ->
                        openDocumentLauncher.launch(arrayOf("*/*"))
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.button_backup) { dialog, _ ->
                        if (!selectedArray.contains(true)) {
                            toast(R.string.no_location_selected)
                            return@setNegativeButton
                        }
                        dialog.dismiss()
                        val selected =
                            filteredLocations.filterIndexed { index, _ -> selectedArray[index] }
                        if (selected.contains(Location.Protected)) {
                            passwordAlertDialog(true) { password ->
                                if (password != null) {
                                    savePrefsToDownloads(
                                        "DantotsuSettings",
                                        PrefManager.exportAllPrefs(selected),
                                        this@SettingsActivity,
                                        password
                                    )
                                } else {
                                    toast(R.string.password_cannot_be_empty)
                                }
                            }
                        } else {
                            savePrefsToDownloads(
                                "DantotsuSettings",
                                PrefManager.exportAllPrefs(selected),
                                this@SettingsActivity,
                                null
                            )
                        }
                    }
                    .setNeutralButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            settingsExtensionDns.setText(exDns[PrefManager.getVal(PrefName.DohProvider)])
            settingsExtensionDns.setAdapter(
                ArrayAdapter(
                    this@SettingsActivity,
                    R.layout.item_dropdown,
                    exDns
                )
            )
            settingsExtensionDns.setOnItemClickListener { _, _, i, _ ->
                PrefManager.setVal(PrefName.DohProvider, i)
                settingsExtensionDns.clearFocus()
                restartApp(binding.root)
            }

            settingsContinueMedia.isChecked = PrefManager.getVal(PrefName.ContinueMedia)
            settingsContinueMedia.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.ContinueMedia, isChecked)
            }

            settingsSearchSources.isChecked = PrefManager.getVal(PrefName.SearchSources)
            settingsSearchSources.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.SearchSources, isChecked)
            }

            settingsRecentlyListOnly.isChecked = PrefManager.getVal(PrefName.RecentlyListOnly)
            settingsRecentlyListOnly.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.RecentlyListOnly, isChecked)
            }
            settingsAdultAnimeOnly.isChecked = PrefManager.getVal(PrefName.AdultOnly)
            settingsAdultAnimeOnly.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.AdultOnly, isChecked)
                restartApp(binding.root)
            }

            settingsDownloadLocation.setOnClickListener {
                val dialog = AlertDialog.Builder(this@SettingsActivity, R.style.MyPopup)
                    .setTitle(R.string.change_download_location)
                    .setMessage(R.string.download_location_msg)
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        val oldUri = PrefManager.getVal<String>(PrefName.DownloadsDir)
                        launcher.registerForCallback { success ->
                            if (success) {
                                toast(getString(R.string.please_wait))
                                val newUri = PrefManager.getVal<String>(PrefName.DownloadsDir)
                                GlobalScope.launch(Dispatchers.IO) {
                                    Injekt.get<DownloadsManager>().moveDownloadsDir(
                                        this@SettingsActivity,
                                        Uri.parse(oldUri), Uri.parse(newUri)
                                    ) { finished, message ->
                                        if (finished) {
                                            toast(getString(R.string.success))
                                        } else {
                                            toast(message)
                                        }
                                    }
                                }
                            } else {
                                toast(getString(R.string.error))
                            }
                        }
                        launcher.launch()
                        dialog.dismiss()
                    }
                    .setNeutralButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            var previousStart: View = when (PrefManager.getVal<Int>(PrefName.DefaultStartUpTab)) {
                0 -> uiSettingsAnime
                1 -> uiSettingsHome
                2 -> uiSettingsManga
                else -> uiSettingsHome
            }
            previousStart.alpha = 1f
            fun uiDefault(mode: Int, current: View) {
                previousStart.alpha = 0.33f
                previousStart = current
                current.alpha = 1f
                PrefManager.setVal(PrefName.DefaultStartUpTab, mode)
                initActivity(this@SettingsActivity)
            }

            uiSettingsAnime.setOnClickListener {
                uiDefault(0, it)
            }

            uiSettingsHome.setOnClickListener {
                uiDefault(1, it)
            }

            uiSettingsManga.setOnClickListener {
                uiDefault(2, it)
            }

            settingsUi.setOnClickListener {
                startActivity(
                    Intent(
                        this@SettingsActivity,
                        UserInterfaceSettingsActivity::class.java
                    )
                )
            }
        }

        bindingNotifications = ActivitySettingsNotificationsBinding.bind(binding.root).apply {
            var curTime = PrefManager.getVal<Int>(PrefName.SubscriptionNotificationInterval)
            val timeNames = checkIntervals.map {
                val mins = it % 60
                val hours = it / 60
                if (it > 0) "${if (hours > 0) "$hours hrs " else ""}${if (mins > 0) "$mins mins" else ""}"
                else getString(R.string.do_not_update)
            }.toTypedArray()

            settingsSubscriptionsTime.text =
                getString(R.string.subscriptions_checking_time_s, timeNames[curTime])
            val speedDialog = AlertDialog.Builder(this@SettingsActivity, R.style.MyPopup)
                .setTitle(R.string.subscriptions_checking_time)
            settingsSubscriptionsTime.setOnClickListener {
                val dialog = speedDialog.setSingleChoiceItems(timeNames, curTime) { dialog, i ->
                    curTime = i
                    settingsSubscriptionsTime.text =
                        getString(R.string.subscriptions_checking_time_s, timeNames[i])
                    PrefManager.setVal(PrefName.SubscriptionNotificationInterval, curTime)
                    dialog.dismiss()
                    TaskScheduler.create(
                        this@SettingsActivity,
                        PrefManager.getVal(PrefName.UseAlarmManager)
                    ).scheduleAllTasks(this@SettingsActivity)
                }.show()
                dialog.window?.setDimAmount(0.8f)
            }

            settingsSubscriptionsTime.setOnLongClickListener {
                TaskScheduler.create(
                    this@SettingsActivity,
                    PrefManager.getVal(PrefName.UseAlarmManager)
                ).scheduleAllTasks(this@SettingsActivity)
                true
            }

            val aTimeNames = AnilistNotificationWorker.checkIntervals.map { it.toInt() }
            val aItems = aTimeNames.map {
                val mins = it % 60
                val hours = it / 60
                if (it > 0) "${if (hours > 0) "$hours hrs " else ""}${if (mins > 0) "$mins mins" else ""}"
                else getString(R.string.do_not_update)
            }
            settingsAnilistSubscriptionsTime.text =
                getString(
                    R.string.anilist_notifications_checking_time,
                    aItems[PrefManager.getVal(PrefName.AnilistNotificationInterval)]
                )
            settingsAnilistSubscriptionsTime.setOnClickListener {

                val selected = PrefManager.getVal<Int>(PrefName.AnilistNotificationInterval)
                val dialog = AlertDialog.Builder(this@SettingsActivity, R.style.MyPopup)
                    .setTitle(R.string.subscriptions_checking_time)
                    .setSingleChoiceItems(aItems.toTypedArray(), selected) { dialog, i ->
                        PrefManager.setVal(PrefName.AnilistNotificationInterval, i)
                        settingsAnilistSubscriptionsTime.text =
                            getString(R.string.anilist_notifications_checking_time, aItems[i])
                        dialog.dismiss()
                        TaskScheduler.create(
                            this@SettingsActivity,
                            PrefManager.getVal(PrefName.UseAlarmManager)
                        ).scheduleAllTasks(this@SettingsActivity)
                    }
                    .create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            settingsAnilistNotifications.setOnClickListener {
                val types = NotificationType.entries.map { it.name }
                val filteredTypes =
                    PrefManager.getVal<Set<String>>(PrefName.AnilistFilteredTypes).toMutableSet()
                val selected = types.map { filteredTypes.contains(it) }.toBooleanArray()
                val dialog = AlertDialog.Builder(this@SettingsActivity, R.style.MyPopup)
                    .setTitle(R.string.anilist_notification_filters)
                    .setMultiChoiceItems(types.toTypedArray(), selected) { _, which, isChecked ->
                        val type = types[which]
                        if (isChecked) {
                            filteredTypes.add(type)
                        } else {
                            filteredTypes.remove(type)
                        }
                        PrefManager.setVal(PrefName.AnilistFilteredTypes, filteredTypes)
                    }
                    .create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            val cTimeNames = CommentNotificationWorker.checkIntervals.map { it.toInt() }
            val cItems = cTimeNames.map {
                val mins = it % 60
                val hours = it / 60
                if (it > 0) "${if (hours > 0) "$hours hrs " else ""}${if (mins > 0) "$mins mins" else ""}"
                else getString(R.string.do_not_update)
            }

            settingsCommentSubscriptionsTime.text =
                getString(
                    R.string.comment_notification_checking_time,
                    cItems[PrefManager.getVal(PrefName.CommentNotificationInterval)]
                )
            settingsCommentSubscriptionsTime.setOnClickListener {
                val selected = PrefManager.getVal<Int>(PrefName.CommentNotificationInterval)
                val dialog = AlertDialog.Builder(this@SettingsActivity, R.style.MyPopup)
                    .setTitle(R.string.subscriptions_checking_time)
                    .setSingleChoiceItems(cItems.toTypedArray(), selected) { dialog, i ->
                        PrefManager.setVal(PrefName.CommentNotificationInterval, i)
                        settingsCommentSubscriptionsTime.text =
                            getString(R.string.comment_notification_checking_time, cItems[i])
                        dialog.dismiss()
                        TaskScheduler.create(
                            this@SettingsActivity,
                            PrefManager.getVal(PrefName.UseAlarmManager)
                        ).scheduleAllTasks(this@SettingsActivity)
                    }
                    .create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            settingsNotificationsCheckingSubscriptions.isChecked =
                PrefManager.getVal(PrefName.SubscriptionCheckingNotifications)
            settingsNotificationsCheckingSubscriptions.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.SubscriptionCheckingNotifications, isChecked)
            }

            settingsNotificationsCheckingSubscriptions.setOnLongClickListener {
                openSettings(this@SettingsActivity, null)
            }

            settingsNotificationsUseAlarmManager.isChecked =
                PrefManager.getVal(PrefName.UseAlarmManager)

            settingsNotificationsUseAlarmManager.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    val alertDialog = AlertDialog.Builder(this@SettingsActivity, R.style.MyPopup)
                        .setTitle(R.string.use_alarm_manager)
                        .setMessage(R.string.use_alarm_manager_confirm)
                        .setPositiveButton(R.string.use) { dialog, _ ->
                            PrefManager.setVal(PrefName.UseAlarmManager, true)
                            if (SDK_INT >= Build.VERSION_CODES.S) {
                                if (!(getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()) {
                                    val intent =
                                        Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM")
                                    startActivity(intent)
                                    settingsNotificationsCheckingSubscriptions.isChecked = true
                                }
                            }
                            dialog.dismiss()
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ ->
                            settingsNotificationsCheckingSubscriptions.isChecked = false
                            PrefManager.setVal(PrefName.UseAlarmManager, false)
                            dialog.dismiss()
                        }
                        .create()
                    alertDialog.window?.setDimAmount(0.8f)
                    alertDialog.show()
                } else {
                    PrefManager.setVal(PrefName.UseAlarmManager, false)
                    TaskScheduler.create(this@SettingsActivity, true).cancelAllTasks()
                    TaskScheduler.create(this@SettingsActivity, false)
                        .scheduleAllTasks(this@SettingsActivity)
                }
            }
        }

        bindingAbout = ActivitySettingsAboutBinding.bind(binding.root).apply {
            settingsDev.setOnClickListener {
                DevelopersDialogFragment().show(supportFragmentManager, "dialog")
            }
            settingsForks.setOnClickListener {
                ForksDialogFragment().show(supportFragmentManager, "dialog")
            }
            settingsDisclaimer.setOnClickListener {
                val title = getString(R.string.disclaimer)
                val text = TextView(this@SettingsActivity)
                text.setText(R.string.full_disclaimer)

                CustomBottomDialog.newInstance().apply {
                    setTitleText(title)
                    addView(text)
                    setNegativeButton(currContext()!!.getString(R.string.close)) {
                        dismiss()
                    }
                    show(supportFragmentManager, "dialog")
                }
            }

            settingsFAQ.setOnClickListener {
                startActivity(Intent(this@SettingsActivity, FAQActivity::class.java))
            }

            if (!BuildConfig.FLAVOR.contains("fdroid")) {
                binding.settingsLogo.setOnLongClickListener {
                    lifecycleScope.launch(Dispatchers.IO) {
                        AppUpdater.check(this@SettingsActivity, true)
                    }
                    true
                }

                settingsCheckUpdate.isChecked = PrefManager.getVal(PrefName.CheckUpdate)
                settingsCheckUpdate.setOnCheckedChangeListener { _, isChecked ->
                    PrefManager.setVal(PrefName.CheckUpdate, isChecked)
                    if (!isChecked) {
                        snackString(getString(R.string.long_click_to_check_update))
                    }
                }

                settingsCheckUpdate.setOnLongClickListener {
                    lifecycleScope.launch(Dispatchers.IO) {
                        AppUpdater.check(this@SettingsActivity, true)
                    }
                    true
                }

                settingsShareUsername.isChecked = PrefManager.getVal(PrefName.SharedUserID)
                settingsShareUsername.setOnCheckedChangeListener { _, isChecked ->
                    PrefManager.setVal(PrefName.SharedUserID, isChecked)
                }

            } else {
                settingsCheckUpdate.visibility = View.GONE
                settingsShareUsername.visibility = View.GONE
                settingsCheckUpdate.isEnabled = false
                settingsShareUsername.isEnabled = false
                settingsCheckUpdate.isChecked = false
                settingsShareUsername.isChecked = false
            }

            settingsLogToFile.isChecked = PrefManager.getVal(PrefName.LogToFile)
            settingsLogToFile.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.LogToFile, isChecked)
                restartApp(binding.root)
            }

            settingsShareLog.setOnClickListener {
                Logger.shareLog(this@SettingsActivity)
            }
        }

        binding.settingBuyMeCoffee.setOnClickListener {
            lifecycleScope.launch {
                it.pop()
            }
            openLinkInBrowser(getString(R.string.coffee))
        }
        lifecycleScope.launch {
            binding.settingBuyMeCoffee.pop()
        }

        binding.loginDiscord.setOnClickListener {
            openLinkInBrowser(getString(R.string.discord))
        }
        binding.loginGithub.setOnClickListener {
            openLinkInBrowser(getString(R.string.github))
        }
        binding.loginTelegram.setOnClickListener {
            openLinkInBrowser(getString(R.string.telegram))
        }


        (binding.settingsLogo.drawable as Animatable).start()
        val array = resources.getStringArray(R.array.tips)

        binding.settingsLogo.setSafeOnClickListener {
            cursedCounter++
            (binding.settingsLogo.drawable as Animatable).start()
            if (cursedCounter % 7 == 0) {
                toast(R.string.you_cursed)
                openLinkInYouTube(getString(R.string.cursed_yt))
                //PrefManager.setVal(PrefName.ImageUrl, !PrefManager.getVal(PrefName.ImageUrl, false))
            } else {
                snackString(array[(Math.random() * array.size).toInt()], this)
            }

        }

        lifecycleScope.launch(Dispatchers.IO) {
            delay(2000)
            runOnUiThread {
                if (Random.nextInt(0, 100) > 69) {
                    CustomBottomDialog.newInstance().apply {
                        title = this@SettingsActivity.getString(R.string.enjoying_app)
                        addView(TextView(this@SettingsActivity).apply {
                            text = context.getString(R.string.consider_donating)
                        })

                        setNegativeButton(this@SettingsActivity.getString(R.string.no_moners)) {
                            snackString(R.string.you_be_rich)
                            dismiss()
                        }

                        setPositiveButton(this@SettingsActivity.getString(R.string.donate)) {
                            binding.settingBuyMeCoffee.performClick()
                            dismiss()
                        }
                        show(supportFragmentManager, "dialog")
                    }
                }
            }
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == BUTTON_POSITIVE) {
            if (dialogTag == "colorPicker") {
                val color = extras.getInt(SimpleColorDialog.COLOR)
                PrefManager.setVal(PrefName.CustomThemeInt, color)
                Logger.log("Custom Theme: $color")
            }
        }
        return true
    }

    private fun passwordAlertDialog(isExporting: Boolean, callback: (CharArray?) -> Unit) {
        val password = CharArray(16).apply { fill('0') }

        // Inflate the dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_agent, null)
        val box = dialogView.findViewById<TextInputEditText>(R.id.userAgentTextBox)
        box?.hint = getString(R.string.password)
        box?.setSingleLine()

        val dialog = AlertDialog.Builder(this, R.style.MyPopup)
            .setTitle(getString(R.string.enter_password))
            .setView(dialogView)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                password.fill('0')
                dialog.dismiss()
                callback(null)
            }
            .create()

        fun handleOkAction() {
            val editText = dialog.findViewById<TextInputEditText>(R.id.userAgentTextBox)
            if (editText?.text?.isNotBlank() == true) {
                editText.text?.toString()?.trim()?.toCharArray(password)
                dialog.dismiss()
                callback(password)
            } else {
                toast(getString(R.string.password_cannot_be_empty))
            }
        }
        box?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleOkAction()
                true
            } else {
                false
            }
        }
        val subtitleTextView = dialogView.findViewById<TextView>(R.id.subtitle)
        subtitleTextView?.visibility = View.VISIBLE
        if (!isExporting)
            subtitleTextView?.text = getString(R.string.enter_password_to_decrypt_file)


        dialog.window?.setDimAmount(0.8f)
        dialog.show()

        // Override the positive button here
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            handleOkAction()
        }

    }


    companion object {
        fun getDeviceInfo(): String {
            return """
                dantotsu Version: ${BuildConfig.VERSION_NAME}
                Device: $BRAND $DEVICE
                Architecture: ${getArch()}
                OS Version: $CODENAME $RELEASE ($SDK_INT)
            """.trimIndent()
        }

        private fun getArch(): String {
            SUPPORTED_ABIS.forEach {
                when (it) {
                    "arm64-v8a" -> return "aarch64"
                    "armeabi-v7a" -> return "arm"
                    "x86_64" -> return "x86_64"
                    "x86" -> return "i686"
                }
            }
            return System.getProperty("os.arch") ?: System.getProperty("os.product.cpu.abi")
            ?: "Unknown Architecture"
        }
    }
}
