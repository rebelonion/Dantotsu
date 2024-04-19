package ani.dantotsu.widgets.statistics

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import ani.dantotsu.MainActivity
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.BitmapUtil.downloadImageAsBitmap
import ani.dantotsu.widgets.WidgetSizeProvider
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.core.util.lang.launchIO

/**
 * Implementation of App Widget functionality.
 */
class ProfileStatsWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }

    companion object {
        @OptIn(DelicateCoroutinesApi::class)
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {

            val prefs =
                context.getSharedPreferences(getPrefsName(appWidgetId), Context.MODE_PRIVATE)
            val backgroundColor =
                prefs.getInt(PREF_BACKGROUND_COLOR, Color.parseColor("#80000000"))
            val backgroundFade = prefs.getInt(PREF_BACKGROUND_FADE, Color.parseColor("#00000000"))
            val titleTextColor = prefs.getInt(PREF_TITLE_TEXT_COLOR, Color.WHITE)
            val statsTextColor = prefs.getInt(PREF_STATS_TEXT_COLOR, Color.WHITE)

            val gradientDrawable = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.linear_gradient_black,
                null
            ) as GradientDrawable
            gradientDrawable.colors = intArrayOf(backgroundColor, backgroundFade)
            val widgetSizeProvider = WidgetSizeProvider(context)
            var (width, height) = widgetSizeProvider.getWidgetsSize(appWidgetId)
            if (width > 0 && height > 0) {
                gradientDrawable.cornerRadius = 64f
            } else {
                width = 300
                height = 300
            }

            launchIO {
                val userPref = PrefManager.getVal(PrefName.AnilistUserId, "")
                if (userPref.isNotEmpty()) {
                    val respond = Anilist.query.getUserProfile(userPref.toInt())
                    respond?.data?.user?.let { user ->
                        withContext(Dispatchers.Main) {
                            val views =
                                RemoteViews(context.packageName, R.layout.statistics_widget).apply {
                                    setImageViewBitmap(
                                        R.id.backgroundView,
                                        gradientDrawable.toBitmap(
                                            width,
                                            height
                                        )
                                    )
                                    setOnClickPendingIntent(
                                        R.id.userAvatar,
                                        PendingIntent.getActivity(
                                            context,
                                            1,
                                            Intent(
                                                context,
                                                ProfileStatsConfigure::class.java
                                            ).apply {
                                                putExtra(
                                                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                                                    appWidgetId
                                                )
                                                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                                            },
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        )
                                    )
                                    setTextColor(R.id.userLabel, titleTextColor)
                                    setTextColor(R.id.topLeftItem, titleTextColor)
                                    setTextColor(R.id.topLeftLabel, statsTextColor)
                                    setTextColor(R.id.topRightItem, titleTextColor)
                                    setTextColor(R.id.topRightLabel, statsTextColor)
                                    setTextColor(R.id.bottomLeftItem, titleTextColor)
                                    setTextColor(R.id.bottomLeftLabel, statsTextColor)
                                    setTextColor(R.id.bottomRightItem, titleTextColor)
                                    setTextColor(R.id.bottomRightLabel, statsTextColor)

                                    setImageViewBitmap(
                                        R.id.userAvatar,
                                        user.avatar?.medium?.let { it1 -> downloadImageAsBitmap(it1) }
                                    )
                                    setTextViewText(
                                        R.id.userLabel,
                                        context.getString(R.string.user_stats, user.name)
                                    )

                                    setTextViewText(
                                        R.id.topLeftItem,
                                        user.statistics.anime.count.toString()
                                    )
                                    setTextViewText(
                                        R.id.topLeftLabel,
                                        context.getString(R.string.anime_watched)
                                    )

                                    setTextViewText(
                                        R.id.topRightItem,
                                        user.statistics.anime.episodesWatched.toString()
                                    )
                                    setTextViewText(
                                        R.id.topRightLabel,
                                        context.getString(R.string.episodes_watched_n)
                                    )

                                    setTextViewText(
                                        R.id.bottomLeftItem,
                                        user.statistics.manga.count.toString()
                                    )
                                    setTextViewText(
                                        R.id.bottomLeftLabel,
                                        context.getString(R.string.manga_read)
                                    )

                                    setTextViewText(
                                        R.id.bottomRightItem,
                                        user.statistics.manga.chaptersRead.toString()
                                    )
                                    setTextViewText(
                                        R.id.bottomRightLabel,
                                        context.getString(R.string.chapters_read_n)
                                    )

                                    val intent = Intent(context, ProfileActivity::class.java)
                                        .putExtra("userId", userPref.toInt())
                                    val pendingIntent = PendingIntent.getActivity(
                                        context, 0, intent, PendingIntent.FLAG_IMMUTABLE
                                    )
                                    setOnClickPendingIntent(R.id.widgetContainer, pendingIntent)
                                }
                            // Instruct the widget manager to update the widget
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    } ?: showLoginCascade(context, appWidgetManager, appWidgetId)
                } else showLoginCascade(context, appWidgetManager, appWidgetId)
            }
        }

        private suspend fun showLoginCascade(
            context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int
        ) {

            withContext(Dispatchers.Main) {
                val views = RemoteViews(context.packageName, R.layout.statistics_widget)

                views.setTextViewText(R.id.topLeftItem, "")
                views.setTextViewText(
                    R.id.topLeftLabel,
                    context.getString(R.string.please)
                )

                views.setTextViewText(R.id.topRightItem, "")
                views.setTextViewText(
                    R.id.topRightLabel,
                    context.getString(R.string.log_in)
                )

                views.setTextViewText(
                    R.id.bottomLeftItem,
                    context.getString(R.string.or_join)
                )
                views.setTextViewText(R.id.bottomLeftLabel, "")

                views.setTextViewText(
                    R.id.bottomRightItem,
                    context.getString(R.string.anilist)
                )
                views.setTextViewText(R.id.bottomRightLabel, "")

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun getPrefsName(appWidgetId: Int): String {
            return "ani.dantotsu.widgets.Statistics.${appWidgetId}"
        }

        const val PREF_BACKGROUND_COLOR = "background_color"
        const val PREF_BACKGROUND_FADE = "background_fade"
        const val PREF_TITLE_TEXT_COLOR = "title_text_color"
        const val PREF_STATS_TEXT_COLOR = "stats_text_color"
    }
}