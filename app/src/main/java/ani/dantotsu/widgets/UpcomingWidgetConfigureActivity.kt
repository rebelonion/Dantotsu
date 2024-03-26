package ani.dantotsu.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
                        ContextCompat.getColor(this, R.color.theme)
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
        binding.bottomBackgroundButton.setOnClickListener {
            val tag = UpcomingWidget.PREF_BACKGROUND_FADE
            SimpleColorDialog().title(R.string.custom_theme)
                .colorPreset(prefs.getInt(UpcomingWidget.PREF_BACKGROUND_FADE, Color.GRAY))
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

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE) {
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
        return true
    }

}