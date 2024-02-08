package ani.dantotsu.media.manga.mangareader

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.KeyEvent.*
import android.view.animation.OvershootInterpolator
import android.widget.AdapterView
import android.widget.CheckBox
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.math.MathUtils.clamp
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.*
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.connections.discord.Discord
import ani.dantotsu.connections.discord.DiscordService
import ani.dantotsu.connections.discord.DiscordServiceRunningSingleton
import ani.dantotsu.connections.discord.RPC
import ani.dantotsu.connections.updateProgress
import ani.dantotsu.databinding.ActivityMangaReaderBinding
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsViewModel
import ani.dantotsu.media.MediaSingleton
import ani.dantotsu.media.manga.MangaCache
import ani.dantotsu.media.manga.MangaChapter
import ani.dantotsu.media.manga.MangaNameAdapter
import ani.dantotsu.others.ImageViewDialog
import ani.dantotsu.parsers.HMangaSources
import ani.dantotsu.parsers.MangaImage
import ani.dantotsu.parsers.MangaSources
import ani.dantotsu.settings.CurrentReaderSettings
import ani.dantotsu.settings.CurrentReaderSettings.Companion.applyWebtoon
import ani.dantotsu.settings.CurrentReaderSettings.Directions.*
import ani.dantotsu.settings.CurrentReaderSettings.DualPageModes.*
import ani.dantotsu.settings.CurrentReaderSettings.Layouts.*
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.themes.ThemeManager
import com.alexvasilkov.gestures.views.GestureFrameLayout
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*
import kotlin.math.min
import kotlin.properties.Delegates

@SuppressLint("SetTextI18n")
class MangaReaderActivity : AppCompatActivity() {
    private val mangaCache = Injekt.get<MangaCache>()

    private lateinit var binding: ActivityMangaReaderBinding
    private val model: MediaDetailsViewModel by viewModels()
    private val scope = lifecycleScope

    var defaultSettings = CurrentReaderSettings()

    private lateinit var media: Media
    private lateinit var chapter: MangaChapter
    private lateinit var chapters: MutableMap<String, MangaChapter>
    private lateinit var chaptersArr: List<String>
    private lateinit var chaptersTitleArr: ArrayList<String>
    private var currentChapterIndex = 0

    private var isContVisible = false
    private var showProgressDialog = true

    private var maxChapterPage = 0L
    private var currentChapterPage = 0L

    private var notchHeight: Int? = null

    private var imageAdapter: BaseImageAdapter? = null

    var sliding = false
    var isAnimating = false

