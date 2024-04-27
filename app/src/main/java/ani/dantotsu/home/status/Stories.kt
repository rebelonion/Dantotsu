package ani.dantotsu.home.status

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.buildMarkwon
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Activity
import ani.dantotsu.home.status.listener.StoriesCallback
import ani.dantotsu.loadImage
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.User
import ani.dantotsu.profile.UsersDialogFragment
import ani.dantotsu.profile.activity.ActivityItemBuilder
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.util.AniMarkdown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale


class Stories @JvmOverloads
constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), View.OnTouchListener {
    private lateinit var activityList: List<Activity>
    private lateinit var activ: FragmentActivity
    private lateinit var loadingViewLayout: ConstraintLayout
    private lateinit var leftTouchPanel: FrameLayout
    private lateinit var rightTouchPanel: FrameLayout
    private lateinit var statusUserContainer: LinearLayout
    private lateinit var imageContentView: ImageView
    private lateinit var imageContentViewKen: ImageView
    private lateinit var loadingView: ProgressBar
    private lateinit var activityLikeCount: TextView
    private lateinit var textActivity: TextView
    private lateinit var textActivityContainer: LinearLayout
    private lateinit var activityLike: ImageView
    private lateinit var activityLikeContainer: LinearLayout
    private lateinit var userName: TextView
    private lateinit var userAvatar: ImageView
    private lateinit var time: TextView
    private lateinit var infoText: TextView
    private lateinit var coverImage: ImageView
    private var storyDuration: String = "6"
    private lateinit var animation: ObjectAnimator
    private var storyIndex: Int = 1
    private var userClicked: Boolean = false
    private lateinit var storiesListener: StoriesCallback

    init {
        initLayout()
    }
    @SuppressLint("ClickableViewAccessibility")
    fun initLayout() {
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.fragment_status, this, false)
        addView(view)

        if (context is StoriesCallback)
            storiesListener = context as StoriesCallback


        loadingViewLayout = findViewById(R.id.progressBarContainer)
        leftTouchPanel = findViewById(R.id.leftTouchPanel)
        rightTouchPanel = findViewById(R.id.rightTouchPanel)
        imageContentView = findViewById(R.id.contentImageView)
        imageContentViewKen = findViewById(R.id.contentImageViewKen)
        statusUserContainer = findViewById(R.id.statusUserContainer)
        loadingView = findViewById(R.id.androidStoriesLoadingView)
        textActivityContainer = findViewById(R.id.textActivityContainer)
        textActivity = findViewById(R.id.textActivity)
        coverImage = findViewById(R.id.coverImage)
        userName = findViewById(R.id.statusUserName)
        userAvatar = findViewById(R.id.statusUserAvatar)
        time = findViewById(R.id.statusUserTime)
        infoText = findViewById(R.id.infoText)
        activityLikeCount = findViewById(R.id.activityLikeCount)
        activityLike = findViewById(R.id.activityLike)
        activityLikeContainer = findViewById(R.id.statusUserActions)

        leftTouchPanel.setOnTouchListener(this)
        rightTouchPanel.setOnTouchListener(this)
    }


    fun setStoriesList(activityList: List<Activity>, activity: FragmentActivity, startIndex : Int = 1) {
        this.activityList = activityList
        this.activ = activity
        this.storyIndex = startIndex
        addLoadingViews(activityList)
    }

    private fun addLoadingViews(storiesList: List<Activity>) {
        var idCounter = 1
        for (story in storiesList) {
            loadingViewLayout.removeView(findViewWithTag<ProgressBar>("story${idCounter}"))
            val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
            progressBar.visibility = View.VISIBLE
            progressBar.id = idCounter
            progressBar.tag = "story${idCounter++}"
            val params = LayoutParams(0, LayoutParams.WRAP_CONTENT)
            params.marginEnd = 5
            params.marginStart = 5
            loadingViewLayout.addView(progressBar, params)
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(loadingViewLayout)

        var counter = storiesList.size
        for (story in storiesList) {
            val progressBar = findViewWithTag<ProgressBar>("story${counter}")
            if (progressBar != null) {
                if (storiesList.size > 1) {
                    when (counter) {
                        storiesList.size -> {
                            constraintSet.connect(
                                getId("story${counter}"),
                                ConstraintSet.END,
                                LayoutParams.PARENT_ID,
                                ConstraintSet.END
                            )
                            constraintSet.connect(
                                getId("story${counter}"),
                                ConstraintSet.TOP,
                                LayoutParams.PARENT_ID,
                                ConstraintSet.TOP
                            )
                            constraintSet.connect(
                                getId("story${counter}"),
                                ConstraintSet.START,
                                getId("story${counter - 1}"),
                                ConstraintSet.END
                            )
                        }

                        1 -> {
                            constraintSet.connect(
                                getId("story${counter}"),
                                ConstraintSet.TOP,
                                LayoutParams.PARENT_ID,
                                ConstraintSet.TOP
                            )
                            constraintSet.connect(
                                getId("story${counter}"),
                                ConstraintSet.START,
                                LayoutParams.PARENT_ID,
                                ConstraintSet.START
                            )
                            constraintSet.connect(
                                getId("story${counter}"),
                                ConstraintSet.END,
                                getId("story${counter + 1}"),
                                ConstraintSet.START
                            )
                        }

                        else -> {
                            constraintSet.connect(
                                getId("story${counter}"),
                                ConstraintSet.TOP,
                                LayoutParams.PARENT_ID,
                                ConstraintSet.TOP
                            )
                            constraintSet.connect(
                                getId("story${counter}"),
                                ConstraintSet.START,
                                getId("story${counter - 1}"),
                                ConstraintSet.END
                            )
                            constraintSet.connect(
                                getId("story${counter}"),
                                ConstraintSet.END,
                                getId("story${counter + 1}"),
                                ConstraintSet.START
                            )
                        }
                    }
                } else {
                    constraintSet.connect(
                        getId("story${counter}"),
                        ConstraintSet.END,
                        LayoutParams.PARENT_ID,
                        ConstraintSet.END
                    )
                    constraintSet.connect(
                        getId("story${counter}"),
                        ConstraintSet.TOP,
                        LayoutParams.PARENT_ID,
                        ConstraintSet.TOP
                    )
                    constraintSet.connect(
                        getId("story${counter}"),
                        ConstraintSet.START,
                        LayoutParams.PARENT_ID,
                        ConstraintSet.START
                    )
                }
            }
            counter--
        }
        constraintSet.applyTo(loadingViewLayout)
        startShowContent()
    }

    private fun startShowContent() {
        showStory()
    }

    private fun showStory() {
        if (storyIndex > 1) {
            completeProgressBar(storyIndex - 1)
        }
        val progressBar = findViewWithTag<ProgressBar>("story${storyIndex}")
        loadingView.visibility = View.VISIBLE
        animation = ObjectAnimator.ofInt(progressBar, "progress", 0, 100)
        animation.duration = secondsToMillis(storyDuration)
        animation.interpolator = LinearInterpolator()
        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {}

            override fun onAnimationEnd(animator: Animator) {
                if (storyIndex - 1 <= activityList.size) {
                    if (userClicked) {
                        userClicked = false
                    } else {
                        if (storyIndex < activityList.size) {
                            storyIndex += 1
                            showStory()
                        } else {
                            // on stories end
                            loadingView.visibility = View.GONE
                            onStoriesCompleted()
                        }
                    }
                } else {
                    // on stories end
                    loadingView.visibility = View.GONE
                    onStoriesCompleted()
                }
            }

            override fun onAnimationCancel(animator: Animator) {
                progressBar.progress = 100
            }

            override fun onAnimationRepeat(animator: Animator) {}
        })
        loadStory(activityList[storyIndex - 1])
    }

    private fun getId(tag: String): Int {
        return findViewWithTag<ProgressBar>(tag).id
    }

    private fun secondsToMillis(seconds: String): Long {
        return (seconds.toLongOrNull() ?: 3).times(1000)
    }

    private fun resetProgressBar(storyIndex: Int) {
        val currentProgressBar = findViewWithTag<ProgressBar>("story${storyIndex}")
        val lastProgressBar = findViewWithTag<ProgressBar>("story${storyIndex - 1}")
        currentProgressBar?.let {
            it.progress = 0
        }
        lastProgressBar?.let {
            it.progress = 0
        }
    }

    private fun completeProgressBar(storyIndex: Int) {
        for (i in 1 until storyIndex + 1) {
            val progressBar = findViewWithTag<ProgressBar>("story${i}")
            progressBar?.let {
                it.progress = 100
            }
        }
    }


    private var startClickTime = 0L
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        val maxClickDuration = 200
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                startClickTime = Calendar.getInstance().timeInMillis
                animation.pause()
            }

            MotionEvent.ACTION_UP -> {
                val clickDuration = Calendar.getInstance().timeInMillis - startClickTime
                if (clickDuration < maxClickDuration) {
                    //click occurred
                    view?.let {
                        if (it.id == R.id.leftTouchPanel) {
                            leftPanelTouch()
                        } else if (it.id == R.id.rightTouchPanel) {
                            rightPanelTouch()
                        }
                    }
                } else {
                    //hold click occurred
                    animation.resume()
                }
            }
        }
        return true
    }

    private fun rightPanelTouch() {
        if (storyIndex == activityList.size) {
            completeProgressBar(storyIndex)
            onStoriesCompleted()
            return
        }
        userClicked = true
        animation.end()
        if (storyIndex < activityList.size)
            storyIndex += 1
        showStory()
    }

    private fun leftPanelTouch() {
        if (storyIndex == 1) {
            onStoriesPrevious()
            return
        }
        userClicked = true
        animation.end()
        resetProgressBar(storyIndex)
        if (storyIndex > 1)
            storyIndex -= 1
        showStory()
    }

    private fun onStoriesCompleted() {
        if (::storiesListener.isInitialized){
            storyIndex = 1
            storiesListener.onStoriesEnd()
            resetProgressBar(storyIndex)
        }
    }

    private fun onStoriesPrevious() {
        if (::storiesListener.isInitialized) {
            storyIndex = 1
            storiesListener.onStoriesStart()
            resetProgressBar(storyIndex)
        }
    }
    fun pause() {
        animation.pause()
    }
    fun resume() {
        animation.resume()
    }
    private fun loadStory(story: Activity) {
        loadingView.visibility = View.GONE
        animation.start()

        val key = "${story.user?.id}_activities"
        val set = PrefManager.getCustomVal<Set<Int>>(key, setOf()).plus((story.id))
        PrefManager.setCustomVal(key, set)

        userAvatar.loadImage(story.user?.avatar?.large)
        userName.text = story.user?.name
        time.text = ActivityItemBuilder.getDateTime(story.createdAt)
        statusUserContainer.setOnClickListener {
            ContextCompat.startActivity(context, Intent(context, ProfileActivity::class.java)
                .putExtra("userId", story.userId),
                null)
        }

        fun visible(isList: Boolean){
            val visible = if (isList) View.VISIBLE else View.GONE
            val gone = if (isList) View.GONE else View.VISIBLE
            textActivity.visibility = gone
            textActivityContainer.visibility = gone
            infoText.visibility = visible
            coverImage.visibility = visible
            infoText.visibility = if (isList) View.VISIBLE else View.INVISIBLE
            imageContentViewKen.visibility = visible
            imageContentView.visibility = visible
        }
        when (story.typename){
            "ListActivity" -> {
                visible(true)
                val text  = "${story.status?.replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase(Locale.ROOT)
                    }
                    else {
                        it.toString()
                    }
                }} ${story.progress ?: story.media?.title?.userPreferred} " +
                    if (story.status?.contains("completed") == false) {
                        "of ${story.media?.title?.userPreferred}"
                    }else {
                        ""
                    }
                infoText.text = text
                val bannerAnimations: Boolean = PrefManager.getVal(PrefName.BannerAnimations)
                blurImage(if (bannerAnimations)imageContentViewKen else imageContentView, story.media?.bannerImage ?: story.media?.coverImage?.extraLarge)
                coverImage.loadImage(story.media?.coverImage?.extraLarge)
                coverImage.setOnClickListener{
                    ContextCompat.startActivity(context, Intent(context, MediaDetailsActivity::class.java)
                        .putExtra("mediaId", story.media?.id),
                        null)
                }

            }

            "TextActivity" -> {
                visible(false)
                if (!(context as android.app.Activity).isDestroyed) {
                    val markwon = buildMarkwon(context, false)
                    markwon.setMarkdown(
                        textActivity,
                        AniMarkdown.getBasicAniHTML(story.text ?: "")
                    )
                }
            }
            "MessageActivity" -> {
                visible(false)
                if (!(context as android.app.Activity).isDestroyed) {
                    val markwon = buildMarkwon(context, false)
                    markwon.setMarkdown(
                        textActivity,
                        AniMarkdown.getBasicAniHTML(story.message ?: "")
                    )
                }
            }
        }
        val userList = arrayListOf<User>()
        story.likes?.forEach { i ->
            userList.add(User(i.id, i.name.toString(), i.avatar?.medium, i.bannerImage))
        }


        val likeColor = ContextCompat.getColor(context, R.color.yt_red)
        val notLikeColor = ContextCompat.getColor(context, R.color.bg_opp)
        activityLikeCount.text = story.likeCount.toString()
        activityLike.setColorFilter(if (story.isLiked == true) likeColor else notLikeColor)
        activityLikeContainer.setOnClickListener {
            like()
        }
        activityLikeContainer.setOnLongClickListener {
            UsersDialogFragment().apply {
                userList(userList)
                show(activ.supportFragmentManager, "dialog")
            }
            true
        }
    }
    fun like(){
        val story = activityList[storyIndex - 1]
        val likeColor = ContextCompat.getColor(context, R.color.yt_red)
        val notLikeColor = ContextCompat.getColor(context, R.color.bg_opp)
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val res = Anilist.query.toggleLike(story.id, "ACTIVITY")
            withContext(Dispatchers.Main) {
                if (res != null) {
                    if (story.isLiked == true) {
                        story.likeCount = story.likeCount?.minus(1)
                    } else {
                        story.likeCount = story.likeCount?.plus(1)
                    }
                    activityLikeCount.text = (story.likeCount ?: 0).toString()
                    story.isLiked = !story.isLiked!!
                    activityLike.setColorFilter(if (story.isLiked == true) likeColor else notLikeColor)

                } else {
                    snackString("Failed to like activity")
                }
            }
        }
    }
}