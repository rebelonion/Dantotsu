package ani.dantotsu.profile

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.databinding.ActivityFollowBinding
import ani.dantotsu.initActivity
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager

class ActivityActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFollowBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityFollowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.listTitle.text = "Activity"
        binding.listToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = statusBarHeight }
        binding.followerGrid.visibility = ViewGroup.GONE
        binding.followerList.visibility = ViewGroup.GONE
    }
}