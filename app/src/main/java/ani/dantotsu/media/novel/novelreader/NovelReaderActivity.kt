package ani.dantotsu.media.novel.novelreader

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewCompat
import ani.dantotsu.GesturesListener
import ani.dantotsu.NoPaddingArrayAdapter
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityNovelReaderBinding
import ani.dantotsu.hideSystemBars
import ani.dantotsu.loadData
import ani.dantotsu.others.ImageViewDialog
import ani.dantotsu.others.LangSet
import ani.dantotsu.saveData
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.settings.CurrentNovelReaderSettings
import ani.dantotsu.settings.CurrentReaderSettings
import ani.dantotsu.settings.NovelReaderSettings
import ani.dantotsu.settings.UserInterfaceSettings
import ani.dantotsu.snackString
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.tryWith
import com.google.android.material.slider.Slider
import com.vipulog.ebookreader.Book
import com.vipulog.ebookreader.EbookReaderEventListener
import com.vipulog.ebookreader.ReaderError
import com.vipulog.ebookreader.ReaderFlow
import com.vipulog.ebookreader.ReaderTheme
import com.vipulog.ebookreader.RelocationInfo
import com.vipulog.ebookreader.TocItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.min
import kotlin.properties.Delegates


class NovelReaderActivity : AppCompatActivity(), EbookReaderEventListener {
    private lateinit var binding: ActivityNovelReaderBinding
    private val scope = lifecycleScope

    lateinit var settings: NovelReaderSettings
    private lateinit var uiSettings: UserInterfaceSettings

    private var notchHeight: Int? = null

    var loaded = false

    private lateinit var book: Book
    private lateinit var sanitizedBookId: String
    private lateinit var toc: List<TocItem>
    private var currentTheme: ReaderTheme? = null
    private var currentCfi: String? = null

    val themes = ArrayList<ReaderTheme>()


    init {
        val forestTheme = ReaderTheme(
            name = "Forest",
            lightFg = Color.parseColor("#000000"),
            lightBg = Color.parseColor("#E7F6E7"),
            lightLink = Color.parseColor("#008000"),
            darkFg = Color.parseColor("#FFFFFF"),
            darkBg = Color.parseColor("#084D08"),
            darkLink = Color.parseColor("#00B200")
        )

        val oceanTheme = ReaderTheme(
            name = "Ocean",
            lightFg = Color.parseColor("#000000"),
            lightBg = Color.parseColor("#E4F0F9"),
            lightLink = Color.parseColor("#007BFF"),
            darkFg = Color.parseColor("#FFFFFF"),
            darkBg = Color.parseColor("#0A2E3E"),
            darkLink = Color.parseColor("#00A5E4")
        )

        val sunsetTheme = ReaderTheme(
            name = "Sunset",
            lightFg = Color.parseColor("#000000"),
            lightBg = Color.parseColor("#FDEDE6"),
            lightLink = Color.parseColor("#FF5733"),
            darkFg = Color.parseColor("#FFFFFF"),
            darkBg = Color.parseColor("#441517"),
            darkLink = Color.parseColor("#FF6B47")
        )

        val desertTheme = ReaderTheme(
            name = "Desert",
            lightFg = Color.parseColor("#000000"),
            lightBg = Color.parseColor("#FDF5E6"),
            lightLink = Color.parseColor("#FFA500"),
            darkFg = Color.parseColor("#FFFFFF"),
            darkBg = Color.parseColor("#523B19"),
            darkLink = Color.parseColor("#FFBF00")
        )

        val galaxyTheme = ReaderTheme(
            name = "Galaxy",
            lightFg = Color.parseColor("#000000"),
            lightBg = Color.parseColor("#F2F2F2"),
            lightLink = Color.parseColor("#800080"),
            darkFg = Color.parseColor("#FFFFFF"),
            darkBg = Color.parseColor("#000000"),
            darkLink = Color.parseColor("#B300B3")
        )

        themes.addAll(listOf(forestTheme, oceanTheme, sunsetTheme, desertTheme, galaxyTheme))
    }


