package ani.dantotsu.settings

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivitySettingsThemeBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.reloadActivity
import ani.dantotsu.restartApp
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import eltos.simpledialogfragment.color.SimpleColorDialog

class SettingsThemeActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsThemeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this
        binding = ActivitySettingsThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            settingsThemeLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            settingsThemeTitle.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

            settingsUseMaterialYou.apply {
                isChecked = PrefManager.getVal(PrefName.UseMaterialYou)

                setOnCheckedChangeListener { _, isChecked ->
                    PrefManager.setVal(PrefName.UseMaterialYou, isChecked)
                    if (isChecked) settingsUseCustomTheme.isChecked = false
                    restartApp(binding.root)
                }
            }

            settingsUseCustomTheme.isChecked = PrefManager.getVal(PrefName.UseCustomTheme)
            settingsUseCustomTheme.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.UseCustomTheme, isChecked)
                if (isChecked) {
                    settingsUseMaterialYou.isChecked = false
                }

                restartApp(binding.root)
            }

            settingsUseSourceTheme.isChecked = PrefManager.getVal(PrefName.UseSourceTheme)
            settingsUseSourceTheme.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.UseSourceTheme, isChecked)
                restartApp(binding.root)
            }

            settingsUseOLED.isChecked = PrefManager.getVal(PrefName.UseOLED)
            settingsUseOLED.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.UseOLED, isChecked)
                restartApp(binding.root)
            }

            val themeString: String = PrefManager.getVal(PrefName.Theme)
            val themeText = themeString.substring(0, 1) + themeString.substring(1).lowercase()
            themeSwitcher.setText(themeText)

            themeSwitcher.setAdapter(
                ArrayAdapter(context,
                    R.layout.item_dropdown,
                    ThemeManager.Companion.Theme.entries.map {
                        it.theme.substring(
                            0,
                            1
                        ) + it.theme.substring(1).lowercase()
                    })
            )

            themeSwitcher.setOnItemClickListener { _, _, i, _ ->
                PrefManager.setVal(PrefName.Theme, ThemeManager.Companion.Theme.entries[i].theme)
                //ActivityHelper.shouldRefreshMainActivity = true
                themeSwitcher.clearFocus()
                restartApp(binding.root)
            }

            customTheme.setOnClickListener {
                val originalColor: Int = PrefManager.getVal(PrefName.CustomThemeInt)

                class CustomColorDialog : SimpleColorDialog() { //idk where to put it
                    override fun onPositiveButtonClick() {
                        restartApp(binding.root)
                        super.onPositiveButtonClick()
                    }
                }

                val tag = "colorPicker"
                CustomColorDialog().title(R.string.custom_theme).colorPreset(originalColor)
                    .colors(context, SimpleColorDialog.MATERIAL_COLOR_PALLET)
                    .allowCustom(true).showOutline(0x46000000).gridNumColumn(5)
                    .choiceMode(SimpleColorDialog.SINGLE_CHOICE).neg()
                    .show(context, tag)
            }

            var previous: View = when (PrefManager.getVal<Int>(PrefName.DarkMode)) {
                0 -> settingsUiAuto
                1 -> settingsUiLight
                2 -> settingsUiDark
                else -> settingsUiAuto
            }
            previous.alpha = 1f
            fun uiTheme(mode: Int, current: View) {
                previous.alpha = 0.33f
                previous = current
                current.alpha = 1f
                PrefManager.setVal(PrefName.DarkMode, mode)
                reloadActivity()
            }

            settingsUiAuto.setOnClickListener {
                uiTheme(0, it)
            }

            settingsUiLight.setOnClickListener {
                settingsUseOLED.isChecked = false
                uiTheme(1, it)
            }

            settingsUiDark.setOnClickListener {
                uiTheme(2, it)
            }
        }
    }
}