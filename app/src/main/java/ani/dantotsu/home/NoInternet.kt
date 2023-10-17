package ani.dantotsu.home

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.databinding.ActivityNoInternetBinding
import ani.dantotsu.isOnline
import ani.dantotsu.navBarHeight
import ani.dantotsu.startMainActivity
import ani.dantotsu.statusBarHeight

class NoInternet : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityNoInternetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.refreshContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }
        binding.refreshButton.setOnClickListener {
            if (isOnline(this)) {
                startMainActivity(this)
            }
        }
    }
}