    override fun onAttachedToWindow() {
        checkNotch()
        super.onAttachedToWindow()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //check for supported webview
        val webViewVersion = WebViewCompat.getCurrentWebViewPackage(this)?.versionName
        val firstVersion = webViewVersion?.split(".")?.firstOrNull()?.toIntOrNull()
        if (webViewVersion == null || firstVersion == null || firstVersion < 87) {
            Toast.makeText(this, "Please update WebView from PlayStore", Toast.LENGTH_LONG).show()
            //open playstore
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.webview")
            startActivity(intent)
            //stop reader
            finish()
            return
        }

        LangSet.setLocale(this)
        ThemeManager(this).applyTheme()
        binding = ActivityNovelReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = loadData("novel_reader_settings", this)
            ?: NovelReaderSettings().apply { saveData("novel_reader_settings", this) }
        uiSettings = loadData("ui_settings", this)
            ?: UserInterfaceSettings().also { saveData("ui_settings", it) }

        controllerDuration = (uiSettings.animationSpeed * 200).toLong()

        setupViews()
        setupBackPressedHandler()
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupViews() {
        scope.launch { binding.bookReader.openBook(intent.data!!) }
        binding.bookReader.setEbookReaderListener(this)

        binding.novelReaderBack.setOnClickListener { finish() }
        binding.novelReaderSettings.setSafeOnClickListener {
            NovelReaderSettingsDialogFragment.newInstance()
                .show(supportFragmentManager, NovelReaderSettingsDialogFragment.TAG)
        }

        val gestureDetector = GestureDetectorCompat(this, object : GesturesListener() {
            override fun onSingleClick(event: MotionEvent) {
                handleController()
            }
        })

        binding.bookReader.setOnTouchListener { _, event ->
            if (event != null) tryWith { gestureDetector.onTouchEvent(event) } ?: false
            else false
        }

        binding.novelReaderNextChap.setOnClickListener { binding.novelReaderNextChapter.performClick() }
        binding.novelReaderNextChapter.setOnClickListener { binding.bookReader.next() }
        binding.novelReaderPrevChap.setOnClickListener { binding.novelReaderPreviousChapter.performClick() }
        binding.novelReaderPreviousChapter.setOnClickListener { binding.bookReader.prev() }

        binding.novelReaderSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                binding.bookReader.gotoFraction(slider.value.toDouble())
            }
        })

        onVolumeUp = { binding.novelReaderNextChapter.performClick() }

        onVolumeDown = { binding.novelReaderPreviousChapter.performClick() }
    }

    private fun setupBackPressedHandler() {
        var lastBackPressedTime: Long = 0
        val doublePressInterval: Long = 2000

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.bookReader.canGoBack()) {
                    binding.bookReader.goBack()
                } else {
                    if (lastBackPressedTime + doublePressInterval > System.currentTimeMillis()) {
                        finish()
                    } else {
                        snackString("Press back again to exit")
                        lastBackPressedTime = System.currentTimeMillis()
                    }
                }
            }
        })
    }


    override fun onBookLoadFailed(error: ReaderError) {
        snackString(error.message)
        finish()
    }


    override fun onBookLoaded(book: Book) {
        this.book = book
        val bookId = book.identifier!!
        toc = book.toc

        val illegalCharsRegex = Regex("[^a-zA-Z0-9._-]")
        sanitizedBookId = bookId.replace(illegalCharsRegex, "_")

        binding.novelReaderTitle.text = book.title
        binding.novelReaderSource.text = book.author?.joinToString(", ")

        val tocLabels = book.toc.map { it.label ?: "" }
        binding.novelReaderChapterSelect.adapter =
            NoPaddingArrayAdapter(this, R.layout.item_dropdown, tocLabels)
        binding.novelReaderChapterSelect.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    binding.bookReader.goto(book.toc[position].href)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        binding.bookReader.getAppearance {
            currentTheme = it
            themes.add(0, it)
            settings.default = loadData("${sanitizedBookId}_current_settings") ?: settings.default
            applySettings()
        }

        val cfi = loadData<String>("${sanitizedBookId}_progress")

        cfi?.let { binding.bookReader.goto(it) }
        binding.progress.visibility = View.GONE
        loaded = true
    }


    override fun onProgressChanged(info: RelocationInfo) {
        currentCfi = info.cfi
        binding.novelReaderSlider.value = info.fraction.toFloat()
        val pos = info.tocItem?.let { item -> toc.indexOfFirst { it == item } }
        if (pos != null) binding.novelReaderChapterSelect.setSelection(pos)
        saveData("${sanitizedBookId}_progress", info.cfi)
    }


    override fun onImageSelected(base64String: String) {
        scope.launch(Dispatchers.IO) {
            val base64Data = base64String.substringAfter(",")
            val imageBytes: ByteArray = Base64.decode(base64Data, Base64.DEFAULT)
            val imageFile = File(cacheDir, "/images/ln.jpg")

            imageFile.parentFile?.mkdirs()
            imageFile.createNewFile()

            FileOutputStream(imageFile).use { outputStream -> outputStream.write(imageBytes) }

            ImageViewDialog.newInstance(
                this@NovelReaderActivity,
                book.title,
                imageFile.toUri().toString()
            )
        }
    }


    override fun onTextSelectionModeChange(mode: Boolean) {
        // TODO: Show ui for adding annotations and notes
    }


    private var onVolumeUp: (() -> Unit)? = null
    private var onVolumeDown: (() -> Unit)? = null
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_PAGE_UP -> {
                if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                    if (!settings.default.volumeButtons)
                        return false
                if (event.action == KeyEvent.ACTION_DOWN) {
                    onVolumeUp?.invoke()
                    true
                } else false
            }

            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> {
                if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                    if (!settings.default.volumeButtons)
                        return false
                if (event.action == KeyEvent.ACTION_DOWN) {
                    onVolumeDown?.invoke()
                    true
                } else false
            }

            else -> {
                super.dispatchKeyEvent(event)
            }
        }
    }


    fun applySettings() {
        saveData("${sanitizedBookId}_current_settings", settings.default)
        hideBars()

        currentTheme =
            themes.first { it.name.equals(settings.default.currentThemeName, ignoreCase = true) }

        when (settings.default.layout) {
            CurrentNovelReaderSettings.Layouts.PAGED -> {
                currentTheme?.flow = ReaderFlow.PAGINATED
            }

            CurrentNovelReaderSettings.Layouts.SCROLLED -> {
                currentTheme?.flow = ReaderFlow.SCROLLED
            }
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        when (settings.default.dualPageMode) {
            CurrentReaderSettings.DualPageModes.No -> currentTheme?.maxColumnCount = 1
            CurrentReaderSettings.DualPageModes.Automatic -> currentTheme?.maxColumnCount = 2
            CurrentReaderSettings.DualPageModes.Force -> requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        currentTheme?.lineHeight = settings.default.lineHeight
        currentTheme?.gap = settings.default.margin
        currentTheme?.maxInlineSize = settings.default.maxInlineSize
        currentTheme?.maxBlockSize = settings.default.maxBlockSize
        currentTheme?.useDark = settings.default.useDarkTheme

        currentTheme?.let { binding.bookReader.setAppearance(it) }

        if (settings.default.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


    // region Handle Controls
    private var isContVisible = false
    private var isAnimating = false
    private var goneTimer = Timer()
    private var controllerDuration by Delegates.notNull<Long>()
    private val overshoot = OvershootInterpolator(1.4f)

    fun gone() {
        goneTimer.cancel()
        goneTimer.purge()
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                if (!isContVisible) binding.novelReaderCont.post {
                    binding.novelReaderCont.visibility = View.GONE
                    isAnimating = false
                }
            }
        }
        goneTimer = Timer()
        goneTimer.schedule(timerTask, controllerDuration)
    }

    fun handleController(shouldShow: Boolean? = null) {
        if (!loaded) return

        if (!settings.showSystemBars) {
            hideBars()
            applyNotchMargin()
        }

        shouldShow?.apply { isContVisible = !this }
        if (isContVisible) {
            isContVisible = false
            if (!isAnimating) {
                isAnimating = true
                ObjectAnimator.ofFloat(binding.novelReaderCont, "alpha", 1f, 0f)
                    .setDuration(controllerDuration).start()
                ObjectAnimator.ofFloat(binding.novelReaderBottomCont, "translationY", 0f, 128f)
                    .apply { interpolator = overshoot;duration = controllerDuration;start() }
                ObjectAnimator.ofFloat(binding.novelReaderTopLayout, "translationY", 0f, -128f)
                    .apply { interpolator = overshoot;duration = controllerDuration;start() }
            }
            gone()
        } else {
            isContVisible = true
            binding.novelReaderCont.visibility = View.VISIBLE
            ObjectAnimator.ofFloat(binding.novelReaderCont, "alpha", 0f, 1f)
                .setDuration(controllerDuration).start()
            ObjectAnimator.ofFloat(binding.novelReaderTopLayout, "translationY", -128f, 0f)
                .apply { interpolator = overshoot;duration = controllerDuration;start() }
            ObjectAnimator.ofFloat(binding.novelReaderBottomCont, "translationY", 128f, 0f)
                .apply { interpolator = overshoot;duration = controllerDuration;start() }
        }
    }
    // endregion Handle Controls


    private fun checkNotch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !settings.showSystemBars) {
            val displayCutout = window.decorView.rootWindowInsets.displayCutout
            if (displayCutout != null) {
                if (displayCutout.boundingRects.size > 0) {
                    notchHeight = min(
                        displayCutout.boundingRects[0].width(),
                        displayCutout.boundingRects[0].height()
                    )
                    applyNotchMargin()
                }
            }
        }
    }


    private fun applyNotchMargin() {
        binding.novelReaderTopLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = notchHeight ?: return
        }
    }


    private fun hideBars() {
        if (!settings.showSystemBars) hideSystemBars()
    }
}