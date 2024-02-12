package ani.dantotsu.media

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.FragmentCommentsBinding
import ani.dantotsu.loadImage
import ani.dantotsu.navBarHeight
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager

class CommentsFragment : AppCompatActivity(){
    lateinit var binding: FragmentCommentsBinding
    //Comments
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        binding = FragmentCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.CommentsLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }
        binding.commentUserAvatar.loadImage(Anilist.avatar)
        binding.commentTitle.text = "Work in progress"
        binding.commentSend.setOnClickListener {
            //TODO
        }
    }
}