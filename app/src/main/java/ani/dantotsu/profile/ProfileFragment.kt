package ani.dantotsu.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ani.dantotsu.buildMarkwon
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.FragmentProfileBinding
import ani.dantotsu.loadImage
import ani.dantotsu.media.user.ListActivity

class ProfileFragment(private val user: Query.UserProfile, private val activity: ProfileActivity): Fragment() {
    lateinit var binding: FragmentProfileBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val markwon = buildMarkwon(activity, false)
        markwon.setMarkdown(binding.profileUserBio, user.about?:"")
        binding.userInfoContainer.visibility = if (user.about != null) View.VISIBLE else View.GONE

        binding.profileAnimeList.setOnClickListener {
            ContextCompat.startActivity(
                activity, Intent(activity, ListActivity::class.java)
                    .putExtra("anime", true)
                    .putExtra("userId", user.id)
                    .putExtra("username", user.name), null
            )
        }
        binding.profileMangaList.setOnClickListener {
            ContextCompat.startActivity(
                activity, Intent(activity, ListActivity::class.java)
                    .putExtra("anime", false)
                    .putExtra("userId", user.id)
                    .putExtra("username", user.name), null
            )
        }
        binding.profileAnimeListImage.loadImage("https://bit.ly/31bsIHq")
        binding.profileMangaListImage.loadImage("https://bit.ly/2ZGfcuG")
        binding.statsEpisodesWatched.text = user.statistics.anime.episodesWatched.toString()
        binding.statsDaysWatched.text = (user.statistics.anime.minutesWatched / (24 * 60)).toString()
        binding.statsTotalAnime.text = user.statistics.anime.count.toString()
        binding.statsAnimeMeanScore.text = user.statistics.anime.meanScore.toString()
        binding.statsChaptersRead.text = user.statistics.manga.chaptersRead.toString()
        binding.statsVolumeRead.text = (user.statistics.manga.volumesRead).toString()
        binding.statsTotalManga.text = user.statistics.manga.count.toString()
        binding.statsMangaMeanScore.text = user.statistics.manga.meanScore.toString()


    }
}