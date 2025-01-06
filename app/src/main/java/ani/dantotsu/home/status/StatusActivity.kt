package ani.dantotsu.home.status

import android.os.Bundle
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.api.Activity
import ani.dantotsu.databinding.ActivityStatusBinding
import ani.dantotsu.home.status.listener.StoriesCallback
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.profile.User
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.Logger

class StatusActivity : AppCompatActivity(), StoriesCallback {
    private lateinit var activity: ArrayList<User>
    private lateinit var binding: ActivityStatusBinding
    private var position: Int = -1
    private lateinit var slideInLeft: Animation
    private lateinit var slideOutRight: Animation
    private lateinit var slideOutLeft: Animation
    private lateinit var slideInRight: Animation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        activity = user
        position = intent.getIntExtra("position", -1)
        binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }
        slideInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        slideOutRight = AnimationUtils.loadAnimation(this, R.anim.slide_out_right)
        slideOutLeft = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)

        val key = "activities"
        val watchedActivity = PrefManager.getCustomVal<Set<Int>>(key, setOf())
        if (activity.getOrNull(position) != null) {
            val startFrom = findFirstNonMatch(watchedActivity, activity[position].activity)
            val startIndex = if (startFrom > 0) startFrom else 0
            binding.stories.setStoriesList(
                activityList = activity[position].activity,
                startIndex = startIndex + 1
            )
        } else {
            Logger.log("index out of bounds for position $position of size ${activity.size}")
            finish()
        }

    }

    private fun findFirstNonMatch(watchedActivity: Set<Int>, activity: List<Activity>): Int {
        for (activityItem in activity) {
            if (activityItem.id !in watchedActivity) {
                return activity.indexOf(activityItem)
            }
        }
        return -1
    }

    override fun onPause() {
        super.onPause()
        binding.stories.pause()
    }

    override fun onResume() {
        super.onResume()
        if (hasWindowFocus())
            binding.stories.resume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            binding.stories.resume()
        } else {
            binding.stories.pause()
        }
    }

    override fun onStoriesEnd() {
        position += 1
        if (position < activity.size) {
            val key = "activities"
            val watchedActivity = PrefManager.getCustomVal<Set<Int>>(key, setOf())
            val startFrom = findFirstNonMatch(watchedActivity, activity[position].activity)
            val startIndex = if (startFrom > 0) startFrom else 0
            binding.stories.startAnimation(slideOutLeft)
            binding.stories.setStoriesList(activity[position].activity, startIndex + 1)
            binding.stories.startAnimation(slideInRight)
        } else {
            finish()
        }
    }

    override fun onStoriesStart() {
        position -= 1
        if (position >= 0 && activity[position].activity.isNotEmpty()) {
            val key = "activities"
            val watchedActivity = PrefManager.getCustomVal<Set<Int>>(key, setOf())
            val startFrom = findFirstNonMatch(watchedActivity, activity[position].activity)
            val startIndex = if (startFrom > 0) startFrom else 0
            binding.stories.startAnimation(slideOutRight)
            binding.stories.setStoriesList(activity[position].activity, startIndex + 1)
            binding.stories.startAnimation(slideInLeft)
        } else {
            finish()
        }
    }

    companion object {
        var user: ArrayList<User> = arrayListOf()
    }
}