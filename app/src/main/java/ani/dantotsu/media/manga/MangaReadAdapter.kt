package ani.dantotsu.media.manga

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.currActivity
import ani.dantotsu.currContext
import ani.dantotsu.databinding.CustomDialogLayoutBinding
import ani.dantotsu.databinding.DialogLayoutBinding
import ani.dantotsu.databinding.ItemMediaSourceBinding
import ani.dantotsu.databinding.ItemChipBinding
import ani.dantotsu.isOnline
import ani.dantotsu.loadImage
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.media.MediaNameAdapter
import ani.dantotsu.media.SourceSearchDialogFragment
import ani.dantotsu.media.anime.handleProgress
import ani.dantotsu.openSettings
import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.others.webview.CookieCatcher
import ani.dantotsu.parsers.DynamicMangaParser
import ani.dantotsu.parsers.MangaReadSources
import ani.dantotsu.parsers.MangaSources
import ani.dantotsu.parsers.OfflineMangaParser
import ani.dantotsu.px
import ani.dantotsu.settings.FAQActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.toast
import ani.dantotsu.util.customAlertDialog
import com.google.android.material.chip.Chip
import eu.kanade.tachiyomi.data.notification.Notifications.CHANNEL_SUBSCRIPTION_CHECK
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.system.WebViewUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


