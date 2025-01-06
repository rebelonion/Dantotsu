package ani.dantotsu

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources.getSystem
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_LOWPAN
import android.net.NetworkCapabilities.TRANSPORT_USB
import android.net.NetworkCapabilities.TRANSPORT_VPN
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.NetworkCapabilities.TRANSPORT_WIFI_AWARE
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.InputFilter
import android.text.Spanned
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.DatePicker
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import androidx.core.math.MathUtils.clamp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.BuildConfig.APPLICATION_ID
import ani.dantotsu.connections.anilist.Genre
import ani.dantotsu.connections.anilist.api.FuzzyDate
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.databinding.ItemCountDownBinding
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.notifications.IncognitoNotificationClickReceiver
import ani.dantotsu.others.AlignTagHandler
import ani.dantotsu.others.ImageViewDialog
import ani.dantotsu.others.SpoilerPlugin
import ani.dantotsu.parsers.ShowResponse
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.settings.saving.internal.PreferenceKeystore
import ani.dantotsu.settings.saving.internal.PreferenceKeystore.Companion.generateSalt
import ani.dantotsu.util.Logger
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.internal.ViewUtils
import com.google.android.material.snackbar.Snackbar
import eu.kanade.tachiyomi.data.notification.Notifications
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.html.TagHandlerNoOp
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.glide.GlideImagesPlugin
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.joery.animatedbottombar.AnimatedBottomBar
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.reflect.Field
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask
import kotlin.collections.set
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


var statusBarHeight = 0
var navBarHeight = 0
val Int.dp: Float get() = (this / getSystem().displayMetrics.density)
val Float.px: Int get() = (this * getSystem().displayMetrics.density).toInt()

lateinit var bottomBar: AnimatedBottomBar
var selectedOption = 1

object Refresh {
    fun all() {
        for (i in activity) {
            activity[i.key]!!.postValue(true)
        }
    }

    val activity = mutableMapOf<Int, MutableLiveData<Boolean>>()
}

fun currContext(): Context? {
    return App.currentContext()
}

fun currActivity(): Activity? {
    return App.currentActivity()
}

var loadMedia: Int? = null
var loadIsMAL = false

val Int.toPx
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), getSystem().displayMetrics
    ).toInt()

fun initActivity(a: Activity) {
    val window = a.window
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val darkMode = PrefManager.getVal<Int>(PrefName.DarkMode)
    val immersiveMode: Boolean = PrefManager.getVal(PrefName.ImmersiveMode)
    darkMode.apply {
        AppCompatDelegate.setDefaultNightMode(
            when (this) {
                2 -> AppCompatDelegate.MODE_NIGHT_YES
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
    if (immersiveMode) {
        if (navBarHeight == 0) {
            ViewCompat.getRootWindowInsets(window.decorView.findViewById(android.R.id.content))
                ?.apply {
                    navBarHeight = this.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) navBarHeight += 48.toPx
                }
        }
        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).hide(WindowInsetsCompat.Type.statusBars())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && statusBarHeight == 0
            && a.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        ) {
            window.decorView.rootWindowInsets?.displayCutout?.apply {
                if (boundingRects.size > 0) {
                    statusBarHeight = min(boundingRects[0].width(), boundingRects[0].height())
                }
            }
        }
    } else
        if (statusBarHeight == 0) {
            val windowInsets =
                ViewCompat.getRootWindowInsets(window.decorView.findViewById(android.R.id.content))
            if (windowInsets != null) {
                statusBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                navBarHeight =
                    windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) navBarHeight += 48.toPx
            }
        }
    if (a !is MainActivity) a.setNavigationTheme()
}

fun Activity.hideSystemBars() {
    WindowInsetsControllerCompat(window, window.decorView).let { controller ->
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

fun Activity.hideSystemBarsExtendView() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    hideSystemBars()
}

fun Activity.showSystemBars() {
    WindowInsetsControllerCompat(window, window.decorView).let { controller ->
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}

fun Activity.showSystemBarsRetractView() {
    WindowCompat.setDecorFitsSystemWindows(window, true)
    showSystemBars()
}

fun Activity.setNavigationTheme() {
    val tv = TypedValue()
    theme.resolveAttribute(android.R.attr.colorBackground, tv, true)
    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && tv.isColorType)
        || (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT)
    ) {
        window.navigationBarColor = tv.data
    }
}

/**
 * Sets clipToPadding false and sets the combined height of navigation bars as bottom padding.
 *
 * When nesting multiple scrolling views, only call this method on the inner most scrolling view.
 */
fun ViewGroup.setBaseline(navBar: AnimatedBottomBar) {
    navBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    clipToPadding = false
    setPadding(paddingLeft, paddingTop, paddingRight, navBarHeight + navBar.measuredHeight)
}

