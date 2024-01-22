package ani.dantotsu.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import ani.dantotsu.R

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [CurrentlyAiringWidgetConfigureActivity]
 */
class CurrentlyAiringWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val intent = Intent(context, CurrentlyAiringRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            val rv = RemoteViews(context.packageName, R.layout.currently_airing_widget).apply {
                setRemoteAdapter(R.id.widgetListView, intent)
                setEmptyView(R.id.widgetListView, R.id.empty_view)
            }

            appWidgetManager.updateAppWidget(appWidgetId, rv)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            deleteTitlePref(context, appWidgetId)
        }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            color: Int
        ) {
            // Create an intent to launch the configuration activity when the widget is clicked
            val intent = Intent(context, CurrentlyAiringWidgetConfigureActivity::class.java)
            val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            // Get the gradient drawable resource and update its start color with the user-selected color
            val gradientDrawable = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.gradient_background,
                null
            ) as GradientDrawable
            gradientDrawable.colors = intArrayOf(color, Color.GRAY) // End color is gray.

            // Create the RemoteViews object and set the background
            val views = RemoteViews(context.packageName, R.layout.currently_airing_widget).apply {
                //setImageViewBitmap(R.id.backgroundView, convertDrawableToBitmap(gradientDrawable))
                //setOnClickPendingIntent(R.id.backgroundView, pendingIntent)
            }

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun convertDrawableToBitmap(drawable: Drawable): Bitmap {
            val bitmap = Bitmap.createBitmap(100, 300, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val widgetText = loadTitlePref(context, appWidgetId)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.currently_airing_widget)
    views.setTextViewText(R.id.appwidget_text, widgetText)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}