    override fun onAttachedToWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !PrefManager.getVal<Boolean>(PrefName.ShowSystemBars)) {
            val displayCutout = window.decorView.rootWindowInsets.displayCutout
            if (displayCutout != null) {
                if (displayCutout.boundingRects.size > 0) {
                    notchHeight = min(
                        displayCutout.boundingRects[0].width(),
                        displayCutout.boundingRects[0].height()
                    )
                    checkNotch()
                }
            }
        }
        super.onAttachedToWindow()
    }

    private fun checkNotch() {
        binding.mangaReaderTopLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = notchHeight ?: return
        }
    }

    private fun hideBars() {
        if (!PrefManager.getVal<Boolean>(PrefName.ShowSystemBars)) hideSystemBars()
    }

    override fun onDestroy() {
        mangaCache.clear()
        if (DiscordServiceRunningSingleton.running) {
            DiscordServiceRunningSingleton.running = false
            val stopIntent = Intent(this, DiscordService::class.java)
            stopService(stopIntent)
        }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        binding = ActivityMangaReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mangaReaderBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        defaultSettings = loadReaderSettings("reader_settings") ?: defaultSettings

        onBackPressedDispatcher.addCallback(this) {
            progress { finish() }
        }

        controllerDuration = (PrefManager.getVal<Float>(PrefName.AnimationSpeed) * 200).toLong()

        hideBars()

        var pageSliderTimer = Timer()
        fun pageSliderHide() {
            pageSliderTimer.cancel()
            pageSliderTimer.purge()
            val timerTask: TimerTask = object : TimerTask() {
                override fun run() {
                    binding.mangaReaderCont.post {
                        sliding = false
                        handleController(false)
                    }
                }
            }
            pageSliderTimer = Timer()
            pageSliderTimer.schedule(timerTask, 3000)
        }

        binding.mangaReaderSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                sliding = true
                if (defaultSettings.layout != PAGED)
                    binding.mangaReaderRecycler.scrollToPosition((value.toInt() - 1) / (dualPage { 2 }
                        ?: 1))
                else
                    binding.mangaReaderPager.currentItem =
                        (value.toInt() - 1) / (dualPage { 2 } ?: 1)
                pageSliderHide()
            }
        }

        media = if (model.getMedia().value == null)
            try {
                //(intent.getSerialized("media")) ?: return
                MediaSingleton.media ?: return
            } catch (e: Exception) {
                logError(e)
                return
            } finally {
                MediaSingleton.media = null
            }
        else model.getMedia().value ?: return
        model.setMedia(media)

        if (PrefManager.getVal(PrefName.AutoDetectWebtoon) && media.countryOfOrigin != "JP") applyWebtoon(
            defaultSettings
        )
        defaultSettings = loadReaderSettings("${media.id}_current_settings") ?: defaultSettings

        chapters = media.manga?.chapters ?: return
        chapter = chapters[media.manga!!.selectedChapter] ?: return

        model.mangaReadSources = if (media.isAdult) HMangaSources else MangaSources
        binding.mangaReaderSource.visibility =
            if (PrefManager.getVal(PrefName.ShowSource)) View.VISIBLE else View.GONE
        if (model.mangaReadSources!!.names.isEmpty()) {
            //try to reload sources
            try {
                val mangaSources = MangaSources
                val scope = lifecycleScope
                scope.launch(Dispatchers.IO) {
                    mangaSources.init(
                        Injekt.get<MangaExtensionManager>().installedExtensionsFlow
                    )
                }
                model.mangaReadSources = mangaSources
            } catch (e: Exception) {
                Injekt.get<CrashlyticsInterface>().logException(e)
                logError(e)
            }
        }
        //check that index is not out of bounds (crash fix)
        if (media.selected!!.sourceIndex >= model.mangaReadSources!!.names.size) {
            media.selected!!.sourceIndex = 0
        }
        binding.mangaReaderSource.text =
            model.mangaReadSources!!.names[media.selected!!.sourceIndex]

        binding.mangaReaderTitle.text = media.userPreferredName

        chaptersArr = chapters.keys.toList()
        currentChapterIndex = chaptersArr.indexOf(media.manga!!.selectedChapter)

        chaptersTitleArr = arrayListOf()
        chapters.forEach {
            val chapter = it.value
            chaptersTitleArr.add("${if (!chapter.title.isNullOrEmpty() && chapter.title != "null") "" else "Chapter "}${chapter.number}${if (!chapter.title.isNullOrEmpty() && chapter.title != "null") " : " + chapter.title else ""}")
        }

        showProgressDialog =
            if (PrefManager.getVal(PrefName.AskIndividualReader)) PrefManager.getCustomVal(
                "${media.id}_progressDialog",
                true
            ) else false

        //Chapter Change
        fun change(index: Int) {
            mangaCache.clear()
            PrefManager.setCustomVal(
                "${media.id}_${chaptersArr[currentChapterIndex]}",
                currentChapterPage
            )
            ChapterLoaderDialog.newInstance(chapters[chaptersArr[index]]!!)
                .show(supportFragmentManager, "dialog")
        }

        //ChapterSelector
        binding.mangaReaderChapterSelect.adapter =
            NoPaddingArrayAdapter(this, R.layout.item_dropdown, chaptersTitleArr)
        binding.mangaReaderChapterSelect.setSelection(currentChapterIndex)
        binding.mangaReaderChapterSelect.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    position: Int,
                    p3: Long
                ) {
                    if (position != currentChapterIndex) change(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        binding.mangaReaderSettings.setSafeOnClickListener {
            ReaderSettingsDialogFragment.newInstance().show(supportFragmentManager, "settings")
        }

        //Next Chapter
        binding.mangaReaderNextChap.setOnClickListener {
            binding.mangaReaderNextChapter.performClick()
        }
        binding.mangaReaderNextChapter.setOnClickListener {
            if (chaptersArr.size > currentChapterIndex + 1) progress { change(currentChapterIndex + 1) }
            else snackString(getString(R.string.next_chapter_not_found))
        }
        //Prev Chapter
        binding.mangaReaderPrevChap.setOnClickListener {
            binding.mangaReaderPreviousChapter.performClick()
        }
        binding.mangaReaderPreviousChapter.setOnClickListener {
            if (currentChapterIndex > 0) change(currentChapterIndex - 1)
            else snackString(getString(R.string.first_chapter))
        }

        model.getMangaChapter().observe(this) { chap ->
            if (chap != null) {
                chapter = chap
                media.manga!!.selectedChapter = chapter.number
                media.selected = model.loadSelected(media)
                PrefManager.setCustomVal("${media.id}_current_chp", chap.number)
                currentChapterIndex = chaptersArr.indexOf(chap.number)
                binding.mangaReaderChapterSelect.setSelection(currentChapterIndex)
                binding.mangaReaderNextChap.text =
                    chaptersTitleArr.getOrNull(currentChapterIndex + 1) ?: ""
                binding.mangaReaderPrevChap.text =
                    chaptersTitleArr.getOrNull(currentChapterIndex - 1) ?: ""
                applySettings()
                val context = this
                val offline: Boolean = PrefManager.getVal(PrefName.OfflineMode)
                val incognito: Boolean = PrefManager.getVal(PrefName.Incognito)
                if ((isOnline(context) && !offline) && Discord.token != null && !incognito) {
                    lifecycleScope.launch {
                        val presence = RPC.createPresence(
                            RPC.Companion.RPCData(
                                applicationId = Discord.application_Id,
                                type = RPC.Type.WATCHING,
                                activityName = media.userPreferredName,
                                details = chap.title?.takeIf { it.isNotEmpty() }
                                    ?: getString(R.string.chapter_num, chap.number),
                                state = "${chap.number}/${media.manga?.totalChapters ?: "??"}",
                                largeImage = media.cover?.let { cover ->
                                    RPC.Link(media.userPreferredName, cover)
                                },
                                smallImage = RPC.Link(
                                    "Dantotsu",
                                    Discord.small_Image
                                ),
                                buttons = mutableListOf(
                                    RPC.Link(getString(R.string.view_manga), media.shareLink ?: ""),
                                    RPC.Link(
                                        "Stream on Dantotsu",
                                        "https://github.com/rebelonion/Dantotsu/"
                                    )
                                )
                            )
                        )
                        val intent = Intent(context, DiscordService::class.java).apply {
                            putExtra("presence", presence)
                        }
                        DiscordServiceRunningSingleton.running = true
                        startService(intent)
                    }
                }
            }
        }

        scope.launch(Dispatchers.IO) {
            model.loadMangaChapterImages(
                chapter,
                media.selected!!,
                media.mainName()
            )
        }
    }

    private val snapHelper = PagerSnapHelper()

    fun <T> dualPage(callback: () -> T): T? {
        return when (defaultSettings.dualPageMode) {
            No -> null
            Automatic -> {
                val orientation = resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) callback.invoke()
                else null
            }

            Force -> callback.invoke()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun applySettings() {

        saveReaderSettings("${media.id}_current_settings", defaultSettings)
        hideBars()

        //true colors
        SubsamplingScaleImageView.setPreferredBitmapConfig(
            if (defaultSettings.trueColors) Bitmap.Config.ARGB_8888
            else Bitmap.Config.RGB_565
        )

        //keep screen On
        if (defaultSettings.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.mangaReaderPager.unregisterOnPageChangeCallback(pageChangeCallback)

        currentChapterPage = PrefManager.getCustomVal("${media.id}_${chapter.number}", 1L)

        val chapImages = chapter.images()

        maxChapterPage = 0
        if (chapImages.isNotEmpty()) {
            maxChapterPage = chapImages.size.toLong()
            PrefManager.setCustomVal("${media.id}_${chapter.number}_max", maxChapterPage)

            imageAdapter =
                dualPage { DualPageAdapter(this, chapter) } ?: ImageAdapter(this, chapter)

            if (chapImages.size > 1) {
                binding.mangaReaderSlider.apply {
                    visibility = View.VISIBLE
                    valueTo = maxChapterPage.toFloat()
                    value = clamp(currentChapterPage.toFloat(), 1f, valueTo)
                }
            } else {
                binding.mangaReaderSlider.visibility = View.GONE
            }
            binding.mangaReaderPageNumber.text =
                if (defaultSettings.hidePageNumbers) "" else "${currentChapterPage}/$maxChapterPage"

        }

        val currentPage = currentChapterPage.toInt()

        if ((defaultSettings.direction == TOP_TO_BOTTOM || defaultSettings.direction == BOTTOM_TO_TOP)) {
            binding.mangaReaderSwipy.vertical = true
            if (defaultSettings.direction == TOP_TO_BOTTOM) {
                binding.BottomSwipeText.text = chaptersTitleArr.getOrNull(currentChapterIndex + 1)
                    ?: getString(R.string.no_chapter)
                binding.TopSwipeText.text = chaptersTitleArr.getOrNull(currentChapterIndex - 1)
                    ?: getString(R.string.no_chapter)
                binding.mangaReaderSwipy.onTopSwiped = {
                    binding.mangaReaderPreviousChapter.performClick()
                }
                binding.mangaReaderSwipy.onBottomSwiped = {
                    binding.mangaReaderNextChapter.performClick()
                }
            } else {
                binding.BottomSwipeText.text = chaptersTitleArr.getOrNull(currentChapterIndex - 1)
                    ?: getString(R.string.no_chapter)
                binding.TopSwipeText.text = chaptersTitleArr.getOrNull(currentChapterIndex + 1)
                    ?: getString(R.string.no_chapter)
                binding.mangaReaderSwipy.onTopSwiped = {
                    binding.mangaReaderNextChapter.performClick()
                }
                binding.mangaReaderSwipy.onBottomSwiped = {
                    binding.mangaReaderPreviousChapter.performClick()
                }
            }
            binding.mangaReaderSwipy.topBeingSwiped = { value ->
                binding.TopSwipeContainer.apply {
                    alpha = value
                    translationY = -height.dp * (1 - min(value, 1f))
                }
            }
            binding.mangaReaderSwipy.bottomBeingSwiped = { value ->
                binding.BottomSwipeContainer.apply {
                    alpha = value
                    translationY = height.dp * (1 - min(value, 1f))
                }
            }
        } else {
            binding.mangaReaderSwipy.vertical = false
            if (defaultSettings.direction == RIGHT_TO_LEFT) {
                binding.LeftSwipeText.text = chaptersTitleArr.getOrNull(currentChapterIndex + 1)
                    ?: getString(R.string.no_chapter)
                binding.RightSwipeText.text = chaptersTitleArr.getOrNull(currentChapterIndex - 1)
                    ?: getString(R.string.no_chapter)
                binding.mangaReaderSwipy.onLeftSwiped = {
                    binding.mangaReaderNextChapter.performClick()
                }
                binding.mangaReaderSwipy.onRightSwiped = {
                    binding.mangaReaderPreviousChapter.performClick()
                }
            } else {
                binding.LeftSwipeText.text = chaptersTitleArr.getOrNull(currentChapterIndex - 1)
                    ?: getString(R.string.no_chapter)
                binding.RightSwipeText.text = chaptersTitleArr.getOrNull(currentChapterIndex + 1)
                    ?: getString(R.string.no_chapter)
                binding.mangaReaderSwipy.onLeftSwiped = {
                    binding.mangaReaderPreviousChapter.performClick()
                }
                binding.mangaReaderSwipy.onRightSwiped = {
                    binding.mangaReaderNextChapter.performClick()
                }
            }
            binding.mangaReaderSwipy.leftBeingSwiped = { value ->
                binding.LeftSwipeContainer.apply {
                    alpha = value
                    translationX = -width.dp * (1 - min(value, 1f))
                }
            }
            binding.mangaReaderSwipy.rightBeingSwiped = { value ->
                binding.RightSwipeContainer.apply {
                    alpha = value
                    translationX = width.dp * (1 - min(value, 1f))
                }
            }
        }

        if (defaultSettings.layout != PAGED) {

            binding.mangaReaderRecyclerContainer.visibility = View.VISIBLE
            binding.mangaReaderRecyclerContainer.controller.settings.isRotationEnabled =
                defaultSettings.rotation

            val detector = GestureDetectorCompat(this, object : GesturesListener() {
                override fun onLongPress(e: MotionEvent) {
                    if (binding.mangaReaderRecycler.findChildViewUnder(e.x, e.y).let { child ->
                            child ?: return@let false
                            val pos = binding.mangaReaderRecycler.getChildAdapterPosition(child)
                            val callback: (ImageViewDialog) -> Unit = { dialog ->
                                lifecycleScope.launch {
                                    imageAdapter?.loadImage(
                                        pos,
                                        child as GestureFrameLayout
                                    )
                                }
                                binding.mangaReaderRecycler.performHapticFeedback(
                                    HapticFeedbackConstants.LONG_PRESS
                                )
                                dialog.dismiss()
                            }
                            dualPage {
                                val page =
                                    chapter.dualPages().getOrNull(pos) ?: return@dualPage false
                                val nextPage = page.second
                                if (defaultSettings.direction != LEFT_TO_RIGHT && nextPage != null)
                                    onImageLongClicked(pos * 2, nextPage, page.first, callback)
                                else
                                    onImageLongClicked(pos * 2, page.first, nextPage, callback)
                            } ?: onImageLongClicked(
                                pos,
                                chapImages.getOrNull(pos) ?: return@let false,
                                null,
                                callback
                            )
                        }
                    ) binding.mangaReaderRecycler.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    super.onLongPress(e)
                }

                override fun onSingleClick(event: MotionEvent) {
                    handleController()
                }
            })

            val manager = PreloadLinearLayoutManager(
                this,
                if (defaultSettings.direction == TOP_TO_BOTTOM || defaultSettings.direction == BOTTOM_TO_TOP)
                    RecyclerView.VERTICAL
                else
                    RecyclerView.HORIZONTAL,
                !(defaultSettings.direction == TOP_TO_BOTTOM || defaultSettings.direction == LEFT_TO_RIGHT)
            )
            manager.preloadItemCount = 5

            binding.mangaReaderPager.visibility = View.GONE

            binding.mangaReaderRecycler.apply {
                clearOnScrollListeners()
                binding.mangaReaderSwipy.child = this
                adapter = imageAdapter
                layoutManager = manager
                setOnTouchListener { _, event ->
                    if (event != null)
                        tryWith { detector.onTouchEvent(event) } ?: false
                    else false
                }

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(v: RecyclerView, dx: Int, dy: Int) {
                        defaultSettings.apply {
                            if (
                                ((direction == TOP_TO_BOTTOM || direction == BOTTOM_TO_TOP)
                                        && (!v.canScrollVertically(-1) || !v.canScrollVertically(1)))
                                ||
                                ((direction == LEFT_TO_RIGHT || direction == RIGHT_TO_LEFT)
                                        && (!v.canScrollHorizontally(-1) || !v.canScrollHorizontally(
                                    1
                                )))
                            ) {
                                handleController(true)
                            } else handleController(false)
                        }
                        updatePageNumber(
                            manager.findLastVisibleItemPosition().toLong() * (dualPage { 2 }
                                ?: 1) + 1)
                        super.onScrolled(v, dx, dy)
                    }
                })
                if ((defaultSettings.direction == TOP_TO_BOTTOM || defaultSettings.direction == BOTTOM_TO_TOP))
                    updatePadding(0, 128f.px, 0, 128f.px)
                else
                    updatePadding(128f.px, 0, 128f.px, 0)

                snapHelper.attachToRecyclerView(
                    if (defaultSettings.layout == CONTINUOUS_PAGED) this
                    else null
                )

                onVolumeUp = {
                    if ((defaultSettings.direction == TOP_TO_BOTTOM || defaultSettings.direction == BOTTOM_TO_TOP))
                        smoothScrollBy(0, -500)
                    else
                        smoothScrollBy(-500, 0)
                }

                onVolumeDown = {
                    if ((defaultSettings.direction == TOP_TO_BOTTOM || defaultSettings.direction == BOTTOM_TO_TOP))
                        smoothScrollBy(0, 500)
                    else
                        smoothScrollBy(500, 0)
                }

                scrollToPosition(currentPage / (dualPage { 2 } ?: 1) - 1)
            }
        } else {
            binding.mangaReaderRecyclerContainer.visibility = View.GONE
            binding.mangaReaderPager.apply {
                binding.mangaReaderSwipy.child = this
                visibility = View.VISIBLE
                adapter = imageAdapter
                layoutDirection =
                    if (defaultSettings.direction == BOTTOM_TO_TOP || defaultSettings.direction == RIGHT_TO_LEFT)
                        View.LAYOUT_DIRECTION_RTL
                    else View.LAYOUT_DIRECTION_LTR
                orientation =
                    if (defaultSettings.direction == LEFT_TO_RIGHT || defaultSettings.direction == RIGHT_TO_LEFT)
                        ViewPager2.ORIENTATION_HORIZONTAL
                    else ViewPager2.ORIENTATION_VERTICAL
                registerOnPageChangeCallback(pageChangeCallback)
                offscreenPageLimit = 5

                setCurrentItem(currentPage / (dualPage { 2 } ?: 1) - 1, false)
            }
            onVolumeUp = {
                binding.mangaReaderPager.currentItem -= 1
            }
            onVolumeDown = {
                binding.mangaReaderPager.currentItem += 1
            }
        }
    }

    private var onVolumeUp: (() -> Unit)? = null
    private var onVolumeDown: (() -> Unit)? = null
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return when (event.keyCode) {
            KEYCODE_VOLUME_UP, KEYCODE_DPAD_UP, KEYCODE_PAGE_UP -> {
                if (event.keyCode == KEYCODE_VOLUME_UP)
                    if (!defaultSettings.volumeButtons)
                        return false
                if (event.action == ACTION_DOWN) {
                    onVolumeUp?.invoke()
                    true
                } else false
            }

            KEYCODE_VOLUME_DOWN, KEYCODE_DPAD_DOWN, KEYCODE_PAGE_DOWN -> {
                if (event.keyCode == KEYCODE_VOLUME_DOWN)
                    if (!defaultSettings.volumeButtons)
                        return false
                if (event.action == ACTION_DOWN) {
                    onVolumeDown?.invoke()
                    true
                } else false
            }

            else -> {
                super.dispatchKeyEvent(event)
            }
        }
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updatePageNumber(position.toLong() * (dualPage { 2 } ?: 1) + 1)
            handleController(position == 0 || position + 1 >= maxChapterPage)
            super.onPageSelected(position)
        }
    }

    private val overshoot = OvershootInterpolator(1.4f)
    private var controllerDuration by Delegates.notNull<Long>()
    private var goneTimer = Timer()
    fun gone() {
        goneTimer.cancel()
        goneTimer.purge()
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                if (!isContVisible) binding.mangaReaderCont.post {
                    binding.mangaReaderCont.visibility = View.GONE
                    isAnimating = false
                }
            }
        }
        goneTimer = Timer()
        goneTimer.schedule(timerTask, controllerDuration)
    }

    enum class pressPos {
        LEFT, RIGHT, CENTER
    }

    fun handleController(shouldShow: Boolean? = null, event: MotionEvent? = null) {
        var pressLocation = pressPos.CENTER
        if (!sliding) {
            if (event != null && defaultSettings.layout == PAGED) {
                if (event.action != MotionEvent.ACTION_UP) return
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                val screenWidth = Resources.getSystem().displayMetrics.widthPixels
                //if in the 1st 1/5th of the screen width, left and lower than 1/5th of the screen height, left
                if (screenWidth / 5 in x + 1..<y) {
                    pressLocation = if (defaultSettings.direction == RIGHT_TO_LEFT) {
                        pressPos.RIGHT
                    } else {
                        pressPos.LEFT
                    }
                }
                //if in the last 1/5th of the screen width, right and lower than 1/5th of the screen height, right
                else if (x > screenWidth - screenWidth / 5 && y > screenWidth / 5) {
                    pressLocation = if (defaultSettings.direction == RIGHT_TO_LEFT) {
                        pressPos.LEFT
                    } else {
                        pressPos.RIGHT
                    }
                }
            }

            // if pressLocation is left or right go to previous or next page (paged mode only)
            if (pressLocation == pressPos.LEFT) {

                if (binding.mangaReaderPager.currentItem > 0) {
                    //if  the current images zoomed in, go back to normal before going to previous page
                    if (imageAdapter?.isZoomed() == true) {
                        imageAdapter?.setZoom(1f)
                    }
                    binding.mangaReaderPager.currentItem -= 1
                    return
                }

            } else if (pressLocation == pressPos.RIGHT) {
                if (binding.mangaReaderPager.currentItem < maxChapterPage - 1) {
                    //if  the current images zoomed in, go back to normal before going to next page
                    if (imageAdapter?.isZoomed() == true) {
                        imageAdapter?.setZoom(1f)
                    }
                    //if right to left, go to previous page
                    binding.mangaReaderPager.currentItem += 1
                    return
                }
            }

            if (!PrefManager.getVal<Boolean>(PrefName.ShowSystemBars)) {
                hideBars()
                checkNotch()
            }
            //horizontal scrollbar
            if (defaultSettings.horizontalScrollBar) {
                binding.mangaReaderSliderContainer.updateLayoutParams {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                }

                binding.mangaReaderSlider.apply {
                    updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        width = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                    rotation = 0f
                }

            } else {
                binding.mangaReaderSliderContainer.updateLayoutParams {
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                    width = 48f.px
                }

                binding.mangaReaderSlider.apply {
                    updateLayoutParams {
                        width = binding.mangaReaderSliderContainer.height - 16f.px
                    }
                    rotation = 90f
                }
            }
            binding.mangaReaderSlider.layoutDirection =
                if (defaultSettings.direction == RIGHT_TO_LEFT || defaultSettings.direction == BOTTOM_TO_TOP)
                    View.LAYOUT_DIRECTION_RTL
                else View.LAYOUT_DIRECTION_LTR
            shouldShow?.apply { isContVisible = !this }
            if (isContVisible) {
                isContVisible = false
                if (!isAnimating) {
                    isAnimating = true
                    ObjectAnimator.ofFloat(binding.mangaReaderCont, "alpha", 1f, 0f)
                        .setDuration(controllerDuration).start()
                    ObjectAnimator.ofFloat(
                        binding.mangaReaderBottomLayout,
                        "translationY",
                        0f,
                        128f
                    )
                        .apply { interpolator = overshoot;duration = controllerDuration;start() }
                    ObjectAnimator.ofFloat(binding.mangaReaderTopLayout, "translationY", 0f, -128f)
                        .apply { interpolator = overshoot;duration = controllerDuration;start() }
                }
                gone()
            } else {
                isContVisible = true
                binding.mangaReaderCont.visibility = View.VISIBLE
                ObjectAnimator.ofFloat(binding.mangaReaderCont, "alpha", 0f, 1f)
                    .setDuration(controllerDuration).start()
                ObjectAnimator.ofFloat(binding.mangaReaderTopLayout, "translationY", -128f, 0f)
                    .apply { interpolator = overshoot;duration = controllerDuration;start() }
                ObjectAnimator.ofFloat(binding.mangaReaderBottomLayout, "translationY", 128f, 0f)
                    .apply { interpolator = overshoot;duration = controllerDuration;start() }
            }
        }
    }

    private var loading = false
    fun updatePageNumber(page: Long) {
        if (currentChapterPage != page) {
            currentChapterPage = page
            PrefManager.setCustomVal("${media.id}_${chapter.number}", page)
            binding.mangaReaderPageNumber.text =
                if (defaultSettings.hidePageNumbers) "" else "${currentChapterPage}/$maxChapterPage"
            if (!sliding) binding.mangaReaderSlider.apply {
                value = clamp(currentChapterPage.toFloat(), 1f, valueTo)
            }
        }
        if (maxChapterPage - currentChapterPage <= 1 && !loading)
            scope.launch(Dispatchers.IO) {
                loading = true
                model.loadMangaChapterImages(
                    chapters[chaptersArr.getOrNull(currentChapterIndex + 1) ?: return@launch]!!,
                    media.selected!!,
                    media.mainName(),
                    false
                )
                loading = false
            }
    }

    private fun progress(runnable: Runnable) {
        if (maxChapterPage - currentChapterPage <= 1 && Anilist.userid != null) {
            showProgressDialog =
                if (PrefManager.getVal(PrefName.AskIndividualReader)) PrefManager.getCustomVal(
                    "${media.id}_progressDialog",
                    true
                )
                else false
            val incognito: Boolean = PrefManager.getVal(PrefName.Incognito)
            if (showProgressDialog && !incognito) {

                val dialogView = layoutInflater.inflate(R.layout.item_custom_dialog, null)
                val checkbox = dialogView.findViewById<CheckBox>(R.id.dialog_checkbox)
                checkbox.text = getString(R.string.dont_ask_again, media.userPreferredName)
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    PrefManager.setCustomVal("${media.id}_progressDialog", !isChecked)
                    showProgressDialog = !isChecked
                }
                AlertDialog.Builder(this, R.style.MyPopup)
                    .setTitle(getString(R.string.title_update_progress))
                    .setView(dialogView)
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                        PrefManager.setCustomVal("${media.id}_save_progress", true)
                        updateProgress(
                            media,
                            MangaNameAdapter.findChapterNumber(media.manga!!.selectedChapter!!)
                                .toString()
                        )
                        dialog.dismiss()
                        runnable.run()
                    }
                    .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                        PrefManager.setCustomVal("${media.id}_save_progress", false)
                        dialog.dismiss()
                        runnable.run()
                    }
                    .setOnCancelListener { hideBars() }
                    .create()
                    .show()
            } else {
                if (!incognito && PrefManager.getCustomVal(
                        "${media.id}_save_progress",
                        true
                    ) && if (media.isAdult) PrefManager.getVal<Boolean>(PrefName.UpdateForHReader) else true
                )
                    updateProgress(
                        media,
                        MangaNameAdapter.findChapterNumber(media.manga!!.selectedChapter!!)
                            .toString()
                    )
                runnable.run()
            }
        } else {
            runnable.run()
        }
    }


    @Suppress("UNCHECKED_CAST")
    private fun <T> loadReaderSettings(
        fileName: String,
        context: Context? = null,
        toast: Boolean = true
    ): T? {
        val a = context ?: currContext()
        try {
            if (a?.fileList() != null)
                if (fileName in a.fileList()) {
                    val fileIS: FileInputStream = a.openFileInput(fileName)
                    val objIS = ObjectInputStream(fileIS)
                    val data = objIS.readObject() as T
                    objIS.close()
                    fileIS.close()
                    return data
                }
        } catch (e: Exception) {
            if (toast) snackString(a?.getString(R.string.error_loading_data, fileName))
            //try to delete the file
            try {
                a?.deleteFile(fileName)
            } catch (e: Exception) {
                Injekt.get<CrashlyticsInterface>().log("Failed to delete file $fileName")
                Injekt.get<CrashlyticsInterface>().logException(e)
            }
            e.printStackTrace()
        }
        return null
    }

    private fun saveReaderSettings(fileName: String, data: Any?, context: Context? = null) {
        tryWith {
            val a = context ?: currContext()
            if (a != null) {
                val fos: FileOutputStream = a.openFileOutput(fileName, Context.MODE_PRIVATE)
                val os = ObjectOutputStream(fos)
                os.writeObject(data)
                os.close()
                fos.close()
            }
        }
    }

    fun getTransformation(mangaImage: MangaImage): BitmapTransformation? {
        return model.loadTransformation(mangaImage, media.selected!!.sourceIndex)
    }

    fun onImageLongClicked(
        pos: Int,
        img1: MangaImage,
        img2: MangaImage?,
        callback: ((ImageViewDialog) -> Unit)? = null
    ): Boolean {
        if (!defaultSettings.longClickImage) return false
        val title = "(Page ${pos + 1}${if (img2 != null) "-${pos + 2}" else ""}) ${
            chaptersTitleArr.getOrNull(currentChapterIndex)?.replace(" : ", " - ") ?: ""
        } [${media.userPreferredName}]"

        ImageViewDialog.newInstance(title, img1.url, true, img2?.url).apply {
            val transforms1 = mutableListOf<BitmapTransformation>()
            val parserTransformation1 = getTransformation(img1)
            if (parserTransformation1 != null) transforms1.add(parserTransformation1)
            val transforms2 = mutableListOf<BitmapTransformation>()
            if (img2 != null) {
                val parserTransformation2 = getTransformation(img2)
                if (parserTransformation2 != null) transforms2.add(parserTransformation2)
            }
            val threshold = defaultSettings.cropBorderThreshold
            if (defaultSettings.cropBorders) {
                transforms1.add(RemoveBordersTransformation(true, threshold))
                transforms1.add(RemoveBordersTransformation(false, threshold))
                if (img2 != null) {
                    transforms2.add(RemoveBordersTransformation(true, threshold))
                    transforms2.add(RemoveBordersTransformation(false, threshold))
                }
            }
            trans1 = transforms1.ifEmpty { null }
            trans2 = transforms2.ifEmpty { null }
            onReloadPressed = callback
            show(supportFragmentManager, "image")
        }
        return true
    }
}