/**
 * Sets clipToPadding false and sets the combined height of navigation bars as bottom padding.
 *
 * When nesting multiple scrolling views, only call this method on the inner most scrolling view.
 */
fun ViewGroup.setBaseline(navBar: AnimatedBottomBar, overlayView: View) {
    navBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    overlayView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    clipToPadding = false
    setPadding(
        paddingLeft,
        paddingTop,
        paddingRight,
        navBarHeight + navBar.measuredHeight + overlayView.measuredHeight
    )
}

fun Activity.reloadActivity() {
    Refresh.all()
    finish()
    startActivity(Intent(this, this::class.java))
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    initActivity(this)
}

fun Activity.restartApp() {
    val mainIntent = Intent.makeRestartActivityTask(
        packageManager.getLaunchIntentForPackage(this.packageName)!!.component
    )
    val component =
        ComponentName(this@restartApp.packageName, this@restartApp::class.qualifiedName!!)
    try {
        startActivity(Intent().setComponent(component))
    } catch (e: Exception) {
        startActivity(mainIntent)
    }
    finishAndRemoveTask()
    PrefManager.setCustomVal("reload", true)
}

open class BottomSheetDialogFragment : BottomSheetDialogFragment() {
    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val immersiveMode: Boolean = PrefManager.getVal(PrefName.ImmersiveMode)
            if (immersiveMode) {
                WindowInsetsControllerCompat(
                    window, window.decorView
                ).hide(WindowInsetsCompat.Type.statusBars())
            }
            if (this.resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
                val behavior = BottomSheetBehavior.from(requireView().parent as View)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            window.navigationBarColor =
                requireContext().getThemeColor(com.google.android.material.R.attr.colorSurface)
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }
}

fun isOnline(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return tryWith {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cap = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            return@tryWith if (cap != null) {
                when {
                    cap.hasTransport(TRANSPORT_BLUETOOTH) ||
                            cap.hasTransport(TRANSPORT_CELLULAR) ||
                            cap.hasTransport(TRANSPORT_ETHERNET) ||
                            cap.hasTransport(TRANSPORT_LOWPAN) ||
                            cap.hasTransport(TRANSPORT_USB) ||
                            cap.hasTransport(TRANSPORT_VPN) ||
                            cap.hasTransport(TRANSPORT_WIFI) ||
                            cap.hasTransport(TRANSPORT_WIFI_AWARE) -> true

                    else -> false
                }
            } else false
        } else {
            @Suppress("DEPRECATION")
            return@tryWith connectivityManager.activeNetworkInfo?.run {
                type == ConnectivityManager.TYPE_BLUETOOTH ||
                        type == ConnectivityManager.TYPE_ETHERNET ||
                        type == ConnectivityManager.TYPE_MOBILE ||
                        type == ConnectivityManager.TYPE_MOBILE_DUN ||
                        type == ConnectivityManager.TYPE_MOBILE_HIPRI ||
                        type == ConnectivityManager.TYPE_WIFI ||
                        type == ConnectivityManager.TYPE_WIMAX ||
                        type == ConnectivityManager.TYPE_VPN
            } ?: false
        }
    } ?: false
}

fun startMainActivity(activity: Activity, bundle: Bundle? = null) {
    activity.finishAffinity()
    activity.startActivity(
        Intent(
            activity,
            MainActivity::class.java
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            if (bundle != null) putExtras(bundle)
        }
    )
}


class DatePickerFragment(activity: Activity, var date: FuzzyDate = FuzzyDate().getToday()) :
    DialogFragment(),
    DatePickerDialog.OnDateSetListener {
    var dialog: DatePickerDialog

    init {
        val c = Calendar.getInstance()
        val year = date.year ?: c.get(Calendar.YEAR)
        val month = if (date.month != null) date.month!! - 1 else c.get(Calendar.MONTH)
        val day = date.day ?: c.get(Calendar.DAY_OF_MONTH)
        dialog = DatePickerDialog(activity, this, year, month, day)
        dialog.setButton(
            DialogInterface.BUTTON_NEUTRAL,
            activity.getString(R.string.remove)
        ) { _, which ->
            if (which == DialogInterface.BUTTON_NEUTRAL) {
                date = FuzzyDate()
            }
        }
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        date = FuzzyDate(year, month + 1, day)
    }
}

class InputFilterMinMax(
    private val min: Double,
    private val max: Double,
    private val status: AutoCompleteTextView? = null
) :
    InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        try {
            val input = (dest.toString() + source.toString()).toDouble()
            if (isInRange(min, max, input)) return null
        } catch (nfe: NumberFormatException) {
            Logger.log(nfe)
        }
        return ""
    }

    private fun isInRange(a: Double, b: Double, c: Double): Boolean {
        val statusStrings = currContext()!!.resources.getStringArray(R.array.status_manga)[2]

        if (c == b) {
            status?.setText(statusStrings, false)
            status?.parent?.requestLayout()
        }
        return if (b > a) c in a..b else c in b..a
    }
}


