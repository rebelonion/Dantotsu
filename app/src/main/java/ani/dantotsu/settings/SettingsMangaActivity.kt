package ani.dantotsu.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivitySettingsMangaBinding
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.initActivity
import ani.dantotsu.media.MediaType
import ani.dantotsu.navBarHeight
import ani.dantotsu.restartApp
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsMangaActivity: AppCompatActivity(){
    private lateinit var binding: ActivitySettingsMangaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this
        binding = ActivitySettingsMangaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            settingsMangaLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            mangaSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            purgeMangaDownloads.setOnClickListener {
                val dialog = AlertDialog.Builder(context, R.style.MyPopup)
                    .setTitle(R.string.purge_manga_downloads)
                    .setMessage(getString(R.string.purge_confirm, getString(R.string.manga)))
                    .setPositiveButton(R.string.yes) { dialog, _ ->
                        val downloadsManager = Injekt.get<DownloadsManager>()
                        downloadsManager.purgeDownloads(MediaType.MANGA)
                        dialog.dismiss()
                    }.setNegativeButton(R.string.no) { dialog, _ ->
                        dialog.dismiss()
                    }.create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            purgeNovelDownloads.setOnClickListener {
                val dialog = AlertDialog.Builder(context, R.style.MyPopup)
                    .setTitle(R.string.purge_novel_downloads)
                    .setMessage(getString(R.string.purge_confirm, getString(R.string.novels)))
                    .setPositiveButton(R.string.yes) { dialog, _ ->
                        val downloadsManager = Injekt.get<DownloadsManager>()
                        downloadsManager.purgeDownloads(MediaType.NOVEL)
                        dialog.dismiss()
                    }.setNegativeButton(R.string.no) { dialog, _ ->
                        dialog.dismiss()
                    }.create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            settingsReader.setOnClickListener {
                startActivity(Intent(context, ReaderSettingsActivity::class.java))
            }

            var previousChp: View = when (PrefManager.getVal<Int>(PrefName.MangaDefaultView)) {
                0 -> settingsChpList
                1 -> settingsChpCompact
                else -> settingsChpList
            }
            previousChp.alpha = 1f
            fun uiChp(mode: Int, current: View) {
                previousChp.alpha = 0.33f
                previousChp = current
                current.alpha = 1f
                PrefManager.setVal(PrefName.MangaDefaultView, mode)
            }

            settingsChpList.setOnClickListener {
                uiChp(0, it)
            }

            settingsChpCompact.setOnClickListener {
                uiChp(1, it)
            }

            settingsIncludeMangaList.isChecked = PrefManager.getVal(PrefName.IncludeMangaList)
            settingsIncludeMangaList.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.IncludeMangaList, isChecked)
                restartApp(binding.root)
            }
        }
    }
}