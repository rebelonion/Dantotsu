package ani.dantotsu.widgets.statistics

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ani.dantotsu.MainActivity
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
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

            launchIO {
                val userPref = PrefManager.getVal(PrefName.AnilistUserId, "")
                val userId = if (userPref.isNotEmpty()) userPref.toInt() else Anilist.userid
                    ?: if (Anilist.query.getUserData()) Anilist.userid else null
                userId?.let {
                    val respond = Anilist.query.getUserProfile(it)
                    respond?.data?.user?.let { user ->
                        withContext(Dispatchers.Main) {
                            val views = RemoteViews(context.packageName, R.layout.statistics_widget)

                            views.setTextViewText(
                                R.id.topLeftItem,
                                user.statistics.anime.count.toString()
                            )
                            views.setTextViewText(
                                R.id.topLeftLabel,
                                context.getString(R.string.anime_watched)
                            )

                            views.setTextViewText(
                                R.id.topRightItem,
                                user.statistics.anime.episodesWatched.toString()
                            )
                            views.setTextViewText(
                                R.id.topRightLabel,
                                context.getString(R.string.episodes_watched)
                            )

                            views.setTextViewText(
                                R.id.bottomLeftItem,
                                user.statistics.manga.count.toString()
                            )
                            views.setTextViewText(
                                R.id.bottomLeftLabel,
                                context.getString(R.string.manga_read)
                            )

                            views.setTextViewText(
                                R.id.bottomRightItem,
                                user.statistics.manga.chaptersRead.toString()
                            )
                            views.setTextViewText(
                                R.id.bottomRightLabel,
                                context.getString(R.string.chapters_read)
                            )

                            val intent = Intent(context, ProfileActivity::class.java)
                                .putExtra("userId", it)
                            val pendingIntent = PendingIntent.getActivity(
                                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent)

                            // Instruct the widget manager to update the widget
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    } ?: showLoginCascade(context, appWidgetManager, appWidgetId)
                } ?: showLoginCascade(context, appWidgetManager, appWidgetId)
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
    }
}