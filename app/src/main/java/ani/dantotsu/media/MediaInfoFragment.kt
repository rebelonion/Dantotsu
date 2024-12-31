package ani.dantotsu.media

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.GenresViewModel
import ani.dantotsu.copyToClipboard
import ani.dantotsu.currActivity
import ani.dantotsu.databinding.ActivityGenreBinding
import ani.dantotsu.databinding.FragmentMediaInfoBinding
import ani.dantotsu.databinding.ItemChipBinding
import ani.dantotsu.databinding.ItemQuelsBinding
import ani.dantotsu.databinding.ItemTitleChipgroupBinding
import ani.dantotsu.databinding.ItemTitleRecyclerBinding
import ani.dantotsu.databinding.ItemTitleTextBinding
import ani.dantotsu.databinding.ItemTitleTrailerBinding
import ani.dantotsu.displayTimer
import ani.dantotsu.isOnline
import ani.dantotsu.loadImage
import ani.dantotsu.navBarHeight
import ani.dantotsu.profile.User
import ani.dantotsu.px
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import com.xwray.groupie.GroupieAdapter
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.Serializable
import java.net.URLEncoder


class MediaInfoFragment : Fragment() {
    private var _binding: FragmentMediaInfoBinding? = null
    private val binding get() = _binding!!
    private var timer: CountDownTimer? = null
    private var loaded = false
    private var type = "ANIME"
    private val genreModel: GenresViewModel by activityViewModels()

