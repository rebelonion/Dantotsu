package ani.dantotsu.settings

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import ani.dantotsu.*
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.discord.Discord
import ani.dantotsu.connections.mal.MAL
import ani.dantotsu.databinding.ActivitySettingsBinding
import ani.dantotsu.others.AppUpdater
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.others.LangSet
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.parsers.MangaSources
import ani.dantotsu.subcriptions.Notifications
import ani.dantotsu.subcriptions.Notifications.Companion.openSettings
import ani.dantotsu.subcriptions.Subscription.Companion.defaultTime
import ani.dantotsu.subcriptions.Subscription.Companion.startSubscription
import ani.dantotsu.subcriptions.Subscription.Companion.timeMinutes
import ani.dantotsu.themes.ThemeManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE
import eltos.simpledialogfragment.color.SimpleColorDialog
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.network.NetworkPreferences
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.random.Random


class SettingsActivity : AppCompatActivity(),  SimpleDialog.OnDialogResultListener {
    private val restartMainActivity = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = startMainActivity(this@SettingsActivity)
    }
    lateinit var binding: ActivitySettingsBinding
    private val extensionInstaller = Injekt.get<BasePreferences>().extensionInstaller()
    private val networkPreferences = Injekt.get<NetworkPreferences>()
    private var cursedCounter = 0

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LangSet.setLocale(this)
        ThemeManager(this).applyTheme()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)

        binding.settingsVersion.text = getString(R.string.version_current, BuildConfig.VERSION_NAME)
        binding.settingsVersion.setOnLongClickListener {
            fun getArch(): String {
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

            val info = """
                dantotsu Version: ${BuildConfig.VERSION_NAME}
                Device: $BRAND $DEVICE
                Architecture: ${getArch()}
                OS Version: $CODENAME $RELEASE ($SDK_INT)
            """.trimIndent()
            copyToClipboard(info, false)
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

        binding.settingsUseMaterialYou.isChecked =
            getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).getBoolean(
                "use_material_you",
                false
            )
        binding.settingsUseMaterialYou.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).edit()
                .putBoolean("use_material_you", isChecked).apply()
            if (isChecked) binding.settingsUseCustomTheme.isChecked = false
            restartApp()
        }

        binding.settingsUseCustomTheme.isChecked =
            getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).getBoolean(
                "use_custom_theme",
                false
            )
        binding.settingsUseCustomTheme.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).edit()
                .putBoolean("use_custom_theme", isChecked).apply()
            if (isChecked) {
                binding.settingsUseMaterialYou.isChecked = false
            }

            restartApp()
        }

        binding.settingsUseSourceTheme.isChecked =
            getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).getBoolean(
                "use_source_theme",
                false
            )
        binding.settingsUseSourceTheme.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).edit()
                .putBoolean("use_source_theme", isChecked).apply()
        }

        binding.settingsUseOLED.isChecked =
            getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).getBoolean("use_oled", false)
        binding.settingsUseOLED.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).edit()
                .putBoolean("use_oled", isChecked).apply()
            restartApp()
        }

        val themeString =
            getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).getString("theme", "PURPLE")!!
        binding.themeSwitcher.setText(
            themeString.substring(0, 1) + themeString.substring(1).lowercase()
        )

        binding.themeSwitcher.setAdapter(
            ArrayAdapter(
                this,
                R.layout.item_dropdown,
                ThemeManager.Companion.Theme.values()
                    .map { it.theme.substring(0, 1) + it.theme.substring(1).lowercase() })
        )

        binding.themeSwitcher.setOnItemClickListener { _, _, i, _ ->
            getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).edit()
                .putString("theme", ThemeManager.Companion.Theme.values()[i].theme).apply()
            //ActivityHelper.shouldRefreshMainActivity = true
            binding.themeSwitcher.clearFocus()
            restartApp()

        }


        binding.customTheme.setOnClickListener {
            val originalColor = getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).getInt(
                "custom_theme_int",
                Color.parseColor("#6200EE")
            )
            val tag = "colorPicker"
            SimpleColorDialog.build()
                .title("Custom Theme")
                .colorPreset(originalColor)
                .colors(this, SimpleColorDialog.BEIGE_COLOR_PALLET)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this, tag)
        }

        //val animeSource = loadData<Int>("settings_def_anime_source_s")?.let { if (it >= AnimeSources.names.size) 0 else it } ?: 0
        val animeSource = getSharedPreferences(
            "Dantotsu",
            Context.MODE_PRIVATE
        ).getInt("settings_def_anime_source_s_r", 0)
        if (AnimeSources.names.isNotEmpty() && animeSource in 0 until AnimeSources.names.size) {
            binding.animeSource.setText(AnimeSources.names[animeSource], false)

        }

        binding.animeSource.setAdapter(
            ArrayAdapter(
                this,
                R.layout.item_dropdown,
                AnimeSources.names
            )
        )

        binding.animeSource.setOnItemClickListener { _, _, i, _ ->
            //saveData("settings_def_anime_source_s", i)
            getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).edit()
                .putInt("settings_def_anime_source_s_r", i).apply()
            binding.animeSource.clearFocus()
        }

        binding.settingsPlayer.setOnClickListener {
            startActivity(Intent(this, PlayerSettingsActivity::class.java))
        }

        val managers = arrayOf("Default", "1DM", "ADM")
        val downloadManagerDialog =
            AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Download Manager")
        var downloadManager = loadData<Int>("settings_download_manager") ?: 0
        binding.settingsDownloadManager.setOnClickListener {
            val dialog = downloadManagerDialog.setSingleChoiceItems(managers, downloadManager) { dialog, count ->
                downloadManager = count
                saveData("settings_download_manager", downloadManager)
                dialog.dismiss()
            }.show()
            dialog.window?.setDimAmount(0.8f)
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

        binding.skipExtensionIcons.isChecked = loadData("skip_extension_icons") ?: false
        binding.skipExtensionIcons.setOnCheckedChangeListener { _, isChecked ->
            saveData("skip_extension_icons", isChecked)
        }
        binding.NSFWExtension.isChecked = loadData("NFSWExtension") ?: true
        binding.NSFWExtension.setOnCheckedChangeListener { _, isChecked ->
            saveData("NFSWExtension", isChecked)

        }

        binding.userAgent.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_user_agent, null)
            val editText = dialogView.findViewById<TextInputEditText>(R.id.userAgentTextBox)
            editText.setText(networkPreferences.defaultUserAgent().get())
            val alertDialog = AlertDialog.Builder(this, R.style.MyPopup)
                .setTitle("User Agent")
                .setView(dialogView)
                .setPositiveButton("OK") { dialog, _ ->
                    networkPreferences.defaultUserAgent().set(editText.text.toString())
                    dialog.dismiss()
                }
                .setNeutralButton("Reset") { dialog, _ ->
                    networkPreferences.defaultUserAgent()
                        .set("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:110.0) Gecko/20100101 Firefox/110.0") // Reset to default or empty
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
        binding.settingsExtensionDns.setText(exDns[networkPreferences.dohProvider().get()], false)
        binding.settingsExtensionDns.setAdapter(ArrayAdapter(this, R.layout.item_dropdown, exDns))
        binding.settingsExtensionDns.setOnItemClickListener { _, _, i, _ ->
            networkPreferences.dohProvider().set(i)
            binding.settingsExtensionDns.clearFocus()
            Toast.makeText(this, "Restart app to apply changes", Toast.LENGTH_LONG).show()
        }

        binding.settingsDownloadInSd.isChecked = loadData("sd_dl") ?: false
        binding.settingsDownloadInSd.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val arrayOfFiles = ContextCompat.getExternalFilesDirs(this, null)
                if (arrayOfFiles.size > 1 && arrayOfFiles[1] != null) {
                    saveData("sd_dl", true)
                } else {
                    binding.settingsDownloadInSd.isChecked = false
                    saveData("sd_dl", false)
                    snackString(getString(R.string.noSdFound))
                }
            } else saveData("sd_dl", false)
        }

        binding.settingsContinueMedia.isChecked = loadData("continue_media") ?: true
        binding.settingsContinueMedia.setOnCheckedChangeListener { _, isChecked ->
            saveData("continue_media", isChecked)
        }

        binding.settingsRecentlyListOnly.isChecked = loadData("recently_list_only") ?: false
        binding.settingsRecentlyListOnly.setOnCheckedChangeListener { _, isChecked ->
            saveData("recently_list_only", isChecked)
        }
        binding.settingsPreferDub.isChecked = loadData("settings_prefer_dub") ?: false
        binding.settingsPreferDub.setOnCheckedChangeListener { _, isChecked ->
            saveData("settings_prefer_dub", isChecked)
        }

        //val mangaSource = loadData<Int>("settings_def_manga_source_s")?.let { if (it >= MangaSources.names.size) 0 else it } ?: 0
        val mangaSource = getSharedPreferences(
            "Dantotsu",
            Context.MODE_PRIVATE
        ).getInt("settings_def_manga_source_s_r", 0)
        if (MangaSources.names.isNotEmpty() && mangaSource in 0 until MangaSources.names.size) {
            binding.mangaSource.setText(MangaSources.names[mangaSource], false)
        }

        // Set up the dropdown adapter.
        binding.mangaSource.setAdapter(
            ArrayAdapter(
                this,
                R.layout.item_dropdown,
                MangaSources.names
            )
        )

        // Set up the item click listener for the dropdown.
        binding.mangaSource.setOnItemClickListener { _, _, i, _ ->
            //saveData("settings_def_manga_source_s", i)
            getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).edit()
                .putInt("settings_def_manga_source_s_r", i).apply()
            binding.mangaSource.clearFocus()
        }

        binding.settingsReader.setOnClickListener {
            startActivity(Intent(this, ReaderSettingsActivity::class.java))
        }

        val uiSettings: UserInterfaceSettings =
            loadData("ui_settings", toast = false)
                ?: UserInterfaceSettings().apply { saveData("ui_settings", this) }
        var previous: View = when (uiSettings.darkMode) {
            null -> binding.settingsUiAuto
            true -> binding.settingsUiDark
            false -> binding.settingsUiLight
        }
        previous.alpha = 1f
        fun uiTheme(mode: Boolean?, current: View) {
            previous.alpha = 0.33f
            previous = current
            current.alpha = 1f
            uiSettings.darkMode = mode
            saveData("ui_settings", uiSettings)
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

        var previousStart: View = when (uiSettings.defaultStartUpTab) {
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
            uiSettings.defaultStartUpTab = mode
            saveData("ui_settings", uiSettings)
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

        binding.settingsShowYt.isChecked = uiSettings.showYtButton
        binding.settingsShowYt.setOnCheckedChangeListener { _, isChecked ->
            uiSettings.showYtButton = isChecked
            saveData("ui_settings", uiSettings)
        }

        var previousEp: View = when (uiSettings.animeDefaultView) {
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
            uiSettings.animeDefaultView = mode
            saveData("ui_settings", uiSettings)
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

        var previousChp: View = when (uiSettings.mangaDefaultView) {
            0 -> binding.settingsChpList
            1 -> binding.settingsChpCompact
            else -> binding.settingsChpList
        }
        previousChp.alpha = 1f
        fun uiChp(mode: Int, current: View) {
            previousChp.alpha = 0.33f
            previousChp = current
            current.alpha = 1f
            uiSettings.mangaDefaultView = mode
            saveData("ui_settings", uiSettings)
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
        binding.settingUPI.visibility = if (checkCountry(this)) View.VISIBLE else View.GONE
        lifecycleScope.launch {
            binding.settingUPI.pop()
        }

        binding.loginDiscord.setOnClickListener {
            openLinkInBrowser(getString(R.string.discord))
        }
        binding.loginGithub.setOnClickListener {
            openLinkInBrowser(getString(R.string.github))
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
                getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).edit().putBoolean(
                    "use_cursed_lang",
                    getSharedPreferences(
                        "Dantotsu",
                        Context.MODE_PRIVATE
                    ).getBoolean("use_cursed_lang", false).not()
                ).apply()
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

        var curTime = loadData<Int>("subscriptions_time_s") ?: defaultTime
        val timeNames = timeMinutes.map {
            val mins = it % 60
            val hours = it / 60
            if (it > 0) "${if (hours > 0) "$hours hrs " else ""}${if (mins > 0) "$mins mins" else ""}"
            else getString(R.string.do_not_update)
        }.toTypedArray()
        binding.settingsSubscriptionsTime.text =
            getString(R.string.subscriptions_checking_time_s, timeNames[curTime])
        val speedDialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(R.string.subscriptions_checking_time)
        binding.settingsSubscriptionsTime.setOnClickListener {
            val dialog = speedDialog.setSingleChoiceItems(timeNames, curTime) { dialog, i ->
                curTime = i
                binding.settingsSubscriptionsTime.text =
                    getString(R.string.subscriptions_checking_time_s, timeNames[i])
                saveData("subscriptions_time_s", curTime)
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
            loadData("subscription_checking_notifications") ?: true
        binding.settingsNotificationsCheckingSubscriptions.setOnCheckedChangeListener { _, isChecked ->
            saveData("subscription_checking_notifications", isChecked)
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


        binding.settingsCheckUpdate.isChecked = loadData("check_update") ?: true
        binding.settingsCheckUpdate.setOnCheckedChangeListener { _, isChecked ->
            saveData("check_update", isChecked)
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
                val id = getSharedPreferences(
                    getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                ).getString("discord_id", null)
                val avatar = getSharedPreferences(
                    getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                ).getString("discord_avatar", null)
                val username = getSharedPreferences(
                    getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                ).getString("discord_username", null)
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
                            if (binding.settingUPI.visibility == View.VISIBLE) binding.settingUPI.performClick()
                            else binding.settingBuyMeCoffee.performClick()
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
                getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).edit()
                    .putInt("custom_theme_int", color).apply()
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
}