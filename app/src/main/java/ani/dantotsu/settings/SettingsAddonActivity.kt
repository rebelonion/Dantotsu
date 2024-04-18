package ani.dantotsu.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.addons.AddonDownloader
import ani.dantotsu.addons.download.DownloadAddonManager
import ani.dantotsu.addons.torrent.ServerService
import ani.dantotsu.addons.torrent.TorrentAddonManager
import ani.dantotsu.databinding.ActivitySettingsAddonsBinding
import ani.dantotsu.databinding.ItemSettingsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import tachiyomi.core.util.lang.launchIO
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsAddonActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsAddonsBinding

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this
        binding = ActivitySettingsAddonsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            settingsAddonsLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }

            binding.addonSettingsBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

            val torrentAddonManager = Injekt.get<TorrentAddonManager>()
            val downloadAddonManager = Injekt.get<DownloadAddonManager>()
            binding.settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = 1,
                        name = getString(R.string.anime_downloader_addon),
                        desc = getString(R.string.not_installed),
                        icon = R.drawable.anim_play_to_pause,
                        isActivity = true,
                        attach = {
                            setStatus(
                                view = it,
                                context = context,
                                status = torrentAddonManager.hadError(context),
                                hasUpdate = torrentAddonManager.hasUpdate
                            )

                            it.settingsIconRight.setOnClickListener { _ ->
                                if (it.settingsDesc.text == getString(R.string.installed)) {
                                    toast(getString(R.string.error))
                                    return@setOnClickListener //uninstall logic here
                                }
                                lifecycleScope.launchIO {
                                    AddonDownloader.update(
                                        activity = context,
                                        repo = DownloadAddonManager.REPO,
                                        currentVersion = downloadAddonManager.getVersion() ?: "",
                                        success = { isInstalled ->
                                            if (!isInstalled) {
                                                toast(getString(R.string.error))
                                                return@update
                                            }
                                            toast(getString(R.string.success))
                                            lifecycleScope.launch {
                                                downloadAddonManager.init()
                                            }
                                            setStatus(
                                                view = it,
                                                context = context,
                                                status = torrentAddonManager.hadError(context),
                                                hasUpdate = torrentAddonManager.hasUpdate
                                            )
                                        }
                                    )
                                }
                            }
                        },
                    ), Settings(
                        type = 1,
                        name = getString(R.string.torrent_addon),
                        desc = getString(R.string.not_installed),
                        icon = R.drawable.anim_play_to_pause,
                        isActivity = true,
                        attach = {
                            setStatus(
                                view = it,
                                context = context,
                                status = torrentAddonManager.hadError(context),
                                hasUpdate = torrentAddonManager.hasUpdate
                            )
                            it.settingsIconRight.setOnClickListener { _ ->
                                if (it.settingsDesc.text == getString(R.string.installed)) {
                                    snackString(getString(R.string.error))
                                    return@setOnClickListener //uninstall logic here
                                }
                                lifecycleScope.launchIO {
                                    AddonDownloader.update(
                                        activity = context,
                                        repo = TorrentAddonManager.REPO,
                                        currentVersion = torrentAddonManager.getVersion() ?: "",
                                        success = { isInstalled ->
                                            if (!isInstalled) {
                                                snackString(getString(R.string.error))
                                                return@update
                                            }
                                            snackString(getString(R.string.success))
                                            lifecycleScope.launch {
                                                torrentAddonManager.init()
                                            }
                                            setStatus(
                                                view = it,
                                                context = context,
                                                status = torrentAddonManager.hadError(context),
                                                hasUpdate = torrentAddonManager.hasUpdate
                                            )
                                        }
                                    )
                                }
                            }
                        },
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.enable_torrent),
                        desc = getString(R.string.enable_torrent),
                        icon = R.drawable.ic_round_dns_24,
                        isChecked = PrefManager.getVal(PrefName.TorrentEnabled),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.TorrentEnabled, isChecked)
                            Injekt.get<TorrentAddonManager>().extension?.let {
                                if (isChecked) {
                                    lifecycleScope.launchIO {
                                        if (!ServerService.isRunning()) {
                                            ServerService.start()
                                        }
                                    }
                                } else {
                                    lifecycleScope.launchIO {
                                        if (ServerService.isRunning()) {
                                            ServerService.stop()
                                        }
                                    }
                                }
                            }
                        }
                    )
                )
            )
            binding.settingsRecyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)


        }
    }
    private fun setStatus(
        view: ItemSettingsBinding,
        context: Context,
        status: String?,
        hasUpdate: Boolean
    ) {
        when (status) {
            context.getString(R.string.loaded_successfully) -> {
                view.settingsIconRight.setImageResource(R.drawable.ic_round_delete_24)
                view.settingsIconRight.rotation = 0f
                view.settingsDesc.text = context.getString(R.string.installed)
            }

            null -> {
                view.settingsIconRight.setImageResource(R.drawable.ic_download_24)
                view.settingsIconRight.rotation = 0f
                view.settingsDesc.text = context.getString(R.string.not_installed)
            }

            else -> {
                view.settingsIconRight.setImageResource(R.drawable.ic_round_new_releases_24)
                view.settingsIconRight.rotation = 0f
                view.settingsDesc.text = context.getString(R.string.error_msg, status)
            }
        }
        if (hasUpdate) {
            view.settingsIconRight.setImageResource(R.drawable.ic_round_sync_24)
            view.settingsDesc.text = context.getString(R.string.update_addon)
        }
    }
}