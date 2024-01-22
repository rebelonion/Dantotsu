package ani.dantotsu.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import ani.dantotsu.R
import ani.dantotsu.logger
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class CurrentlyAiringRemoteViewsFactory(private val context: Context, intent: Intent) :
    RemoteViewsService.RemoteViewsFactory {
    private var widgetItems = mutableListOf<WidgetItem>()

    override fun onCreate() {
        // 4 items for testing
        widgetItems.clear()
        logger("CurrentlyAiringRemoteViewsFactory onCreate")
        widgetItems = List(4) {
            WidgetItem(
                "Show $it",
                "$it days $it hours $it minutes",
                "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx14741-alxqoP4yx6WF.jpg"
            )
        }.toMutableList()
    }

    override fun onDataSetChanged() {
        // 4 items for testing
        logger("CurrentlyAiringRemoteViewsFactory onDataSetChanged")
        widgetItems.clear()
        widgetItems.add(
            WidgetItem(
                "Show 1",
                "1 day 2 hours 3 minutes",
                "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx14741-alxqoP4yx6WF.jpg"
            )
        )
        widgetItems.add(
            WidgetItem(
                "Show 2",
                "2 days 3 hours 4 minutes",
                "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx14741-alxqoP4yx6WF.jpg"
            )
        )
        widgetItems.add(
            WidgetItem(
                "Show 3",
                "3 days 4 hours 5 minutes",
                "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx14741-alxqoP4yx6WF.jpg"
            )
        )
        widgetItems.add(
            WidgetItem(
                "Show 4",
                "4 days 5 hours 6 minutes",
                "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx14741-alxqoP4yx6WF.jpg"
            )
        )
        widgetItems.add(
            WidgetItem(
                "Show 5",
                "5 days 6 hours 7 minutes",
                "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx14741-alxqoP4yx6WF.jpg"
            )
        )
    }

    override fun onDestroy() {
        widgetItems.clear()
    }

    override fun getCount(): Int {
        return widgetItems.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        logger("CurrentlyAiringRemoteViewsFactory getViewAt")
        val item = widgetItems[position]
        val rv = RemoteViews(context.packageName, R.layout.item_currently_airing_widget).apply {
            setTextViewText(R.id.text_show_title, item.title)
            setTextViewText(R.id.text_show_countdown, item.countdown)
            val bitmap = downloadImageAsBitmap(item.image)
            //setImageViewUri(R.id.image_show_icon, Uri.parse(item.image))
            setImageViewBitmap(R.id.image_show_icon, bitmap)
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
            // Handle the error according to your needs
        } finally {
            // Clean up resources
            inputStream?.close()
            urlConnection?.disconnect()
        }

        return bitmap
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.item_currently_airing_widget)
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

data class WidgetItem(val title: String, val countdown: String, val image: String)