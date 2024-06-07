package ani.dantotsu.settings

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivitySettingsAnilistBinding
import ani.dantotsu.databinding.ItemSettingsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.restartApp
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.Logger

class SettingsAnilistActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsAnilistBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this
        binding = ActivitySettingsAnilistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            settingsAnilistLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            binding.anilistSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            val titleLang = listOf(
                "English (Attack on Titan)",
                "Romaji (Shingeki no Kyojin)",
                "Native (進撃の巨人)"
            )
            settingsAnilistLanguage.setText(titleLang[PrefManager.getVal(PrefName.SelectedLanguage)])
            settingsAnilistLanguage.setAdapter(
                ArrayAdapter(
                    context, R.layout.item_dropdown, titleLang
                )
            )
            settingsAnilistLanguage.setOnItemClickListener { _, _, i, _ ->
                PrefManager.setVal(PrefName.SelectedLanguage, i)
                settingsAnilistLanguage.clearFocus()
            }

            binding.settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = 2,
                        name = getString(R.string.restrict_messages),
                        desc = getString(R.string.restrict_messages_desc),
                        icon = R.drawable.ic_round_lock_24,
                        isChecked = PrefManager.getVal(PrefName.SettingsPreferDub),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.SettingsPreferDub, isChecked)
                        }
                    ),
                )
            )
            binding.settingsRecyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        }
    }
}