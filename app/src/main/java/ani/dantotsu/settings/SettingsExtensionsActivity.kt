package ani.dantotsu.settings

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
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
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.customAlertDialog
import eu.kanade.domain.base.BasePreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsExtensionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsExtensionsBinding
    private val extensionInstaller = Injekt.get<BasePreferences>().extensionInstaller()

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
                val prefName: PrefName = when (type) {
                    MediaType.ANIME -> {
                        PrefName.AnimeExtensionRepos
                    }

                    MediaType.MANGA -> {
                        PrefName.MangaExtensionRepos
                    }

                    MediaType.NOVEL -> {
                        PrefName.NovelExtensionRepos
                    }
                }
                PrefManager.getVal<Set<String>>(prefName).forEach { item ->
                    val view = ItemRepositoryBinding.inflate(
                        LayoutInflater.from(repoInventory.context), repoInventory, true
                    )
                    view.repositoryItem.text =
                        item.removePrefix("https://raw.githubusercontent.com/")

                    view.repositoryItem.setOnLongClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        copyToClipboard(item, true)
                        true
                    }
                }
                repoInventory.isVisible = repoInventory.childCount > 0
            }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = 1,
                        name = getString(R.string.anime_add_repository),
                        desc = getString(R.string.anime_add_repository_desc),
                        icon = R.drawable.ic_github,
                        onClick = {
                            val animeRepos =
                                PrefManager.getVal<Set<String>>(PrefName.AnimeExtensionRepos)
                            AddRepositoryBottomSheet.newInstance(
                                MediaType.ANIME,
                                animeRepos.toList(),
                                onRepositoryAdded = { input, mediaType ->
                                    AddRepositoryBottomSheet.addRepo(input, mediaType)
                                    setExtensionOutput(it.attachView, mediaType)
                                },
                                onRepositoryRemoved = { item, mediaType ->
                                    AddRepositoryBottomSheet.removeRepo(item, mediaType)
                                    setExtensionOutput(it.attachView, mediaType)
                                }
                            ).show(supportFragmentManager, "add_repo")
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
                            val mangaRepos =
                                PrefManager.getVal<Set<String>>(PrefName.MangaExtensionRepos)
                            AddRepositoryBottomSheet.newInstance(
                                MediaType.MANGA,
                                mangaRepos.toList(),
                                onRepositoryAdded = { input, mediaType ->
                                    AddRepositoryBottomSheet.addRepo(input, mediaType)
                                    setExtensionOutput(it.attachView, mediaType)
                                },
                                onRepositoryRemoved = { item, mediaType ->
                                    AddRepositoryBottomSheet.removeRepo(item, mediaType)
                                    setExtensionOutput(it.attachView, mediaType)
                                }
                            ).show(supportFragmentManager, "add_repo")
                        },
                        attach = {
                            setExtensionOutput(it.attachView, MediaType.MANGA)
                        }
                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.novel_add_repository),
                        desc = getString(R.string.novel_add_repository_desc),
                        icon = R.drawable.ic_github,
                        onClick = {
                            val novelRepos =
                                PrefManager.getVal<Set<String>>(PrefName.NovelExtensionRepos)
                            AddRepositoryBottomSheet.newInstance(
                                MediaType.NOVEL,
                                novelRepos.toList(),
                                onRepositoryAdded = { input, mediaType ->
                                    AddRepositoryBottomSheet.addRepo(input, mediaType)
                                    setExtensionOutput(it.attachView, mediaType)
                                },
                                onRepositoryRemoved = { item, mediaType ->
                                    AddRepositoryBottomSheet.removeRepo(item, mediaType)
                                    setExtensionOutput(it.attachView, mediaType)
                                }
                            ).show(supportFragmentManager, "add_repo")
                        },
                        attach = {
                            setExtensionOutput(it.attachView, MediaType.NOVEL)
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
                                    PrefManager.setVal(
                                        PrefName.DefaultUserAgent,
                                        editText.text.toString()
                                    )
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