package ani.dantotsu.download.manga

import android.annotation.SuppressLint
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
    private var style = context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).getInt("offline_view", 0)

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val view: View = convertView ?: when(style) {
            0 -> inflater.inflate(R.layout.item_media_large, parent, false) // large view
            1 -> inflater.inflate(R.layout.item_media_compact, parent, false) // compact view
            else -> inflater.inflate(R.layout.item_media_compact, parent, false) // compact view
        }

        val item = getItem(position) as OfflineMangaModel
        val imageView = view!!.findViewById<ImageView>(R.id.itemCompactImage)
        val titleTextView = view.findViewById<TextView>(R.id.itemCompactTitle)
        val itemScore = view.findViewById<TextView>(R.id.itemCompactScore)
        val itemScoreBG = view.findViewById<View>(R.id.itemCompactScoreBG)
        val ongoing = view.findViewById<CardView>(R.id.itemCompactOngoing)
        val totalchapter = view.findViewById<TextView>(R.id.itemCompactTotal)
        val typeimage = view.findViewById<ImageView>(R.id.itemCompactTypeImage)
        val type = view.findViewById<TextView>(R.id.itemCompactRelation)
        val typeView = view.findViewById<LinearLayout>(R.id.itemCompactType)

        if (style == 0){
            val bannerView = view.findViewById<ImageView>(R.id.itemCompactBanner) // for large view
            val chapters = view.findViewById<TextView>(R.id.itemTotal)
            chapters.text = " Chapters"
            bannerView.setImageURI(item.banner)
            totalchapter.text = item.totalchapter
        }

        else if (style == 1){
            val readchapter = view.findViewById<TextView>(R.id.itemCompactUserProgress) // for compact view
            readchapter.text = item.readchapter
            totalchapter.text = " | " + item.totalchapter
        }

        // Bind item data to the views
        typeimage.setImageResource(if (item.type == "Novel" ) R.drawable.ic_round_book_24 else R.drawable.ic_round_import_contacts_24)
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

    fun setItems(items: List<OfflineMangaModel>) {
        this.items = items
        this.originalItems = items
        notifyDataSetChanged()
    }

    fun notifyNewGrid(){
        style = context.getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).getInt("offline_view", 0)
        notifyDataSetChanged()
    }
}