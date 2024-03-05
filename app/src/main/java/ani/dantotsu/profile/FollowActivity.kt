package ani.dantotsu.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.ActivityFollowBinding
import ani.dantotsu.initActivity
import ani.dantotsu.themes.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FollowActivity : AppCompatActivity(){
    private lateinit var binding: ActivityFollowBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityFollowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.listTitle.text = intent.getStringExtra("title")
        lifecycleScope.launch(Dispatchers.IO) {
            val respond = Anilist.query.userFollowing(intent.getIntExtra("userId", 0))
            val user = respond?.data?.following
            withContext(Dispatchers.Main) {
                user?.id
            }
        }
    }
}