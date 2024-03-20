package ani.dantotsu.settings

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Build
import android.os.Build.BRAND
import android.os.Build.DEVICE
import android.os.Build.SUPPORTED_ABIS
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.RELEASE
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.NotificationType
import ani.dantotsu.connections.discord.Discord
import ani.dantotsu.connections.mal.MAL
import ani.dantotsu.copyToClipboard
import ani.dantotsu.currContext
import ani.dantotsu.databinding.ActivitySettingsBinding
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.video.ExoplayerDownloadService
import ani.dantotsu.downloadsPermission
import ani.dantotsu.initActivity
import ani.dantotsu.loadImage
import ani.dantotsu.util.Logger
import ani.dantotsu.navBarHeight
import ani.dantotsu.notifications.TaskScheduler
import ani.dantotsu.notifications.comment.CommentNotificationWorker
import ani.dantotsu.notifications.anilist.AnilistNotificationWorker
import ani.dantotsu.notifications.subscription.SubscriptionNotificationWorker.Companion.checkIntervals
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.openSettings
import ani.dantotsu.others.AppUpdater
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.pop
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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE
import eltos.simpledialogfragment.color.SimpleColorDialog
import eu.kanade.domain.base.BasePreferences
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.random.Random


class SettingsActivity : AppCompatActivity(), SimpleDialog.OnDialogResultListener {
    private val restartMainActivity = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = startMainActivity(this@SettingsActivity)
    }
    lateinit var binding: ActivitySettingsBinding
    private val extensionInstaller = Injekt.get<BasePreferences>().extensionInstaller()
    private var cursedCounter = 0

    @OptIn(UnstableApi::class)
    @SuppressLint("SetTextI18n", "Recycle")
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
                                        toast("Incorrect password")
                                        return@passwordAlertDialog
                                    }
                                    if (PreferencePackager.unpack(decryptedJson))
                                        restartApp()
                                } else {
                                    toast("Password cannot be empty")
                                }
                            }
                        } else if (name.endsWith(".ani")) {
                            val decryptedJson = jsonString.toString(Charsets.UTF_8)
                            if (PreferencePackager.unpack(decryptedJson))
                                restartApp()
                        } else {
                            toast("Unknown file type")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        toast("Error importing settings")
                    }
                }
            }

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

        binding.settingsUseMaterialYou.isChecked = PrefManager.getVal(PrefName.UseMaterialYou)
        binding.settingsUseMaterialYou.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.UseMaterialYou, isChecked)
            if (isChecked) binding.settingsUseCustomTheme.isChecked = false
            restartApp()
        }

        binding.settingsUseCustomTheme.isChecked = PrefManager.getVal(PrefName.UseCustomTheme)
        binding.settingsUseCustomTheme.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.UseCustomTheme, isChecked)
            if (isChecked) {
                binding.settingsUseMaterialYou.isChecked = false
            }

            restartApp()
        }

        binding.settingsUseSourceTheme.isChecked = PrefManager.getVal(PrefName.UseSourceTheme)
        binding.settingsUseSourceTheme.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.UseSourceTheme, isChecked)
            restartApp()
        }

        binding.settingsUseOLED.isChecked = PrefManager.getVal(PrefName.UseOLED)
        binding.settingsUseOLED.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.UseOLED, isChecked)
            restartApp()
        }

        val themeString: String = PrefManager.getVal(PrefName.Theme)
        binding.themeSwitcher.setText(
            themeString.substring(0, 1) + themeString.substring(1).lowercase()
        )

        binding.themeSwitcher.setAdapter(
            ArrayAdapter(
                this,
                R.layout.item_dropdown,
                ThemeManager.Companion.Theme.entries
                    .map { it.theme.substring(0, 1) + it.theme.substring(1).lowercase() })
        )

        binding.themeSwitcher.setOnItemClickListener { _, _, i, _ ->
            PrefManager.setVal(PrefName.Theme, ThemeManager.Companion.Theme.entries[i].theme)
            //ActivityHelper.shouldRefreshMainActivity = true
            binding.themeSwitcher.clearFocus()
            restartApp()

        }


        binding.customTheme.setOnClickListener {
            val originalColor: Int = PrefManager.getVal(PrefName.CustomThemeInt)

            class CustomColorDialog : SimpleColorDialog() { //idk where to put it
                override fun onPositiveButtonClick() {
                    restartApp()
                    super.onPositiveButtonClick()
                }
            }

            val tag = "colorPicker"
            CustomColorDialog().title("Custom Theme")
                .colorPreset(originalColor)
                .colors(this, SimpleColorDialog.MATERIAL_COLOR_PALLET)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this, tag)
        }

        binding.settingsPlayer.setOnClickListener {
            startActivity(Intent(this, PlayerSettingsActivity::class.java))
        }

        val managers = arrayOf("Default", "1DM", "ADM")
        val downloadManagerDialog =
            AlertDialog.Builder(this, R.style.MyPopup).setTitle("Download Manager")
        var downloadManager: Int = PrefManager.getVal(PrefName.DownloadManager)
        binding.settingsDownloadManager.setOnClickListener {
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

        binding.importExportSettings.setOnClickListener {
            downloadsPermission(this)
            val selectedArray = mutableListOf(false)
            val filteredLocations = Location.entries.filter { it.exportable }
            selectedArray.addAll(List(filteredLocations.size - 1) { false })
            val dialog = AlertDialog.Builder(this, R.style.MyPopup)
                .setTitle("Import/Export Settings")
                .setMultiChoiceItems(
                    filteredLocations.map { it.name }.toTypedArray(),
                    selectedArray.toBooleanArray()
                ) { _, which, isChecked ->
                    selectedArray[which] = isChecked
                }
                .setPositiveButton("Import...") { dialog, _ ->
                    openDocumentLauncher.launch(arrayOf("*/*"))
                    dialog.dismiss()
                }
                .setNegativeButton("Export...") { dialog, _ ->
                    if (!selectedArray.contains(true)) {
                        toast("No location selected")
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
                                toast("Password cannot be empty")
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
                .setNeutralButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            dialog.window?.setDimAmount(0.8f)
            dialog.show()
        }

        binding.purgeAnimeDownloads.setOnClickListener {
            val dialog = AlertDialog.Builder(this, R.style.MyPopup)
                .setTitle("Purge Anime Downloads")
                .setMessage("Are you sure you want to purge all anime downloads?")
                .setPositiveButton("Yes") { dialog, _ ->
                    val downloadsManager = Injekt.get<DownloadsManager>()
                    downloadsManager.purgeDownloads(DownloadedType.Type.ANIME)
                    DownloadService.sendRemoveAllDownloads(
                        this,
                        ExoplayerDownloadService::class.java,
                        false
                    )
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            dialog.window?.setDimAmount(0.8f)
            dialog.show()
        }

        binding.purgeMangaDownloads.setOnClickListener {
            val dialog = AlertDialog.Builder(this, R.style.MyPopup)
                .setTitle("Purge Manga Downloads")
                .setMessage("Are you sure you want to purge all manga downloads?")
                .setPositiveButton("Yes") { dialog, _ ->
                    val downloadsManager = Injekt.get<DownloadsManager>()
                    downloadsManager.purgeDownloads(DownloadedType.Type.MANGA)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            dialog.window?.setDimAmount(0.8f)
            dialog.show()
        }

        binding.purgeNovelDownloads.setOnClickListener {
            val dialog = AlertDialog.Builder(this, R.style.MyPopup)
                .setTitle("Purge Novel Downloads")
                .setMessage("Are you sure you want to purge all novel downloads?")
                .setPositiveButton("Yes") { dialog, _ ->
                    val downloadsManager = Injekt.get<DownloadsManager>()
                    downloadsManager.purgeDownloads(DownloadedType.Type.NOVEL)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            dialog.window?.setDimAmount(0.8f)
            dialog.show()
        }

        binding.settingsForceLegacyInstall.isChecked =
            extensionInstaller.get() == BasePreferences.ExtensionInstaller.LEGACY
        binding.settingsForceLegacyInstall.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                extensionInstaller.set(BasePreferences.ExtensionInstaller.LEGACY)
            } else {
                extensionInstaller.set(BasePreferences.ExtensionInstaller.PACKAGEINSTALLER)
            }
        }

        binding.skipExtensionIcons.isChecked = PrefManager.getVal(PrefName.SkipExtensionIcons)
        binding.skipExtensionIcons.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.getVal(PrefName.SkipExtensionIcons, isChecked)
        }
        binding.NSFWExtension.isChecked = PrefManager.getVal(PrefName.NSFWExtension)
        binding.NSFWExtension.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.NSFWExtension, isChecked)

        }

        binding.userAgent.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_user_agent, null)
            val editText = dialogView.findViewById<TextInputEditText>(R.id.userAgentTextBox)
            editText.setText(PrefManager.getVal<String>(PrefName.DefaultUserAgent))
            val alertDialog = AlertDialog.Builder(this, R.style.MyPopup)
                .setTitle("User Agent")
                .setView(dialogView)
                .setPositiveButton("OK") { dialog, _ ->
                    PrefManager.setVal(PrefName.DefaultUserAgent, editText.text.toString())
                    dialog.dismiss()
                }
                .setNeutralButton("Reset") { dialog, _ ->
                    PrefManager.removeVal(PrefName.DefaultUserAgent)
                    editText.setText("")
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()

            alertDialog.show()
            alertDialog.window?.setDimAmount(0.8f)
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
        binding.settingsExtensionDns.setText(exDns[PrefManager.getVal(PrefName.DohProvider)])
        binding.settingsExtensionDns.setAdapter(ArrayAdapter(this, R.layout.item_dropdown, exDns))
        binding.settingsExtensionDns.setOnItemClickListener { _, _, i, _ ->
            PrefManager.setVal(PrefName.DohProvider, i)
            binding.settingsExtensionDns.clearFocus()
            Toast.makeText(this, "Restart app to apply changes", Toast.LENGTH_LONG).show()
        }

        binding.settingsDownloadInSd.isChecked = PrefManager.getVal(PrefName.SdDl)
        binding.settingsDownloadInSd.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val arrayOfFiles = ContextCompat.getExternalFilesDirs(this, null)
                if (arrayOfFiles.size > 1 && arrayOfFiles[1] != null) {
                    PrefManager.setVal(PrefName.SdDl, true)
                } else {
                    binding.settingsDownloadInSd.isChecked = false
                    PrefManager.setVal(PrefName.SdDl, true)
                    snackString(getString(R.string.noSdFound))
                }
            } else PrefManager.setVal(PrefName.SdDl, true)
        }

        binding.settingsContinueMedia.isChecked = PrefManager.getVal(PrefName.ContinueMedia)
        binding.settingsContinueMedia.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.ContinueMedia, isChecked)
        }

        binding.settingsRecentlyListOnly.isChecked = PrefManager.getVal(PrefName.RecentlyListOnly)
        binding.settingsRecentlyListOnly.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.RecentlyListOnly, isChecked)
        }
        binding.settingsPreferDub.isChecked = PrefManager.getVal(PrefName.SettingsPreferDub)
        binding.settingsPreferDub.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.SettingsPreferDub, isChecked)
        }

        binding.settingsReader.setOnClickListener {
            startActivity(Intent(this, ReaderSettingsActivity::class.java))
        }

        var previous: View = when (PrefManager.getVal<Int>(PrefName.DarkMode)) {
            0 -> binding.settingsUiAuto
            1 -> binding.settingsUiLight
            2 -> binding.settingsUiDark
            else -> binding.settingsUiAuto
        }
        previous.alpha = 1f
        fun uiTheme(mode: Int, current: View) {
            previous.alpha = 0.33f
            previous = current
            current.alpha = 1f
            PrefManager.setVal(PrefName.DarkMode, mode)
            Refresh.all()
            finish()
            startActivity(Intent(this, SettingsActivity::class.java))
            initActivity(this)
        }

        binding.settingsUiAuto.setOnClickListener {
            uiTheme(0, it)
        }

        binding.settingsUiLight.setOnClickListener {
            binding.settingsUseOLED.isChecked = false
            uiTheme(1, it)
        }

        binding.settingsUiDark.setOnClickListener {
            uiTheme(2, it)
        }

        var previousStart: View = when (PrefManager.getVal<Int>(PrefName.DefaultStartUpTab)) {
            0 -> binding.uiSettingsAnime
            1 -> binding.uiSettingsHome
            2 -> binding.uiSettingsManga
            else -> binding.uiSettingsHome
        }
        previousStart.alpha = 1f
        fun uiDefault(mode: Int, current: View) {
            previousStart.alpha = 0.33f
            previousStart = current
            current.alpha = 1f
            PrefManager.setVal(PrefName.DefaultStartUpTab, mode)
            initActivity(this)
        }


        binding.uiSettingsAnime.setOnClickListener {
            uiDefault(0, it)
        }

        binding.uiSettingsHome.setOnClickListener {
            uiDefault(1, it)
        }

        binding.uiSettingsManga.setOnClickListener {
            uiDefault(2, it)
        }

        binding.settingsShowYt.isChecked = PrefManager.getVal(PrefName.ShowYtButton)
        binding.settingsShowYt.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.ShowYtButton, isChecked)
        }

        var previousEp: View = when (PrefManager.getVal<Int>(PrefName.AnimeDefaultView)) {
            0 -> binding.settingsEpList
            1 -> binding.settingsEpGrid
            2 -> binding.settingsEpCompact
            else -> binding.settingsEpList
        }
        previousEp.alpha = 1f
        fun uiEp(mode: Int, current: View) {
            previousEp.alpha = 0.33f
            previousEp = current
            current.alpha = 1f
            PrefManager.setVal(PrefName.AnimeDefaultView, mode)
        }

        binding.settingsEpList.setOnClickListener {
            uiEp(0, it)
        }

        binding.settingsEpGrid.setOnClickListener {
            uiEp(1, it)
        }

        binding.settingsEpCompact.setOnClickListener {
            uiEp(2, it)
        }

        var previousChp: View = when (PrefManager.getVal<Int>(PrefName.MangaDefaultView)) {
            0 -> binding.settingsChpList
            1 -> binding.settingsChpCompact
            else -> binding.settingsChpList
        }
        previousChp.alpha = 1f
        fun uiChp(mode: Int, current: View) {
            previousChp.alpha = 0.33f
            previousChp = current
            current.alpha = 1f
            PrefManager.setVal(PrefName.MangaDefaultView, mode)
        }

        binding.settingsChpList.setOnClickListener {
            uiChp(0, it)
        }

        binding.settingsChpCompact.setOnClickListener {
            uiChp(1, it)
        }

        binding.settingBuyMeCoffee.setOnClickListener {
            lifecycleScope.launch {
                it.pop()
            }
            openLinkInBrowser("https://www.buymeacoffee.com/rebelonion")
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
        binding.settingsUi.setOnClickListener {
            startActivity(Intent(this, UserInterfaceSettingsActivity::class.java))
        }

        binding.settingsFAQ.setOnClickListener {
            startActivity(Intent(this, FAQActivity::class.java))
        }

        (binding.settingsLogo.drawable as Animatable).start()
        val array = resources.getStringArray(R.array.tips)

        binding.settingsLogo.setSafeOnClickListener {
            cursedCounter++
            (binding.settingsLogo.drawable as Animatable).start()
            if (cursedCounter % 7 == 0) {
                Toast.makeText(this, "youwu have been cuwsed :pwayge:", Toast.LENGTH_LONG).show()
                val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                openLinkInBrowser(url)
                //PrefManager.setVal(PrefName.ImageUrl, !PrefManager.getVal(PrefName.ImageUrl, false))
            } else {
                snackString(array[(Math.random() * array.size).toInt()], this)
            }

        }

        binding.settingsDev.setOnClickListener {
            DevelopersDialogFragment().show(supportFragmentManager, "dialog")
        }
        binding.settingsForks.setOnClickListener {
            ForksDialogFragment().show(supportFragmentManager, "dialog")
        }
        binding.settingsDisclaimer.setOnClickListener {
            val title = getString(R.string.disclaimer)
            val text = TextView(this)
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

        var curTime = PrefManager.getVal<Int>(PrefName.SubscriptionNotificationInterval)
        val timeNames = checkIntervals.map {
            val mins = it % 60
            val hours = it / 60
            if (it > 0) "${if (hours > 0) "$hours hrs " else ""}${if (mins > 0) "$mins mins" else ""}"
            else getString(R.string.do_not_update)
        }.toTypedArray()
        binding.settingsSubscriptionsTime.text =
            getString(R.string.subscriptions_checking_time_s, timeNames[curTime])
        val speedDialog = AlertDialog.Builder(this, R.style.MyPopup)
            .setTitle(R.string.subscriptions_checking_time)
        binding.settingsSubscriptionsTime.setOnClickListener {
            val dialog = speedDialog.setSingleChoiceItems(timeNames, curTime) { dialog, i ->
                curTime = i
                binding.settingsSubscriptionsTime.text =
                    getString(R.string.subscriptions_checking_time_s, timeNames[i])
                PrefManager.setVal(PrefName.SubscriptionNotificationInterval, curTime)
                dialog.dismiss()
                TaskScheduler.create(this,
                    PrefManager.getVal(PrefName.UseAlarmManager)
                ).scheduleAllTasks(this)
            }.show()
            dialog.window?.setDimAmount(0.8f)
        }

        binding.settingsSubscriptionsTime.setOnLongClickListener {
            TaskScheduler.create(this,
                PrefManager.getVal(PrefName.UseAlarmManager)
            ).scheduleAllTasks(this)
            true
        }

        val aTimeNames = AnilistNotificationWorker.checkIntervals.map { it.toInt() }
        val aItems = aTimeNames.map {
            val mins = it % 60
            val hours = it / 60
            if (it > 0) "${if (hours > 0) "$hours hrs " else ""}${if (mins > 0) "$mins mins" else ""}"
            else getString(R.string.do_not_update)
        }
        binding.settingsAnilistSubscriptionsTime.text =
            getString(R.string.anilist_notifications_checking_time, aItems[PrefManager.getVal(PrefName.AnilistNotificationInterval)])
        binding.settingsAnilistSubscriptionsTime.setOnClickListener {

            val selected = PrefManager.getVal<Int>(PrefName.AnilistNotificationInterval)
            val dialog = AlertDialog.Builder(this, R.style.MyPopup)
                .setTitle(R.string.subscriptions_checking_time)
                .setSingleChoiceItems(aItems.toTypedArray(), selected) { dialog, i ->
                    PrefManager.setVal(PrefName.AnilistNotificationInterval, i)
                    binding.settingsAnilistSubscriptionsTime.text =
                        getString(R.string.anilist_notifications_checking_time, aItems[i])
                    dialog.dismiss()
                    TaskScheduler.create(this,
                        PrefManager.getVal(PrefName.UseAlarmManager)
                    ).scheduleAllTasks(this)
                }
                .create()
            dialog.window?.setDimAmount(0.8f)
            dialog.show()
        }

        binding.settingsAnilistNotifications.setOnClickListener {
            val types = NotificationType.entries.map { it.name }
            val filteredTypes = PrefManager.getVal<Set<String>>(PrefName.AnilistFilteredTypes).toMutableSet()
            val selected = types.map { filteredTypes.contains(it) }.toBooleanArray()
            val dialog = AlertDialog.Builder(this, R.style.MyPopup)
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
        binding.settingsCommentSubscriptionsTime.text =
            getString(R.string.comment_notification_checking_time, cItems[PrefManager.getVal(PrefName.CommentNotificationInterval)])
        binding.settingsCommentSubscriptionsTime.setOnClickListener {
            val selected = PrefManager.getVal<Int>(PrefName.CommentNotificationInterval)
            val dialog = AlertDialog.Builder(this, R.style.MyPopup)
                .setTitle(R.string.subscriptions_checking_time)
                .setSingleChoiceItems(cItems.toTypedArray(), selected) { dialog, i ->
                    PrefManager.setVal(PrefName.CommentNotificationInterval, i)
                    binding.settingsCommentSubscriptionsTime.text =
                        getString(R.string.comment_notification_checking_time, cItems[i])
                    dialog.dismiss()
                    TaskScheduler.create(this,
                        PrefManager.getVal(PrefName.UseAlarmManager)
                    ).scheduleAllTasks(this)
                }
                .create()
            dialog.window?.setDimAmount(0.8f)
            dialog.show()
        }

        binding.settingsNotificationsCheckingSubscriptions.isChecked =
            PrefManager.getVal(PrefName.SubscriptionCheckingNotifications)
        binding.settingsNotificationsCheckingSubscriptions.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.SubscriptionCheckingNotifications, isChecked)
        }

        binding.settingsNotificationsCheckingSubscriptions.setOnLongClickListener {
            openSettings(this, null)
        }

        binding.settingsNotificationsUseAlarmManager.isChecked =
            PrefManager.getVal(PrefName.UseAlarmManager)

        binding.settingsNotificationsUseAlarmManager.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val alertDialog = AlertDialog.Builder(this, R.style.MyPopup)
                    .setTitle("Use Alarm Manager")
                    .setMessage("Using Alarm Manger can help fight against battery optimization, but may consume more battery. It also requires the Alarm Manager permission.")
                    .setPositiveButton("Use") { dialog, _ ->
                        PrefManager.setVal(PrefName.UseAlarmManager, true)
                        if (SDK_INT >= Build.VERSION_CODES.S) {
                            if (!(getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()) {
                                val intent = Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM")
                                startActivity(intent)
                                binding.settingsNotificationsCheckingSubscriptions.isChecked = true
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        binding.settingsNotificationsCheckingSubscriptions.isChecked = false
                        PrefManager.setVal(PrefName.UseAlarmManager, false)
                        dialog.dismiss()
                    }
                    .create()
                alertDialog.window?.setDimAmount(0.8f)
                alertDialog.show()
            } else {
                PrefManager.setVal(PrefName.UseAlarmManager, false)
                TaskScheduler.create(this, true).cancelAllTasks()
                TaskScheduler.create(this, false).scheduleAllTasks(this)
            }
        }

        if (!BuildConfig.FLAVOR.contains("fdroid")) {
            binding.settingsLogo.setOnLongClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    AppUpdater.check(this@SettingsActivity, true)
                }
                true
            }

            binding.settingsCheckUpdate.isChecked = PrefManager.getVal(PrefName.CheckUpdate)
            binding.settingsCheckUpdate.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.CheckUpdate, isChecked)
                if (!isChecked) {
                    snackString(getString(R.string.long_click_to_check_update))
                }
            }

            binding.settingsCheckUpdate.setOnLongClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    AppUpdater.check(this@SettingsActivity, true)
                }
                true
            }

            binding.settingsShareUsername.isChecked = PrefManager.getVal(PrefName.SharedUserID)
            binding.settingsShareUsername.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.SharedUserID, isChecked)
            }

        } else {
            binding.settingsCheckUpdate.visibility = View.GONE
            binding.settingsShareUsername.visibility = View.GONE
            binding.settingsCheckUpdate.isEnabled = false
            binding.settingsShareUsername.isEnabled = false
            binding.settingsCheckUpdate.isChecked = false
            binding.settingsShareUsername.isChecked = false
        }

        binding.settingsLogToFile.isChecked = PrefManager.getVal(PrefName.LogToFile)
        binding.settingsLogToFile.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.LogToFile, isChecked)
            restartApp()
        }

        binding.settingsShareLog.setOnClickListener {
            Logger.shareLog(this)
        }

        binding.settingsAccountHelp.setOnClickListener {
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
                binding.settingsAnilistLogin.setText(R.string.logout)
                binding.settingsAnilistLogin.setOnClickListener {
                    Anilist.removeSavedToken()
                    restartMainActivity.isEnabled = true
                    reload()
                }
                binding.settingsAnilistUsername.visibility = View.VISIBLE
                binding.settingsAnilistUsername.text = Anilist.username
                binding.settingsAnilistAvatar.loadImage(Anilist.avatar)

                binding.settingsMALLoginRequired.visibility = View.GONE
                binding.settingsMALLogin.visibility = View.VISIBLE
                binding.settingsMALUsername.visibility = View.VISIBLE

                if (MAL.token != null) {
                    binding.settingsMALLogin.setText(R.string.logout)
                    binding.settingsMALLogin.setOnClickListener {
                        MAL.removeSavedToken(it.context)
                        restartMainActivity.isEnabled = true
                        reload()
                    }
                    binding.settingsMALUsername.visibility = View.VISIBLE
                    binding.settingsMALUsername.text = MAL.username
                    binding.settingsMALAvatar.loadImage(MAL.avatar)
                } else {
                    binding.settingsMALAvatar.setImageResource(R.drawable.ic_round_person_24)
                    binding.settingsMALUsername.visibility = View.GONE
                    binding.settingsMALLogin.setText(R.string.login)
                    binding.settingsMALLogin.setOnClickListener {
                        MAL.loginIntent(this)
                    }
                }
            } else {
                binding.settingsAnilistAvatar.setImageResource(R.drawable.ic_round_person_24)
                binding.settingsAnilistUsername.visibility = View.GONE
                binding.settingsAnilistLogin.setText(R.string.login)
                binding.settingsAnilistLogin.setOnClickListener {
                    Anilist.loginIntent(this)
                }
                binding.settingsMALLoginRequired.visibility = View.VISIBLE
                binding.settingsMALLogin.visibility = View.GONE
                binding.settingsMALUsername.visibility = View.GONE
            }

            if (Discord.token != null) {
                val id = PrefManager.getVal(PrefName.DiscordId, null as String?)
                val avatar = PrefManager.getVal(PrefName.DiscordAvatar, null as String?)
                val username = PrefManager.getVal(PrefName.DiscordUserName, null as String?)
                if (id != null && avatar != null) {
                    binding.settingsDiscordAvatar.loadImage("https://cdn.discordapp.com/avatars/$id/$avatar.png")
                }
                binding.settingsDiscordUsername.visibility = View.VISIBLE
                binding.settingsDiscordUsername.text =
                    username ?: Discord.token?.replace(Regex("."), "*")
                binding.settingsDiscordLogin.setText(R.string.logout)
                binding.settingsDiscordLogin.setOnClickListener {
                    Discord.removeSavedToken(this)
                    restartMainActivity.isEnabled = true
                    reload()
                }

                binding.imageSwitcher.visibility = View.VISIBLE
                var initialStatus = when (PrefManager.getVal<String>(PrefName.DiscordStatus)) {
                    "online" -> R.drawable.discord_status_online
                    "idle" -> R.drawable.discord_status_idle
                    "dnd" -> R.drawable.discord_status_dnd
                    else -> R.drawable.discord_status_online
                }
                binding.imageSwitcher.setImageResource(initialStatus)

                val zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce_zoom)
                binding.imageSwitcher.setOnClickListener {
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
                    binding.imageSwitcher.setImageResource(initialStatus)
                    binding.imageSwitcher.startAnimation(zoomInAnimation)
                }
            } else {
                binding.imageSwitcher.visibility = View.GONE
                binding.settingsDiscordAvatar.setImageResource(R.drawable.ic_round_person_24)
                binding.settingsDiscordUsername.visibility = View.GONE
                binding.settingsDiscordLogin.setText(R.string.login)
                binding.settingsDiscordLogin.setOnClickListener {
                    Discord.warning(this).show(supportFragmentManager, "dialog")
                }
            }
        }
        reload()

        lifecycleScope.launch(Dispatchers.IO) {
            delay(2000)
            runOnUiThread {
                if (Random.nextInt(0, 100) > 69) {
                    CustomBottomDialog.newInstance().apply {
                        title = "Enjoying the App?"
                        addView(TextView(this@SettingsActivity).apply {
                            text =
                                "Consider donating!"
                        })

                        setNegativeButton("no moners :(") {
                            snackString("That's alright, you'll be a rich man soon :prayge:")
                            dismiss()
                        }

                        setPositiveButton("denote :)") {
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

    private fun restartApp() {
        Snackbar.make(
            binding.root,
            R.string.restart_app, Snackbar.LENGTH_SHORT
        ).apply {
            val mainIntent =
                Intent.makeRestartActivityTask(
                    context.packageManager.getLaunchIntentForPackage(
                        context.packageName
                    )!!.component
                )
            setAction("Do it!") {
                context.startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }
            show()
        }
    }

    private fun passwordAlertDialog(isExporting: Boolean, callback: (CharArray?) -> Unit) {
        val password = CharArray(16).apply { fill('0') }

        // Inflate the dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_agent, null)
        val box = dialogView.findViewById<TextInputEditText>(R.id.userAgentTextBox)
        box?.hint = "Password"
        box?.setSingleLine()

        val dialog = AlertDialog.Builder(this, R.style.MyPopup)
            .setTitle("Enter Password")
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .setNegativeButton("Cancel") { dialog, _ ->
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
                toast("Password cannot be empty")
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
            subtitleTextView?.text = "Enter your password to decrypt the file"


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