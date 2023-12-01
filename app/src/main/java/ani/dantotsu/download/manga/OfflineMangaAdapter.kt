package ani.dantotsu.download.manga

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import ani.dantotsu.R


class OfflineMangaAdapter(
    private val context: Context,
    private val items: List<OfflineMangaModel>
) : BaseAdapter() {
    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

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
            view = inflater.inflate(R.layout.item_media_compact, parent, false)
        }

        val item = getItem(position) as OfflineMangaModel
        val imageView = view!!.findViewById<ImageView>(R.id.itemCompactImage)
        val titleTextView = view.findViewById<TextView>(R.id.itemCompactTitle)
        val itemScore = view.findViewById<TextView>(R.id.itemCompactScore)
        val itemScoreBG = view.findViewById<View>(R.id.itemCompactScoreBG)
        val ongoing = view.findViewById<CardView>(R.id.itemCompactOngoing)
        // Bind item data to the views
        // For example:
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
}