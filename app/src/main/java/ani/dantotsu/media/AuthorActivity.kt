package ani.dantotsu.media

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils.clamp
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.EmptyAdapter
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.AnilistMutations
import ani.dantotsu.databinding.ActivityCharacterBinding
import ani.dantotsu.initActivity
import ani.dantotsu.loadImage
import ani.dantotsu.navBarHeight
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.others.ImageViewDialog
import ani.dantotsu.others.SpoilerPlugin
import ani.dantotsu.others.getSerialized
import ani.dantotsu.px
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import com.google.android.material.appbar.AppBarLayout
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class AuthorActivity : AppCompatActivity(), AppBarLayout.OnOffsetChangedListener {
    private lateinit var binding: ActivityCharacterBinding
    private val scope = lifecycleScope
    private val model: OtherDetailsViewModel by viewModels()
    private lateinit var author: Author
    private var loaded = false

    private var screenWidth: Float = 0f
    private val percent = 30
    private var mMaxScrollSize = 0
    private var isCollapsed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()
        binding = ActivityCharacterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)
        screenWidth = resources.displayMetrics.run { widthPixels / density }
        if (PrefManager.getVal(PrefName.ImmersiveMode)) this.window.statusBarColor =
            ContextCompat.getColor(this, R.color.transparent)

        val banner =
            if (PrefManager.getVal(PrefName.BannerAnimations)) binding.characterBanner else binding.characterBannerNoKen

        banner.updateLayoutParams { height += statusBarHeight }
        binding.characterClose.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
        binding.characterCollapsing.minimumHeight = statusBarHeight
        binding.characterCover.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
        binding.characterRecyclerView.updatePadding(bottom = 64f.px + navBarHeight)
        binding.characterTitle.isSelected = true
        binding.characterAppBar.addOnOffsetChangedListener(this)

        binding.characterClose.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        author = intent.getSerialized("author") ?: return
        binding.characterTitle.text = author.name
        binding.characterCoverImage.loadImage(author.image)
        binding.characterCoverImage.setOnLongClickListener {
            ImageViewDialog.newInstance(
                this,
                author.name,
                author.image
            )
        }
        val link = "https://anilist.co/staff/${author.id}"
        binding.characterShare.setOnClickListener {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_TEXT, link)
            startActivity(Intent.createChooser(i, author.name))
        }
        binding.characterShare.setOnLongClickListener {
            openLinkInBrowser(link)
            true
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                author.isFav =
                    Anilist.query.isUserFav(AnilistMutations.FavType.STAFF, author.id)
            }
            withContext(Dispatchers.Main) {
                binding.characterFav.setImageResource(
                    if (author.isFav) R.drawable.ic_round_favorite_24 else R.drawable.ic_round_favorite_border_24
                )
            }
        }
        binding.characterFav.setOnClickListener {
            scope.launch {
                lifecycleScope.launch {
                    if (Anilist.mutation.toggleFav(AnilistMutations.FavType.CHARACTER, author.id)) {
                        author.isFav = !author.isFav
                        binding.characterFav.setImageResource(
                            if (author.isFav) R.drawable.ic_round_favorite_24 else R.drawable.ic_round_favorite_border_24
                        )
                    } else {
                        snackString("Failed to toggle favorite")
                    }
                }
            }
        }
        model.getAuthor().observe(this) {
            if (it != null) {
                author = it
                loaded = true
                binding.characterProgress.visibility = View.GONE
                binding.characterRecyclerView.visibility = View.VISIBLE
                if (author.yearMedia.isNullOrEmpty()) {
                    binding.characterRecyclerView.visibility = View.GONE
                }
                val titlePosition = arrayListOf<Int>()
                val concatAdapter = ConcatAdapter()
                val map = author.yearMedia ?: return@observe
                val keys = map.keys.toTypedArray()
                var pos = 0

                val gridSize = (screenWidth / 124f).toInt()
                val gridLayoutManager = GridLayoutManager(this, gridSize)
                gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (position in titlePosition) {
                            true -> gridSize
                            else -> 1
                        }
                    }
                }
                val desc = createDesc(author)
                val markWon = Markwon.builder(this).usePlugin(SoftBreakAddsNewLinePlugin.create())
                    .usePlugin(SpoilerPlugin()).build()
                markWon.setMarkdown(binding.authorCharacterDesc, desc)
                for (i in keys.indices) {
                    val medias = map[keys[i]]!!
                    val empty = if (medias.size >= 4) medias.size % 4 else 4 - medias.size
                    titlePosition.add(pos)
                    pos += (empty + medias.size + 1)

                    concatAdapter.addAdapter(TitleAdapter("${keys[i]} (${medias.size})"))
                    concatAdapter.addAdapter(MediaAdaptor(0, medias, this, true))
                    concatAdapter.addAdapter(EmptyAdapter(empty))
                }
                binding.characterRecyclerView.adapter = concatAdapter
                binding.characterRecyclerView.layoutManager = gridLayoutManager

                binding.authorCharactersRecycler.visibility = View.VISIBLE
                binding.AuthorCharactersText.visibility = View.VISIBLE
                binding.authorCharactersRecycler.adapter =
                    CharacterAdapter(author.character ?: arrayListOf())
                binding.authorCharactersRecycler.layoutManager =
                    LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                if (author.character.isNullOrEmpty()) {
                    binding.authorCharactersRecycler.visibility = View.GONE
                    binding.AuthorCharactersText.visibility = View.GONE
                }
            }
        }
        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(this) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO) { model.loadAuthor(author) }
                    live.postValue(false)
                }
            }
        }
    }

    private fun createDesc(author: Author): String {
        val age = if (author.age != null) "${getString(R.string.age)} ${author.age}" else ""
        val yearsActive =
            if (author.yearsActive != null) "${getString(R.string.years_active)} ${author.yearsActive}" else ""
        val dob =
            if (author.dateOfBirth != null) "${getString(R.string.birthday)} ${author.dateOfBirth}" else ""
        val homeTown =
            if (author.homeTown != null) "${getString(R.string.hometown)} ${author.homeTown}" else ""
        val dod =
            if (author.dateOfDeath != null) "${getString(R.string.date_of_death)} ${author.dateOfDeath}" else ""

        return "$age $yearsActive $dob $homeTown $dod"
    }


    override fun onDestroy() {
        if (Refresh.activity.containsKey(this.hashCode())) {
            Refresh.activity.remove(this.hashCode())
        }
        super.onDestroy()
    }

    override fun onResume() {
        binding.characterProgress.visibility = if (!loaded) View.VISIBLE else View.GONE
        super.onResume()
    }

    override fun onOffsetChanged(appBar: AppBarLayout, i: Int) {
        if (mMaxScrollSize == 0) mMaxScrollSize = appBar.totalScrollRange
        val percentage = abs(i) * 100 / mMaxScrollSize
        val cap = clamp((percent - percentage) / percent.toFloat(), 0f, 1f)

        binding.characterCover.scaleX = 1f * cap
        binding.characterCover.scaleY = 1f * cap
        binding.characterCover.cardElevation = 32f * cap

        binding.characterCover.visibility =
            if (binding.characterCover.scaleX == 0f) View.GONE else View.VISIBLE
        val immersiveMode: Boolean = PrefManager.getVal(PrefName.ImmersiveMode)
        if (percentage >= percent && !isCollapsed) {
            isCollapsed = true
            if (immersiveMode) this.window.statusBarColor =
                ContextCompat.getColor(this, R.color.nav_bg)
        }
        if (percentage <= percent && isCollapsed) {
            isCollapsed = false
            if (immersiveMode) this.window.statusBarColor =
                ContextCompat.getColor(this, R.color.transparent)
        }
    }
}