class ZoomOutPageTransformer :
    ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        if (position == 0.0f && PrefManager.getVal(PrefName.LayoutAnimations)) {
            setAnimation(
                view.context,
                view,
                300,
                floatArrayOf(1.3f, 1f, 1.3f, 1f),
                0.5f to 0f
            )
            ObjectAnimator.ofFloat(view, "alpha", 0f, 1.0f)
                .setDuration((200 * (PrefManager.getVal(PrefName.AnimationSpeed) as Float)).toLong())
                .start()
        }
    }
}

fun setAnimation(
    context: Context,
    viewToAnimate: View,
    duration: Long = 150,
    list: FloatArray = floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f),
    pivot: Pair<Float, Float> = 0.5f to 0.5f
) {
    if (PrefManager.getVal(PrefName.LayoutAnimations)) {
        val anim = ScaleAnimation(
            list[0],
            list[1],
            list[2],
            list[3],
            Animation.RELATIVE_TO_SELF,
            pivot.first,
            Animation.RELATIVE_TO_SELF,
            pivot.second
        )
        anim.duration = (duration * (PrefManager.getVal(PrefName.AnimationSpeed) as Float)).toLong()
        anim.setInterpolator(context, R.anim.over_shoot)
        viewToAnimate.startAnimation(anim)
    }
}


class FadingEdgeRecyclerView : RecyclerView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun isPaddingOffsetRequired(): Boolean {
        return !clipToPadding
    }

    override fun getLeftPaddingOffset(): Int {
        return if (clipToPadding) 0 else -paddingLeft
    }

    override fun getTopPaddingOffset(): Int {
        return if (clipToPadding) 0 else -paddingTop
    }

    override fun getRightPaddingOffset(): Int {
        return if (clipToPadding) 0 else paddingRight
    }

    override fun getBottomPaddingOffset(): Int {
        return if (clipToPadding) 0 else paddingBottom
    }
}

fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
    if (lhs == rhs) {
        return 0
    }
    if (lhs.isEmpty()) {
        return rhs.length
    }
    if (rhs.isEmpty()) {
        return lhs.length
    }

    val lhsLength = lhs.length + 1
    val rhsLength = rhs.length + 1

    var cost = Array(lhsLength) { it }
    var newCost = Array(lhsLength) { 0 }

    for (i in 1 until rhsLength) {
        newCost[0] = i

        for (j in 1 until lhsLength) {
            val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

            val costReplace = cost[j - 1] + match
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1

            newCost[j] = min(min(costInsert, costDelete), costReplace)
        }

        val swap = cost
        cost = newCost
        newCost = swap
    }

    return cost[lhsLength - 1]
}

fun List<ShowResponse>.sortByTitle(string: String): List<ShowResponse> {
    val list = this.toMutableList()
    list.sortByTitle(string)
    return list
}

fun MutableList<ShowResponse>.sortByTitle(string: String) {
    val temp: MutableMap<Int, Int> = mutableMapOf()
    for (i in 0 until this.size) {
        temp[i] = levenshtein(string.lowercase(), this[i].name.lowercase())
    }
    val c = temp.toList().sortedBy { (_, value) -> value }.toMap()
    val a = ArrayList(c.keys.toList().subList(0, min(this.size, 25)))
    val b = c.values.toList().subList(0, min(this.size, 25))
    for (i in b.indices.reversed()) {
        if (b[i] > 18 && i < a.size) a.removeAt(i)
    }
    val temp2 = this.toMutableList()
    this.clear()
    for (i in a.indices) {
        this.add(temp2[a[i]])
    }
}

fun String.findBetween(a: String, b: String): String? {
    val string = substringAfter(a, "").substringBefore(b, "")
    return string.ifEmpty { null }
}

fun ImageView.loadImage(url: String?, size: Int = 0) {
    if (!url.isNullOrEmpty()) {
        val localFile = File(url)
        if (localFile.exists()) {
            loadLocalImage(localFile, size)
        } else {
            loadImage(FileUrl(url), size)
        }
    }
}

fun ImageView.loadImage(file: FileUrl?, size: Int = 0) {
    file?.url = PrefManager.getVal<String>(PrefName.ImageUrl).ifEmpty { file?.url ?: "" }
    if (file?.url?.isNotEmpty() == true) {
        tryWith {
            if (file.url.startsWith("content://")) {
                Glide.with(this.context).load(Uri.parse(file.url)).transition(withCrossFade())
                    .override(size).into(this)
            } else {
                val glideUrl = GlideUrl(file.url) { file.headers }
                Glide.with(this.context).load(glideUrl).transition(withCrossFade()).override(size)
                    .into(this)
            }
        }
    }
}

