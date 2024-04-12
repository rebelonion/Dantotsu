package ani.dantotsu.settings

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.documentfile.provider.DocumentFile
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivitySettingsCommonBinding
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.restartApp
import ani.dantotsu.savePrefsToDownloads
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.settings.saving.internal.Location
import ani.dantotsu.settings.saving.internal.PreferenceKeystore
import ani.dantotsu.settings.saving.internal.PreferencePackager
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import ani.dantotsu.util.LauncherWrapper
import ani.dantotsu.util.StoragePermissions
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsCommonActivity: AppCompatActivity(){
    private lateinit var binding: ActivitySettingsCommonBinding
    private lateinit var launcher: LauncherWrapper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this
        binding = ActivitySettingsCommonBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
                                            password, encrypted, salt
                                        )
                                    } catch (e: Exception) {
                                        toast(getString(R.string.incorrect_password))
                                        return@passwordAlertDialog
                                    }
                                    if (PreferencePackager.unpack(decryptedJson)) restartApp(binding.root)
                                } else {
                                    toast(getString(R.string.password_cannot_be_empty))
                                }
                            }
                        } else if (name.endsWith(".ani")) {
                            val decryptedJson = jsonString.toString(Charsets.UTF_8)
                            if (PreferencePackager.unpack(decryptedJson)) restartApp(binding.root)
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
        val managers = arrayOf("Default", "1DM", "ADM")
        val downloadManagerDialog =
            AlertDialog.Builder(this, R.style.MyPopup).setTitle(R.string.download_manager)
        var downloadManager: Int = PrefManager.getVal(PrefName.DownloadManager)
        binding.apply {

            settingsCommonLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            commonSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            settingsDownloadManager.setOnClickListener {
                val dialog = downloadManagerDialog.setSingleChoiceItems(
                    managers, downloadManager
                ) { dialog, count ->
                    downloadManager = count
                    PrefManager.setVal(PrefName.DownloadManager, downloadManager)
                    dialog.dismiss()
                }.show()
                dialog.window?.setDimAmount(0.8f)
            }

            importExportSettings.setOnClickListener {
                StoragePermissions.downloadsPermission(context)
                val selectedArray = mutableListOf(false)
                val filteredLocations = Location.entries.filter { it.exportable }
                selectedArray.addAll(List(filteredLocations.size - 1) { false })
                val dialog = AlertDialog.Builder(context, R.style.MyPopup)
                    .setTitle(R.string.backup_restore).setMultiChoiceItems(
                        filteredLocations.map { it.name }.toTypedArray(),
                        selectedArray.toBooleanArray()
                    ) { _, which, isChecked ->
                        selectedArray[which] = isChecked
                    }.setPositiveButton(R.string.button_restore) { dialog, _ ->
                        openDocumentLauncher.launch(arrayOf("*/*"))
                        dialog.dismiss()
                    }.setNegativeButton(R.string.button_backup) { dialog, _ ->
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
                                        context,
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
                                context,
                                null
                            )
                        }
                    }.setNeutralButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }.create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
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
            settingsExtensionDns.setText(exDns[PrefManager.getVal(PrefName.DohProvider)])
            settingsExtensionDns.setAdapter(
                ArrayAdapter(
                    context, R.layout.item_dropdown, exDns
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
                val dialog = AlertDialog.Builder(context, R.style.MyPopup)
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
                                        context, Uri.parse(oldUri), Uri.parse(newUri)
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
                    }.setNeutralButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }.create()
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
                initActivity(context)
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
                        context, UserInterfaceSettingsActivity::class.java
                    )
                )
            }
        }
    }
    private fun passwordAlertDialog(isExporting: Boolean, callback: (CharArray?) -> Unit) {
        val password = CharArray(16).apply { fill('0') }

        // Inflate the dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_agent, null)
        val box = dialogView.findViewById<TextInputEditText>(R.id.userAgentTextBox)
        box?.hint = getString(R.string.password)
        box?.setSingleLine()

        val dialog =
            AlertDialog.Builder(this, R.style.MyPopup).setTitle(getString(R.string.enter_password))
                .setView(dialogView).setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    password.fill('0')
                    dialog.dismiss()
                    callback(null)
                }.create()

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
        if (!isExporting) subtitleTextView?.text =
            getString(R.string.enter_password_to_decrypt_file)


        dialog.window?.setDimAmount(0.8f)
        dialog.show()

        // Override the positive button here
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            handleOkAction()
        }

    }

}