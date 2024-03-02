package ani.dantotsu.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.ActivityProfileBinding
import ani.dantotsu.loadImage
import ani.dantotsu.media.user.ListActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ProfileActivity : AppCompatActivity(){
    private lateinit var binding: ActivityProfileBinding
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycleScope.launch(Dispatchers.IO) {
            val userid = intent.getIntExtra("userId", 0)
            val respond = Anilist.query.getUserProfile(userid)
            val user = respond?.data?.user ?: return@launch
            val userLevel =  intent.getStringExtra("username")
            withContext(Dispatchers.Main) {
                binding.profileProgressBar.visibility = View.GONE
                binding.profileBannerImage.loadImage(user.bannerImage)
                binding.profileUserAvatar.loadImage(user.avatar?.medium)
                binding.profileUserName.text = "${user.name} $userLevel"
                binding.profileUserInfo.text = user.about
                binding.profileAnimeList.setOnClickListener {
                    ContextCompat.startActivity(
                        this@ProfileActivity, Intent(this@ProfileActivity, ListActivity::class.java)
                            .putExtra("anime", true)
                            .putExtra("userId", user.id)
                            .putExtra("username", user.name), null
                    )
                }
                binding.profileMangaList.setOnClickListener {
                    ContextCompat.startActivity(
                        this@ProfileActivity, Intent(this@ProfileActivity, ListActivity::class.java)
                            .putExtra("anime", false)
                            .putExtra("userId", user.id)
                            .putExtra("username", user.name), null
                    )
                }
                binding.profileUserEpisodesWatched.text = user.statistics.anime.episodesWatched.toString()
                binding.profileUserChaptersRead.text = user.statistics.manga.chaptersRead.toString()

                binding.profileAnimeListImage.loadImage("https://bit.ly/31bsIHq")
                binding.profileMangaListImage.loadImage("https://bit.ly/2ZGfcuG")
            }
        }

    }

}