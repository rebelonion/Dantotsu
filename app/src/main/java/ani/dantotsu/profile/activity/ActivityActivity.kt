package ani.dantotsu.profile.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Activity
import ani.dantotsu.databinding.ActivityFollowBinding
import ani.dantotsu.initActivity
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFollowBinding
    private var adapter: GroupieAdapter = GroupieAdapter()
    private var activityList: List<Activity> = emptyList()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityFollowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.listTitle.text = "Activity"
        binding.listToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = statusBarHeight }
        binding.listRecyclerView.adapter = adapter
        binding.listRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.followerGrid.visibility = ViewGroup.GONE
        binding.followerList.visibility = ViewGroup.GONE
        binding.listBack.setOnClickListener {
            onBackPressed()
        }
        binding.listProgressBar.visibility = ViewGroup.VISIBLE
        var userId: Int? = intent.getIntExtra("userId", -1)
        if (userId == -1) userId = null
        val global = intent.getBooleanExtra("global", false)

        lifecycleScope.launch(Dispatchers.IO) {
            val res = Anilist.query.getFeed(userId, global)

            withContext(Dispatchers.Main){
                res?.data?.page?.activities?.let { activities ->
                    activityList = activities
                    adapter.update(activityList.map { ActivityItem(it){} })
                }
                binding.listProgressBar.visibility = ViewGroup.GONE
            }
        }
    }
}