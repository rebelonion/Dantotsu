package ani.dantotsu.settings

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Build.*
import android.os.Build.VERSION.*
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import ani.dantotsu.*
import ani.dantotsu.Mapper.json
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.discord.Discord
import ani.dantotsu.connections.mal.MAL
import ani.dantotsu.databinding.ActivitySettingsBinding
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.video.ExoplayerDownloadService
import ani.dantotsu.others.AppUpdater
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.parsers.MangaSources
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.internal.Location
import ani.dantotsu.subcriptions.Notifications
import ani.dantotsu.subcriptions.Notifications.Companion.openSettings
import ani.dantotsu.subcriptions.Subscription.Companion.defaultTime
import ani.dantotsu.subcriptions.Subscription.Companion.startSubscription
import ani.dantotsu.subcriptions.Subscription.Companion.timeMinutes
import ani.dantotsu.themes.ThemeManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    private lateinit var openDocumentLauncher: ActivityResultLauncher<String>

    @OptIn(UnstableApi::class)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)

        var selectedImpExp = ""
        openDocumentLauncher = registerForActivityResult(CreateDocument("*/*")) { uri ->
            if (uri != null) {
                try {
                    val jsonString = contentResolver.openInputStream(uri)?.bufferedReader()
                        .use { it?.readText() }
                    val location: Location =
                        Location.entries.find { it.name.lowercase() == selectedImpExp.lowercase() }
                            ?: return@registerForActivityResult

                    val gson = Gson()
                    val type = object : TypeToken<Map<String, Map<String, Any>>>() {}.type
                    val rawMap: Map<String, Map<String, Any>> = gson.fromJson(jsonString, type)

                    val deserializedMap = mutableMapOf<String, Any?>()

                    rawMap.forEach { (key, typeValueMap) ->
                        val typeName = typeValueMap["type"] as? String
                        val value = typeValueMap["value"]

                        deserializedMap[key] = when (typeName) {  //wierdly null sometimes so cast to string
                            "kotlin.Int" -> (value as? Double)?.toInt()
                            "kotlin.String" -> value.toString()
                            "kotlin.Boolean" -> value as? Boolean
                            "kotlin.Float" -> value.toString().toFloatOrNull()
                            "kotlin.Long" -> (value as? Double)?.toLong()
                            "java.util.HashSet" -> value as? ArrayList<*>
                            else -> null
                        }
                    }

                    PrefManager.importAllPrefs(deserializedMap, location)
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
                .colors(this, SimpleColorDialog.BEIGE_COLOR_PALLET)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this, tag)
        }

        binding.settingsPinnedAnimeSources.setOnClickListener {
            val animeSourcesWithoutDownloadsSource =
                AnimeSources.list.filter { it.name != "Downloaded" }
            val names = animeSourcesWithoutDownloadsSource.map { it.name }
            val pinnedSourcesBoolean =
                animeSourcesWithoutDownloadsSource.map { it.name in AnimeSources.pinnedAnimeSources }
            val pinnedSourcesOriginal: Set<String> = PrefManager.getVal(PrefName.PinnedAnimeSources)
            val pinnedSources = pinnedSourcesOriginal.toMutableSet() ?: mutableSetOf()
            val alertDialog = AlertDialog.Builder(this, R.style.MyPopup)
                .setTitle("Pinned Anime Sources")
                .setMultiChoiceItems(
                    names.toTypedArray(),
                    pinnedSourcesBoolean.toBooleanArray()
                ) { _, which, isChecked ->
                    if (isChecked) {
                        pinnedSources.add(AnimeSources.names[which])
                    } else {
                        pinnedSources.remove(AnimeSources.names[which])
                    }
                }
                .setPositiveButton("OK") { dialog, _ ->
                    PrefManager.setVal(PrefName.PinnedAnimeSources, pinnedSources)
                    AnimeSources.pinnedAnimeSources = pinnedSources
                    AnimeSources.performReorderAnimeSources()
                    dialog.dismiss()
                }
                .create()
            alertDialog.show()
            alertDialog.window?.setDimAmount(0.8f)
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
            var i = 0
            val dialog = AlertDialog.Builder(this, R.style.MyPopup)
                .setTitle("Import/Export Settings")
                .setSingleChoiceItems(Location.entries.map { it.name }.toTypedArray(), 0) { dialog, which ->
                    selectedImpExp = Location.entries[which].name
                    i = which
                }
                .setPositiveButton("Import...") { dialog, _ ->
                    openDocumentLauncher.launch("Select a file")
                    dialog.dismiss()
                }
                .setNegativeButton("Export...") { dialog, _ ->
                    if (i < 0) return@setNegativeButton
                    savePrefsToDownloads(Location.entries[i].name,
                        PrefManager.exportAllPrefs(Location.entries[i]),
                        this@SettingsActivity)
                    dialog.dismiss()
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
            PrefManager.setVal(PrefName.NSFWExtension,isChecked)

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
        binding.settingsShareUsername.isChecked = PrefManager.getVal(PrefName.SharedUserID)
        binding.settingsShareUsername.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.SharedUserID, isChecked)
        }

        binding.settingsPreferDub.isChecked = PrefManager.getVal(PrefName.SettingsPreferDub)
        binding.settingsPreferDub.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.SettingsPreferDub, isChecked)
        }

        binding.settingsPinnedMangaSources.setOnClickListener {
            val mangaSourcesWithoutDownloadsSource =
                MangaSources.list.filter { it.name != "Downloaded" }
            val names = mangaSourcesWithoutDownloadsSource.map { it.name }
            val pinnedSourcesBoolean =
                mangaSourcesWithoutDownloadsSource.map { it.name in MangaSources.pinnedMangaSources }
            val pinnedSourcesOriginal: Set<String> = PrefManager.getVal(PrefName.PinnedMangaSources)
            val pinnedSources = pinnedSourcesOriginal.toMutableSet()
            val alertDialog = AlertDialog.Builder(this, R.style.MyPopup)
                .setTitle("Pinned Manga Sources")
                .setMultiChoiceItems(
                    names.toTypedArray(),
                    pinnedSourcesBoolean.toBooleanArray()
                ) { _, which, isChecked ->
                    if (isChecked) {
                        pinnedSources.add(MangaSources.names[which])
                    } else {
                        pinnedSources.remove(MangaSources.names[which])
                    }
                }
                .setPositiveButton("OK") { dialog, _ ->
                    PrefManager.setVal(PrefName.PinnedMangaSources, pinnedSources)
                    MangaSources.pinnedMangaSources = pinnedSources
                    MangaSources.performReorderMangaSources()
                    dialog.dismiss()
                }
                .create()
            alertDialog.show()
            alertDialog.window?.setDimAmount(0.8f)
        }

        binding.settingsReader.setOnClickListener {
            startActivity(Intent(this, ReaderSettingsActivity::class.java))
        }

        var previous: View = when (PrefManager.getNullableVal(PrefName.DarkMode, null as Boolean?)) {
            null -> binding.settingsUiAuto
            true -> binding.settingsUiDark
            false -> binding.settingsUiLight
        }
        previous.alpha = 1f
        fun uiTheme(mode: Boolean?, current: View) {
            previous.alpha = 0.33f
            previous = current
            current.alpha = 1f
            if (mode == null) {
                PrefManager.removeVal(PrefName.DarkMode)
            } else {
                PrefManager.setVal(PrefName.DarkMode, mode)
            }
            Refresh.all()
            finish()
            startActivity(Intent(this, SettingsActivity::class.java))
            initActivity(this)
        }

        binding.settingsUiAuto.setOnClickListener {
            uiTheme(null, it)
        }

        binding.settingsUiLight.setOnClickListener {
            binding.settingsUseOLED.isChecked = false
            uiTheme(false, it)
        }

        binding.settingsUiDark.setOnClickListener {
            uiTheme(true, it)
        }

        var previousStart: View = when (PrefManager.getVal<Int>(PrefName.DefaultStartUpTab)) {
            0 -> binding.uiSettingsAnime
            1 -> binding.uiSettingsHome
            2 -> binding.uiSettingsManga
            else -> binding.uiSettingsHome
        }
        previousStart.alpha = 1f
        fun uiTheme(mode: Int, current: View) {
            previousStart.alpha = 0.33f
            previousStart = current
            current.alpha = 1f
            PrefManager.setVal(PrefName.DefaultStartUpTab, mode)
            initActivity(this)
        }


        binding.uiSettingsAnime.setOnClickListener {
            uiTheme(0, it)
        }

        binding.uiSettingsHome.setOnClickListener {
            uiTheme(1, it)
        }

        binding.uiSettingsManga.setOnClickListener {
            uiTheme(2, it)
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
                //PrefManager.setVal(PrefName.SomethingSpecial, !PrefManager.getVal(PrefName.SomethingSpecial, false))
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

        var curTime = PrefManager.getVal(PrefName.SubscriptionsTimeS, defaultTime)
        val timeNames = timeMinutes.map {
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
                PrefManager.setVal(PrefName.SubscriptionsTimeS, curTime)
                dialog.dismiss()
                startSubscription(true)
            }.show()
            dialog.window?.setDimAmount(0.8f)
        }

        binding.settingsSubscriptionsTime.setOnLongClickListener {
            startSubscription(true)
            true
        }

        binding.settingsNotificationsCheckingSubscriptions.isChecked =
            PrefManager.getVal(PrefName.SubscriptionCheckingNotifications)
        binding.settingsNotificationsCheckingSubscriptions.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.SubscriptionCheckingNotifications, isChecked)
            if (isChecked)
                Notifications.createChannel(
                    this,
                    null,
                    "subscription_checking",
                    getString(R.string.checking_subscriptions),
                    false
                )
            else
                Notifications.deleteChannel(this, "subscription_checking")
        }

        binding.settingsNotificationsCheckingSubscriptions.setOnLongClickListener {
            openSettings(this, null)
        }


        binding.settingsCheckUpdate.isChecked = PrefManager.getVal(PrefName.CheckUpdate)
        binding.settingsCheckUpdate.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.CheckUpdate, isChecked)
            if (!isChecked) {
                snackString(getString(R.string.long_click_to_check_update))
            }
        }

        binding.settingsLogo.setOnLongClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                AppUpdater.check(this@SettingsActivity, true)
            }
            true
        }

        binding.settingsCheckUpdate.setOnLongClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                AppUpdater.check(this@SettingsActivity, true)
            }
            true
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
                    Anilist.removeSavedToken(it.context)
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
            } else {
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
                logger("Custom Theme: $color")
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