fun ImageView.loadImage(file: FileUrl?, width: Int = 0, height: Int = 0) {
    file?.url = PrefManager.getVal<String>(PrefName.ImageUrl).ifEmpty { file?.url ?: "" }
    if (file?.url?.isNotEmpty() == true) {
        tryWith {
            if (file.url.startsWith("content://")) {
                Glide.with(this.context).load(Uri.parse(file.url)).transition(withCrossFade())
                    .override(width, height).into(this)
            } else {
                val glideUrl = GlideUrl(file.url) { file.headers }
                Glide.with(this.context).load(glideUrl).transition(withCrossFade())
                    .override(width, height)
                    .into(this)
            }
        }
    }
}


fun ImageView.loadLocalImage(file: File?, size: Int = 0) {
    if (file?.exists() == true) {
        tryWith {
            Glide.with(this.context).load(file).transition(withCrossFade()).override(size)
                .into(this)
        }
    }
}

class SafeClickListener(
    private var defaultInterval: Int = 1000,
    private val onSafeCLick: (View) -> Unit
) : View.OnClickListener {

    private var lastTimeClicked: Long = 0

    override fun onClick(v: View) {
        if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
            return
        }
        lastTimeClicked = SystemClock.elapsedRealtime()
        onSafeCLick(v)
    }
}

fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
    val safeClickListener = SafeClickListener {
        onSafeClick(it)
    }
    setOnClickListener(safeClickListener)
}

suspend fun getSize(file: FileUrl): Double? {
    return tryWithSuspend {
        client.head(file.url, file.headers, timeout = 1000).size?.toDouble()?.div(1024 * 1024)
    }
}

suspend fun getSize(file: String): Double? {
    return getSize(FileUrl(file))
}


abstract class GesturesListener : GestureDetector.SimpleOnGestureListener() {
    private var timer: Timer? = null //at class level;
    private val delay: Long = 200

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        processSingleClickEvent(e)
        return super.onSingleTapUp(e)
    }

    override fun onLongPress(e: MotionEvent) {
        processLongClickEvent(e)
        super.onLongPress(e)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        processDoubleClickEvent(e)
        return super.onDoubleTap(e)
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        onScrollYClick(distanceY)
        onScrollXClick(distanceX)
        return super.onScroll(e1, e2, distanceX, distanceY)
    }

    private fun processSingleClickEvent(e: MotionEvent) {
        val handler = Handler(Looper.getMainLooper())
        val mRunnable = Runnable {
            onSingleClick(e)
        }
        timer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    handler.post(mRunnable)
                }
            }, delay)
        }
    }

    private fun processDoubleClickEvent(e: MotionEvent) {
        timer?.apply {
            cancel()
            purge()
        }
        onDoubleClick(e)
    }

    private fun processLongClickEvent(e: MotionEvent) {
        timer?.apply {
            cancel()
            purge()
        }
        onLongClick(e)
    }

    open fun onSingleClick(event: MotionEvent) {}
    open fun onDoubleClick(event: MotionEvent) {}
    open fun onScrollYClick(y: Float) {}
    open fun onScrollXClick(y: Float) {}
    open fun onLongClick(event: MotionEvent) {}
}

fun View.circularReveal(ex: Int, ey: Int, subX: Boolean, time: Long) {
    ViewAnimationUtils.createCircularReveal(
        this,
        if (subX) (ex - x.toInt()) else ex,
        ey - y.toInt(),
        0f,
        max(height, width).toFloat()
    ).setDuration(time).start()
}

fun openLinkInBrowser(link: String?) {
    link?.let {
        try {
            val emptyBrowserIntent = Intent(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                data = Uri.fromParts("http", "", null)
            }
            val sendIntent = Intent().apply {
                action = Intent.ACTION_VIEW
                addCategory(Intent.CATEGORY_BROWSABLE)
                data = Uri.parse(link)
                selector = emptyBrowserIntent
            }
            currContext()!!.startActivity(sendIntent)
        } catch (e: ActivityNotFoundException) {
            snackString("No browser found")
        } catch (e: Exception) {
            Logger.log(e)
        }
    }
}

fun openLinkInYouTube(link: String?) {
    link?.let {
        try {
            val videoIntent = Intent(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                data = Uri.parse(link)
                setPackage("com.google.android.youtube")
            }
            currContext()!!.startActivity(videoIntent)
        } catch (e: ActivityNotFoundException) {
            openLinkInBrowser(link)
        } catch (e: Exception) {
            Logger.log(e)
        }
    }
}

fun saveImageToDownloads(title: String, bitmap: Bitmap, context: Activity) {
    FileProvider.getUriForFile(
        context,
        "$APPLICATION_ID.provider",
        saveImage(
            bitmap,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
            title
        ) ?: return
    )
}

