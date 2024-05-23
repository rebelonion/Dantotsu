package ani.dantotsu.widgets.statistics

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ani.dantotsu.R
import ani.dantotsu.databinding.StatisticsWidgetConfigureBinding
import ani.dantotsu.getThemeColor
import ani.dantotsu.themes.ThemeManager
import com.google.android.material.button.MaterialButton
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.color.SimpleColorDialog

/**
 * The configuration screen for the [ProfileStatsWidget] AppWidget.
 */
class ProfileStatsConfigure : AppCompatActivity(),
    SimpleDialog.OnDialogResultListener {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isMonetEnabled = false
    private var onClickListener = View.OnClickListener {
        val context = this@ProfileStatsConfigure

        // It is the responsibility of the configuration activity to update the app widget
        val appWidgetManager = AppWidgetManager.getInstance(context)
        //updateAppWidget(context, appWidgetManager, appWidgetId)


        ProfileStatsWidget.updateAppWidget(
            context,
            appWidgetManager,
            appWidgetId
        )

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
    private lateinit var binding: StatisticsWidgetConfigureBinding

    public override fun onCreate(icicle: Bundle?) {

        ThemeManager(this).applyTheme()
        super.onCreate(icicle)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        binding = StatisticsWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val prefs =
            getSharedPreferences(ProfileStatsWidget.getPrefsName(appWidgetId), Context.MODE_PRIVATE)
        val topBackground =
            prefs.getInt(ProfileStatsWidget.PREF_BACKGROUND_COLOR, Color.parseColor("#80000000"))
        (binding.topBackgroundButton as MaterialButton).iconTint =
            ColorStateList.valueOf(topBackground)
        binding.topBackgroundButton.setOnClickListener {
            val tag = ProfileStatsWidget.PREF_BACKGROUND_COLOR
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(topBackground)
                .colors(
                    this@ProfileStatsConfigure,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .setupColorWheelAlpha(true)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@ProfileStatsConfigure, tag)
        }
        val bottomBackground =
            prefs.getInt(ProfileStatsWidget.PREF_BACKGROUND_FADE, Color.parseColor("#00000000"))
        (binding.bottomBackgroundButton as MaterialButton).iconTint =
            ColorStateList.valueOf(bottomBackground)
        binding.bottomBackgroundButton.setOnClickListener {
            val tag = ProfileStatsWidget.PREF_BACKGROUND_FADE
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(bottomBackground)
                .colors(
                    this@ProfileStatsConfigure,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .setupColorWheelAlpha(true)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@ProfileStatsConfigure, tag)
        }
        val titleColor = prefs.getInt(ProfileStatsWidget.PREF_TITLE_TEXT_COLOR, Color.WHITE)
        (binding.titleColorButton as MaterialButton).iconTint = ColorStateList.valueOf(titleColor)
        binding.titleColorButton.setOnClickListener {
            val tag = ProfileStatsWidget.PREF_TITLE_TEXT_COLOR
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(titleColor)
                .colors(
                    this@ProfileStatsConfigure,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .setupColorWheelAlpha(true)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@ProfileStatsConfigure, tag)
        }
        val statsColor = prefs.getInt(ProfileStatsWidget.PREF_STATS_TEXT_COLOR, Color.WHITE)
        (binding.statsColorButton as MaterialButton).iconTint = ColorStateList.valueOf(statsColor)
        binding.statsColorButton.setOnClickListener {
            val tag = ProfileStatsWidget.PREF_STATS_TEXT_COLOR
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(statsColor)
                .colors(
                    this@ProfileStatsConfigure,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .setupColorWheelAlpha(true)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@ProfileStatsConfigure, tag)
        }
        binding.useAppTheme.setOnCheckedChangeListener { _, isChecked ->
            isMonetEnabled = isChecked
            if (isChecked) {
                binding.topBackgroundButton.visibility = View.GONE
                binding.bottomBackgroundButton.visibility = View.GONE
                binding.titleColorButton.visibility = View.GONE
                binding.statsColorButton.visibility = View.GONE
                themeColors()

            } else {
                binding.topBackgroundButton.visibility = View.VISIBLE
                binding.bottomBackgroundButton.visibility = View.VISIBLE
                binding.titleColorButton.visibility = View.VISIBLE
                binding.statsColorButton.visibility = View.VISIBLE
            }
        }
        binding.addButton.setOnClickListener(onClickListener)

        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
    }

    private fun themeColors() {

        val backgroundColor = getThemeColor(com.google.android.material.R.attr.colorSurface)
        val textColor = getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val subTextColor = getThemeColor(com.google.android.material.R.attr.colorOutline)

        getSharedPreferences(
            ProfileStatsWidget.getPrefsName(appWidgetId),
            Context.MODE_PRIVATE
        ).edit().apply {
            putInt(ProfileStatsWidget.PREF_BACKGROUND_COLOR, backgroundColor)
            putInt(ProfileStatsWidget.PREF_BACKGROUND_FADE, backgroundColor)
            putInt(ProfileStatsWidget.PREF_TITLE_TEXT_COLOR, textColor)
            putInt(ProfileStatsWidget.PREF_STATS_TEXT_COLOR, subTextColor)
            apply()
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE) {
            if (!isMonetEnabled) {
                val prefs = getSharedPreferences(
                    ProfileStatsWidget.getPrefsName(appWidgetId),
                    Context.MODE_PRIVATE
                )
                when (dialogTag) {
                    ProfileStatsWidget.PREF_BACKGROUND_COLOR -> {
                        prefs.edit()
                            .putInt(
                                ProfileStatsWidget.PREF_BACKGROUND_COLOR,
                                extras.getInt(SimpleColorDialog.COLOR)
                            )
                            .apply()
                        (binding.topBackgroundButton as MaterialButton).iconTint =
                            ColorStateList.valueOf(extras.getInt(SimpleColorDialog.COLOR))
                    }

                    ProfileStatsWidget.PREF_BACKGROUND_FADE -> {
                        prefs.edit()
                            .putInt(
                                ProfileStatsWidget.PREF_BACKGROUND_FADE,
                                extras.getInt(SimpleColorDialog.COLOR)
                            )
                            .apply()
                        (binding.bottomBackgroundButton as MaterialButton).iconTint =
                            ColorStateList.valueOf(extras.getInt(SimpleColorDialog.COLOR))
                    }

                    ProfileStatsWidget.PREF_TITLE_TEXT_COLOR -> {
                        prefs.edit()
                            .putInt(
                                ProfileStatsWidget.PREF_TITLE_TEXT_COLOR,
                                extras.getInt(SimpleColorDialog.COLOR)
                            )
                            .apply()
                        (binding.titleColorButton as MaterialButton).iconTint =
                            ColorStateList.valueOf(extras.getInt(SimpleColorDialog.COLOR))
                    }

                    ProfileStatsWidget.PREF_STATS_TEXT_COLOR -> {
                        prefs.edit()
                            .putInt(
                                ProfileStatsWidget.PREF_STATS_TEXT_COLOR,
                                extras.getInt(SimpleColorDialog.COLOR)
                            )
                            .apply()
                        (binding.statsColorButton as MaterialButton).iconTint =
                            ColorStateList.valueOf(extras.getInt(SimpleColorDialog.COLOR))
                    }
                }
            }
        }
        return true
    }
}