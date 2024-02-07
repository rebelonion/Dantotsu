package ani.dantotsu.home

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LayoutAnimationController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.MediaPageTransformer
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.currContext
import ani.dantotsu.databinding.ItemAnimePageBinding
import ani.dantotsu.loadImage
import ani.dantotsu.media.CalendarActivity
import ani.dantotsu.media.GenreActivity
import ani.dantotsu.media.MediaAdaptor
import ani.dantotsu.media.SearchActivity
import ani.dantotsu.px
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.setSlideIn
import ani.dantotsu.setSlideUp
import ani.dantotsu.settings.SettingsDialogFragment
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout

class AnimePageAdapter : RecyclerView.Adapter<AnimePageAdapter.AnimePageViewHolder>() {
    val ready = MutableLiveData(false)
    lateinit var binding: ItemAnimePageBinding
    private var trendHandler: Handler? = null
    private lateinit var trendRun: Runnable
    var trendingViewPager: ViewPager2? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimePageViewHolder {
        val binding =
            ItemAnimePageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimePageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnimePageViewHolder, position: Int) {
        binding = holder.binding
        trendingViewPager = binding.animeTrendingViewPager

        val textInputLayout = holder.itemView.findViewById<TextInputLayout>(R.id.animeSearchBar)
        val currentColor = textInputLayout.boxBackgroundColor
        val semiTransparentColor = (currentColor and 0x00FFFFFF) or 0xA8000000.toInt()
        textInputLayout.boxBackgroundColor = semiTransparentColor
        val materialCardView =
            holder.itemView.findViewById<MaterialCardView>(R.id.animeUserAvatarContainer)
        materialCardView.setCardBackgroundColor(semiTransparentColor)
        val typedValue = TypedValue()
        currContext()?.theme?.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
        val color = typedValue.data

        textInputLayout.boxBackgroundColor = (color and 0x00FFFFFF) or 0x28000000
        materialCardView.setCardBackgroundColor((color and 0x00FFFFFF) or 0x28000000)

        binding.animeTitleContainer.updatePadding(top = statusBarHeight)

        if (PrefManager.getVal(PrefName.SmallView)) binding.animeTrendingContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = (-108f).px
        }

        updateAvatar()

        binding.animeSearchBar.hint = "ANIME"
        binding.animeSearchBarText.setOnClickListener {
            ContextCompat.startActivity(
                it.context,
                Intent(it.context, SearchActivity::class.java).putExtra("type", "ANIME"),
                null
            )
        }

        binding.animeSearchBar.setEndIconOnClickListener {
            binding.animeSearchBarText.performClick()
        }

        binding.animeUserAvatar.setSafeOnClickListener {
            val dialogFragment =
                SettingsDialogFragment.newInstance(SettingsDialogFragment.Companion.PageType.ANIME)
            dialogFragment.show((it.context as AppCompatActivity).supportFragmentManager, "dialog")
        }

        listOf(
            binding.animePreviousSeason,
            binding.animeThisSeason,
            binding.animeNextSeason
        ).forEachIndexed { i, it ->
            it.setSafeOnClickListener { onSeasonClick.invoke(i) }
            it.setOnLongClickListener { onSeasonLongClick.invoke(i) }
        }

        binding.animeGenreImage.loadImage("https://s4.anilist.co/file/anilistcdn/media/anime/banner/16498-8jpFCOcDmneX.jpg")
        binding.animeCalendarImage.loadImage("https://s4.anilist.co/file/anilistcdn/media/anime/banner/125367-hGPJLSNfprO3.jpg")

        binding.animeGenre.setOnClickListener {
            ContextCompat.startActivity(
                it.context,
                Intent(it.context, GenreActivity::class.java).putExtra("type", "ANIME"),
                null
            )
        }
        binding.animeCalendar.setOnClickListener {
            ContextCompat.startActivity(
                it.context,
                Intent(it.context, CalendarActivity::class.java),
                null
            )
        }

        binding.animeIncludeList.visibility =
            if (Anilist.userid != null) View.VISIBLE else View.GONE

        binding.animeIncludeList.isChecked = PrefManager.getVal(PrefName.PopularAnimeList)

        binding.animeIncludeList.setOnCheckedChangeListener { _, isChecked ->
            onIncludeListClick.invoke(isChecked)

            PrefManager.setVal(PrefName.PopularAnimeList, isChecked)
        }
        if (ready.value == false)
            ready.postValue(true)
    }

    lateinit var onSeasonClick: ((Int) -> Unit)
    lateinit var onSeasonLongClick: ((Int) -> Boolean)
    lateinit var onIncludeListClick: ((Boolean) -> Unit)

    override fun getItemCount(): Int = 1

    fun updateHeight() {
        trendingViewPager!!.updateLayoutParams { height += statusBarHeight }
    }

    fun updateTrending(adaptor: MediaAdaptor) {
        binding.animeTrendingProgressBar.visibility = View.GONE
        binding.animeTrendingViewPager.adapter = adaptor
        binding.animeTrendingViewPager.offscreenPageLimit = 3
        binding.animeTrendingViewPager.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        binding.animeTrendingViewPager.setPageTransformer(MediaPageTransformer())

        trendHandler = Handler(Looper.getMainLooper())
        trendRun = Runnable {
            binding.animeTrendingViewPager.currentItem =
                binding.animeTrendingViewPager.currentItem + 1
        }
        binding.animeTrendingViewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    trendHandler!!.removeCallbacks(trendRun)
                    trendHandler!!.postDelayed(trendRun, 4000)
                }
            }
        )

        binding.animeTrendingViewPager.layoutAnimation =
            LayoutAnimationController(setSlideIn(), 0.25f)
        binding.animeTitleContainer.startAnimation(setSlideUp())
        binding.animeListContainer.layoutAnimation =
            LayoutAnimationController(setSlideIn(), 0.25f)
        binding.animeSeasonsCont.layoutAnimation =
            LayoutAnimationController(setSlideIn(), 0.25f)
    }

    fun updateRecent(adaptor: MediaAdaptor) {
        binding.animeUpdatedProgressBar.visibility = View.GONE
        binding.animeUpdatedRecyclerView.adapter = adaptor
        binding.animeUpdatedRecyclerView.layoutManager =
            LinearLayoutManager(
                binding.animeUpdatedRecyclerView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        binding.animeUpdatedRecyclerView.visibility = View.VISIBLE

        binding.animeRecently.visibility = View.VISIBLE
        binding.animeRecently.startAnimation(setSlideUp())
        binding.animeUpdatedRecyclerView.layoutAnimation =
            LayoutAnimationController(setSlideIn(), 0.25f)
        binding.animePopular.visibility = View.VISIBLE
        binding.animePopular.startAnimation(setSlideUp())
    }

    fun updateAvatar() {
        if (Anilist.avatar != null && ready.value == true) {
            binding.animeUserAvatar.loadImage(Anilist.avatar)
            binding.animeUserAvatar.imageTintList = null
        }
    }

    inner class AnimePageViewHolder(val binding: ItemAnimePageBinding) :
        RecyclerView.ViewHolder(binding.root)
}
