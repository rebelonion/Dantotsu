package ani.dantotsu.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
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
import ani.dantotsu.util.customAlertDialog
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsMangaActivity : AppCompatActivity() {
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

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = 1,
                        name = getString(R.string.reader_settings),
                        desc = getString(R.string.reader_settings_desc),
                        icon = R.drawable.ic_round_reader_settings,
                        onClick = {
                            startActivity(Intent(context, ReaderSettingsActivity::class.java))
                        },
                        isActivity = true
                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.purge_manga_downloads),
                        desc = getString(R.string.purge_manga_downloads_desc),
                        icon = R.drawable.ic_round_delete_24,
                        onClick = {
                            context.customAlertDialog().apply {
                                setTitle(R.string.purge_manga_downloads)
                                setMessage(R.string.purge_confirm, getString(R.string.manga))
                                setPosButton(R.string.yes, onClick = {
                                    val downloadsManager = Injekt.get<DownloadsManager>()
                                    downloadsManager.purgeDownloads(MediaType.MANGA)
                                })
                                setNegButton(R.string.no)
                                show()
                            }
                        }

                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.purge_novel_downloads),
                        desc = getString(R.string.purge_novel_downloads_desc),
                        icon = R.drawable.ic_round_delete_24,
                        onClick = {
                            context.customAlertDialog().apply {
                                setTitle(R.string.purge_novel_downloads)
                                setMessage(R.string.purge_confirm, getString(R.string.novels))
                                setPosButton(R.string.yes) {
                                    val downloadsManager = Injekt.get<DownloadsManager>()
                                    downloadsManager.purgeDownloads(MediaType.NOVEL)
                                }
                                setNegButton(R.string.no)
                                show()
                            }
                        }
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.include_list),
                        desc = getString(R.string.include_list_desc),
                        icon = R.drawable.view_list_24,
                        isChecked = PrefManager.getVal(PrefName.IncludeMangaList),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.IncludeMangaList, isChecked)
                            restartApp()
                        }
                    ),
                )
            )
            settingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                setHasFixedSize(true)
            }
        }
    }
}