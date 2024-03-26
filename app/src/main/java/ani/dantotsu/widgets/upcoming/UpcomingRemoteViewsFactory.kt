package ani.dantotsu.widgets.upcoming

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.BitmapUtil.Companion.roundCorners
import ani.dantotsu.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class UpcomingRemoteViewsFactory(private val context: Context) :
    RemoteViewsService.RemoteViewsFactory {
    private var widgetItems = mutableListOf<WidgetItem>()
    private var refreshing = false
    private val prefs =
        context.getSharedPreferences(UpcomingWidget.PREFS_NAME, Context.MODE_PRIVATE)

    override fun onCreate() {
        Logger.log("UpcomingRemoteViewsFactory onCreate")
        fillWidgetItems()
    }

    private fun timeUntil(timeUntil: Long): String {
        val days = timeUntil / (1000 * 60 * 60 * 24)
        val hours = (timeUntil % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
        val minutes = ((timeUntil % (1000 * 60 * 60 * 24)) % (1000 * 60 * 60)) / (1000 * 60)
        return "$days days $hours hours $minutes minutes"
    }

    override fun onDataSetChanged() {
        if (refreshing) return
        Logger.log("UpcomingRemoteViewsFactory onDataSetChanged")
        widgetItems.clear()
        fillWidgetItems()

    }

    private fun fillWidgetItems() {
        refreshing = true
        val userId = PrefManager.getVal<String>(PrefName.AnilistUserId)
        runBlocking(Dispatchers.IO) {
            val upcoming = Anilist.query.getUpcomingAnime(userId)
            upcoming.forEach {
                widgetItems.add(
                    WidgetItem(
                        it.userPreferredName,
                        timeUntil(it.timeUntilAiring ?: 0),
                        it.cover ?: "",
                        it.id
                    )
                )
            }
            refreshing = false
        }
    }

    override fun onDestroy() {
        widgetItems.clear()
    }

    override fun getCount(): Int {
        return widgetItems.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        Logger.log("UpcomingRemoteViewsFactory getViewAt")
        val item = widgetItems[position]
        val titleTextColor = prefs.getInt(UpcomingWidget.PREF_TITLE_TEXT_COLOR, Color.WHITE)
        val countdownTextColor =
            prefs.getInt(UpcomingWidget.PREF_COUNTDOWN_TEXT_COLOR, Color.WHITE)
        val rv = RemoteViews(context.packageName, R.layout.item_upcoming_widget).apply {
            setTextViewText(R.id.text_show_title, item.title)
            setTextViewText(R.id.text_show_countdown, item.countdown)
            setTextColor(R.id.text_show_title, titleTextColor)
            setTextColor(R.id.text_show_countdown, countdownTextColor)
            val bitmap = downloadImageAsBitmap(item.image)
            setImageViewBitmap(R.id.image_show_icon, bitmap)
            val fillInIntent = Intent().apply {
                putExtra("mediaId", item.id)
            }
            setOnClickFillInIntent(R.id.widget_item, fillInIntent)
        }

        return rv
    }

    private fun downloadImageAsBitmap(imageUrl: String): Bitmap? {
        var bitmap: Bitmap? = null
        var inputStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null

        try {
            val url = URL(imageUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.connect()

            if (urlConnection.responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = urlConnection.inputStream
                bitmap = BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
            urlConnection?.disconnect()
        }
        return bitmap?.let { roundCorners(it) }
    }




    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.item_upcoming_widget)
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}

data class WidgetItem(val title: String, val countdown: String, val image: String, val id: Int)