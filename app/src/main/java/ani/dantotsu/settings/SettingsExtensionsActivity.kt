package ani.dantotsu.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ActivitySettingsExtensionsBinding
import ani.dantotsu.databinding.DialogUserAgentBinding
import ani.dantotsu.databinding.ItemRepositoryBinding
import ani.dantotsu.initActivity
import ani.dantotsu.media.MediaType
import ani.dantotsu.navBarHeight
import ani.dantotsu.parsers.ParserTestActivity
import ani.dantotsu.restartApp
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.customAlertDialog
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsExtensionsActivity : AppCompatActivity() {
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
            settingsExtensionsLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            extensionSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
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
                            item.removePrefix("https://raw.githubusercontent.com/")
                        view.repositoryItem.setOnClickListener {
                            context.customAlertDialog().apply {
                                setTitle(R.string.rem_repository)
                                setMessage(item)
                                setPosButton(R.string.ok) {
                                    val repos = PrefManager.getVal<Set<String>>(repoList).minus(item)
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
                                }
                                setNegButton(R.string.cancel)
                                show()
                            }
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

            fun processUserInput(input: String, mediaType: MediaType, view: ViewGroup) {
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
                    setExtensionOutput(view, MediaType.ANIME)
                }
                if (mediaType == MediaType.MANGA) {
                    val manga =
                        PrefManager.getVal<Set<String>>(PrefName.MangaExtensionRepos).plus(entry)
                    PrefManager.setVal(PrefName.MangaExtensionRepos, manga)
                    CoroutineScope(Dispatchers.IO).launch {
                        mangaExtensionManager.findAvailableExtensions()
                    }
                    setExtensionOutput(view, MediaType.MANGA)
                }
            }

            fun processEditorAction(
                dialog: AlertDialog,
                editText: EditText,
                mediaType: MediaType,
                view: ViewGroup
            ) {
                editText.setOnEditorActionListener { textView, action, keyEvent ->
                    if (action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_DONE || (keyEvent?.action == KeyEvent.ACTION_UP && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                        return@setOnEditorActionListener if (textView.text.isNullOrBlank()) {
                            false
                        } else {
                            processUserInput(textView.text.toString(), mediaType, view)
                            dialog.dismiss()
                            true
                        }
                    }
                    false
                }
            }
            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = 1,
                        name = getString(R.string.anime_add_repository),
                        desc = getString(R.string.anime_add_repository_desc),
                        icon = R.drawable.ic_github,
                        onClick = {
                            val dialogView = DialogUserAgentBinding.inflate(layoutInflater)
                            val editText = dialogView.userAgentTextBox.apply {
                                hint = getString(R.string.anime_add_repository)
                            }
                            context.customAlertDialog().apply {
                                setTitle(R.string.anime_add_repository)
                                setCustomView(dialogView.root)
                                setPosButton(getString(R.string.ok)) {
                                    if (!editText.text.isNullOrBlank()) processUserInput(
                                        editText.text.toString(),
                                        MediaType.ANIME,
                                        it.attachView
                                    )
                                }
                                setNegButton(getString(R.string.cancel))
                                attach { dialog ->
                                    processEditorAction(
                                        dialog,
                                        editText,
                                        MediaType.ANIME,
                                        it.attachView
                                    )
                                }
                                show()
                            }
                        },
                        attach = {
                            setExtensionOutput(it.attachView, MediaType.ANIME)
                        }
                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.manga_add_repository),
                        desc = getString(R.string.manga_add_repository_desc),
                        icon = R.drawable.ic_github,
                        onClick = {
                            val dialogView = DialogUserAgentBinding.inflate(layoutInflater)
                            val editText = dialogView.userAgentTextBox.apply {
                                hint = getString(R.string.manga_add_repository)
                            }
                            context.customAlertDialog().apply {
                                setTitle(R.string.manga_add_repository)
                                setCustomView(dialogView.root)
                                setPosButton(R.string.ok) {
                                    if (!editText.text.isNullOrBlank()) processUserInput(
                                        editText.text.toString(),
                                        MediaType.MANGA,
                                        it.attachView
                                    )
                                }
                                setNegButton(R.string.cancel)
                                attach { dialog ->
                                    processEditorAction(
                                        dialog,
                                        editText,
                                        MediaType.MANGA,
                                        it.attachView
                                    )
                                }
                            }.show()

                        },
                        attach = {
                            setExtensionOutput(it.attachView, MediaType.MANGA)
                        }
                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.extension_test),
                        desc = getString(R.string.extension_test_desc),
                        icon = R.drawable.ic_round_search_sources_24,
                        isActivity = true,
                        onClick = {
                            ContextCompat.startActivity(
                                context,
                                Intent(context, ParserTestActivity::class.java),
                                null
                            )
                        }
                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.user_agent),
                        desc = getString(R.string.user_agent_desc),
                        icon = R.drawable.ic_round_video_settings_24,
                        onClick = {
                            val dialogView = DialogUserAgentBinding.inflate(layoutInflater)
                            val editText = dialogView.userAgentTextBox
                            editText.setText(PrefManager.getVal<String>(PrefName.DefaultUserAgent))
                            context.customAlertDialog().apply {
                                setTitle(R.string.user_agent)
                                setCustomView(dialogView.root)
                                setPosButton(R.string.ok) {
                                    PrefManager.setVal(PrefName.DefaultUserAgent, editText.text.toString())
                                }
                                setNeutralButton(R.string.reset) {
                                    PrefManager.removeVal(PrefName.DefaultUserAgent)
                                    editText.setText("")
                                }
                                setNegButton(R.string.cancel)
                            }.show()
                        }
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.proxy),
                        desc = getString(R.string.proxy_desc),
                        icon = R.drawable.swap_horizontal_circle_24,
                        isChecked = PrefManager.getVal(PrefName.EnableSocks5Proxy),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.EnableSocks5Proxy, isChecked)
                            restartApp()
                        }
                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.proxy_setup),
                        desc = getString(R.string.proxy_setup_desc),
                        icon = R.drawable.lan_24,
                        onClick = {
                            ProxyDialogFragment().show(supportFragmentManager, "dialog")
                        }
                    ),
                    Settings( 
                        type = 2,
                        name = getString(R.string.force_legacy_installer),
                        desc = getString(R.string.force_legacy_installer_desc),
                        icon = R.drawable.ic_round_new_releases_24,
                        isChecked = extensionInstaller.get() == BasePreferences.ExtensionInstaller.LEGACY,
                        switch = { isChecked, _ ->
                            if (isChecked) {
                                extensionInstaller.set(BasePreferences.ExtensionInstaller.LEGACY)
                            } else {
                                extensionInstaller.set(BasePreferences.ExtensionInstaller.PACKAGEINSTALLER)
                            }
                        }

                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.skip_loading_extension_icons),
                        desc = getString(R.string.skip_loading_extension_icons_desc),
                        icon = R.drawable.ic_round_no_icon_24,
                        isChecked = PrefManager.getVal(PrefName.SkipExtensionIcons),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.SkipExtensionIcons, isChecked)
                        }
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.NSFWExtention),
                        desc = getString(R.string.NSFWExtention_desc),
                        icon = R.drawable.ic_round_nsfw_24,
                        isChecked = PrefManager.getVal(PrefName.NSFWExtension),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.NSFWExtension, isChecked)
                        }

                    )
                )
            )
            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }
        }

    }
}