package ani.dantotsu.widgets.upcoming

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ani.dantotsu.R
import ani.dantotsu.databinding.UpcomingWidgetConfigureBinding
import ani.dantotsu.getThemeColor
import ani.dantotsu.themes.ThemeManager
import com.google.android.material.button.MaterialButton
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.color.SimpleColorDialog

/**
 * The configuration screen for the [UpcomingWidget] AppWidget.
 */
class UpcomingWidgetConfigure : AppCompatActivity(),
    SimpleDialog.OnDialogResultListener {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isMonetEnabled = false
    private var onClickListener = View.OnClickListener {
        val context = this@UpcomingWidgetConfigure
        val appWidgetManager = AppWidgetManager.getInstance(context)

        updateAppWidget(
            context,
            appWidgetManager,
            appWidgetId,
        )

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
    private lateinit var binding: UpcomingWidgetConfigureBinding

    public override fun onCreate(icicle: Bundle?) {
        ThemeManager(this).applyTheme()
        super.onCreate(icicle)
        setResult(RESULT_CANCELED)

        binding = UpcomingWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val prefs = getSharedPreferences(UpcomingWidget.PREFS_NAME, Context.MODE_PRIVATE)
        val topBackground =
            prefs.getInt(UpcomingWidget.PREF_BACKGROUND_COLOR, Color.parseColor("#80000000"))
        (binding.topBackgroundButton as MaterialButton).iconTint =
            ColorStateList.valueOf(topBackground)
        binding.topBackgroundButton.setOnClickListener {
            val tag = UpcomingWidget.PREF_BACKGROUND_COLOR
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(topBackground)
                .colors(
                    this@UpcomingWidgetConfigure,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .setupColorWheelAlpha(true)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@UpcomingWidgetConfigure, tag)
        }
        val bottomBackground =
            prefs.getInt(UpcomingWidget.PREF_BACKGROUND_FADE, Color.parseColor("#00000000"))
        (binding.bottomBackgroundButton as MaterialButton).iconTint =
            ColorStateList.valueOf(bottomBackground)
        binding.bottomBackgroundButton.setOnClickListener {
            val tag = UpcomingWidget.PREF_BACKGROUND_FADE
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(bottomBackground)
                .colors(
                    this@UpcomingWidgetConfigure,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .setupColorWheelAlpha(true)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@UpcomingWidgetConfigure, tag)
        }
        val titleTextColor = prefs.getInt(UpcomingWidget.PREF_TITLE_TEXT_COLOR, Color.WHITE)
        (binding.titleColorButton as MaterialButton).iconTint =
            ColorStateList.valueOf(titleTextColor)
        binding.titleColorButton.setOnClickListener {
            val tag = UpcomingWidget.PREF_TITLE_TEXT_COLOR
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(titleTextColor)
                .colors(
                    this@UpcomingWidgetConfigure,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@UpcomingWidgetConfigure, tag)
        }
        val countdownTextColor = prefs.getInt(UpcomingWidget.PREF_COUNTDOWN_TEXT_COLOR, Color.WHITE)
        (binding.countdownColorButton as MaterialButton).iconTint =
            ColorStateList.valueOf(countdownTextColor)
        binding.countdownColorButton.setOnClickListener {
            val tag = UpcomingWidget.PREF_COUNTDOWN_TEXT_COLOR
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(countdownTextColor)
                .colors(
                    this@UpcomingWidgetConfigure,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@UpcomingWidgetConfigure, tag)
        }
        binding.useAppTheme.setOnCheckedChangeListener { _, isChecked ->
            isMonetEnabled = isChecked
            if (isChecked) {
                binding.topBackgroundButton.visibility = View.GONE
                binding.bottomBackgroundButton.visibility = View.GONE
                binding.titleColorButton.visibility = View.GONE
                binding.countdownColorButton.visibility = View.GONE
                themeColors()

            } else {
                binding.topBackgroundButton.visibility = View.VISIBLE
                binding.bottomBackgroundButton.visibility = View.VISIBLE
                binding.titleColorButton.visibility = View.VISIBLE
                binding.countdownColorButton.visibility = View.VISIBLE
            }
        }
        binding.addButton.setOnClickListener(onClickListener)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
    }

    private fun themeColors() {
        val backgroundColor = getThemeColor(com.google.android.material.R.attr.colorSurface)
        val textColor = getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val subTextColor = getThemeColor(com.google.android.material.R.attr.colorOutline)

        getSharedPreferences(UpcomingWidget.PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putInt(UpcomingWidget.PREF_BACKGROUND_COLOR, backgroundColor)
            putInt(UpcomingWidget.PREF_BACKGROUND_FADE, backgroundColor)
            putInt(UpcomingWidget.PREF_TITLE_TEXT_COLOR, textColor)
            putInt(UpcomingWidget.PREF_COUNTDOWN_TEXT_COLOR, subTextColor)
            apply()
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE) {
            if (!isMonetEnabled) {
                when (dialogTag) {
                    UpcomingWidget.PREF_BACKGROUND_COLOR -> {
                        getSharedPreferences(
                            UpcomingWidget.PREFS_NAME,
                            Context.MODE_PRIVATE
                        ).edit()
                            .putInt(
                                UpcomingWidget.PREF_BACKGROUND_COLOR,
                                extras.getInt(SimpleColorDialog.COLOR)
                            )
                            .apply()
                        (binding.topBackgroundButton as MaterialButton).iconTint =
                            ColorStateList.valueOf(extras.getInt(SimpleColorDialog.COLOR))
                    }

                    UpcomingWidget.PREF_BACKGROUND_FADE -> {
                        getSharedPreferences(
                            UpcomingWidget.PREFS_NAME,
                            Context.MODE_PRIVATE
                        ).edit()
                            .putInt(
                                UpcomingWidget.PREF_BACKGROUND_FADE,
                                extras.getInt(SimpleColorDialog.COLOR)
                            )
                            .apply()
                        (binding.bottomBackgroundButton as MaterialButton).iconTint =
                            ColorStateList.valueOf(extras.getInt(SimpleColorDialog.COLOR))
                    }

                    UpcomingWidget.PREF_TITLE_TEXT_COLOR -> {
                        getSharedPreferences(
                            UpcomingWidget.PREFS_NAME,
                            Context.MODE_PRIVATE
                        ).edit()
                            .putInt(
                                UpcomingWidget.PREF_TITLE_TEXT_COLOR,
                                extras.getInt(SimpleColorDialog.COLOR)
                            )
                            .apply()
                        (binding.titleColorButton as MaterialButton).iconTint =
                            ColorStateList.valueOf(extras.getInt(SimpleColorDialog.COLOR))
                    }

                    UpcomingWidget.PREF_COUNTDOWN_TEXT_COLOR -> {
                        getSharedPreferences(
                            UpcomingWidget.PREFS_NAME,
                            Context.MODE_PRIVATE
                        ).edit()
                            .putInt(
                                UpcomingWidget.PREF_COUNTDOWN_TEXT_COLOR,
                                extras.getInt(SimpleColorDialog.COLOR)
                            )
                            .apply()
                        (binding.countdownColorButton as MaterialButton).iconTint =
                            ColorStateList.valueOf(extras.getInt(SimpleColorDialog.COLOR))
                    }

                }
            }
        }
        return true
    }
}