package ani.dantotsu.media

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.*
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.GenresViewModel
import ani.dantotsu.databinding.*
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.Serializable
import java.net.URLEncoder


@SuppressLint("SetTextI18n")
class MediaInfoFragment : Fragment() {
    private var _binding: FragmentMediaInfoBinding? = null
    private val binding get() = _binding!!
    private var timer: CountDownTimer? = null
    private var loaded = false
    private var type = "ANIME"
    private val genreModel: GenresViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val model: MediaDetailsViewModel by activityViewModels()
        val offline: Boolean = PrefManager.getVal(PrefName.OfflineMode)
        binding.mediaInfoProgressBar.visibility = if (!loaded) View.VISIBLE else View.GONE
        binding.mediaInfoContainer.visibility = if (loaded) View.VISIBLE else View.GONE
        binding.mediaInfoContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += 128f.px + navBarHeight }

        model.scrolledToTop.observe(viewLifecycleOwner) {
            if (it) binding.mediaInfoScroll.scrollTo(0, 0)
        }

        model.getMedia().observe(viewLifecycleOwner) { media ->
            if (media != null && !loaded) {
                loaded = true


                binding.mediaInfoProgressBar.visibility = View.GONE
                binding.mediaInfoContainer.visibility = View.VISIBLE
                binding.mediaInfoName.text = "\t\t\t" + (media.name ?: media.nameRomaji)
                binding.mediaInfoName.setOnLongClickListener {
                    copyToClipboard(media.name ?: media.nameRomaji)
                    true
                }
                if (media.name != null) binding.mediaInfoNameRomajiContainer.visibility =
                    View.VISIBLE
                binding.mediaInfoNameRomaji.text = "\t\t\t" + media.nameRomaji
                binding.mediaInfoNameRomaji.setOnLongClickListener {
                    copyToClipboard(media.nameRomaji)
                    true
                }
                binding.mediaInfoMeanScore.text =
                    if (media.meanScore != null) (media.meanScore / 10.0).toString() else "??"
                binding.mediaInfoStatus.text = media.status
                binding.mediaInfoFormat.text = media.format
                binding.mediaInfoSource.text = media.source
                binding.mediaInfoStart.text = media.startDate?.toString() ?: "??"
                binding.mediaInfoEnd.text = media.endDate?.toString() ?: "??"
                if (media.anime != null) {
                    val episodeDuration = media.anime.episodeDuration

                    binding.mediaInfoDuration.text = when {
                        episodeDuration != null -> {
                            val hours = episodeDuration / 60
                            val minutes = episodeDuration % 60

                            val formattedDuration = buildString {
                                if (hours > 0) {
                                    append("$hours hour")
                                    if (hours > 1) append("s")
                                }

                                if (minutes > 0) {
                                    if (hours > 0) append(", ")
                                    append("$minutes min")
                                    if (minutes > 1) append("s")
                                }
                            }

                            formattedDuration
                        }

                        else -> "??"
                    }
                    binding.mediaInfoDurationContainer.visibility = View.VISIBLE
                    binding.mediaInfoSeasonContainer.visibility = View.VISIBLE
                    binding.mediaInfoSeason.text =
                        (media.anime.season ?: "??") + " " + (media.anime.seasonYear ?: "??")
                    if (media.anime.mainStudio != null) {
                        binding.mediaInfoStudioContainer.visibility = View.VISIBLE
                        binding.mediaInfoStudio.text = media.anime.mainStudio!!.name
                        if (!offline) {
                            binding.mediaInfoStudioContainer.setOnClickListener {
                                ContextCompat.startActivity(
                                    requireActivity(),
                                    Intent(activity, StudioActivity::class.java).putExtra(
                                        "studio",
                                        media.anime.mainStudio!! as Serializable
                                    ),
                                    null
                                )
                            }
                        }
                    }
                    if (media.anime.author != null) {
                        binding.mediaInfoAuthorContainer.visibility = View.VISIBLE
                        binding.mediaInfoAuthor.text = media.anime.author!!.name
                        if (!offline) {
                            binding.mediaInfoAuthorContainer.setOnClickListener {
                                ContextCompat.startActivity(
                                    requireActivity(),
                                    Intent(activity, AuthorActivity::class.java).putExtra(
                                        "author",
                                        media.anime.author!! as Serializable
                                    ),
                                    null
                                )
                            }
                        }
                    }
                    binding.mediaInfoTotalTitle.setText(R.string.total_eps)
                    binding.mediaInfoTotal.text =
                        if (media.anime.nextAiringEpisode != null) (media.anime.nextAiringEpisode.toString() + " | " + (media.anime.totalEpisodes
                            ?: "~").toString()) else (media.anime.totalEpisodes ?: "~").toString()
                } else if (media.manga != null) {
                    type = "MANGA"
                    binding.mediaInfoTotalTitle.setText(R.string.total_chaps)
                    binding.mediaInfoTotal.text = (media.manga.totalChapters ?: "~").toString()
                    if (media.manga.author != null) {
                        binding.mediaInfoAuthorContainer.visibility = View.VISIBLE
                        binding.mediaInfoAuthor.text = media.manga.author!!.name
                        if (!offline) {
                            binding.mediaInfoAuthorContainer.setOnClickListener {
                                ContextCompat.startActivity(
                                    requireActivity(),
                                    Intent(activity, AuthorActivity::class.java).putExtra(
                                        "author",
                                        media.manga.author!! as Serializable
                                    ),
                                    null
                                )
                            }
                        }
                    }
                }

                val desc = HtmlCompat.fromHtml(
                    (media.description ?: "null").replace("\\n", "<br>").replace("\\\"", "\""),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
                binding.mediaInfoDescription.text =
                    "\t\t\t" + if (desc.toString() != "null") desc else getString(R.string.no_description_available)
                binding.mediaInfoDescription.setOnClickListener {
                    if (binding.mediaInfoDescription.maxLines == 5) {
                        ObjectAnimator.ofInt(binding.mediaInfoDescription, "maxLines", 100)
                            .setDuration(950).start()
                    } else {
                        ObjectAnimator.ofInt(binding.mediaInfoDescription, "maxLines", 5)
                            .setDuration(400).start()
                    }
                }

                countDown(media, binding.mediaInfoContainer)
                val parent = _binding?.mediaInfoContainer!!
                val screenWidth = resources.displayMetrics.run { widthPixels / density }

                if (media.synonyms.isNotEmpty()) {
                    val bind = ItemTitleChipgroupBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    for (position in media.synonyms.indices) {
                        val chip = ItemChipBinding.inflate(
                            LayoutInflater.from(context),
                            bind.itemChipGroup,
                            false
                        ).root
                        chip.text = media.synonyms[position]
                        chip.setOnLongClickListener { copyToClipboard(media.synonyms[position]);true }
                        bind.itemChipGroup.addView(chip)
                    }
                    parent.addView(bind.root)
                }

                if (media.trailer != null && !offline) {
                    @Suppress("DEPRECATION")
                    class MyChrome : WebChromeClient() {
                        private var mCustomView: View? = null
                        private var mCustomViewCallback: CustomViewCallback? = null
                        private var mOriginalSystemUiVisibility = 0

                        override fun onHideCustomView() {
                            (requireActivity().window.decorView as FrameLayout).removeView(
                                mCustomView
                            )
                            mCustomView = null
                            requireActivity().window.decorView.systemUiVisibility =
                                mOriginalSystemUiVisibility
                            mCustomViewCallback!!.onCustomViewHidden()
                            mCustomViewCallback = null
                        }

                        override fun onShowCustomView(
                            paramView: View,
                            paramCustomViewCallback: CustomViewCallback
                        ) {
                            if (mCustomView != null) {
                                onHideCustomView()
                                return
                            }
                            mCustomView = paramView
                            mOriginalSystemUiVisibility =
                                requireActivity().window.decorView.systemUiVisibility
                            mCustomViewCallback = paramCustomViewCallback
                            (requireActivity().window.decorView as FrameLayout).addView(
                                mCustomView,
                                FrameLayout.LayoutParams(-1, -1)
                            )
                            requireActivity().window.decorView.systemUiVisibility =
                                3846 or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        }
                    }

                    val bind = ItemTitleTrailerBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    bind.mediaInfoTrailer.apply {
                        visibility = View.VISIBLE
                        settings.javaScriptEnabled = true
                        isSoundEffectsEnabled = true
                        webChromeClient = MyChrome()
                        loadUrl(media.trailer!!)
                    }
                    parent.addView(bind.root)
                }

                if (media.anime != null && (media.anime.op.isNotEmpty() || media.anime.ed.isNotEmpty()) && !offline) {
                    val markWon = Markwon.builder(requireContext())
                        .usePlugin(SoftBreakAddsNewLinePlugin.create()).build()

                    fun makeLink(a: String): String {
                        val first = a.indexOf('"').let { if (it != -1) it else return a } + 1
                        val end = a.indexOf('"', first).let { if (it != -1) it else return a }
                        val name = a.subSequence(first, end).toString()
                        return "${a.subSequence(0, first)}" +
                                "[$name](https://www.youtube.com/results?search_query=${
                                    URLEncoder.encode(
                                        name,
                                        "utf-8"
                                    )
                                })" +
                                "${a.subSequence(end, a.length)}"
                    }

                    fun makeText(textView: TextView, arr: ArrayList<String>) {
                        var op = ""
                        arr.forEach {
                            op += "\n"
                            op += makeLink(it)
                        }
                        op = op.removePrefix("\n")
                        textView.setOnClickListener {
                            if (textView.maxLines == 4) {
                                ObjectAnimator.ofInt(textView, "maxLines", 100)
                                    .setDuration(950).start()
                            } else {
                                ObjectAnimator.ofInt(textView, "maxLines", 4)
                                    .setDuration(400).start()
                            }
                        }
                        markWon.setMarkdown(textView, op)
                    }

                    if (media.anime.op.isNotEmpty()) {
                        val bind = ItemTitleTextBinding.inflate(
                            LayoutInflater.from(context),
                            parent,
                            false
                        )
                        bind.itemTitle.setText(R.string.opening)
                        makeText(bind.itemText, media.anime.op)
                        parent.addView(bind.root)
                    }


                    if (media.anime.ed.isNotEmpty()) {
                        val bind = ItemTitleTextBinding.inflate(
                            LayoutInflater.from(context),
                            parent,
                            false
                        )
                        bind.itemTitle.setText(R.string.ending)
                        makeText(bind.itemText, media.anime.ed)
                        parent.addView(bind.root)
                    }
                }

                if (media.genres.isNotEmpty() && !offline) {
                    val bind = ActivityGenreBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    val adapter = GenreAdapter(type)
                    genreModel.doneListener = {
                        MainScope().launch {
                            bind.mediaInfoGenresProgressBar.visibility = View.GONE
                        }
                    }
                    if (genreModel.genres != null) {
                        adapter.genres = genreModel.genres!!
                        adapter.pos = ArrayList(genreModel.genres!!.keys)
                        if (genreModel.done) genreModel.doneListener?.invoke()
                    }
                    bind.mediaInfoGenresRecyclerView.adapter = adapter
                    bind.mediaInfoGenresRecyclerView.layoutManager =
                        GridLayoutManager(requireActivity(), (screenWidth / 156f).toInt())

                    lifecycleScope.launch(Dispatchers.IO) {
                        genreModel.loadGenres(media.genres) {
                            MainScope().launch {
                                adapter.addGenre(it)
                            }
                        }
                    }
                    parent.addView(bind.root)
                }

                if (media.tags.isNotEmpty() && !offline) {
                    val bind = ItemTitleChipgroupBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    bind.itemTitle.setText(R.string.tags)
                    for (position in media.tags.indices) {
                        val chip = ItemChipBinding.inflate(
                            LayoutInflater.from(context),
                            bind.itemChipGroup,
                            false
                        ).root
                        chip.text = media.tags[position]
                        chip.setSafeOnClickListener {
                            ContextCompat.startActivity(
                                chip.context,
                                Intent(chip.context, SearchActivity::class.java)
                                    .putExtra("type", type)
                                    .putExtra("sortBy", Anilist.sortBy[2])
                                    .putExtra("tag", media.tags[position].substringBefore(" :"))
                                    .putExtra("search", true)
                                    .also {
                                        if (media.isAdult) {
                                            if (!Anilist.adult) Toast.makeText(
                                                chip.context,
                                                currActivity()?.getString(R.string.content_18),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            it.putExtra("hentai", true)
                                        }
                                    },
                                null
                            )
                        }
                        chip.setOnLongClickListener { copyToClipboard(media.tags[position]);true }
                        bind.itemChipGroup.addView(chip)
                    }
                    parent.addView(bind.root)
                }

                if (!media.relations.isNullOrEmpty() && !offline) {
                    if (media.sequel != null || media.prequel != null) {
                        val bind = ItemQuelsBinding.inflate(
                            LayoutInflater.from(context),
                            parent,
                            false
                        )

                        if (media.sequel != null) {
                            bind.mediaInfoSequel.visibility = View.VISIBLE
                            bind.mediaInfoSequelImage.loadImage(
                                media.sequel!!.banner ?: media.sequel!!.cover
                            )
                            bind.mediaInfoSequel.setSafeOnClickListener {
                                ContextCompat.startActivity(
                                    requireContext(),
                                    Intent(
                                        requireContext(),
                                        MediaDetailsActivity::class.java
                                    ).putExtra(
                                        "media",
                                        media.sequel as Serializable
                                    ), null
                                )
                            }
                        }
                        if (media.prequel != null) {
                            bind.mediaInfoPrequel.visibility = View.VISIBLE
                            bind.mediaInfoPrequelImage.loadImage(
                                media.prequel!!.banner ?: media.prequel!!.cover
                            )
                            bind.mediaInfoPrequel.setSafeOnClickListener {
                                ContextCompat.startActivity(
                                    requireContext(),
                                    Intent(
                                        requireContext(),
                                        MediaDetailsActivity::class.java
                                    ).putExtra(
                                        "media",
                                        media.prequel as Serializable
                                    ), null
                                )
                            }
                        }
                        parent.addView(bind.root)
                    }

                    val bindi = ItemTitleRecyclerBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )

                    bindi.itemRecycler.adapter =
                        MediaAdaptor(0, media.relations!!, requireActivity())
                    bindi.itemRecycler.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    parent.addView(bindi.root)
                }
                if (!media.characters.isNullOrEmpty() && !offline) {
                    val bind = ItemTitleRecyclerBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    bind.itemTitle.setText(R.string.characters)
                    bind.itemRecycler.adapter =
                        CharacterAdapter(media.characters!!)
                    bind.itemRecycler.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    parent.addView(bind.root)
                }
                if (!media.staff.isNullOrEmpty() && !offline) {
                    val bind = ItemTitleRecyclerBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    bind.itemTitle.setText(R.string.staff)
                    bind.itemRecycler.adapter =
                        AuthorAdapter(media.staff!!)
                    bind.itemRecycler.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    parent.addView(bind.root)
                }
                if (!media.recommendations.isNullOrEmpty() && !offline) {
                    val bind = ItemTitleRecyclerBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    bind.itemTitle.setText(R.string.recommended)
                    bind.itemRecycler.adapter =
                        MediaAdaptor(0, media.recommendations!!, requireActivity())
                    bind.itemRecycler.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    parent.addView(bind.root)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cornerTop = ObjectAnimator.ofFloat(binding.root, "radius", 0f, 32f).setDuration(200)
            val cornerNotTop =
                ObjectAnimator.ofFloat(binding.root, "radius", 32f, 0f).setDuration(200)
            var cornered = true
            cornerTop.start()
            binding.mediaInfoScroll.setOnScrollChangeListener { v, _, _, _, _ ->
                if (!v.canScrollVertically(-1)) {
                    if (!cornered) {
                        cornered = true
                        cornerTop.start()
                    }
                } else {
                    if (cornered) {
                        cornered = false
                        cornerNotTop.start()
                    }
                }
            }
        }
        super.onViewCreated(view, null)
    }

    override fun onResume() {
        binding.mediaInfoProgressBar.visibility = if (!loaded) View.VISIBLE else View.GONE
        super.onResume()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
