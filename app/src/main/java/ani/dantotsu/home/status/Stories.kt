package ani.dantotsu.home.status

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.buildMarkwon
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Activity
import ani.dantotsu.databinding.FragmentStatusBinding
import ani.dantotsu.getThemeColor
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
    private lateinit var activity: FragmentActivity
    private lateinit var binding: FragmentStatusBinding
    private lateinit var animation: ObjectAnimator
    private lateinit var activityList: List<Activity>
    private lateinit var storiesListener: StoriesCallback
    private var userClicked: Boolean = false
    private var storyIndex: Int = 1
    private var primaryColor : Int = 0
    private var onPrimaryColor : Int = 0
    private var storyDuration: Int = 6

    init {
        initLayout()
    }
    @SuppressLint("ClickableViewAccessibility")
    fun initLayout() {
        val inflater: LayoutInflater = LayoutInflater.from(context)
        binding = FragmentStatusBinding.inflate(inflater, this, false)
        addView(binding.root)
    
        primaryColor = context.getThemeColor(com.google.android.material.R.attr.colorPrimary)
        onPrimaryColor = context.getThemeColor(com.google.android.material.R.attr.colorOnPrimary)

        if (context is StoriesCallback)
            storiesListener = context as StoriesCallback

        binding.leftTouchPanel.setOnTouchListener(this)
        binding.rightTouchPanel.setOnTouchListener(this)
    }


    fun setStoriesList(activityList: List<Activity>, activity: FragmentActivity, startIndex : Int = 1) {
        this.activityList = activityList
        this.activity = activity
        this.storyIndex = startIndex
        addLoadingViews(activityList)
    }

    private fun addLoadingViews(storiesList: List<Activity>) {
        var idCounter = 1
        for (story in storiesList) {
            binding.progressBarContainer.removeView(findViewWithTag<ProgressBar>("story${idCounter}"))
            val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
            progressBar.visibility = View.VISIBLE
            progressBar.id = idCounter
            progressBar.tag = "story${idCounter++}"
            progressBar.progressBackgroundTintList = ColorStateList.valueOf(primaryColor)
            progressBar.progressTintList = ColorStateList.valueOf(onPrimaryColor)
            val params = LayoutParams(0, LayoutParams.WRAP_CONTENT)
            params.marginEnd = 5
            params.marginStart = 5
            binding.progressBarContainer.addView(progressBar, params)
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.progressBarContainer)

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
        constraintSet.applyTo(binding.progressBarContainer)
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
        binding.androidStoriesLoadingView.visibility = View.VISIBLE
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
                            binding.androidStoriesLoadingView.visibility = View.GONE
                            onStoriesCompleted()
                        }
                    }
                } else {
                    // on stories end
                    binding.androidStoriesLoadingView.visibility = View.GONE
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

    private fun secondsToMillis(seconds: Int): Long {
        return (seconds.toLong()).times(1000)
    }

    private fun resetProgressBar(storyIndex: Int) {
        for (i in storyIndex until activityList.size + 1) {
            val progressBar = findViewWithTag<ProgressBar>("story${i}")
            progressBar?.let {
                it.progress = 0
            }
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
        if (storyIndex <= activityList.size)
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

        val key = "activities"
        val set = PrefManager.getCustomVal<Set<Int>>(key, setOf()).plus((story.id))
        val newList = set.sorted().takeLast(200).toSet()
        PrefManager.setCustomVal(key, newList)
        binding.statusUserAvatar.loadImage(story.user?.avatar?.large)
        binding.statusUserName.text = story.user?.name
        binding.statusUserTime.text = ActivityItemBuilder.getDateTime(story.createdAt)
        binding.statusUserContainer.setOnClickListener {
            ContextCompat.startActivity(context, Intent(context, ProfileActivity::class.java)
                .putExtra("userId", story.userId),
                null)
        }

        fun visible(isList: Boolean){
            val visible = if (isList) View.VISIBLE else View.GONE
            val gone = if (isList) View.GONE else View.VISIBLE
            binding.textActivity.visibility = gone
            binding.textActivityContainer.visibility = gone
            binding.infoText.visibility = visible
            binding.coverImage.visibility = visible
            binding.infoText.visibility = if (isList) View.VISIBLE else View.INVISIBLE
            binding.infoText.text = ""
            binding.contentImageViewKen.visibility = visible
            binding.contentImageView.visibility = visible
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
                    if (story.status?.contains("completed") == false && !story.status.contains("plans") && !story.status.contains("repeating")) {
                        "of ${story.media?.title?.userPreferred}"
                    }else {
                        ""
                    }
                binding.infoText.text = text
                val bannerAnimations: Boolean = PrefManager.getVal(PrefName.BannerAnimations)
                blurImage(if (bannerAnimations)binding.contentImageViewKen else binding.contentImageView, story.media?.bannerImage ?: story.media?.coverImage?.extraLarge)
                binding.coverImage.loadImage(story.media?.coverImage?.extraLarge)
                binding.coverImage.setOnClickListener{
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
                        binding.textActivity,
                        AniMarkdown.getBasicAniHTML(story.text ?: "")
                    )
                }
            }
            "MessageActivity" -> {
                visible(false)
                if (!(context as android.app.Activity).isDestroyed) {
                    val markwon = buildMarkwon(context, false)
                    markwon.setMarkdown(
                        binding.textActivity,
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
        binding.activityLikeCount.text = story.likeCount.toString()
        binding.activityLike.setColorFilter(if (story.isLiked == true) likeColor else notLikeColor)
        binding.statusUserActions.setOnClickListener {
            like()
        }

        binding.statusUserActions.setOnLongClickListener {
            val context = activity
            UsersDialogFragment().apply {
                userList(userList)
                show(context.supportFragmentManager, "dialog")
            }
            true
        }
        binding.androidStoriesLoadingView.visibility = View.GONE
        animation.start()
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
                    binding.activityLikeCount.text = (story.likeCount ?: 0).toString()
                    story.isLiked = !story.isLiked!!
                    binding.activityLike.setColorFilter(if (story.isLiked == true) likeColor else notLikeColor)

                } else {
                    snackString("Failed to like activity")
                }
            }
        }
    }
}