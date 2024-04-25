package ani.dantotsu.home.status

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
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
import androidx.webkit.internal.ApiFeature.T
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.home.status.data.StoryItem
import ani.dantotsu.home.status.listener.StoriesCallback
import ani.dantotsu.loadImage
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.User
import ani.dantotsu.profile.UsersDialogFragment
import ani.dantotsu.snackString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar


class Stories @JvmOverloads
constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), View.OnTouchListener {

    private lateinit var storiesList: List<StoryItem>
    private lateinit var activ: FragmentActivity
    private lateinit var loadingViewLayout: ConstraintLayout
    private lateinit var leftTouchPanel: FrameLayout
    private lateinit var rightTouchPanel: FrameLayout
    private lateinit var statusUserContainer: LinearLayout
    private lateinit var imageContentView: ImageView
    private lateinit var loadingView: ProgressBar
    private lateinit var activityLikeCount: TextView
    private lateinit var activityLike: ImageView
    private lateinit var userName: TextView
    private lateinit var userAvatar: ImageView
    private lateinit var time: TextView
    private lateinit var infoText: TextView
    private lateinit var coverImage: ImageView
    private var storyDuration: String = "4"
    private lateinit var animation: ObjectAnimator
    private var storyIndex: Int = 1
    private var userClicked: Boolean = false
    private lateinit var storiesListener: StoriesCallback
    private var oldStoryItem = StoryItem()

    init {
        initLayout()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initLayout() {
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.fragment_status, this, false)
        addView(view)

        if (context is StoriesCallback)
            storiesListener = context as StoriesCallback


        loadingViewLayout = findViewById(R.id.progressBarContainer)
        leftTouchPanel = findViewById(R.id.leftTouchPanel)
        rightTouchPanel = findViewById(R.id.rightTouchPanel)
        imageContentView = findViewById(R.id.contentImageView)
        statusUserContainer = findViewById(R.id.statusUserContainer)
        loadingView = findViewById(R.id.androidStoriesLoadingView)
        coverImage = findViewById(R.id.coverImage)
        userName = findViewById(R.id.statusUserName)
        userAvatar = findViewById(R.id.statusUserAvatar)
        time = findViewById(R.id.statusUserTime)
        infoText = findViewById(R.id.infoText)
        activityLikeCount = findViewById(R.id.activityLikeCount)
        activityLike = findViewById(R.id.activityLike)

        leftTouchPanel.setOnTouchListener(this)
        rightTouchPanel.setOnTouchListener(this)

    }


    fun setStoriesList(storiesList: List<StoryItem>, activity: FragmentActivity) {
        this.storiesList = storiesList
        this.activ = activity
        addLoadingViews(storiesList)
    }

    private fun addLoadingViews(storiesList: List<StoryItem>) {
        var idCounter = 1
        for (story in storiesList) {
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
        val progressBar = findViewWithTag<ProgressBar>("story${storyIndex}")
        loadingView.visibility = View.VISIBLE
        animation = ObjectAnimator.ofInt(progressBar, "progress", 0, 100)
        animation.duration = secondsToMillis(storyDuration)
        animation.interpolator = LinearInterpolator()
        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {
            }

            override fun onAnimationEnd(animator: Animator) {
                if (storyIndex - 1 <= storiesList.size) {
                    if (userClicked) {
                        userClicked = false
                    } else {
                        if (storyIndex < storiesList.size) {
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
        loadStory(storiesList[storyIndex - 1])
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
        val lastProgressBar = findViewWithTag<ProgressBar>("story${storyIndex}")
        lastProgressBar?.let {
            it.progress = 100
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
        if (storyIndex == storiesList.size) {
            completeProgressBar(storyIndex)
            onStoriesCompleted()
            return
        }
        userClicked = true
        animation.end()
        if (storyIndex < storiesList.size)
            storyIndex += 1
        showStory()
    }

    private fun leftPanelTouch() {
        userClicked = true
        animation.end()
        resetProgressBar(storyIndex)
        if (storyIndex > 1)
            storyIndex -= 1
        showStory()
    }

    private fun onStoriesCompleted() {
        if (::storiesListener.isInitialized)
            storiesListener.onStoriesEnd()
    }
    fun pause() {
        animation.pause()
    }
    fun resume() {
        animation.resume()
    }
    private fun loadStory(story: StoryItem) {
        loadingView.visibility = View.GONE
        animation.start()
        blurImage(imageContentView, story.banner)
        userAvatar.loadImage(story.userAvatar)
        coverImage.loadImage(story.cover)
        userName.text = story.userName
        time.text = story.time
        infoText.text = story.info
        statusUserContainer.setOnClickListener {
            ContextCompat.startActivity(context, Intent(context, ProfileActivity::class.java).putExtra("userId", story.id), null)
        }
        coverImage.setOnClickListener{
            ContextCompat.startActivity(context, Intent(context, MediaDetailsActivity::class.java).putExtra("mediaId", story.mediaId), null)
        }
        val likeColor = ContextCompat.getColor(context, R.color.yt_red)
        val notLikeColor = ContextCompat.getColor(context, R.color.bg_opp)
        activityLikeCount.text = story.likes.toString()
        activityLike.setColorFilter(if (story.isLiked) likeColor else notLikeColor)
        activityLike.setOnClickListener {
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            scope.launch {
                val res = Anilist.query.toggleLike(story.activityId!!, "ACTIVITY")
                withContext(Dispatchers.Main) {
                    if (res != null) {

                        if (story.isLiked) {
                            story.likes = story.likes.minus(1)
                        } else {
                            story.likes = story.likes.plus(1)
                        }
                        activityLikeCount.text = (story.likes ?: 0).toString()
                        story.isLiked = !story.isLiked
                        activityLike.setColorFilter(if (story.isLiked) likeColor else notLikeColor)

                    } else {
                        snackString("Failed to like activity")
                    }
                }
            }
        }
        val userList = arrayListOf<User>()
        story.likedBy?.forEach { i ->
            userList.add(User(i.id, i.name.toString(), i.avatar?.medium, i.bannerImage))
        }

        activityLike.setOnLongClickListener {
            UsersDialogFragment().apply {
                userList(userList)
                show(activ.supportFragmentManager, "dialog")
            }
            true
        }
        oldStoryItem = story
    }



}