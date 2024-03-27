package ani.dantotsu.widgets.upcoming

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ani.dantotsu.R
import ani.dantotsu.databinding.UpcomingWidgetConfigureBinding
import ani.dantotsu.themes.ThemeManager
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.color.SimpleColorDialog

/**
 * The configuration screen for the [UpcomingWidget] AppWidget.
 */
class UpcomingWidgetConfigureActivity : AppCompatActivity(),
    SimpleDialog.OnDialogResultListener {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isMonetEnabled = false
    private var onClickListener = View.OnClickListener {
        val context = this@UpcomingWidgetConfigureActivity
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

        binding.topBackgroundButton.setOnClickListener {
            val tag = UpcomingWidget.PREF_BACKGROUND_COLOR
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(
                    prefs.getInt(
                        UpcomingWidget.PREF_BACKGROUND_COLOR,
                        Color.parseColor("#80000000")
                    )
                )
                .colors(
                    this@UpcomingWidgetConfigureActivity,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .setupColorWheelAlpha(true)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@UpcomingWidgetConfigureActivity, tag)
        }
        binding.bottomBackgroundButton.setOnClickListener {
            val tag = UpcomingWidget.PREF_BACKGROUND_FADE
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(prefs.getInt(UpcomingWidget.PREF_BACKGROUND_FADE, Color.parseColor("#00000000")))
                .colors(
                    this@UpcomingWidgetConfigureActivity,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .setupColorWheelAlpha(true)
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@UpcomingWidgetConfigureActivity, tag)
        }
        binding.titleColorButton.setOnClickListener {
            val tag = UpcomingWidget.PREF_TITLE_TEXT_COLOR
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(prefs.getInt(UpcomingWidget.PREF_TITLE_TEXT_COLOR, Color.WHITE))
                .colors(
                    this@UpcomingWidgetConfigureActivity,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@UpcomingWidgetConfigureActivity, tag)
        }
        binding.countdownColorButton.setOnClickListener {
            val tag = UpcomingWidget.PREF_COUNTDOWN_TEXT_COLOR
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(
                    prefs.getInt(
                        UpcomingWidget.PREF_COUNTDOWN_TEXT_COLOR,
                        Color.WHITE
                    )
                )
                .colors(
                    this@UpcomingWidgetConfigureActivity,
                    SimpleColorDialog.MATERIAL_COLOR_PALLET
                )
                .allowCustom(true)
                .showOutline(0x46000000)
                .gridNumColumn(5)
                .choiceMode(SimpleColorDialog.SINGLE_CHOICE)
                .neg()
                .show(this@UpcomingWidgetConfigureActivity, tag)
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
        val typedValueSurface = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValueSurface, true)
        val backgroundColor = typedValueSurface.data

        val typedValuePrimary = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValuePrimary, true)
        val textColor = typedValuePrimary.data

        val typedValueOutline = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorOutline, typedValueOutline, true)
        val subTextColor = typedValueOutline.data

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
                    }

                }
            }
        }
        return true
    }
}