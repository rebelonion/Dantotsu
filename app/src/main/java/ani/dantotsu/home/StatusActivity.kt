package ani.dantotsu.home

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.connections.anilist.api.Activity
import ani.dantotsu.databinding.ActivityStatusBinding
import ani.dantotsu.initActivity
import ani.dantotsu.others.getSerialized
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.home.status.data.StoryItem
import ani.dantotsu.home.status.listener.StoriesCallback
import ani.dantotsu.navBarHeight
import ani.dantotsu.profile.activity.ActivityItemBuilder
import ani.dantotsu.statusBarHeight

class StatusActivity : AppCompatActivity(), StoriesCallback {
    private lateinit var activity: List<Activity>
    private lateinit var binding: ActivityStatusBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        activity = intent.getSerialized("activity")!!
        binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }
        val storiesList = activity.map { StoryItem(
            id = it.userId,
            activityId = it.id,
            mediaId = it.media?.id,
            userName = it.user?.name,
            userAvatar = it.user?.avatar?.large,
            time =  ActivityItemBuilder.getDateTime(it.createdAt),
            info = "${it.user!!.name} ${it.status} ${
                it.progress
                    ?: it.media?.title?.userPreferred
            }",
            cover = it.media?.coverImage?.extraLarge,
            banner = it.media?.bannerImage ?: it.media?.coverImage?.extraLarge,
            likes = it.likeCount ?: 0,
            likedBy = it.likes,
            isLiked = it.isLiked == true
        ) }
        binding.stories.setStoriesList(storiesList, this)
    }

    override fun onPause() {
        super.onPause()
        binding.stories.pause()
    }
    override fun onResume() {
        super.onResume()
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
        finish()
    }

}