package ani.dantotsu.profile

import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityActivityBinding
import ani.dantotsu.initActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager


class ActivityActivity : AppCompatActivity() {
    private lateinit var binding: ActivityActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val immersiveMode = PrefManager.getVal<Boolean>(PrefName.ImmersiveMode)
        if (immersiveMode) {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityActivityBinding.inflate(layoutInflater)
        if (!immersiveMode) {
            this.window.statusBarColor =
                ContextCompat.getColor(this, R.color.nav_bg_inv)
            binding.root.fitsSystemWindows = true

        } else {
            binding.root.fitsSystemWindows = false
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            binding.listTitle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
            }
        }
        setContentView(binding.root)
    }
}