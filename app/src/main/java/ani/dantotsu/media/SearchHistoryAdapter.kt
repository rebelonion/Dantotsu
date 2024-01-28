package ani.dantotsu.media

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemSearchHistoryBinding
import ani.dantotsu.others.SharedPreferenceStringSetLiveData
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SearchHistoryAdapter(private val type: String, private val searchClicked: (String) -> Unit) : ListAdapter<String, SearchHistoryAdapter.SearchHistoryViewHolder>(
    DIFF_CALLBACK_INSTALLED
) {
    private var searchHistoryLiveData: SharedPreferenceStringSetLiveData? = null
    private var searchHistory: MutableSet<String>? = null
    private var sharedPreferences: SharedPreferences? = null

    init {
        sharedPreferences = Injekt.get<SharedPreferences>()
        searchHistoryLiveData = SharedPreferenceStringSetLiveData(
            sharedPreferences!!,
            "searchHistory_$type",
            mutableSetOf()
        )
        searchHistoryLiveData?.observeForever {
            searchHistory = it.toMutableSet()
            submitList(searchHistory?.reversed())
        }
    }

    fun remove(item: String) {
        searchHistory?.remove(item)
        sharedPreferences?.edit()?.putStringSet("searchHistory_$type", searchHistory)?.apply()
    }

    fun add(item: String) {
        if (searchHistory?.contains(item) == true || item.isBlank()) return
        if (sharedPreferences?.getBoolean("incognito", false) == true) return
        searchHistory?.add(item)
        sharedPreferences?.edit()?.putStringSet("searchHistory_$type", searchHistory)?.apply()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SearchHistoryAdapter.SearchHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_history, parent, false)
        return SearchHistoryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: SearchHistoryAdapter.SearchHistoryViewHolder,
        position: Int
    ) {
        holder.binding.searchHistoryTextView.text = getItem(position)
        holder.binding.closeTextView.setOnClickListener {
            if (position >= itemCount || position < 0) return@setOnClickListener
            remove(getItem(position))
        }
        holder.binding.searchHistoryTextView.setOnClickListener {
            if (position >= itemCount || position < 0) return@setOnClickListener
            searchClicked(getItem(position))
        }
    }

    inner class SearchHistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemSearchHistoryBinding.bind(view)
    }

    companion object {
        val DIFF_CALLBACK_INSTALLED = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(
                oldItem: String,
                newItem: String
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: String,
                newItem: String
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
