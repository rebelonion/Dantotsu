package ani.dantotsu.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LayoutAnimationController
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.buildMarkwon
import ani.dantotsu.connections.anilist.ProfileViewModel
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.FragmentProfileBinding
import ani.dantotsu.loadImage
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaAdaptor
import ani.dantotsu.media.user.ListActivity
import ani.dantotsu.setSlideIn
import ani.dantotsu.setSlideUp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileFragment(): Fragment() {
    lateinit var binding: FragmentProfileBinding
    private lateinit var activity: ProfileActivity
    private lateinit var user: Query.UserProfile
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    val model: ProfileViewModel by activityViewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity = requireActivity() as ProfileActivity
        user = arguments?.getSerializable("user") as Query.UserProfile
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


        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            model.setAnimeFav(user.id)
            model.setMangaFav(user.id)
        }
        
        initRecyclerView(
            model.getAnimeFav(),
            binding.profileFavAnimeContainer,
            binding.profileFavAnimeRecyclerView,
            binding.profileFavAnimeProgressBar,
            binding.profileFavAnimeEmpty,
            binding.profileFavAnime
        )
        
        initRecyclerView(
            model.getMangaFav(),
            binding.profileFavMangaContainer,
            binding.profileFavMangaRecyclerView,
            binding.profileFavMangaProgressBar,
            binding.profileFavMangaEmpty,
            binding.profileFavManga
        )
    }

    override fun onResume() {
        super.onResume()
        if (this::binding.isInitialized) {
            binding.root.requestLayout()
        }
    }

    private fun initRecyclerView(
        mode: LiveData<ArrayList<Media>>,
        container: View,
        recyclerView: RecyclerView,
        progress: View,
        empty: View,
        title: View
    ) {
        container.visibility = View.VISIBLE
        progress.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        empty.visibility = View.GONE
        title.visibility = View.INVISIBLE

        mode.observe(viewLifecycleOwner) {
            recyclerView.visibility = View.GONE
            empty.visibility = View.GONE
            if (it != null) {
                if (it.isNotEmpty()) {
                    recyclerView.adapter = MediaAdaptor(0, it, requireActivity())
                    recyclerView.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.layoutAnimation =
                        LayoutAnimationController(setSlideIn(), 0.25f)

                } else {
                    empty.visibility = View.VISIBLE
                }
                title.visibility = View.VISIBLE
                title.startAnimation(setSlideUp())
                progress.visibility = View.GONE
            }
        }
    }

    companion object {
        fun newInstance(query: Query.UserProfile): ProfileFragment {
            val args = Bundle().apply {
                putSerializable("user", query)
            }
            return ProfileFragment().apply {
                arguments = args
            }
        }
    }

}