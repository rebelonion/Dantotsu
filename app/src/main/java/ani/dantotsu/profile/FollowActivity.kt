package ani.dantotsu.profile

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.User
import ani.dantotsu.databinding.ActivityFollowBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.themes.ThemeManager
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FollowActivity : AppCompatActivity(){
    private lateinit var binding: ActivityFollowBinding
    val adapter = GroupieAdapter()
    var users: List<User>? = null
    private lateinit var selected: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityFollowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val layoutType = PrefManager.getVal<Int>(PrefName.FollowerLayout)
        selected = getSelected(layoutType)
        binding.followerGrid.alpha = 0.33f
        binding.followerList.alpha = 0.33f
        selected(selected)
        binding.root.updateLayoutParams<MarginLayoutParams> { topMargin += navBarHeight }
        binding.listRecyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.listRecyclerView.adapter = adapter

        binding.listBack.setOnClickListener { finish() }

        val title = intent.getStringExtra("title")
        binding.listTitle.text = title
        lifecycleScope.launch(Dispatchers.IO) {
            val respond = when (title) {
                "Following" -> Anilist.query.userFollowing(intent.getIntExtra("userId", 0))?.data?.page?.following
                "Followers" -> Anilist.query.userFollowers(intent.getIntExtra("userId", 0))?.data?.page?.followers
                else -> null
            }
            users = respond
            withContext(Dispatchers.Main) {
                fillList()
            }
        }
        binding.followerList.setOnClickListener {
            selected(it as ImageButton)
            PrefManager.setVal(PrefName.FollowerLayout, 0)
            fillList()
        }
        binding.followerGrid.setOnClickListener {
            selected(it as ImageButton)
            PrefManager.setVal(PrefName.FollowerLayout, 1)
            fillList()
        }
    }

    private fun fillList() {
        adapter.clear()
        binding.listRecyclerView.layoutManager = when (getLayoutType(selected)) {
            0 -> LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            1 -> GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
            else -> LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        }
        users?.forEach { user ->
            if (getLayoutType(selected) == 0) {
                adapter.add(FollowerItem(user.id, user.name ?: "Unknown", user.avatar?.medium, user.bannerImage) { onUserClick(it) })
            } else {
                adapter.add(GridFollowerItem(user.id, user.name ?: "Unknown", user.avatar?.medium) { onUserClick(it) })
            }
        }
    }

    fun selected(it: ImageButton) {
        selected.alpha = 0.33f
        selected = it
        selected.alpha = 1f
    }

    private fun getSelected(pos: Int): ImageButton {
        return when (pos) {
            0 -> binding.followerList
            1 -> binding.followerGrid
            else -> binding.followerList
        }
    }

    private fun getLayoutType(it: ImageButton): Int {
        return when (it) {
            binding.followerList -> 0
            binding.followerGrid -> 1
            else -> 0
        }
    }

    private fun onUserClick(id: Int) {
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("userId", id)
        startActivity(intent)
    }
}