fun savePrefsToDownloads(
    title: String,
    serialized: String,
    context: Activity,
    password: CharArray? = null
) {
    FileProvider.getUriForFile(
        context,
        "$APPLICATION_ID.provider",
        if (password != null) {
            savePrefs(
                serialized,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
                title,
                context,
                password
            ) ?: return
        } else {
            savePrefs(
                serialized,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
                title,
                context
            ) ?: return
        }
    )
}

@SuppressLint("StringFormatMatches")
fun savePrefs(serialized: String, path: String, title: String, context: Context): File? {
    var file = File(path, "$title.ani")
    var counter = 1
    while (file.exists()) {
        file = File(path, "${title}_${counter}.ani")
        counter++
    }

    return try {
        file.writeText(serialized)
        scanFile(file.absolutePath, context)
        toast(String.format(context.getString(R.string.saved_to_path, file.absolutePath)))
        file
    } catch (e: Exception) {
        snackString("Failed to save settings: ${e.localizedMessage}")
        null
    }
}

@SuppressLint("StringFormatMatches")
fun savePrefs(
    serialized: String,
    path: String,
    title: String,
    context: Context,
    password: CharArray
): File? {
    var file = File(path, "$title.sani")
    var counter = 1
    while (file.exists()) {
        file = File(path, "${title}_${counter}.sani")
        counter++
    }

    val salt = generateSalt()

    return try {
        val encryptedData = PreferenceKeystore.encryptWithPassword(password, serialized, salt)

        // Combine salt and encrypted data
        val dataToSave = salt + encryptedData

        file.writeBytes(dataToSave)
        scanFile(file.absolutePath, context)
        toast(String.format(context.getString(R.string.saved_to_path, file.absolutePath)))
        file
    } catch (e: Exception) {
        snackString("Failed to save settings: ${e.localizedMessage}")
        null
    }
}

fun shareImage(title: String, bitmap: Bitmap, context: Context) {

    val contentUri = FileProvider.getUriForFile(
        context,
        "$APPLICATION_ID.provider",
        saveImage(bitmap, context.cacheDir.absolutePath, title) ?: return
    )

    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "image/png"
    intent.putExtra(Intent.EXTRA_TEXT, title)
    intent.putExtra(Intent.EXTRA_STREAM, contentUri)
    context.startActivity(Intent.createChooser(intent, "Share $title"))
}

@SuppressLint("StringFormatMatches")
fun saveImage(image: Bitmap, path: String, imageFileName: String): File? {
    val imageFile = File(path, "$imageFileName.png")
    return try {
        val fOut: OutputStream = FileOutputStream(imageFile)
        image.compress(Bitmap.CompressFormat.PNG, 0, fOut)
        fOut.close()
        scanFile(imageFile.absolutePath, currContext()!!)
        toast(String.format(currContext()!!.getString(R.string.saved_to_path, path)))
        imageFile
    } catch (e: Exception) {
        snackString("Failed to save image: ${e.localizedMessage}")
        null
    }
}

private fun scanFile(path: String, context: Context) {
    MediaScannerConnection.scanFile(context, arrayOf(path), null) { p, _ ->
        Logger.log("Finished scanning $p")
    }
}

class MediaPageTransformer : ViewPager2.PageTransformer {
    private fun parallax(view: View, position: Float) {
        if (position > -1 && position < 1) {
            val width = view.width.toFloat()
            view.translationX = -(position * width * 0.8f)
        }
    }

    override fun transformPage(view: View, position: Float) {

        val bannerContainer = view.findViewById<View>(R.id.itemCompactBanner)
        parallax(bannerContainer, position)
    }
}

class NoGestureSubsamplingImageView(context: Context?, attr: AttributeSet?) :
    SubsamplingScaleImageView(context, attr) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return false
    }
}

fun copyToClipboard(string: String, toast: Boolean = true) {
    val activity = currContext() ?: return
    val clipboard = getSystemService(activity, ClipboardManager::class.java)
    val clip = ClipData.newPlainText("label", string)
    clipboard?.setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        if (toast) snackString(activity.getString(R.string.copied_text, string))
    }
}

fun countDown(media: Media, view: ViewGroup) {
    if (media.anime?.nextAiringEpisode != null && media.anime.nextAiringEpisodeTime != null
        && (media.anime.nextAiringEpisodeTime!! - System.currentTimeMillis() / 1000) <= 86400 * 28.toLong()
    ) {
        val v = ItemCountDownBinding.inflate(LayoutInflater.from(view.context), view, false)
        view.addView(v.root, 0)
        v.mediaCountdownText.text =
            currActivity()?.getString(
                R.string.episode_release_countdown,
                media.anime.nextAiringEpisode!! + 1
            )

        object : CountDownTimer(
            (media.anime.nextAiringEpisodeTime!! + 10000) * 1000 - System.currentTimeMillis(),
            1000
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val a = millisUntilFinished / 1000
                v.mediaCountdown.text = currActivity()?.getString(
                    R.string.time_format,
                    a / 86400,
                    a % 86400 / 3600,
                    a % 86400 % 3600 / 60,
                    a % 86400 % 3600 % 60
                )
            }

            override fun onFinish() {
                v.mediaCountdownContainer.visibility = View.GONE
                snackString(currContext()?.getString(R.string.congrats_vro))
            }
        }.start()
    }
}

