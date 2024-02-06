package ani.dantotsu

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Animatable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnticipateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.AnilistHomeViewModel
import ani.dantotsu.databinding.ActivityMainBinding
import ani.dantotsu.databinding.SplashScreenBinding
import ani.dantotsu.download.video.Helper
import ani.dantotsu.home.AnimeFragment
import ani.dantotsu.home.HomeFragment
import ani.dantotsu.home.LoginFragment
import ani.dantotsu.home.MangaFragment
import ani.dantotsu.home.NoInternet
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefManager.asLiveBool
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.settings.saving.SharedPreferenceBooleanLiveData
import ani.dantotsu.subcriptions.Subscription.Companion.startSubscription
import ani.dantotsu.themes.ThemeManager
import eu.kanade.domain.source.service.SourcePreferences
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joery.animatedbottombar.AnimatedBottomBar
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.Serializable


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var incognitoLiveData: SharedPreferenceBooleanLiveData
    private val scope = lifecycleScope
    private var load = false


    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager(this).applyTheme()

        super.onCreate(savedInstanceState)

        //get FRAGMENT_CLASS_NAME from intent
        val fragment = intent.getStringExtra("FRAGMENT_CLASS_NAME")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val _bottomBar = findViewById<AnimatedBottomBar>(R.id.navbar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            val backgroundDrawable = _bottomBar.background as GradientDrawable
            val currentColor = backgroundDrawable.color?.defaultColor ?: 0
            val semiTransparentColor = (currentColor and 0x00FFFFFF) or 0xF9000000.toInt()
            backgroundDrawable.setColor(semiTransparentColor)
            _bottomBar.background = backgroundDrawable
        }
        _bottomBar.background = ContextCompat.getDrawable(this, R.drawable.bottom_nav_gray)


        val offset = try {
            val statusBarHeightId = resources.getIdentifier("status_bar_height", "dimen", "android")
            resources.getDimensionPixelSize(statusBarHeightId)
        } catch (e: Exception) {
            statusBarHeight
        }
        val layoutParams = binding.incognito.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = 11 * offset / 12
        binding.incognito.layoutParams = layoutParams
        incognitoLiveData = PrefManager.getLiveVal(
            PrefName.Incognito,
            false
        ).asLiveBool()
        incognitoLiveData.observe(this) {
            if (it) {
                val slideDownAnim = ObjectAnimator.ofFloat(
                    binding.incognito,
                    View.TRANSLATION_Y,
                    -(binding.incognito.height.toFloat() + statusBarHeight),
                    0f
                )
                slideDownAnim.duration = 200
                slideDownAnim.start()
                binding.incognito.visibility = View.VISIBLE
            } else {
                val slideUpAnim = ObjectAnimator.ofFloat(
                    binding.incognito,
                    View.TRANSLATION_Y,
                    0f,
                    -(binding.incognito.height.toFloat() + statusBarHeight)
                )
                slideUpAnim.duration = 200
                slideUpAnim.start()
                //wait for animation to finish
                Handler(Looper.getMainLooper()).postDelayed(
                    { binding.incognito.visibility = View.GONE },
                    200
                )
            }
        }
        incognitoNotification(this)

        var doubleBackToExitPressedOnce = false
        onBackPressedDispatcher.addCallback(this) {
            if (doubleBackToExitPressedOnce) {
                finish()
            }
            doubleBackToExitPressedOnce = true
            snackString(this@MainActivity.getString(R.string.back_to_exit))
            Handler(Looper.getMainLooper()).postDelayed(
                { doubleBackToExitPressedOnce = false },
                2000
            )
        }

        val preferences: SourcePreferences = Injekt.get()
        if (preferences.animeExtensionUpdatesCount()
                .get() > 0 || preferences.mangaExtensionUpdatesCount().get() > 0
        ) {
            Toast.makeText(
                this,
                "You have extension updates available!",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.root.isMotionEventSplittingEnabled = false

        lifecycleScope.launch {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                val splash = SplashScreenBinding.inflate(layoutInflater)
                binding.root.addView(splash.root)
                (splash.splashImage.drawable as Animatable).start()

                delay(1200)

                ObjectAnimator.ofFloat(
                    splash.root,
                    View.TRANSLATION_Y,
                    0f,
                    -splash.root.height.toFloat()
                ).apply {
                    interpolator = AnticipateInterpolator()
                    duration = 200L
                    doOnEnd { binding.root.removeView(splash.root) }
                    start()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                ObjectAnimator.ofFloat(
                    splashScreenView,
                    View.TRANSLATION_Y,
                    0f,
                    -splashScreenView.height.toFloat()
                ).apply {
                    interpolator = AnticipateInterpolator()
                    duration = 200L
                    doOnEnd { splashScreenView.remove() }
                    start()
                }
            }
        }


        binding.root.doOnAttach {
            initActivity(this)
            selectedOption = if (fragment != null) {
                when (fragment) {
                    AnimeFragment::class.java.name -> 0
                    HomeFragment::class.java.name -> 1
                    MangaFragment::class.java.name -> 2
                    else -> 1
                }
            } else {
                PrefManager.getVal(PrefName.DefaultStartUpTab)
            }
            binding.includedNavbar.navbarContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBarHeight

            }
        }
        val offlineMode: Boolean = PrefManager.getVal(PrefName.OfflineMode)
        if (!isOnline(this)) {
            snackString(this@MainActivity.getString(R.string.no_internet_connection))
            startActivity(Intent(this, NoInternet::class.java))
        } else {
            if (offlineMode) {
                snackString(this@MainActivity.getString(R.string.no_internet_connection))
                startActivity(Intent(this, NoInternet::class.java))
            } else {
                val model: AnilistHomeViewModel by viewModels()
                model.genres.observe(this) { it ->
                    if (it != null) {
                        if (it) {
                            val navbar = binding.includedNavbar.navbar
                            bottomBar = navbar
                            navbar.visibility = View.VISIBLE
                            binding.mainProgressBar.visibility = View.GONE
                            val mainViewPager = binding.viewpager
                            mainViewPager.isUserInputEnabled = false
                            mainViewPager.adapter =
                                ViewPagerAdapter(supportFragmentManager, lifecycle)
                            mainViewPager.setPageTransformer(ZoomOutPageTransformer())
                            navbar.setOnTabSelectListener(object :
                                AnimatedBottomBar.OnTabSelectListener {
                                override fun onTabSelected(
                                    lastIndex: Int,
                                    lastTab: AnimatedBottomBar.Tab?,
                                    newIndex: Int,
                                    newTab: AnimatedBottomBar.Tab
                                ) {
                                    navbar.animate().translationZ(12f).setDuration(200).start()
                                    selectedOption = newIndex
                                    mainViewPager.setCurrentItem(newIndex, false)
                                }
                            })
                            navbar.selectTabAt(selectedOption)
                            mainViewPager.post {
                                mainViewPager.setCurrentItem(
                                    selectedOption,
                                    false
                                )
                            }
                        } else {
                            binding.mainProgressBar.visibility = View.GONE
                        }
                    }
                }
                //Load Data
                if (!load) {
                    scope.launch(Dispatchers.IO) {
                        model.loadMain(this@MainActivity)
                        val id = intent.extras?.getInt("mediaId", 0)
                        val isMAL = intent.extras?.getBoolean("mal") ?: false
                        val cont = intent.extras?.getBoolean("continue") ?: false
                        if (id != null && id != 0) {
                            val media = withContext(Dispatchers.IO) {
                                Anilist.query.getMedia(id, isMAL)
                            }
                            if (media != null) {
                                media.cameFromContinue = cont
                                startActivity(
                                    Intent(this@MainActivity, MediaDetailsActivity::class.java)
                                        .putExtra("media", media as Serializable)
                                )
                            } else {
                                snackString(this@MainActivity.getString(R.string.anilist_not_found))
                            }
                        }
                        delay(500)
                        startSubscription()
                    }
                    load = true
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!(PrefManager.getVal(PrefName.AllowOpeningLinks) as Boolean)) {
                        CustomBottomDialog.newInstance().apply {
                            title = "Allow Dantotsu to automatically open Anilist & MAL Links?"
                            val md = "Open settings & click +Add Links & select Anilist & Mal urls"
                            addView(TextView(this@MainActivity).apply {
                                val markWon =
                                    Markwon.builder(this@MainActivity)
                                        .usePlugin(SoftBreakAddsNewLinePlugin.create()).build()
                                markWon.setMarkdown(this, md)
                            })

                            setNegativeButton(this@MainActivity.getString(R.string.no)) {
                                PrefManager.setVal(PrefName.AllowOpeningLinks, true)
                                dismiss()
                            }

                            setPositiveButton(this@MainActivity.getString(R.string.yes)) {
                                PrefManager.setVal(PrefName.AllowOpeningLinks, true)
                                tryWith(true) {
                                    startActivity(
                                        Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS)
                                            .setData(Uri.parse("package:$packageName"))
                                    )
                                }
                                dismiss()
                            }
                        }.show(supportFragmentManager, "dialog")
                    }
                }
            }
        }
        //TODO: Remove this
        GlobalScope.launch(Dispatchers.IO) {
            val index = Helper.downloadManager(this@MainActivity).downloadIndex
            val downloadCursor = index.getDownloads()
            while (downloadCursor.moveToNext()) {
                val download = downloadCursor.download
                Log.e("Downloader", download.request.uri.toString())
                Log.e("Downloader", download.request.id)
                Log.e("Downloader", download.request.mimeType.toString())
                Log.e("Downloader", download.request.data.size.toString())
                Log.e("Downloader", download.bytesDownloaded.toString())
                Log.e("Downloader", download.state.toString())
                Log.e("Downloader", download.failureReason.toString())

                if (download.state == Download.STATE_FAILED) {  //simple cleanup
                    Helper.downloadManager(this@MainActivity).removeDownload(download.request.id)
                }
            }
        }
    }


    //ViewPager
    private class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return AnimeFragment()
                1 -> return if (Anilist.token != null) HomeFragment() else LoginFragment()
                2 -> return MangaFragment()
            }
            return LoginFragment()
        }
    }

}
