package ani.dantotsu.media.novel.novelreader

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
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
import android.webkit.WebView
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
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.currContext
import ani.dantotsu.databinding.ActivityNovelReaderBinding
import ani.dantotsu.hideSystemBars
import ani.dantotsu.others.ImageViewDialog
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.settings.CurrentNovelReaderSettings
import ani.dantotsu.settings.CurrentReaderSettings
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.tryWith
import com.google.android.material.slider.Slider
import com.vipulog.ebookreader.Book
import com.vipulog.ebookreader.EbookReaderEventListener
import com.vipulog.ebookreader.EbookReaderView
import com.vipulog.ebookreader.ReaderError
import com.vipulog.ebookreader.ReaderFlow
import com.vipulog.ebookreader.ReaderTheme
import com.vipulog.ebookreader.RelocationInfo
import com.vipulog.ebookreader.TocItem
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Timer
import java.util.TimerTask
import kotlin.math.min
import kotlin.properties.Delegates


class NovelReaderActivity : AppCompatActivity(), EbookReaderEventListener {
    private lateinit var binding: ActivityNovelReaderBinding
    private val scope = lifecycleScope

    private var notchHeight: Int? = null

    var loaded = false

    private lateinit var book: Book
    private lateinit var sanitizedBookId: String
    private lateinit var toc: List<TocItem>
    private var currentTheme: ReaderTheme? = null
    private var currentCfi: String? = null

    val themes = ArrayList<ReaderTheme>()

    var defaultSettings = CurrentNovelReaderSettings()


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


    @SuppressLint("WebViewApiAvailability")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //check for supported webview
        val webViewVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WebView.getCurrentWebViewPackage()?.versionName
        } else {
            WebViewCompat.getCurrentWebViewPackage(this)?.versionName
        }
        val firstVersion = webViewVersion?.split(".")?.firstOrNull()?.toIntOrNull()
        if (webViewVersion == null || firstVersion == null || firstVersion < 87) {
            val text = if (webViewVersion == null) {
                "Could not find webView installed"
            } else if (firstVersion == null) {
                "Could not find WebView Version Number: $webViewVersion"
            } else if (firstVersion < 87) { //false positive?
                "Webview Versiom: $firstVersion. PLease update"
            } else {
                "Please update WebView from PlayStore"
            }
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
            //open playstore
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data =
                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.webview")
            startActivity(intent)
            //stop reader
            finish()
            return
        }


        ThemeManager(this).applyTheme()
        binding = ActivityNovelReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        controllerDuration = (PrefManager.getVal<Float>(PrefName.AnimationSpeed) * 200).toLong()

        setupViews()
        setupBackPressedHandler()
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupViews() {
        binding.bookReader.useSafeScope(this)

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
            defaultSettings =
                loadReaderSettings("${sanitizedBookId}_current_settings") ?: defaultSettings
            applySettings()
        }

        val cfi = PrefManager.getNullableCustomVal(
            "${sanitizedBookId}_progress",
            null,
            String::class.java
        )

        cfi?.let { binding.bookReader.goto(it) }
        binding.progress.visibility = View.GONE
        loaded = true
    }


    override fun onProgressChanged(info: RelocationInfo) {
        currentCfi = info.cfi
        binding.novelReaderSlider.value = info.fraction.toFloat()
        val pos = info.tocItem?.let { item -> toc.indexOfFirst { it == item } }
        if (pos != null) binding.novelReaderChapterSelect.setSelection(pos)
        PrefManager.setCustomVal("${sanitizedBookId}_progress", info.cfi)
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
                    if (!defaultSettings.volumeButtons)
                        return false
                if (event.action == KeyEvent.ACTION_DOWN) {
                    onVolumeUp?.invoke()
                    true
                } else false
            }

            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> {
                if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                    if (!defaultSettings.volumeButtons)
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
        saveReaderSettings("${sanitizedBookId}_current_settings", defaultSettings)
        hideBars()

        if (defaultSettings.useOledTheme) {
            themes.forEach { theme ->
                theme.darkBg = Color.parseColor("#000000")
            }
        }
        currentTheme =
            themes.first { it.name.equals(defaultSettings.currentThemeName, ignoreCase = true) }

        when (defaultSettings.layout) {
            CurrentNovelReaderSettings.Layouts.PAGED -> {
                currentTheme?.flow = ReaderFlow.PAGINATED
            }

            CurrentNovelReaderSettings.Layouts.SCROLLED -> {
                currentTheme?.flow = ReaderFlow.SCROLLED
            }
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        when (defaultSettings.dualPageMode) {
            CurrentReaderSettings.DualPageModes.No -> currentTheme?.maxColumnCount = 1
            CurrentReaderSettings.DualPageModes.Automatic -> currentTheme?.maxColumnCount = 2
            CurrentReaderSettings.DualPageModes.Force -> requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        currentTheme?.lineHeight = defaultSettings.lineHeight
        currentTheme?.gap = defaultSettings.margin
        currentTheme?.maxInlineSize = defaultSettings.maxInlineSize
        currentTheme?.maxBlockSize = defaultSettings.maxBlockSize
        currentTheme?.useDark = defaultSettings.useDarkTheme

        currentTheme?.let { binding.bookReader.setAppearance(it) }

        if (defaultSettings.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

        if (!PrefManager.getVal<Boolean>(PrefName.ShowSystemBars)) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !PrefManager.getVal<Boolean>(PrefName.ShowSystemBars)) {
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

    private fun hideBars() {
        if (!PrefManager.getVal<Boolean>(PrefName.ShowSystemBars)) {
            hideSystemBars()
        }
    }
}


/**
 * ⚠️ TEMPORARY HOTFIX ⚠️
 *
 * This is a hacky workaround to handle crashes in the deprecated ebookreader library.
 *
 * Current implementation:
 * - Uses reflection to access the private `scope` field in `EbookReaderView`.
 * - Replaces the existing `CoroutineScope` with a new one that includes a
 *   `CoroutineExceptionHandler`.
 * - Ensures that uncaught exceptions in coroutines are handled gracefully by showing a snackbar
 *   with error details.
 *
 * TODO:
 * - This is NOT a long-term solution
 * - The underlying library is archived and unmaintained
 * - Schedule migration to an actively maintained library
 * - Consider alternatives like https://github.com/readium/kotlin-toolkit
 */
fun EbookReaderView.useSafeScope(activity: Activity) {
    runCatching {
        val scopeField = javaClass.getDeclaredField("scope").apply { isAccessible = true }
        val currentScope = scopeField.get(this) as CoroutineScope
        val safeScope = CoroutineScope(
            SupervisorJob() +
                    currentScope.coroutineContext.minusKey(Job) +
                    scopeExceptionHandler(activity)
        )
        scopeField.set(this, safeScope)
    }.onFailure { e ->
        snackString(e.localizedMessage, activity, e.stackTraceToString())
    }
}

private fun scopeExceptionHandler(activity: Activity) = CoroutineExceptionHandler { _, e ->
    snackString(e.localizedMessage, activity, e.stackTraceToString())
}