class MangaReadAdapter(
    private val media: Media,
    private val fragment: MangaReadFragment,
    private val mangaReadSources: MangaReadSources
) : RecyclerView.Adapter<MangaReadAdapter.ViewHolder>() {

    var subscribe: MediaDetailsActivity.PopImageButton? = null
    private var _binding: ItemMediaSourceBinding? = null
    val hiddenScanlators = mutableListOf<String>()
    var scanlatorSelectionListener: ScanlatorSelectionListener? = null
    var options = listOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val bind = ItemMediaSourceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(bind)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        _binding = binding
        binding.sourceTitle.setText(R.string.chaps)

        // Fuck u launch
        binding.faqbutton.setOnClickListener {
            val intent = Intent(fragment.requireContext(), FAQActivity::class.java)
            startActivity(fragment.requireContext(), intent, null)
        }

        // Wrong Title
        binding.mediaSourceSearch.setOnClickListener {
            SourceSearchDialogFragment().show(
                fragment.requireActivity().supportFragmentManager,
                null
            )
        }
        val offline = !isOnline(binding.root.context) || PrefManager.getVal(PrefName.OfflineMode)

        binding.mediaSourceNameContainer.isGone = offline
        binding.mediaSourceSettings.isGone = offline
        binding.mediaSourceSearch.isGone = offline
        binding.mediaSourceTitle.isGone = offline
        // Source Selection
        var source =
            media.selected!!.sourceIndex.let { if (it >= mangaReadSources.names.size) 0 else it }
        setLanguageList(media.selected!!.langIndex, source)
        if (mangaReadSources.names.isNotEmpty() && source in 0 until mangaReadSources.names.size) {
            binding.mediaSource.setText(mangaReadSources.names[source])
            mangaReadSources[source].apply {
                binding.mediaSourceTitle.text = showUserText
                showUserTextListener = { MainScope().launch { binding.mediaSourceTitle.text = it } }
            }
        }
        media.selected?.scanlators?.let {
            hiddenScanlators.addAll(it)
        }
        binding.mediaSource.setAdapter(
            ArrayAdapter(
                fragment.requireContext(),
                R.layout.item_dropdown,
                mangaReadSources.names
            )
        )
        binding.mediaSourceTitle.isSelected = true
        binding.mediaSource.setOnItemClickListener { _, _, i, _ ->
            fragment.onSourceChange(i).apply {
                binding.mediaSourceTitle.text = showUserText
                showUserTextListener = { MainScope().launch { binding.mediaSourceTitle.text = it } }
                source = i
                setLanguageList(0, i)
            }
            subscribeButton(false)
            // Invalidate if it's the last source
            val invalidate = i == mangaReadSources.names.size - 1
            fragment.loadChapters(i, invalidate)
        }

        binding.mediaSourceLanguage.setOnItemClickListener { _, _, i, _ ->
            // Check if 'extension' and 'selected' properties exist and are accessible
            (mangaReadSources[source] as? DynamicMangaParser)?.let { ext ->
                ext.sourceLanguage = i
                fragment.onLangChange(i, ext.saveName)
                fragment.onSourceChange(media.selected!!.sourceIndex).apply {
                    binding.mediaSourceTitle.text = showUserText
                    showUserTextListener =
                        { MainScope().launch { binding.mediaSourceTitle.text = it } }
                    setLanguageList(i, source)
                }
                subscribeButton(false)
                fragment.loadChapters(media.selected!!.sourceIndex, true)
            } ?: run {
            }
        }

        // Settings
        binding.mediaSourceSettings.setOnClickListener {
            (mangaReadSources[source] as? DynamicMangaParser)?.let { ext ->
                fragment.openSettings(ext.extension)
            }
        }

        // Grids
        subscribe = MediaDetailsActivity.PopImageButton(
            fragment.lifecycleScope,
            binding.mediaSourceSubscribe,
            R.drawable.ic_round_notifications_active_24,
            R.drawable.ic_round_notifications_none_24,
            R.color.bg_opp,
            R.color.violet_400,
            fragment.subscribed,
            true
        ) {
            fragment.onNotificationPressed(it, binding.mediaSource.text.toString())
        }

        subscribeButton(false)

        binding.mediaSourceSubscribe.setOnLongClickListener {
            openSettings(fragment.requireContext(), CHANNEL_SUBSCRIPTION_CHECK)
        }

        binding.mediaNestedButton.setOnClickListener {
            val dialogBinding = DialogLayoutBinding.inflate(fragment.layoutInflater)
            var refresh = false
            var run = false
            var reversed = media.selected!!.recyclerReversed
            var style =
                media.selected!!.recyclerStyle ?: PrefManager.getVal(PrefName.MangaDefaultView)
            dialogBinding.apply {
                mediaSourceTop.rotation = if (reversed) -90f else 90f
                sortText.text = if (reversed) "Down to Up" else "Up to Down"
                mediaSourceTop.setOnClickListener {
                    reversed = !reversed
                    mediaSourceTop.rotation = if (reversed) -90f else 90f
                    sortText.text = if (reversed) "Down to Up" else "Up to Down"
                    run = true
                }

                // Grids
                mediaSourceGrid.visibility = View.GONE
                var selected = when (style) {
                    0 -> mediaSourceList
                    1 -> mediaSourceCompact
                    else -> mediaSourceList
                }
                when (style) {
                    0 -> layoutText.setText(R.string.list)
                    1 -> layoutText.setText(R.string.compact)
                    else -> mediaSourceList
                }
                selected.alpha = 1f
                fun selected(it: ImageButton) {
                    selected.alpha = 0.33f
                    selected = it
                    selected.alpha = 1f
                }
                mediaSourceList.setOnClickListener {
                    selected(it as ImageButton)
                    style = 0
                    layoutText.setText(R.string.list)
                    run = true
                }
                mediaSourceCompact.setOnClickListener {
                    selected(it as ImageButton)
                    style = 1
                    layoutText.setText(R.string.compact)
                    run = true
                }
                mediaWebviewContainer.setOnClickListener {
                    if (!WebViewUtil.supportsWebView(fragment.requireContext())) {
                        toast(R.string.webview_not_installed)
                    }
                    // Start CookieCatcher activity
                    if (mangaReadSources.names.isNotEmpty() && source in 0 until mangaReadSources.names.size) {
                        val sourceAHH = mangaReadSources[source] as? DynamicMangaParser
                        val sourceHttp = sourceAHH?.extension?.sources?.firstOrNull() as? HttpSource
                        val url = sourceHttp?.baseUrl
                        url?.let {
                            refresh = true
                            val intent =
                                Intent(fragment.requireContext(), CookieCatcher::class.java)
                                    .putExtra("url", url)
                            startActivity(fragment.requireContext(), intent, null)
                        }
                    }
                }

                // Multi download
                downloadNo.text = "0"
                mediaDownloadTop.setOnClickListener {
                    // Alert dialog asking for the number of chapters to download
                    fragment.requireContext().customAlertDialog().apply {
                        setTitle("Multi Chapter Downloader")
                        setMessage("Enter the number of chapters to download")
                        val input = NumberPicker(currContext())
                        input.minValue = 1
                        input.maxValue = 20
                        input.value = 1
                        setCustomView(input)
                        setPosButton(R.string.ok) {
                            downloadNo.text = "${input.value}"
                        }
                        setNegButton(R.string.cancel)
                        show()
                    }
                }

                //Scanlator
                dialogBinding.animeScanlatorContainer.isVisible = options.count() > 1
                dialogBinding.scanlatorNo.text = "${options.count()}"
                
                // Log the scanlators to debug UI issues
                Log.d("ScanlatorOptions", "Scanlators count: ${options.count()} - Scanlators: $options")
                
                dialogBinding.animeScanlatorTop.setOnClickListener {
                    val dialogView2 =
                        LayoutInflater.from(currContext()).inflate(R.layout.custom_dialog_layout, null)
                    val checkboxContainer =
                        dialogView2.findViewById<LinearLayout>(R.id.checkboxContainer)
                    val tickAllButton = dialogView2.findViewById<ImageButton>(R.id.toggleButton)
                
                    // Function to get the right image resource for the toggle button
                    fun getToggleImageResource(container: ViewGroup): Int {
                        var allChecked = true
                        var allUnchecked = true
                
                        for (i in 0 until container.childCount) {
                            val checkBox = container.getChildAt(i) as CheckBox
                            if (!checkBox.isChecked) {
                                allChecked = false
                            } else {
                                allUnchecked = false
                            }
                        }
                        return when {
                            allChecked -> R.drawable.untick_all_boxes
                            allUnchecked -> R.drawable.tick_all_boxes
                            else -> R.drawable.invert_all_boxes
                        }
                    }

                    options.forEach { option ->
                        val checkBox = CheckBox(currContext()).apply {
                            text = option
                            setOnCheckedChangeListener { _, _ ->
                                tickAllButton.setImageResource(getToggleImageResource(checkboxContainer))
                            }
                        }

                        if (media.selected!!.scanlators != null) {
                            checkBox.isChecked = media.selected!!.scanlators?.contains(option) != true
                            scanlatorSelectionListener?.onScanlatorsSelected()
                        } else {
                            checkBox.isChecked = true
                        }
                        checkboxContainer.addView(checkBox)
                    }

                    fragment.requireContext().customAlertDialog().apply {
                        setCustomView(dialogView.root)
                        setPosButton("OK") {
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
                        setNegButton("Cancel")
                    }.show()

                    tickAllButton.setImageResource(getToggleImageResource(checkboxContainer))

                    tickAllButton.setOnClickListener {
                        for (i in 0 until checkboxContainer.childCount) {
                            val checkBox = checkboxContainer.getChildAt(i) as CheckBox
                            checkBox.isChecked = !checkBox.isChecked
                        }
                        tickAllButton.setImageResource(getToggleImageResource(checkboxContainer))
                    }
                }

                fragment.requireContext().customAlertDialog().apply {
                    setTitle("Options")
                    setCustomView(root)
                    setPosButton("OK") {
                        if (run) fragment.onIconPressed(style, reversed)
                        if (downloadNo.text != "0") {
                            fragment.multiDownload(downloadNo.text.toString().toInt())
                        }
                        if (refresh) fragment.loadChapters(source, true)
                    }
                    setNegButton("Cancel") {
                        if (refresh) fragment.loadChapters(source, true)
                    }
                    show()
                }
            }
        }
        // Chapter Handling
        handleChapters()
    }

    fun subscribeButton(enabled: Boolean) {
        subscribe?.enabled(enabled)
    }

    // Chips
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
                        binding.mediaSourceChipGroup,
                        false
                    ).root
                chip.isCheckable = true
                fun selected() {
                    chip.isChecked = true
                    binding.mediaWatchChipScroll.smoothScrollTo(
                        (chip.left - screenWidth / 2) + (chip.width / 2),
                        0
                    )
                }

                val startChapter = MediaNameAdapter.findChapterNumber(names[limit * (position)])
                val endChapter = MediaNameAdapter.findChapterNumber(names[last - 1])
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
                // chip.text = "${names[limit * (position)]} - ${names[last - 1]}"
                val chipText = "$startChapterString - $endChapterString"
                chip.text = chipText
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
                binding.mediaSourceChipGroup.addView(chip)
                if (selected == position) {
                    selected()
                    select = chip
                }
            }
            if (select != null)
                binding.mediaWatchChipScroll.apply {
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
        _binding?.mediaSourceChipGroup?.removeAllViews()
    }

    fun handleChapters() {

        val binding = _binding
        if (binding != null) {
            if (media.manga?.chapters != null) {
                val chapters = media.manga.chapters!!.keys.toTypedArray()
                val anilistEp = (media.userProgress ?: 0).plus(1)
                val appEp = PrefManager.getNullableCustomVal(
                    "${media.id}_current_chp",
                    null,
                    String::class.java
                )
                    ?.toIntOrNull() ?: 1
                var continueEp = (if (anilistEp > appEp) anilistEp else appEp).toString()
                val filteredChapters = chapters.filter { chapterKey ->
                    val chapter = media.manga.chapters!![chapterKey]!!
                    chapter.scanlator !in hiddenScanlators
                }
                val formattedChapters = filteredChapters.map {
                    MediaNameAdapter.findChapterNumber(it)?.toInt()?.toString()
                }
                if (formattedChapters.contains(continueEp)) {
                    continueEp = chapters[formattedChapters.indexOf(continueEp)]
                    binding.sourceContinue.visibility = View.VISIBLE
                    handleProgress(
                        binding.itemMediaProgressCont,
                        binding.itemMediaProgress,
                        binding.itemMediaProgressEmpty,
                        media.id,
                        continueEp
                    )
                    if ((binding.itemMediaProgress.layoutParams as LinearLayout.LayoutParams).weight > 0.8f) {
                        val e = chapters.indexOf(continueEp)
                        if (e != -1 && e + 1 < chapters.size) {
                            continueEp = chapters[e + 1]
                        }
                    }
                    val ep = media.manga.chapters!![continueEp]!!
                    binding.itemMediaImage.loadImage(media.banner ?: media.cover)
                    binding.mediaSourceContinueText.text =
                        currActivity()!!.getString(
                            R.string.continue_chapter,
                            ep.number,
                            if (!ep.title.isNullOrEmpty()) ep.title else ""
                        )
                    binding.sourceContinue.setOnClickListener {
                        fragment.onMangaChapterClick(continueEp)
                    }
                    if (fragment.continueEp) {
                        if ((binding.itemMediaProgress.layoutParams as LinearLayout.LayoutParams).weight < 0.8f) {
                            binding.sourceContinue.performClick()
                            fragment.continueEp = false
                        }

                    }
                } else {
                    binding.sourceContinue.visibility = View.GONE
                }

                binding.sourceProgressBar.visibility = View.GONE

                val sourceFound = filteredChapters.isNotEmpty()
                val isDownloadedSource = mangaReadSources[media.selected!!.sourceIndex] is OfflineMangaParser

                if (isDownloadedSource) {
                    binding.sourceNotFound.text = if (sourceFound) {
                        currActivity()!!.getString(R.string.source_not_found)
                    } else {
                        currActivity()!!.getString(R.string.download_not_found)
                    }
                } else {
                    binding.sourceNotFound.text = currActivity()!!.getString(R.string.source_not_found)
                }

                binding.sourceNotFound.isGone = sourceFound
                binding.faqbutton.isGone = sourceFound


                if (!sourceFound && PrefManager.getVal(PrefName.SearchSources)) {
                    if (binding.mediaSource.adapter.count > media.selected!!.sourceIndex + 1) {
                        val nextIndex = media.selected!!.sourceIndex + 1
                        binding.mediaSource.setText(
                            binding.mediaSource.adapter
                                .getItem(nextIndex).toString(), false
                        )
                        fragment.onSourceChange(nextIndex).apply {
                            binding.mediaSourceTitle.text = showUserText
                            showUserTextListener =
                                { MainScope().launch { binding.mediaSourceTitle.text = it } }
                            setLanguageList(0, nextIndex)
                        }
                        subscribeButton(false)
                        // Invalidate if it's the last source
                        val invalidate = nextIndex == mangaReadSources.names.size - 1
                        fragment.loadChapters(nextIndex, invalidate)
                    }
                }
            } else {
                binding.sourceContinue.visibility = View.GONE
                binding.sourceNotFound.visibility = View.GONE
                binding.faqbutton.visibility = View.GONE
                clearChips()
                binding.sourceProgressBar.visibility = View.VISIBLE
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
                    binding?.mediaSourceLanguage?.setText(parser.extension.sources[lang].lang)
                } catch (e: IndexOutOfBoundsException) {
                    binding?.mediaSourceLanguage?.setText(
                        parser.extension.sources.firstOrNull()?.lang ?: "Unknown"
                    )
                }
                val adapter = ArrayAdapter(
                    fragment.requireContext(),
                    R.layout.item_dropdown,
                    parser.extension.sources.map { LanguageMapper.getLanguageName(it.lang) }
                )
                val items = adapter.count
                binding?.mediaSourceLanguageContainer?.isVisible = items > 1

                binding?.mediaSourceLanguage?.setAdapter(adapter)

            }
        }
    }

    override fun getItemCount(): Int = 1

    inner class ViewHolder(val binding: ItemMediaSourceBinding) :
        RecyclerView.ViewHolder(binding.root)
}

interface ScanlatorSelectionListener {
    fun onScanlatorsSelected()
}
