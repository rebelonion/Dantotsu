package ani.dantotsu.media.manga

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.*
import ani.dantotsu.databinding.DialogLayoutBinding
import ani.dantotsu.databinding.ItemAnimeWatchBinding
import ani.dantotsu.databinding.ItemChipBinding
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.media.SourceSearchDialogFragment
import ani.dantotsu.media.anime.handleProgress
import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.parsers.DynamicMangaParser
import ani.dantotsu.parsers.MangaReadSources
import ani.dantotsu.parsers.MangaSources
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
    val hiddenScanlators = mutableListOf<String>()
    var scanlatorSelectionListener: ScanlatorSelectionListener? = null
    var options = listOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val bind = ItemAnimeWatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(bind)
    }
    private var nestedDialog: AlertDialog? = null
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        _binding = binding
        binding.sourceTitle.setText(R.string.chaps)

        //Wrong Title
        binding.animeSourceSearch.setOnClickListener {
            SourceSearchDialogFragment().show(
                fragment.requireActivity().supportFragmentManager,
                null
            )
        }

        //Source Selection
        var source =
            media.selected!!.sourceIndex.let { if (it >= mangaReadSources.names.size) 0 else it }
        setLanguageList(media.selected!!.langIndex, source)
        if (mangaReadSources.names.isNotEmpty() && source in 0 until mangaReadSources.names.size) {
            binding.animeSource.setText(mangaReadSources.names[source])
            mangaReadSources[source].apply {
                binding.animeSourceTitle.text = showUserText
                showUserTextListener = { MainScope().launch { binding.animeSourceTitle.text = it } }
            }
        }
        binding.animeSource.setAdapter(
            ArrayAdapter(
                fragment.requireContext(),
                R.layout.item_dropdown,
                mangaReadSources.names
            )
        )
        binding.animeSourceTitle.isSelected = true
        binding.animeSource.setOnItemClickListener { _, _, i, _ ->
            fragment.onSourceChange(i).apply {
                binding.animeSourceTitle.text = showUserText
                showUserTextListener = { MainScope().launch { binding.animeSourceTitle.text = it } }
                source = i
                setLanguageList(0, i)
            }
            subscribeButton(false)
            //invalidate if it's the last source
            val invalidate = i == mangaReadSources.names.size - 1
            fragment.loadChapters(i, invalidate)
        }

        binding.animeSourceLanguage.setOnItemClickListener { _, _, i, _ ->
            // Check if 'extension' and 'selected' properties exist and are accessible
            (mangaReadSources[source] as? DynamicMangaParser)?.let { ext ->
                ext.sourceLanguage = i
                fragment.onLangChange(i)
                fragment.onSourceChange(media.selected!!.sourceIndex).apply {
                    binding.animeSourceTitle.text = showUserText
                    showUserTextListener =
                        { MainScope().launch { binding.animeSourceTitle.text = it } }
                    setLanguageList(i, source)
                }
                subscribeButton(false)
                fragment.loadChapters(media.selected!!.sourceIndex, true)
            } ?: run {
            }
        }

        //settings
        binding.animeSourceSettings.setOnClickListener {
            (mangaReadSources[source] as? DynamicMangaParser)?.let { ext ->
                fragment.openSettings(ext.extension)
            }
        }

        //Grids
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

        binding.animeNestedButton.setOnClickListener {

            val dialogView =
                LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_layout, null)
            val dialogBinding = DialogLayoutBinding.bind(dialogView)

            var run = false
            var reversed = media.selected!!.recyclerReversed
            var style = media.selected!!.recyclerStyle ?: fragment.uiSettings.animeDefaultView
            dialogBinding.animeSourceTop.rotation = if (reversed) -90f else 90f
            dialogBinding.sortText.text = if (reversed) "Down to Up" else "Up to Down"
            dialogBinding.animeSourceTop.setOnClickListener {
                reversed = !reversed
                dialogBinding.animeSourceTop.rotation = if (reversed) -90f else 90f
                dialogBinding.sortText.text = if (reversed) "Down to Up" else "Up to Down"
                run = true
            }

            //Grids
            dialogBinding.animeSourceGrid.visibility = View.GONE
            var selected = when (style) {
                0 -> dialogBinding.animeSourceList
                1 -> dialogBinding.animeSourceCompact
                else -> dialogBinding.animeSourceList
            }
            when (style) {
                0 -> dialogBinding.layoutText.text = "List"
                1 -> dialogBinding.layoutText.text = "Compact"
                else -> dialogBinding.animeSourceList
            }
            selected.alpha = 1f
            fun selected(it: ImageButton) {
                selected.alpha = 0.33f
                selected = it
                selected.alpha = 1f
            }
            dialogBinding.animeSourceList.setOnClickListener {
                selected(it as ImageButton)
                style = 0
                dialogBinding.layoutText.text = "List"
                run = true
            }
            dialogBinding.animeSourceCompact.setOnClickListener {
                selected(it as ImageButton)
                style = 1
                dialogBinding.layoutText.text = "Compact"
                run = true
            }

            //Multi download
            dialogBinding.downloadNo.text = "0"
            dialogBinding.animeDownloadTop.setOnClickListener {
                //Alert dialog asking for the number of chapters to download
                val alertDialog = AlertDialog.Builder(currContext(), R.style.MyPopup)
                alertDialog.setTitle("Multi Chapter Downloader")
                alertDialog.setMessage("Enter the number of chapters to download")
                val input = NumberPicker(currContext())
                input.minValue = 1
                input.maxValue = 20
                input.value = 1
                alertDialog.setView(input)
                alertDialog.setPositiveButton("OK") { _, _ ->
                    dialogBinding.downloadNo.text = "${input.value}"
                }
                alertDialog.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                val dialog = alertDialog.show()
                dialog.window?.setDimAmount(0.8f)
            }

            //Scanlator
            dialogBinding.animeScanlatorTop.setOnClickListener {
                val dialogView2 =
                    LayoutInflater.from(currContext()).inflate(R.layout.custom_dialog_layout, null)
                val checkboxContainer = dialogView2.findViewById<LinearLayout>(R.id.checkboxContainer)

                // Dynamically add checkboxes
                options.forEach { option ->
                    val checkBox = CheckBox(currContext()).apply {
                        text = option
                    }
                    //set checked if it's already selected
                    if (media.selected!!.scanlators != null) {
                        checkBox.isChecked = media.selected!!.scanlators?.contains(option) != true
                        scanlatorSelectionListener?.onScanlatorsSelected()
                    } else {
                        checkBox.isChecked = true
                    }
                    checkboxContainer.addView(checkBox)
                }

                // Create AlertDialog
                val dialog = AlertDialog.Builder(currContext(), R.style.MyPopup)
                    .setView(dialogView2)
                    .setPositiveButton("OK") { _, _ ->
                        //add unchecked to hidden
                        hiddenScanlators.clear()
                        for (i in 0 until checkboxContainer.childCount) {
                            val checkBox = checkboxContainer.getChildAt(i) as CheckBox
                            if (!checkBox.isChecked) {
                                hiddenScanlators.add(checkBox.text.toString())
                            }
                        }
                        fragment.onScanlatorChange(hiddenScanlators)
                        scanlatorSelectionListener?.onScanlatorsSelected()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                dialog.window?.setDimAmount(0.8f)
            }

            nestedDialog = AlertDialog.Builder(fragment.requireContext() , R.style.MyPopup)
                .setTitle("Options")
                .setView(dialogView)
                .setPositiveButton("OK") { _, _ ->
                    if(run) fragment.onIconPressed(style, reversed)
                    if (dialogBinding.downloadNo.text != "0"){
                        fragment.multiDownload(dialogBinding.downloadNo.text.toString().toInt())
                    }
                }
                .setNegativeButton("Cancel") { _, _ ->
                }
                .create()
            nestedDialog?.show()
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
                    ItemChipBinding.inflate(
                        LayoutInflater.from(fragment.context),
                        binding.animeSourceChipGroup,
                        false
                    ).root
                chip.isCheckable = true
                fun selected() {
                    chip.isChecked = true
                    binding.animeWatchChipScroll.smoothScrollTo(
                        (chip.left - screenWidth / 2) + (chip.width / 2),
                        0
                    )
                }
                val startChapter = MangaNameAdapter.findChapterNumber(names[limit * (position)])
                val endChapter = MangaNameAdapter.findChapterNumber(names[last - 1])
                val startChapterString = if (startChapter != null) {
                    "Ch.$startChapter"
                } else {
                    names[limit * (position)]
                }
                val endChapterString = if (endChapter != null) {
                    "Ch.$endChapter"
                } else {
                    names[last - 1]
                }
                //chip.text = "${names[limit * (position)]} - ${names[last - 1]}"
                chip.text = "$startChapterString - $endChapterString"
                chip.setTextColor(
                    ContextCompat.getColorStateList(
                        fragment.requireContext(),
                        R.color.chip_text_color
                    )
                )

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
                binding.animeWatchChipScroll.apply {
                    post {
                        scrollTo(
                            (select.left - screenWidth / 2) + (select.width / 2),
                            0
                        )
                    }
                }
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
                val filteredChapters = chapters.filter { chapterKey ->
                    val chapter = media.manga.chapters!![chapterKey]!!
                    chapter.scanlator !in hiddenScanlators
                }
                val formattedChapters = filteredChapters.map {
                    MangaNameAdapter.findChapterNumber(it)?.toInt()?.toString()
                }
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

    private fun setLanguageList(lang: Int, source: Int) {
        val binding = _binding
        if (mangaReadSources is MangaSources) {
            val parser = mangaReadSources[source] as? DynamicMangaParser
            if (parser != null) {
                (mangaReadSources[source] as? DynamicMangaParser)?.let { ext ->
                    ext.sourceLanguage = lang
                }
                try {
                    binding?.animeSourceLanguage?.setText(parser.extension.sources[lang].lang)
                } catch (e: IndexOutOfBoundsException) {
                    binding?.animeSourceLanguage?.setText(
                        parser.extension.sources.firstOrNull()?.lang ?: "Unknown"
                    )
                }
                val adapter = ArrayAdapter(
                    fragment.requireContext(),
                    R.layout.item_dropdown,
                    parser.extension.sources.map { LanguageMapper.mapLanguageCodeToName(it.lang) }
                )
                val items = adapter.count
                if (items > 1) binding?.animeSourceLanguageContainer?.visibility  =  View.VISIBLE else binding?.animeSourceLanguageContainer?.visibility  =  View.GONE

                binding?.animeSourceLanguage?.setAdapter(adapter)

            }
        }
    }

    override fun getItemCount(): Int = 1

    inner class ViewHolder(val binding: ItemAnimeWatchBinding) :
        RecyclerView.ViewHolder(binding.root)
}

interface ScanlatorSelectionListener {
    fun onScanlatorsSelected()
}