fun displayTimer(media: Media, view: ViewGroup) {
    when {
        media.anime != null -> countDown(media, view)
        else -> {}
    }
}

fun MutableMap<String, Genre>.checkId(id: Int): Boolean {
    this.forEach {
        if (it.value.id == id) {
            return false
        }
    }
    return true
}

fun MutableMap<String, Genre>.checkGenreTime(genre: String): Boolean {
    if (containsKey(genre))
        return (System.currentTimeMillis() - get(genre)!!.time) >= (1000 * 60 * 60 * 24 * 7)
    return true
}

fun setSlideIn() = AnimationSet(false).apply {
    if (PrefManager.getVal(PrefName.LayoutAnimations)) {
        var animation: Animation = AlphaAnimation(0.0f, 1.0f)
        val animationSpeed: Float = PrefManager.getVal(PrefName.AnimationSpeed)
        animation.duration = (500 * animationSpeed).toLong()
        animation.interpolator = AccelerateDecelerateInterpolator()
        addAnimation(animation)

        animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0f
        )

        animation.duration = (750 * animationSpeed).toLong()
        animation.interpolator = OvershootInterpolator(1.1f)
        addAnimation(animation)
    }
}

fun setSlideUp() = AnimationSet(false).apply {
    if (PrefManager.getVal(PrefName.LayoutAnimations)) {
        var animation: Animation = AlphaAnimation(0.0f, 1.0f)
        val animationSpeed: Float = PrefManager.getVal(PrefName.AnimationSpeed)
        animation.duration = (500 * animationSpeed).toLong()
        animation.interpolator = AccelerateDecelerateInterpolator()
        addAnimation(animation)

        animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0f
        )

        animation.duration = (750 * animationSpeed).toLong()
        animation.interpolator = OvershootInterpolator(1.1f)
        addAnimation(animation)
    }
}

class EmptyAdapter(private val count: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return EmptyViewHolder(View(parent.context))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}

    override fun getItemCount(): Int = count

    inner class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}

fun getAppString(res: Int): String {
    return currContext()?.getString(res) ?: ""
}

fun toast(string: String?) {
    if (string != null) {
        Logger.log(string)
        MainScope().launch {
            Toast.makeText(currActivity()?.application ?: return@launch, string, Toast.LENGTH_SHORT)
                .show()
        }
    }
}

fun toast(res: Int) {
    toast(getAppString(res))
}

fun snackString(s: String?, activity: Activity? = null, clipboard: String? = null): Snackbar? {
    try { //I have no idea why this sometimes crashes for some people...
        if (s != null) {
            (activity ?: currActivity())?.apply {
                val snackBar = Snackbar.make(
                    window.decorView.findViewById(android.R.id.content),
                    s,
                    Snackbar.LENGTH_SHORT
                )
                runOnUiThread {
                    snackBar.view.apply {
                        updateLayoutParams<FrameLayout.LayoutParams> {
                            gravity = (Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
                            width = WRAP_CONTENT
                        }
                        translationY = -(navBarHeight.dp + 32f)
                        translationZ = 32f
                        updatePadding(16f.px, right = 16f.px)
                        setOnClickListener {
                            snackBar.dismiss()
                        }
                        setOnLongClickListener {
                            copyToClipboard(clipboard ?: s, false)
                            toast(getString(R.string.copied_to_clipboard))
                            true
                        }
                    }
                    snackBar.show()
                }
                return snackBar
            }
            Logger.log(s)
        }
    } catch (e: Exception) {
        Logger.log(e)
        Injekt.get<CrashlyticsInterface>().logException(e)
    }
    return null
}

fun snackString(r: Int, activity: Activity? = null, clipboard: String? = null): Snackbar? {
    return snackString(getAppString(r), activity, clipboard)
}

open class NoPaddingArrayAdapter<T>(context: Context, layoutId: Int, items: List<T>) :
    ArrayAdapter<T>(context, layoutId, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        view.setPadding(0, view.paddingTop, view.paddingRight, view.paddingBottom)
        (view as TextView).setTextColor(Color.WHITE)
        return view
    }
}

@SuppressLint("ClickableViewAccessibility")
class SpinnerNoSwipe : androidx.appcompat.widget.AppCompatSpinner {
    private var mGestureDetector: GestureDetector? = null

