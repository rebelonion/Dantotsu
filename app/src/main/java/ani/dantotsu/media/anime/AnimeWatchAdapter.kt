package ani.dantotsu.media.anime

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.FileUrl
import ani.dantotsu.R
import ani.dantotsu.currActivity
import ani.dantotsu.databinding.DialogLayoutBinding
import ani.dantotsu.databinding.ItemAnimeWatchBinding
import ani.dantotsu.databinding.ItemChipBinding
import ani.dantotsu.displayTimer
import ani.dantotsu.isOnline
import ani.dantotsu.loadImage
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.media.MediaNameAdapter
import ani.dantotsu.media.SourceSearchDialogFragment
import ani.dantotsu.openSettings
import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.others.webview.CookieCatcher
import ani.dantotsu.parsers.AnimeSources
import ani.dantotsu.parsers.DynamicAnimeParser
import ani.dantotsu.parsers.WatchSources
import ani.dantotsu.px
import ani.dantotsu.settings.FAQActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.toast
import com.google.android.material.chip.Chip
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.notification.Notifications.CHANNEL_SUBSCRIPTION_CHECK
import eu.kanade.tachiyomi.util.system.WebViewUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


class AnimeWatchAdapter(
    private val media: Media,
    private val fragment: AnimeWatchFragment,
    private val watchSources: WatchSources
) : RecyclerView.Adapter<AnimeWatchAdapter.ViewHolder>() {
    private var autoSelect = true
    var subscribe: MediaDetailsActivity.PopImageButton? = null
    private var _binding: ItemAnimeWatchBinding? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val bind = ItemAnimeWatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(bind)
    }

    private var nestedDialog: AlertDialog? = null


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        _binding = binding

        binding.faqbutton.setOnClickListener {
            startActivity(
                fragment.requireContext(),
                Intent(fragment.requireContext(), FAQActivity::class.java),
                null
            )
        }
        //Youtube
        if (media.anime?.youtube != null && PrefManager.getVal(PrefName.ShowYtButton)) {
            binding.animeSourceYT.visibility = View.VISIBLE
            binding.animeSourceYT.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(media.anime.youtube))
                fragment.requireContext().startActivity(intent)
            }
        }
        binding.animeSourceDubbed.isChecked = media.selected!!.preferDub
        binding.animeSourceDubbedText.text =
            if (media.selected!!.preferDub) currActivity()!!.getString(R.string.dubbed) else currActivity()!!.getString(
                R.string.subbed
            )

        //PreferDub
        var changing = false
        binding.animeSourceDubbed.setOnCheckedChangeListener { _, isChecked ->
            binding.animeSourceDubbedText.text =
                if (isChecked) currActivity()!!.getString(R.string.dubbed) else currActivity()!!.getString(
                    R.string.subbed
                )
            if (!changing) fragment.onDubClicked(isChecked)
        }

        //Wrong Title
        binding.animeSourceSearch.setOnClickListener {
            SourceSearchDialogFragment().show(
                fragment.requireActivity().supportFragmentManager,
                null
            )
        }
        val offline = !isOnline(binding.root.context) || PrefManager.getVal(PrefName.OfflineMode)

        binding.animeSourceNameContainer.isGone = offline
        binding.animeSourceSettings.isGone = offline
        binding.animeSourceSearch.isGone = offline
        binding.animeSourceTitle.isGone = offline

        //Source Selection
        var source =
            media.selected!!.sourceIndex.let { if (it >= watchSources.names.size) 0 else it }
        setLanguageList(media.selected!!.langIndex, source)
        if (watchSources.names.isNotEmpty() && source in 0 until watchSources.names.size) {
            binding.animeSource.setText(watchSources.names[source])
            watchSources[source].apply {
                this.selectDub = media.selected!!.preferDub
                binding.animeSourceTitle.text = showUserText
                showUserTextListener = { MainScope().launch { binding.animeSourceTitle.text = it } }
                binding.animeSourceDubbedCont.isVisible = isDubAvailableSeparately()
            }
        }

        binding.animeSource.setAdapter(
            ArrayAdapter(
                fragment.requireContext(),
                R.layout.item_dropdown,
                watchSources.names
            )
        )
        binding.animeSourceTitle.isSelected = true
        binding.animeSource.setOnItemClickListener { _, _, i, _ ->
            fragment.onSourceChange(i).apply {
                binding.animeSourceTitle.text = showUserText
                showUserTextListener = { MainScope().launch { binding.animeSourceTitle.text = it } }
                changing = true
                binding.animeSourceDubbed.isChecked = selectDub
                changing = false
                binding.animeSourceDubbedCont.isVisible = isDubAvailableSeparately()
                source = i
                setLanguageList(0, i)
            }
            subscribeButton(false)
            fragment.loadEpisodes(i, false)
        }

        binding.animeSourceLanguage.setOnItemClickListener { _, _, i, _ ->
            // Check if 'extension' and 'selected' properties exist and are accessible
            (watchSources[source] as? DynamicAnimeParser)?.let { ext ->
                ext.sourceLanguage = i
                fragment.onLangChange(i)
                fragment.onSourceChange(media.selected!!.sourceIndex).apply {
                    binding.animeSourceTitle.text = showUserText
                    showUserTextListener =
                        { MainScope().launch { binding.animeSourceTitle.text = it } }
                    changing = true
                    binding.animeSourceDubbed.isChecked = selectDub
                    changing = false
                    binding.animeSourceDubbedCont.isVisible = isDubAvailableSeparately()
                    setLanguageList(i, source)
                }
                subscribeButton(false)
                fragment.loadEpisodes(media.selected!!.sourceIndex, true)
            } ?: run { }
        }

        //settings
        binding.animeSourceSettings.setOnClickListener {
            (watchSources[source] as? DynamicAnimeParser)?.let { ext ->
                fragment.openSettings(ext.extension)
            }
        }

        //Icons

        //subscribe
        subscribe = MediaDetailsActivity.PopImageButton(
            fragment.lifecycleScope,
            binding.animeSourceSubscribe,
            R.drawable.ic_round_notifications_active_24,
            R.drawable.ic_round_notifications_none_24,
            R.color.bg_opp,
            R.color.violet_400,
            fragment.subscribed,
            true
        ) {
            fragment.onNotificationPressed(it, binding.animeSource.text.toString())
        }

        subscribeButton(false)

        binding.animeSourceSubscribe.setOnLongClickListener {
            openSettings(fragment.requireContext(), CHANNEL_SUBSCRIPTION_CHECK)
        }

        //Nested Button
        binding.animeNestedButton.setOnClickListener {
            val dialogView =
                LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_layout, null)
            val dialogBinding = DialogLayoutBinding.bind(dialogView)
            var refresh = false
            var run = false
            var reversed = media.selected!!.recyclerReversed
            var style =
                media.selected!!.recyclerStyle ?: PrefManager.getVal(PrefName.AnimeDefaultView)
            dialogBinding.animeSourceTop.rotation = if (reversed) -90f else 90f
            dialogBinding.sortText.text = if (reversed) "Down to Up" else "Up to Down"
            dialogBinding.animeSourceTop.setOnClickListener {
                reversed = !reversed
                dialogBinding.animeSourceTop.rotation = if (reversed) -90f else 90f
                dialogBinding.sortText.text = if (reversed) "Down to Up" else "Up to Down"
                run = true
            }
            //Grids
            var selected = when (style) {
                0 -> dialogBinding.animeSourceList
                1 -> dialogBinding.animeSourceGrid
                2 -> dialogBinding.animeSourceCompact
                else -> dialogBinding.animeSourceList
            }
            when (style) {
                0 -> dialogBinding.layoutText.setText(R.string.list)
                1 -> dialogBinding.layoutText.setText(R.string.grid)
                2 -> dialogBinding.layoutText.setText(R.string.compact)
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
                dialogBinding.layoutText.setText(R.string.list)
                run = true
            }
            dialogBinding.animeSourceGrid.setOnClickListener {
                selected(it as ImageButton)
                style = 1
                dialogBinding.layoutText.setText(R.string.grid)
                run = true
            }
            dialogBinding.animeSourceCompact.setOnClickListener {
                selected(it as ImageButton)
                style = 2
                dialogBinding.layoutText.setText(R.string.compact)
                run = true
            }
            dialogBinding.animeWebviewContainer.setOnClickListener {
                if (!WebViewUtil.supportsWebView(fragment.requireContext())) {
                    toast(R.string.webview_not_installed)
                }
                //start CookieCatcher activity
                if (watchSources.names.isNotEmpty() && source in 0 until watchSources.names.size) {
                    val sourceAHH = watchSources[source] as? DynamicAnimeParser
                    val sourceHttp =
                        sourceAHH?.extension?.sources?.firstOrNull() as? AnimeHttpSource
                    val url = sourceHttp?.baseUrl
                    url?.let {
                        refresh = true
                        val headersMap = try {
                            sourceHttp.headers.toMultimap()
                                .mapValues { it.value.getOrNull(0) ?: "" }
                        } catch (e: Exception) {
                            emptyMap()
                        }
                        val intent = Intent(fragment.requireContext(), CookieCatcher::class.java)
                            .putExtra("url", url)
                            .putExtra("headers", headersMap as HashMap<String, String>)
                        startActivity(fragment.requireContext(), intent, null)
                    }
                }
            }

            //hidden
            dialogBinding.animeScanlatorContainer.visibility = View.GONE
            dialogBinding.animeDownloadContainer.visibility = View.GONE

            nestedDialog = AlertDialog.Builder(fragment.requireContext(), R.style.MyPopup)
                .setTitle("Options")
                .setView(dialogView)
                .setPositiveButton("OK") { _, _ ->
                    if (run) fragment.onIconPressed(style, reversed)
                    if (refresh) fragment.loadEpisodes(source, true)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    if (refresh) fragment.loadEpisodes(source, true)
                }
                .setOnCancelListener {
                    if (refresh) fragment.loadEpisodes(source, true)
                }
                .create()
            nestedDialog?.show()
        }
        //Episode Handling
        handleEpisodes()
    }

    fun subscribeButton(enabled: Boolean) {
        subscribe?.enabled(enabled)
    }

    //Chips
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
                val chipText = "${names[limit * (position)]} - ${names[last - 1]}"
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

    fun handleEpisodes() {
        val binding = _binding
        if (binding != null) {
            if (media.anime?.episodes != null) {
                val episodes = media.anime.episodes!!.keys.toTypedArray()

                val anilistEp = (media.userProgress ?: 0).plus(1)
                val appEp = PrefManager.getCustomVal<String?>(
                    "${media.id}_current_ep", ""
                )?.toIntOrNull() ?: 1

                var continueEp = (if (anilistEp > appEp) anilistEp else appEp).toString()
                if (episodes.contains(continueEp)) {
                    binding.animeSourceContinue.visibility = View.VISIBLE
                    handleProgress(
                        binding.itemEpisodeProgressCont,
                        binding.itemEpisodeProgress,
                        binding.itemEpisodeProgressEmpty,
                        media.id,
                        continueEp
                    )
                    if ((binding.itemEpisodeProgress.layoutParams as LinearLayout.LayoutParams).weight > PrefManager.getVal<Float>(
                            PrefName.WatchPercentage
                        )
                    ) {
                        val e = episodes.indexOf(continueEp)
                        if (e != -1 && e + 1 < episodes.size) {
                            continueEp = episodes[e + 1]
                            handleProgress(
                                binding.itemEpisodeProgressCont,
                                binding.itemEpisodeProgress,
                                binding.itemEpisodeProgressEmpty,
                                media.id,
                                continueEp
                            )
                        }
                    }
                    val ep = media.anime.episodes!![continueEp]!!

                    val cleanedTitle = ep.title?.let { MediaNameAdapter.removeEpisodeNumber(it) }

                    binding.itemEpisodeImage.loadImage(
                        ep.thumb ?: FileUrl[media.banner ?: media.cover], 0
                    )
                    if (ep.filler) binding.itemEpisodeFillerView.visibility = View.VISIBLE

                    binding.animeSourceContinueText.text =
                        currActivity()!!.getString(R.string.continue_episode, ep.number, if (ep.filler)
                            currActivity()!!.getString(R.string.filler_tag)
                        else
                            "", cleanedTitle)
                    binding.animeSourceContinue.setOnClickListener {
                        fragment.onEpisodeClick(continueEp)
                    }
                    if (fragment.continueEp) {
                        if (
                            (binding.itemEpisodeProgress.layoutParams as LinearLayout.LayoutParams)
                                .weight < PrefManager.getVal<Float>(PrefName.WatchPercentage)
                        ) {
                            binding.animeSourceContinue.performClick()
                            fragment.continueEp = false
                        }
                    }
                } else {
                    binding.animeSourceContinue.visibility = View.GONE
                }

                binding.animeSourceProgressBar.visibility = View.GONE

                val sourceFound = media.anime.episodes!!.isNotEmpty()
                binding.animeSourceNotFound.isGone = sourceFound
                binding.faqbutton.isGone = sourceFound

                if (!sourceFound && PrefManager.getVal(PrefName.SearchSources) && autoSelect) {
                    if (binding.animeSource.adapter.count > media.selected!!.sourceIndex + 1) {
                        val nextIndex = media.selected!!.sourceIndex + 1
                        binding.animeSource.setText(binding.animeSource.adapter
                            .getItem(nextIndex).toString(), false)
                        fragment.onSourceChange(nextIndex).apply {
                            binding.animeSourceTitle.text = showUserText
                            showUserTextListener = { MainScope().launch { binding.animeSourceTitle.text = it } }
                            binding.animeSourceDubbed.isChecked = selectDub
                            binding.animeSourceDubbedCont.isVisible = isDubAvailableSeparately()
                            setLanguageList(0, nextIndex)
                        }
                        subscribeButton(false)
                        fragment.loadEpisodes(nextIndex, false)
                    }
                }
                binding.animeSource.setOnClickListener { autoSelect = false }
            } else {
                binding.animeSourceContinue.visibility = View.GONE
                binding.animeSourceNotFound.visibility = View.GONE
                binding.faqbutton.visibility = View.GONE
                clearChips()
                binding.animeSourceProgressBar.visibility = View.VISIBLE
            }
        }
    }

    private fun setLanguageList(lang: Int, source: Int) {
        val binding = _binding
        if (watchSources is AnimeSources) {
            val parser = watchSources[source] as? DynamicAnimeParser
            if (parser != null) {
                (watchSources[source] as? DynamicAnimeParser)?.let { ext ->
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

                binding?.animeSourceLanguageContainer?.visibility =
                    if (items > 1) View.VISIBLE else View.GONE
                binding?.animeSourceLanguage?.setAdapter(adapter)

            }
        }
    }

    override fun getItemCount(): Int = 1

    inner class ViewHolder(val binding: ItemAnimeWatchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            displayTimer(media, binding.animeSourceContainer)
        }
    }
}
