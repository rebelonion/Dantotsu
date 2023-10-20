package ani.dantotsu.media.novel

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.databinding.FragmentAnimeWatchBinding
import ani.dantotsu.loadData
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsViewModel
import ani.dantotsu.navBarHeight
import ani.dantotsu.saveData
import ani.dantotsu.settings.UserInterfaceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NovelReadFragment : Fragment() {

    private var _binding: FragmentAnimeWatchBinding? = null
    private val binding get() = _binding!!
    private val model: MediaDetailsViewModel by activityViewModels()

    private lateinit var media: Media
    var source = 0
    lateinit var novelName: String

    private lateinit var headerAdapter: NovelReadAdapter
    private lateinit var novelResponseAdapter: NovelResponseAdapter
    private var progress = View.VISIBLE

    private var continueEp: Boolean = false
    var loaded = false

    val uiSettings = loadData("ui_settings", toast = false) ?: UserInterfaceSettings().apply { saveData("ui_settings", this) }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.animeSourceRecycler.updatePadding(bottom = binding.animeSourceRecycler.paddingBottom + navBarHeight)

        binding.animeSourceRecycler.layoutManager = LinearLayoutManager(requireContext())
        model.scrolledToTop.observe(viewLifecycleOwner) {
            if (it) binding.animeSourceRecycler.scrollToPosition(0)
        }

        continueEp = model.continueMedia ?: false
        model.getMedia().observe(viewLifecycleOwner) {
            if (it != null) {
                media = it
                novelName = media.userPreferredName
                progress = View.GONE
                binding.mediaInfoProgressBar.visibility = progress
                if (!loaded) {
                    val sel = media.selected
                    searchQuery = sel?.server ?: media.name ?: media.nameRomaji
                    headerAdapter = NovelReadAdapter(media, this, model.novelSources)
                    novelResponseAdapter = NovelResponseAdapter(this)
                    binding.animeSourceRecycler.adapter = ConcatAdapter(headerAdapter, novelResponseAdapter)
                    loaded = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        search(searchQuery, sel?.sourceIndex ?: 0, auto = sel?.server == null)
                    }, 100)
                }
            }
        }
        model.novelResponses.observe(viewLifecycleOwner) {
            if (it != null) {
                searching = false
                novelResponseAdapter.submitList(it)
                headerAdapter.progress?.visibility = View.GONE
            }
        }
    }

    lateinit var searchQuery: String
    private var searching = false
    fun search(query: String, source: Int, save: Boolean = false, auto: Boolean = false) {
        if (!searching) {
            novelResponseAdapter.clear()
            searchQuery = query
            headerAdapter.progress?.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.IO) {
                if (auto || query=="") model.autoSearchNovels(media)
                else model.searchNovels(query, source)
            }
            searching = true
            if (save) {
                val selected = model.loadSelected(media)
                selected.server = query
                model.saveSelected(media.id, selected, requireActivity())
            }
        }
    }

    fun onSourceChange(i: Int) {
        val selected = model.loadSelected(media)
        selected.sourceIndex = i
        source = i
        selected.server = null
        model.saveSelected(media.id, selected, requireActivity())
        media.selected = selected
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAnimeWatchBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onDestroy() {
        model.mangaReadSources?.flushText()
        super.onDestroy()
    }

    private var state: Parcelable? = null
    override fun onResume() {
        super.onResume()
        binding.mediaInfoProgressBar.visibility = progress
        binding.animeSourceRecycler.layoutManager?.onRestoreInstanceState(state)
    }

    override fun onPause() {
        super.onPause()
        state = binding.animeSourceRecycler.layoutManager?.onSaveInstanceState()
    }
}