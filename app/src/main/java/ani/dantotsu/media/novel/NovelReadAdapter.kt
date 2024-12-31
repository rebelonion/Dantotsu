package ani.dantotsu.media.novel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemNovelHeaderBinding
import ani.dantotsu.media.Media
import ani.dantotsu.parsers.NovelReadSources

class NovelReadAdapter(
    private val media: Media,
    private val fragment: NovelReadFragment,
    private val novelReadSources: NovelReadSources
) : RecyclerView.Adapter<NovelReadAdapter.ViewHolder>() {

    var progress: View? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NovelReadAdapter.ViewHolder {
        val binding =
            ItemNovelHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        progress = binding.progress.root
        return ViewHolder(binding)
    }

    private val imm = fragment.requireContext()
        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        progress = binding.progress.root

        fun search(): Boolean {
            val query = binding.searchBarText.text.toString()
            val source =
                media.selected!!.sourceIndex.let { if (it >= novelReadSources.names.size) 0 else it }
            fragment.source = source

            binding.searchBarText.clearFocus()
            imm.hideSoftInputFromWindow(binding.searchBarText.windowToken, 0)
            fragment.search(query, source, true)
            return true
        }

        val source =
            media.selected!!.sourceIndex.let { if (it >= novelReadSources.names.size) 0 else it }
        if (novelReadSources.names.isNotEmpty() && source in 0 until novelReadSources.names.size) {
            binding.mediaSource.setText(novelReadSources.names[source], false)
        }
        binding.mediaSource.setAdapter(
            ArrayAdapter(
                fragment.requireContext(),
                R.layout.item_dropdown,
                novelReadSources.names
            )
        )
        binding.mediaSource.setOnItemClickListener { _, _, i, _ ->
            fragment.onSourceChange(i)
            search()
        }

        binding.searchBarText.setText(fragment.searchQuery)
        binding.searchBarText.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                IME_ACTION_SEARCH -> search()
                else -> false
            }
        }
        binding.searchBar.setEndIconOnClickListener { search() }
    }

    override fun getItemCount(): Int = 1

    inner class ViewHolder(val binding: ItemNovelHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)
}