    constructor(context: Context) : super(context) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setup()
    }

    private fun setup() {
        mGestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    return performClick()
                }
            })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mGestureDetector!!.onTouchEvent(event)
        return true
    }
}

@SuppressLint("RestrictedApi")
class CustomBottomNavBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : BottomNavigationView(context, attrs) {
    init {
        ViewUtils.doOnApplyWindowInsets(
            this
        ) { view, insets, initialPadding ->
            initialPadding.bottom = 0
            updateLayoutParams<MarginLayoutParams> { bottomMargin = navBarHeight }
            initialPadding.applyToView(view)
            insets
        }
    }
}

fun getCurrentBrightnessValue(context: Context): Float {
    fun getMax(): Int {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        val fields: Array<Field> = powerManager.javaClass.declaredFields
        for (field in fields) {
            if (field.name.equals("BRIGHTNESS_ON")) {
                field.isAccessible = true
                return try {
                    field.get(powerManager)?.toString()?.toInt() ?: 255
                } catch (e: IllegalAccessException) {
                    255
                }
            }
        }
        return 255
    }

    fun getCur(): Float {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            127
        ).toFloat()
    }

    return brightnessConverter(getCur() / getMax(), true)
}

fun brightnessConverter(it: Float, fromLog: Boolean) =
    clamp(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            if (fromLog) log2((it * 256f)) * 12.5f / 100f else 2f.pow(it * 100f / 12.5f) / 256f
        else it, 0.001f, 1f
    )


fun checkCountry(context: Context): Boolean {
    val telMgr = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    return when (telMgr.simState) {
        TelephonyManager.SIM_STATE_ABSENT -> {
            val tz = TimeZone.getDefault().id
            tz.equals("Asia/Kolkata", ignoreCase = true)
        }

        TelephonyManager.SIM_STATE_READY -> {
            val countryCodeValue = telMgr.networkCountryIso
            countryCodeValue.equals("in", ignoreCase = true)
        }

        else -> false
    }
}

const val INCOGNITO_CHANNEL_ID = 26

@SuppressLint("LaunchActivityFromNotification")
fun incognitoNotification(context: Context) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val incognito: Boolean = PrefManager.getVal(PrefName.Incognito)
    if (incognito) {
        val intent = Intent(context, IncognitoNotificationClickReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, Notifications.CHANNEL_INCOGNITO_MODE)
            .setSmallIcon(R.drawable.ic_incognito_24)
            .setContentTitle("Incognito Mode")
            .setContentText("Disable Incognito Mode")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
        notificationManager.notify(INCOGNITO_CHANNEL_ID, builder.build())
    } else {
        notificationManager.cancel(INCOGNITO_CHANNEL_ID)
    }
}

fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

fun openSettings(context: Context, channelId: String?): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val intent = Intent(
            if (channelId != null) Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
            else Settings.ACTION_APP_NOTIFICATION_SETTINGS
        ).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        }
        context.startActivity(intent)
        true
    } else false
}

suspend fun View.pop() {
    currActivity()?.runOnUiThread {
        ObjectAnimator.ofFloat(this@pop, "scaleX", 1f, 1.25f).setDuration(120).start()
        ObjectAnimator.ofFloat(this@pop, "scaleY", 1f, 1.25f).setDuration(120).start()
    }
    delay(120)
    currActivity()?.runOnUiThread {
        ObjectAnimator.ofFloat(this@pop, "scaleX", 1.25f, 1f).setDuration(100).start()
        ObjectAnimator.ofFloat(this@pop, "scaleY", 1.25f, 1f).setDuration(100).start()
    }
    delay(100)
}

fun blurImage(imageView: ImageView, banner: String?) {
    if (banner != null) {
        val radius = PrefManager.getVal<Float>(PrefName.BlurRadius).toInt()
        val sampling = PrefManager.getVal<Float>(PrefName.BlurSampling).toInt()
        val context = imageView.context
        if (!(context as Activity).isDestroyed) {
            val url = PrefManager.getVal<String>(PrefName.ImageUrl).ifEmpty { banner }
            if (PrefManager.getVal(PrefName.BlurBanners)) {
                Glide.with(context as Context)
                    .load(
                        if (banner.startsWith("http")) GlideUrl(url) else if (banner.startsWith("content://")) Uri.parse(
                            url
                        ) else File(url)
                    )
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE).override(400)
                    .apply(RequestOptions.bitmapTransform(BlurTransformation(radius, sampling)))
                    .into(imageView)

            } else {
                Glide.with(context as Context)
                    .load(
                        if (banner.startsWith("http")) GlideUrl(url) else if (banner.startsWith("content://")) Uri.parse(
                            url
                        ) else File(url)
                    )
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE).override(400)
                    .into(imageView)
            }
        }
    } else {
        imageView.setImageResource(R.drawable.linear_gradient_bg)
    }
}

