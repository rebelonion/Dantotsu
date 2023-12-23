package ani.dantotsu.download.manga

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


class OfflineMangaAdapter(
    private val context: Context,
    private var items: List<OfflineMangaModel>,
    private val searchListener: OfflineMangaSearchListener
) : BaseAdapter() {
    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var originalItems: List<OfflineMangaModel> = items

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
        var view = convertView
        if (view == null) {
            view = inflater.inflate(R.layout.item_media_large, parent, false)
        }

        val item = getItem(position) as OfflineMangaModel
        val imageView = view!!.findViewById<ImageView>(R.id.itemCompactImage)
        val bannerView = view.findViewById<ImageView>(R.id.itemCompactBanner)
        val titleTextView = view.findViewById<TextView>(R.id.itemCompactTitle)
        val itemScore = view.findViewById<TextView>(R.id.itemCompactScore)
        val itemScoreBG = view.findViewById<View>(R.id.itemCompactScoreBG)
        val ongoing = view.findViewById<CardView>(R.id.itemCompactOngoing)
        val chap = view.findViewById<TextView>(R.id.itemCompactTotal)
        val chapters = view.findViewById<TextView>(R.id.itemTotal)
        val typeView = view.findViewById<LinearLayout>(R.id.itemCompactType)
        val type = view.findViewById<TextView>(R.id.itemCompactRelation)
        // Bind item data to the views
        // For example:
        bannerView.setImageURI(item.banner)
        imageView.setImageURI(item.image)
        chapters.text = item.chapters
        chap.text = item.chap
        type.text = item.type
        typeView.visibility = View.VISIBLE
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

    fun setItems(items: List<OfflineMangaModel>) {
        this.items = items
        this.originalItems = items
        notifyDataSetChanged()
    }
}