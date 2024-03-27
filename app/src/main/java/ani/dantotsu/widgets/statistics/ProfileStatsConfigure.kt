package ani.dantotsu.widgets.statistics

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import ani.dantotsu.databinding.StatisticsWidgetConfigureBinding

import ani.dantotsu.themes.ThemeManager

/**
 * The configuration screen for the [ProfileStatsWidget] AppWidget.
 */
class ProfileStatsConfigure : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

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

        val typedValueSurface = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValueSurface, true)
        val backgroundColor = typedValueSurface.data

        val typedValuePrimary = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValuePrimary, true)
        val textColor = typedValuePrimary.data

        val typedValueOutline = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorOutline, typedValueOutline, true)
        val subTextColor = typedValueOutline.data

        getSharedPreferences(ProfileStatsWidget.PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putInt(ProfileStatsWidget.PREF_BACKGROUND_COLOR, backgroundColor)
            putInt(ProfileStatsWidget.PREF_BACKGROUND_FADE, backgroundColor)
            putInt(ProfileStatsWidget.PREF_TITLE_TEXT_COLOR, textColor)
            apply()
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

        // Possibly consider sorting the items or configuring colors

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
    }

}

private const val PROFILE_STATS_PREFS = "ani.dantotsu.widget.ProfileStatsWidget"
private const val PROFILE_STATS_PREFS_PREFIX = "appwidget_"