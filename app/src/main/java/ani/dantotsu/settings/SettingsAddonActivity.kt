package ani.dantotsu.settings

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.addons.AddonDownloader
import ani.dantotsu.addons.download.DownloadAddonManager
import ani.dantotsu.addons.torrent.TorrentAddonManager
import ani.dantotsu.addons.torrent.TorrentServerService
import ani.dantotsu.databinding.ActivitySettingsAddonsBinding
import ani.dantotsu.databinding.ItemSettingsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.core.util.lang.launchIO
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsAddonActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsAddonsBinding
    private val downloadAddonManager: DownloadAddonManager = Injekt.get()
    private val torrentAddonManager: TorrentAddonManager = Injekt.get()

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

            addonSettingsBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = 1,
                        name = getString(R.string.anime_downloader_addon),
                        desc = getString(R.string.not_installed),
                        icon = R.drawable.ic_download_24,
                        isActivity = true,
                        attach = {
                            setStatus(
                                view = it,
                                context = context,
                                status = downloadAddonManager.hadError(context),
                                hasUpdate = downloadAddonManager.hasUpdate
                            )
                            var job = Job()
                            downloadAddonManager.addListenerAction { _ ->
                                job.cancel()
                                it.settingsIconRight.animate().cancel()
                                it.settingsIconRight.rotation = 0f
                                setStatus(
                                    view = it,
                                    context = context,
                                    status = downloadAddonManager.hadError(context),
                                    hasUpdate = false
                                )
                            }
                            it.settingsIconRight.setOnClickListener { _ ->
                                if (it.settingsDesc.text == getString(R.string.installed)) {
                                    downloadAddonManager.uninstall()
                                    return@setOnClickListener
                                } else {
                                    job = Job()
                                    val scope = CoroutineScope(Dispatchers.Main + job)
                                    it.settingsIconRight.setImageResource(R.drawable.ic_sync)
                                    scope.launch {
                                        while (isActive) {
                                            withContext(Dispatchers.Main) {
                                                it.settingsIconRight.animate()
                                                    .rotationBy(360f)
                                                    .setDuration(1000)
                                                    .setInterpolator(LinearInterpolator())
                                                    .start()
                                            }
                                            delay(1000)
                                        }
                                    }
                                    snackString(getString(R.string.downloading))
                                    lifecycleScope.launchIO {
                                        AddonDownloader.update(
                                            activity = context,
                                            downloadAddonManager,
                                            repo = DownloadAddonManager.REPO,
                                            currentVersion = downloadAddonManager.getVersion() ?: ""
                                        )
                                    }
                                }
                            }
                        },
                    ), Settings(
                        type = 1,
                        name = getString(R.string.torrent_addon),
                        desc = getString(R.string.not_installed),
                        icon = R.drawable.ic_round_magnet_24,
                        isActivity = true,
                        attach = {
                            setStatus(
                                view = it,
                                context = context,
                                status = torrentAddonManager.hadError(context),
                                hasUpdate = torrentAddonManager.hasUpdate
                            )
                            var job = Job()
                            torrentAddonManager.addListenerAction { _ ->
                                job.cancel()
                                it.settingsIconRight.animate().cancel()
                                it.settingsIconRight.rotation = 0f
                                setStatus(
                                    view = it,
                                    context = context,
                                    status = torrentAddonManager.hadError(context),
                                    hasUpdate = false
                                )
                            }
                            it.settingsIconRight.setOnClickListener { _ ->
                                if (it.settingsDesc.text == getString(R.string.installed)) {
                                    TorrentServerService.stop()
                                    torrentAddonManager.uninstall()
                                    return@setOnClickListener
                                } else {
                                    job = Job()
                                    val scope = CoroutineScope(Dispatchers.Main + job)
                                    it.settingsIconRight.setImageResource(R.drawable.ic_sync)
                                    scope.launch {
                                        while (isActive) {
                                            withContext(Dispatchers.Main) {
                                                it.settingsIconRight.animate()
                                                    .rotationBy(360f)
                                                    .setDuration(1000)
                                                    .setInterpolator(LinearInterpolator())
                                                    .start()
                                            }
                                            delay(1000)
                                        }
                                    }
                                    snackString(getString(R.string.downloading))
                                    lifecycleScope.launchIO {
                                        AddonDownloader.update(
                                            activity = context,
                                            torrentAddonManager,
                                            repo = TorrentAddonManager.REPO,
                                            currentVersion = torrentAddonManager.getVersion() ?: "",
                                        )
                                    }
                                }
                            }
                        },
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.enable_torrent),
                        desc = getString(R.string.enable_torrent_desc),
                        icon = R.drawable.ic_round_dns_24,
                        isChecked = PrefManager.getVal(PrefName.TorrentEnabled),
                        switch = { isChecked, it ->
                            if (isChecked && !torrentAddonManager.isAvailable(false)) {
                                snackString(getString(R.string.install_torrent_addon))
                                it.settingsButton.isChecked = false
                                PrefManager.setVal(PrefName.TorrentEnabled, false)
                                return@Settings
                            }
                            PrefManager.setVal(PrefName.TorrentEnabled, isChecked)
                            Injekt.get<TorrentAddonManager>().extension?.let {
                                if (isChecked) {
                                    lifecycleScope.launchIO {
                                        if (!TorrentServerService.isRunning()) {
                                            TorrentServerService.start()
                                        }
                                    }
                                } else {
                                    lifecycleScope.launchIO {
                                        if (TorrentServerService.isRunning()) {
                                            TorrentServerService.stop()
                                        }
                                    }
                                }
                            }
                        },
                        isVisible = torrentAddonManager.isAvailable(false)
                    )
                )
            )
            settingsRecyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        torrentAddonManager.removeListenerAction()
        downloadAddonManager.removeListenerAction()
    }

    private fun setStatus(
        view: ItemSettingsBinding,
        context: Context,
        status: String?,
        hasUpdate: Boolean
    ) {
        try {
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
        } catch (e: Exception) {
            Logger.log(e)
        }
    }
}