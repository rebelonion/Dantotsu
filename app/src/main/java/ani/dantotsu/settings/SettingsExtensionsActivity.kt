package ani.dantotsu.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import ani.dantotsu.R
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ActivitySettingsExtensionsBinding
import ani.dantotsu.databinding.ItemRepositoryBinding
import ani.dantotsu.initActivity
import ani.dantotsu.media.MediaType
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import com.google.android.material.textfield.TextInputEditText
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsExtensionsActivity: AppCompatActivity() {
    private lateinit var binding: ActivitySettingsExtensionsBinding
    private val extensionInstaller = Injekt.get<BasePreferences>().extensionInstaller()
    private val animeExtensionManager: AnimeExtensionManager by injectLazy()
    private val mangaExtensionManager: MangaExtensionManager by injectLazy()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this
        binding = ActivitySettingsExtensionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {

            fun setExtensionOutput(repoInventory: ViewGroup, type: MediaType) {
                repoInventory.removeAllViews()
                val prefName: PrefName? = when (type) {
                    MediaType.ANIME -> {
                        PrefName.AnimeExtensionRepos
                    }

                    MediaType.MANGA -> {
                        PrefName.MangaExtensionRepos
                    }

                    else -> {
                        null
                    }
                }
                prefName?.let { repoList ->
                    PrefManager.getVal<Set<String>>(repoList).forEach { item ->
                        val view = ItemRepositoryBinding.inflate(
                            LayoutInflater.from(repoInventory.context), repoInventory, true
                        )
                        view.repositoryItem.text =
                            item.removePrefix("https://raw.githubusercontent.com")
                        view.repositoryItem.setOnClickListener {
                            AlertDialog.Builder(context, R.style.MyPopup)
                                .setTitle(R.string.rem_repository).setMessage(item)
                                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                                    val repos =
                                        PrefManager.getVal<Set<String>>(repoList).minus(item)
                                    PrefManager.setVal(repoList, repos)
                                    setExtensionOutput(repoInventory, type)
                                    CoroutineScope(Dispatchers.IO).launch {
                                        when (type) {
                                            MediaType.ANIME -> {
                                                animeExtensionManager.findAvailableExtensions()
                                            }

                                            MediaType.MANGA -> {
                                                mangaExtensionManager.findAvailableExtensions()
                                            }

                                            else -> {}
                                        }
                                    }
                                    dialog.dismiss()
                                }.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                                    dialog.dismiss()
                                }.create().show()
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
                val entry =
                    if (input.endsWith("/") || input.endsWith("index.min.json")) input.substring(
                        0,
                        input.lastIndexOf("/")
                    ) else input
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
                    if (action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_DONE || (keyEvent?.action == KeyEvent.ACTION_UP && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
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
            settingsExtensionsLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            settingsExtensionsTitle.setOnClickListener{
                onBackPressedDispatcher.onBackPressed()
            }
            setExtensionOutput(animeRepoInventory, MediaType.ANIME)
            setExtensionOutput(mangaRepoInventory, MediaType.MANGA)
            animeAddRepository.setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.dialog_user_agent, null)
                val editText =
                    dialogView.findViewById<TextInputEditText>(R.id.userAgentTextBox).apply {
                        hint = getString(R.string.anime_add_repository)
                    }
                val alertDialog = AlertDialog.Builder(context, R.style.MyPopup)
                    .setTitle(R.string.anime_add_repository).setView(dialogView)
                    .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                        if (!editText.text.isNullOrBlank()) processUserInput(
                            editText.text.toString(),
                            MediaType.ANIME
                        )
                        dialog.dismiss()
                    }.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }.create()

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
                val alertDialog = AlertDialog.Builder(context, R.style.MyPopup)
                    .setTitle(R.string.manga_add_repository).setView(dialogView)
                    .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                        if (!editText.text.isNullOrBlank()) processUserInput(
                            editText.text.toString(),
                            MediaType.MANGA
                        )
                        dialog.dismiss()
                    }.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }.create()

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

            skipExtensionIcons.isChecked = PrefManager.getVal(PrefName.SkipExtensionIcons)
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
                val alertDialog = AlertDialog.Builder(context, R.style.MyPopup)
                    .setTitle(R.string.user_agent).setView(dialogView)
                    .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                        PrefManager.setVal(PrefName.DefaultUserAgent, editText.text.toString())
                        dialog.dismiss()
                    }.setNeutralButton(getString(R.string.reset)) { dialog, _ ->
                        PrefManager.removeVal(PrefName.DefaultUserAgent)
                        editText.setText("")
                        dialog.dismiss()
                    }.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }.create()

                alertDialog.show()
                alertDialog.window?.setDimAmount(0.8f)
            }
        }
    }

}