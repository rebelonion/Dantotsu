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
import ani.dantotsu.R
import ani.dantotsu.addons.AddonDownloader
import ani.dantotsu.addons.download.DownloadAddonManager
import ani.dantotsu.addons.torrent.ServerService
import ani.dantotsu.addons.torrent.TorrentAddonManager
import ani.dantotsu.databinding.ActivitySettingsAddonsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
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

        binding = ActivitySettingsAddonsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            settingsAddonsLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }

            binding.addonSettingsBack.setOnClickListener { onBackPressed() }

            binding.torrentAddonSwitch.isChecked = PrefManager.getVal(PrefName.TorrentEnabled)
            binding.torrentAddonSwitch.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.TorrentEnabled, isChecked)
                Injekt.get<TorrentAddonManager>().extension?.let {
                    if (isChecked) {
                        launchIO {
                            if (!ServerService.isRunning()) {
                                ServerService.start()
                            }
                        }
                    } else {
                        launchIO {
                            if (ServerService.isRunning()) {
                                ServerService.stop()
                            }
                        }
                    }
                }
            }

            val torrentAddonManager = Injekt.get<TorrentAddonManager>()
            if (torrentAddonManager.hasUpdate || torrentAddonManager.extension == null) {
                binding.settingsTorrentUpdate.visibility = View.VISIBLE
                binding.torrentAddonUpdateText.text = if (torrentAddonManager.hasUpdate) {
                    getString(R.string.update_addon)
                } else {
                    getString(R.string.install_addon)
                }
                binding.settingsTorrentUpdate.setOnClickListener {
                    lifecycleScope.launchIO {
                        AddonDownloader.update(
                            this@SettingsAddonActivity,
                            TorrentAddonManager.REPO,
                            torrentAddonManager.getVersion() ?: ""
                        ) {
                            if (it) {
                                toast(getString(R.string.success))
                                lifecycleScope.launch {
                                    torrentAddonManager.init()
                                }
                            } else {
                                toast(getString(R.string.error))
                            }
                        }


                    }
                }
            } else {
                binding.settingsTorrentUpdate.visibility = View.GONE
            }

            setStatus(
                this@SettingsAddonActivity,
                binding.torrentAddonStatusIcon,
                binding.torrentAddonStatus,
                torrentAddonManager.hadError(this@SettingsAddonActivity),
                torrentAddonManager.hasUpdate
            )

            val downloadAddonManager = Injekt.get<DownloadAddonManager>()
            if (downloadAddonManager.hasUpdate || downloadAddonManager.extension == null) {
                binding.settingsDownloadUpdate.visibility = View.VISIBLE
                binding.downloadAddonUpdateText.text = if (downloadAddonManager.hasUpdate) {
                    getString(R.string.update_addon)
                } else {
                    getString(R.string.install_addon)
                }
                binding.settingsDownloadUpdate.setOnClickListener {
                    lifecycleScope.launchIO {
                        AddonDownloader.update(
                            this@SettingsAddonActivity,
                            DownloadAddonManager.REPO,
                            downloadAddonManager.getVersion() ?: ""
                        ) {
                            if (it) {
                                toast(getString(R.string.success))
                                lifecycleScope.launch {
                                    downloadAddonManager.init()
                                }
                            } else {
                                toast(getString(R.string.error))
                            }
                        }
                    }
                }
            } else {
                binding.settingsDownloadUpdate.visibility = View.GONE
            }

            setStatus(
                this@SettingsAddonActivity,
                binding.downloadAddonStatusIcon,
                binding.downloadAddonStatus,
                downloadAddonManager.hadError(this@SettingsAddonActivity),
                downloadAddonManager.hasUpdate
            )
        }
    }

    private fun setStatus(context: Context, icon: ImageView, textView: TextView, status: String?, hasUpdate: Boolean) {
        when (status) {
            context.getString(R.string.loaded_successfully) -> {
                icon.setImageResource(R.drawable.ic_circle_check)
                icon.setColorFilter(ContextCompat.getColor(context, R.color.literally_just_green))
                textView.text = context.getString(R.string.loaded_successfully)
                textView.setTextColor(ContextCompat.getColor(context, R.color.literally_just_green))
            }

            null -> {
                icon.setImageResource(R.drawable.ic_download_24)
                textView.text = context.getString(R.string.not_installed)
            }

            else -> {
                icon.setImageResource(R.drawable.ic_round_new_releases_24)
                icon.setColorFilter(ContextCompat.getColor(context, R.color.yt_red))
                textView.text = context.getString(R.string.error_msg, status)
                textView.setTextColor(ContextCompat.getColor(context, R.color.yt_red))
            }
        }
        if (hasUpdate) {
            icon.setImageResource(R.drawable.ic_round_new_releases_24)
            icon.setColorFilter(ContextCompat.getColor(context, R.color.light_blue_900))
            textView.text = context.getString(R.string.update_addon)
        }
    }
}