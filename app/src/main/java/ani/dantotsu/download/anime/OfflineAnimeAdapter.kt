package ani.dantotsu.download.anime


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import ani.dantotsu.R
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName


class OfflineAnimeAdapter(
    private val context: Context,
    private var items: List<OfflineAnimeModel>,
    private val searchListener: OfflineAnimeSearchListener
) : BaseAdapter() {
    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var originalItems: List<OfflineAnimeModel> = items
    private var style: Int = PrefManager.getVal(PrefName.OfflineView)

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val view: View = convertView ?: when (style) {
            0 -> inflater.inflate(R.layout.item_media_large, parent, false) // large view
            1 -> inflater.inflate(R.layout.item_media_compact, parent, false) // compact view
            else -> inflater.inflate(R.layout.item_media_compact, parent, false) // compact view
        }

        val item = getItem(position) as OfflineAnimeModel
        val imageView = view.findViewById<ImageView>(R.id.itemCompactImage)
        val titleTextView = view.findViewById<TextView>(R.id.itemCompactTitle)
        val itemScore = view.findViewById<TextView>(R.id.itemCompactScore)
        val ongoing = view.findViewById<CardView>(R.id.itemCompactOngoing)
        val totalEpisodes = view.findViewById<TextView>(R.id.itemCompactTotal)
        val typeImage = view.findViewById<ImageView>(R.id.itemCompactTypeImage)
        val type = view.findViewById<TextView>(R.id.itemCompactRelation)
        val typeView = view.findViewById<LinearLayout>(R.id.itemCompactType)

        if (style == 0) {
            val bannerView = view.findViewById<ImageView>(R.id.itemCompactBanner) // for large view
            val episodes = view.findViewById<TextView>(R.id.itemTotal)
            val text = " ${context.getString(R.string.episodes)}"
            episodes.text = text
            bannerView.setImageURI(item.banner ?: item.image)
            totalEpisodes.text = item.totalEpisodeList
        } else if (style == 1) {
            val watchedEpisodes =
                view.findViewById<TextView>(R.id.itemCompactUserProgress) // for compact view
            watchedEpisodes.text = item.watchedEpisode
            totalEpisodes.text = context.getString(R.string.total_divider, item.totalEpisode)
        }

        // Bind item data to the views
        typeImage.setImageResource(R.drawable.ic_round_movie_filter_24)
        type.text = item.type
        typeView.visibility = View.VISIBLE
        imageView.setImageURI(item.image)
        titleTextView.text = item.title
        itemScore.text = item.score

        if (item.isOngoing) {
            ongoing.visibility = View.VISIBLE
        } else {
            ongoing.visibility = View.GONE
        }
        return view
    }

    fun onSearchQuery(query: String) {
        // Implement the filtering logic here, for example:
        items = if (query.isEmpty()) {
            // Return the original list if the query is empty
            originalItems
        } else {
            // Filter the list based on the query
            originalItems.filter { it.title.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged() // Notify the adapter that the data set has changed
    }

    fun setItems(items: List<OfflineAnimeModel>) {
        this.items = items
        this.originalItems = items
        notifyDataSetChanged()
    }

    fun notifyNewGrid() {
        style = PrefManager.getVal(PrefName.OfflineView)
        notifyDataSetChanged()
    }
}