package ani.dantotsu.media.manga

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.*
import ani.dantotsu.media.anime.handleProgress
import ani.dantotsu.databinding.ItemAnimeWatchBinding
import ani.dantotsu.databinding.ItemChipBinding
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.media.SourceSearchDialogFragment
import ani.dantotsu.parsers.MangaReadSources
import ani.dantotsu.subcriptions.Notifications.Companion.openSettings
import ani.dantotsu.subcriptions.Subscription.Companion.getChannelId
import com.google.android.material.chip.Chip
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MangaReadAdapter(
    private val media: Media,
    private val fragment: MangaReadFragment,
    private val mangaReadSources: MangaReadSources
) : RecyclerView.Adapter<MangaReadAdapter.ViewHolder>() {

    var subscribe: MediaDetailsActivity.PopImageButton? = null
    private var _binding: ItemAnimeWatchBinding? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val bind = ItemAnimeWatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(bind)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        _binding = binding
        binding.sourceTitle.setText(R.string.chaps)

        //Wrong Title
        binding.animeSourceSearch.setOnClickListener {
            SourceSearchDialogFragment().show(fragment.requireActivity().supportFragmentManager, null)
        }

        //Source Selection
        val source = media.selected!!.sourceIndex.let { if (it >= mangaReadSources.names.size) 0 else it }
        if (mangaReadSources.names.isNotEmpty() && source in 0 until mangaReadSources.names.size) {
            binding.animeSource.setText(mangaReadSources.names[source])

            mangaReadSources[source].apply {
                binding.animeSourceTitle.text = showUserText
                showUserTextListener = { MainScope().launch { binding.animeSourceTitle.text = it } }
            }
        }
        binding.animeSource.setAdapter(ArrayAdapter(fragment.requireContext(), R.layout.item_dropdown, mangaReadSources.names))
        binding.animeSourceTitle.isSelected = true
        binding.animeSource.setOnItemClickListener { _, _, i, _ ->
            fragment.onSourceChange(i).apply {
                binding.animeSourceTitle.text = showUserText
                showUserTextListener = { MainScope().launch { binding.animeSourceTitle.text = it } }
            }
            subscribeButton(false)
            fragment.loadChapters(i)
        }

        //Subscription
        subscribe = MediaDetailsActivity.PopImageButton(
            fragment.lifecycleScope,
            binding.animeSourceSubscribe,
            R.drawable.ic_round_notifications_active_24,
            R.drawable.ic_round_notifications_none_24,
            R.color.bg_opp,
            R.color.violet_400,
            fragment.subscribed
        ) {
            fragment.onNotificationPressed(it, binding.animeSource.text.toString())
        }

        subscribeButton(false)

        binding.animeSourceSubscribe.setOnLongClickListener {
            openSettings(fragment.requireContext(), getChannelId(true, media.id))
        }

        //Icons
        binding.animeSourceGrid.visibility = View.GONE
        var reversed = media.selected!!.recyclerReversed
        var style = media.selected!!.recyclerStyle ?: fragment.uiSettings.mangaDefaultView
        binding.animeSourceTop.rotation = if (reversed) -90f else 90f
        binding.animeSourceTop.setOnClickListener {
            reversed = !reversed
            binding.animeSourceTop.rotation = if (reversed) -90f else 90f
            fragment.onIconPressed(style, reversed)
        }
        var selected = when (style) {
            0    -> binding.animeSourceList
            1    -> binding.animeSourceCompact
            else -> binding.animeSourceList
        }
        selected.alpha = 1f
        fun selected(it: ImageView) {
            selected.alpha = 0.33f
            selected = it
            selected.alpha = 1f
        }
        binding.animeSourceList.setOnClickListener {
            selected(it as ImageView)
            style = 0
            fragment.onIconPressed(style, reversed)
        }
        binding.animeSourceCompact.setOnClickListener {
            selected(it as ImageView)
            style = 1
            fragment.onIconPressed(style, reversed)
        }

        //Chapter Handling
        handleChapters()
    }

    fun subscribeButton(enabled: Boolean) {
        subscribe?.enabled(enabled)
    }

    //Chips
    @SuppressLint("SetTextI18n")
    fun updateChips(limit: Int, names: Array<String>, arr: Array<Int>, selected: Int = 0) {
        val binding = _binding
        if (binding != null) {
            val screenWidth = fragment.screenWidth.px
            var select: Chip? = null
            for (position in arr.indices) {
                val last = if (position + 1 == arr.size) names.size else (limit * (position + 1))
                val chip =
                    ItemChipBinding.inflate(LayoutInflater.from(fragment.context), binding.animeSourceChipGroup, false).root
                chip.isCheckable = true
                fun selected() {
                    chip.isChecked = true
                    binding.animeWatchChipScroll.smoothScrollTo((chip.left - screenWidth / 2) + (chip.width / 2), 0)
                }
                chip.text = "${names[limit * (position)]} - ${names[last - 1]}"

                chip.setOnClickListener {
                    selected()
                    fragment.onChipClicked(position, limit * (position), last - 1)
                }
                binding.animeSourceChipGroup.addView(chip)
                if (selected == position) {
                    selected()
                    select = chip
                }
            }
            if (select != null)
                binding.animeWatchChipScroll.apply { post { scrollTo((select.left - screenWidth / 2) + (select.width / 2), 0) } }
        }
    }

    fun clearChips() {
        _binding?.animeSourceChipGroup?.removeAllViews()
    }

    @SuppressLint("SetTextI18n")
    fun handleChapters() {
        val binding = _binding
        if (binding != null) {
            if (media.manga?.chapters != null) {
                val chapters = media.manga.chapters!!.keys.toTypedArray()
                val anilistEp = (media.userProgress ?: 0).plus(1)
                val appEp = loadData<String>("${media.id}_current_chp")?.toIntOrNull() ?: 1
                var continueEp = (if (anilistEp > appEp) anilistEp else appEp).toString()
                val formattedChapters = chapters.map { MangaNameAdapter.findChapterNumber(it)?.toInt()?.toString() }
                if (formattedChapters.contains(continueEp)) {
                    continueEp = chapters[formattedChapters.indexOf(continueEp)]
                    binding.animeSourceContinue.visibility = View.VISIBLE
                    handleProgress(
                        binding.itemEpisodeProgressCont,
                        binding.itemEpisodeProgress,
                        binding.itemEpisodeProgressEmpty,
                        media.id,
                        continueEp
                    )
                    if ((binding.itemEpisodeProgress.layoutParams as LinearLayout.LayoutParams).weight > 0.8f) {
                        val e = chapters.indexOf(continueEp)
                        if (e != -1 && e + 1 < chapters.size) {
                            continueEp = chapters[e + 1]
                        }
                    }
                    val ep = media.manga.chapters!![continueEp]!!
                    binding.itemEpisodeImage.loadImage(media.banner ?: media.cover)
                    binding.animeSourceContinueText.text =
                        currActivity()!!.getString(R.string.continue_chapter) + "${ep.number}${if (!ep.title.isNullOrEmpty()) "\n${ep.title}" else ""}"
                    binding.animeSourceContinue.setOnClickListener {
                        fragment.onMangaChapterClick(continueEp)
                    }
                    if (fragment.continueEp) {
                        if ((binding.itemEpisodeProgress.layoutParams as LinearLayout.LayoutParams).weight < 0.8f) {
                            binding.animeSourceContinue.performClick()
                            fragment.continueEp = false
                        }

                    }
                } else {
                    binding.animeSourceContinue.visibility = View.GONE
                }
                binding.animeSourceProgressBar.visibility = View.GONE
                if (media.manga.chapters!!.isNotEmpty())
                    binding.animeSourceNotFound.visibility = View.GONE
                else
                    binding.animeSourceNotFound.visibility = View.VISIBLE
            } else {
                binding.animeSourceContinue.visibility = View.GONE
                binding.animeSourceNotFound.visibility = View.GONE
                clearChips()
                binding.animeSourceProgressBar.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int = 1

    inner class ViewHolder(val binding: ItemAnimeWatchBinding) : RecyclerView.ViewHolder(binding.root)
}