fun Context.getThemeColor(@AttrRes attribute: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attribute, typedValue, true)
    return typedValue.data
}

fun ImageView.openImage(title: String, image: String) {
    setOnLongClickListener {
        ImageViewDialog.newInstance(
            context as FragmentActivity, title, image
        )
    }
}

/**
 * Attempts to open the link in the app, otherwise copies it to the clipboard
 * @param link the link to open
 */
fun openOrCopyAnilistLink(link: String) {
    if (link.startsWith("https://anilist.co/anime/") || link.startsWith("https://anilist.co/manga/")) {
        val mangaAnime = link.substringAfter("https://anilist.co/").substringBefore("/")
        val id =
            link.substringAfter("https://anilist.co/$mangaAnime/").substringBefore("/")
                .toIntOrNull()
        if (id != null && currContext() != null) {
            ContextCompat.startActivity(
                currContext()!!,
                Intent(currContext()!!, MediaDetailsActivity::class.java)
                    .putExtra("mediaId", id),
                null
            )
        } else {
            copyToClipboard(link, true)
        }
    } else if (link.startsWith("https://anilist.co/user/")) {
        val username = link.substringAfter("https://anilist.co/user/").substringBefore("/")
        val id = username.toIntOrNull()
        if (currContext() != null) {
            val intent = Intent(currContext()!!, ProfileActivity::class.java)
            if (id != null) {
                intent.putExtra("userId", id)
            } else {
                intent.putExtra("username", username)
            }
            ContextCompat.startActivity(
                currContext()!!,
                intent,
                null
            )
        } else {
            copyToClipboard(link, true)
        }
    } else if (getYoutubeId(link).isNotEmpty()) {
        openLinkInYouTube(link)
    } else {
        copyToClipboard(link, true)
    }
}

/**
 * Builds the markwon instance with all the plugins
 * @return the markwon instance
 */
fun buildMarkwon(
    activity: Context,
    userInputContent: Boolean = true,
    fragment: Fragment? = null,
    anilist: Boolean = false
): Markwon {
    val glideContext = fragment?.let { Glide.with(it) } ?: Glide.with(activity)
    val markwon = Markwon.builder(activity)
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.linkResolver { _, link ->
                    openOrCopyAnilistLink(link)
                }
            }
        })

        .usePlugin(SoftBreakAddsNewLinePlugin.create())
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(activity))
        .usePlugin(TaskListPlugin.create(activity))
        .usePlugin(SpoilerPlugin(anilist))
        .usePlugin(HtmlPlugin.create { plugin ->
            if (userInputContent) {
                plugin.addHandler(
                    TagHandlerNoOp.create("h1", "h2", "h3", "h4", "h5", "h6", "hr", "pre", "a")
                )
            }
            plugin.addHandler(AlignTagHandler())
        })
        .usePlugin(GlideImagesPlugin.create(object : GlideImagesPlugin.GlideStore {

            private val requestManager: RequestManager = glideContext.apply {
                addDefaultRequestListener(object : RequestListener<Any> {
                    override fun onResourceReady(
                        resource: Any,
                        model: Any,
                        target: Target<Any>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (resource is GifDrawable) {
                            resource.start()
                        }
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Any>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Logger.log("Image failed to load: $model")
                        Logger.log(e as Exception)
                        return false
                    }
                })
            }

            override fun load(drawable: AsyncDrawable): RequestBuilder<Drawable> {
                Logger.log("Loading image: ${drawable.destination}")
                return requestManager.load(drawable.destination)
            }

            override fun cancel(target: Target<*>) {
                Logger.log("Cancelling image load")
                requestManager.clear(target)
            }
        }))
        .build()
    return markwon
}


fun getYoutubeId(url: String): String {
    val regex =
        """(?:youtube\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|(?:youtu\.be|youtube\.com)/)([^"&?/\s]{11})|youtube\.com/""".toRegex()
    val matchResult = regex.find(url)
    return matchResult?.groupValues?.getOrNull(1) ?: ""
}

fun getLanguageCode(language: String): CharSequence {
    val locales = Locale.getAvailableLocales()
    for (locale in locales) {
        if (locale.displayLanguage.equals(language, ignoreCase = true)) {
            val lang: CharSequence = locale.language
            return lang

        }
    }
    val out: CharSequence = "null"
    return out
}

fun getLanguageName(language: String): String? {
    val locales = Locale.getAvailableLocales()
    for (locale in locales) {
        if (locale.language.equals(language, ignoreCase = true)) {
            return locale.displayLanguage
        }
    }
    return null
}

@OptIn(ExperimentalEncodingApi::class)
fun String.decodeBase64ToString(): String {
    return try {
        String(Base64.decode(this), Charsets.UTF_8)
    } catch (e: Exception) {
        Logger.log(e)
        ""
    }
}