    private val tripleTab = "\t\t\t"

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
        val offline: Boolean =
            PrefManager.getVal(PrefName.OfflineMode) || !isOnline(requireContext())
        binding.mediaInfoProgressBar.isGone = loaded
        binding.mediaInfoContainer.isVisible = loaded
        binding.mediaInfoContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += 128f.px + navBarHeight }

        model.scrolledToTop.observe(viewLifecycleOwner) {
            if (it) binding.mediaInfoScroll.scrollTo(0, 0)
        }

        model.getMedia().observe(viewLifecycleOwner) { media ->
            if (media != null && !loaded) {
                loaded = true

                binding.mediaInfoProgressBar.visibility = View.GONE
                binding.mediaInfoContainer.visibility = View.VISIBLE
                val infoName = tripleTab + (media.name ?: media.nameRomaji)
                binding.mediaInfoName.text = infoName
                binding.mediaInfoName.setOnLongClickListener {
                    copyToClipboard(media.name ?: media.nameRomaji)
                    true
                }
                if (media.name != null) binding.mediaInfoNameRomajiContainer.visibility =
                    View.VISIBLE
                val infoNameRomaji = tripleTab + media.nameRomaji
                binding.mediaInfoNameRomaji.text = infoNameRomaji
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
                binding.mediaInfoPopularity.text = media.popularity.toString()
                binding.mediaInfoFavorites.text = media.favourites.toString()
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
                    val seasonInfo =
                        "${(media.anime.season ?: "??")} ${(media.anime.seasonYear ?: "??")}"
                    binding.mediaInfoSeason.text = seasonInfo

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
                    val infoTotal = if (media.anime.nextAiringEpisode != null)
                        "${media.anime.nextAiringEpisode} | ${media.anime.totalEpisodes ?: "~"}"
                    else
                        (media.anime.totalEpisodes ?: "~").toString()
                    binding.mediaInfoTotal.text = infoTotal

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
                val infoDesc =
                    tripleTab + if (desc.toString() != "null") desc else getString(R.string.no_description_available)
                binding.mediaInfoDescription.text = infoDesc

                binding.mediaInfoDescription.setOnClickListener {
                    if (binding.mediaInfoDescription.maxLines == 5) {
                        ObjectAnimator.ofInt(binding.mediaInfoDescription, "maxLines", 100)
                            .setDuration(950).start()
                    } else {
                        ObjectAnimator.ofInt(binding.mediaInfoDescription, "maxLines", 5)
                            .setDuration(400).start()
                    }
                }
                displayTimer(media, binding.mediaInfoContainer)
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
                if (!media.users.isNullOrEmpty() && !offline) {
                    val users: ArrayList<User> = media.users ?: arrayListOf()
                    if (Anilist.token != null && media.userStatus != null) {
                        users.add(
                            0,
                            User(
                                id = Anilist.userid!!,
                                name = getString(R.string.you),
                                pfp = Anilist.avatar,
                                banner = "",
                                status = media.userStatus,
                                score = media.userScore.toFloat(),
                                progress = media.userProgress,
                                totalEpisodes = media.anime?.totalEpisodes
                                    ?: media.manga?.totalChapters,
                                nextAiringEpisode = media.anime?.nextAiringEpisode
                            )
                        )
                    }
                    ItemTitleRecyclerBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    ).apply {
                        itemTitle.visibility = View.GONE
                        itemRecycler.adapter =
                            MediaSocialAdapter(users, type, requireActivity())
                        itemRecycler.layoutManager = LinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                        parent.addView(root)
                    }
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
                        ItemQuelsBinding.inflate(
                            LayoutInflater.from(context),
                            parent,
                            false
                        ).apply {

                            if (media.sequel != null) {
                                mediaInfoSequel.visibility = View.VISIBLE
                                mediaInfoSequelImage.loadImage(
                                    media.sequel!!.banner ?: media.sequel!!.cover
                                )
                                mediaInfoSequel.setSafeOnClickListener {
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
                                mediaInfoPrequel.visibility = View.VISIBLE
                                mediaInfoPrequelImage.loadImage(
                                    media.prequel!!.banner ?: media.prequel!!.cover
                                )
                                mediaInfoPrequel.setSafeOnClickListener {
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
                            parent.addView(root)
                        }
                    }

                    if (!media.review.isNullOrEmpty()) {
                        ItemTitleRecyclerBinding.inflate(
                            LayoutInflater.from(context),
                            parent,
                            false
                        ).apply {
                            val adapter = GroupieAdapter()
                            media.review!!.forEach { adapter.add(ReviewAdapter(it)) }
                            itemTitle.setText(R.string.reviews)
                            itemRecycler.adapter = adapter
                            itemRecycler.layoutManager = LinearLayoutManager(requireContext())
                            itemMore.visibility = View.VISIBLE
                            itemMore.setSafeOnClickListener {
                                startActivity(
                                    Intent(requireContext(), ReviewActivity::class.java)
                                        .putExtra("mediaId", media.id)
                                )
                            }
                            parent.addView(root)
                        }
                    }

                    ItemTitleRecyclerBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    ).apply {

                        itemRecycler.adapter =
                            MediaAdaptor(0, media.relations!!, requireActivity())
                        itemRecycler.layoutManager = LinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                        parent.addView(root)
                    }
                }
                if (!media.characters.isNullOrEmpty() && !offline) {
                    ItemTitleRecyclerBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    ).apply {
                        itemTitle.setText(R.string.characters)
                        itemRecycler.adapter =
                            CharacterAdapter(media.characters!!)
                        itemRecycler.layoutManager = LinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                        parent.addView(root)
                    }
                }
                if (!media.staff.isNullOrEmpty() && !offline) {
                    ItemTitleRecyclerBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    ).apply {
                        itemTitle.setText(R.string.staff)
                        itemRecycler.adapter =
                            AuthorAdapter(media.staff!!)
                        itemRecycler.layoutManager = LinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                        parent.addView(root)
                    }
                }
                if (!media.recommendations.isNullOrEmpty() && !offline) {
                    ItemTitleRecyclerBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    ).apply {
                        itemTitle.setText(R.string.recommended)
                        itemRecycler.adapter =
                            MediaAdaptor(0, media.recommendations!!, requireActivity())
                        itemRecycler.layoutManager = LinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                        parent.addView(root)
                    }
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
        binding.mediaInfoProgressBar.isGone = loaded
        super.onResume()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
