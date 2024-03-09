package ani.dantotsu.profile

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
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
import ani.dantotsu.connections.anilist.ProfileViewModel
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.FragmentProfileBinding
import ani.dantotsu.loadImage
import ani.dantotsu.media.Author
import ani.dantotsu.media.AuthorAdapter
import ani.dantotsu.media.Character
import ani.dantotsu.media.CharacterAdapter
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaAdaptor
import ani.dantotsu.media.user.ListActivity
import ani.dantotsu.setSlideIn
import ani.dantotsu.setSlideUp
import ani.dantotsu.util.ColorEditor.Companion.toCssColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ProfileFragment : Fragment() {
    lateinit var binding: FragmentProfileBinding
    private lateinit var activity: ProfileActivity
    private lateinit var user: Query.UserProfile
    private val favStaff = arrayListOf<Author>()
    private val favCharacter = arrayListOf<Character>()
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
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            model.setData(user.id)
        }
        val backGroundColorTypedValue = TypedValue()
        val textColorTypedValue = TypedValue()
        activity.theme.resolveAttribute(
            android.R.attr.windowBackground,
            backGroundColorTypedValue,
            true
        )
        activity.theme.resolveAttribute(
            com.google.android.material.R.attr.colorOnBackground,
            textColorTypedValue,
            true
        )

        binding.profileUserBio.settings.loadWithOverviewMode = true
        binding.profileUserBio.settings.useWideViewPort = true
        binding.profileUserBio.setInitialScale(1)
        val styledHtml = styled(
            convertMarkdownToHtml(user.about ?: ""),
            backGroundColorTypedValue.data,
            textColorTypedValue.data
        )
        binding.profileUserBio.loadDataWithBaseURL(
            null,
            styledHtml,
            "text/html; charset=utf-8",
            "UTF-8",
            null
        )
        binding.userInfoContainer.visibility =
            if (user.about != null) View.VISIBLE else View.GONE

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
        binding.statsEpisodesWatched.text = user.statistics.anime.episodesWatched.toString()
        binding.statsDaysWatched.text =
            (user.statistics.anime.minutesWatched / (24 * 60)).toString()
        binding.statsTotalAnime.text = user.statistics.anime.count.toString()
        binding.statsAnimeMeanScore.text = user.statistics.anime.meanScore.toString()
        binding.statsChaptersRead.text = user.statistics.manga.chaptersRead.toString()
        binding.statsVolumeRead.text = (user.statistics.manga.volumesRead).toString()
        binding.statsTotalManga.text = user.statistics.manga.count.toString()
        binding.statsMangaMeanScore.text = user.statistics.manga.meanScore.toString()
        model.getListImages().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                binding.profileAnimeListImage.loadImage(it[0] ?: "https://bit.ly/31bsIHq")
                binding.profileMangaListImage.loadImage(it[1] ?: "https://bit.ly/2ZGfcuG")
            }
        }
        initRecyclerView(
            model.getAnimeFav(),
            binding.profileFavAnimeContainer,
            binding.profileFavAnimeRecyclerView,
            binding.profileFavAnimeProgressBar,
            binding.profileFavAnime
        )

        initRecyclerView(
            model.getMangaFav(),
            binding.profileFavMangaContainer,
            binding.profileFavMangaRecyclerView,
            binding.profileFavMangaProgressBar,
            binding.profileFavManga
        )

        user.favourites?.characters?.nodes?.forEach { i ->
            favCharacter.add(Character(i.id, i.name.full, i.image.large, i.image.large, ""))
        }

        user.favourites?.staff?.nodes?.forEach { i ->
            favStaff.add(Author(i.id, i.name.full, i.image.large , "" ))
        }

        setFavPeople()
    }

    override fun onResume() {
        super.onResume()
        if (this::binding.isInitialized) {
            binding.root.requestLayout()
            setFavPeople()
            model.refresh()
        }
    }

    private fun setFavPeople() {
        if (favStaff.isEmpty()) {
            binding.profileFavStaffContainer.visibility = View.GONE
        }
        binding.profileFavStaffRecycler.adapter = AuthorAdapter(favStaff)
        binding.profileFavStaffRecycler.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        if (favCharacter.isEmpty()) {
            binding.profileFavCharactersContainer.visibility = View.GONE
        }
        binding.profileFavCharactersRecycler.adapter = CharacterAdapter(favCharacter)
        binding.profileFavCharactersRecycler.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
    }

    private fun convertMarkdownToHtml(markdown: String): String {
        val regex = """\[\!\[(.*?)\]\((.*?)\)\]\((.*?)\)""".toRegex()
        return regex.replace(markdown) { matchResult ->
            val altText = matchResult.groupValues[1]
            val imageUrl = matchResult.groupValues[2]
            val linkUrl = matchResult.groupValues[3]
            """<a href="$linkUrl"><img src="$imageUrl" alt="$altText"></a>"""
        }
    }

    private fun initRecyclerView(
        mode: LiveData<ArrayList<Media>>,
        container: View,
        recyclerView: RecyclerView,
        progress: View,
        title: View
    ) {
        container.visibility = View.VISIBLE
        progress.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        title.visibility = View.INVISIBLE

        mode.observe(viewLifecycleOwner) {
            recyclerView.visibility = View.GONE
            if (it != null) {
                if (it.isNotEmpty()) {
                    recyclerView.adapter = MediaAdaptor(0, it, requireActivity(), fav=true)
                    recyclerView.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.layoutAnimation =
                        LayoutAnimationController(setSlideIn(), 0.25f)

                } else {
                    container.visibility = View.GONE
                }
                title.visibility = View.VISIBLE
                title.startAnimation(setSlideUp())
                progress.visibility = View.GONE
            }
        }
    }

    private fun styled(html: String, backGroundColor: Int, textColor: Int): String {  //istg anilist has the worst api
        //remove some of the html entities
        val step1 = html.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("<pre>", "")
            .replace("`", "")
            .replace("~", "")

        val step2 = step1.replace("(?s)___(.*?)___".toRegex(), "<br><em><strong>$1</strong></em><br>")
        val step3 = step2.replace("(?s)__(.*?)__".toRegex(), "<br><strong>$1</strong><br>")


        return """
            <html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, charset=UTF-8">
        <style>
            body {
                background-color: ${backGroundColor.toCssColor()};
                color: ${textColor.toCssColor()};
                margin: 0;
                padding: 0;
                max-width: 100%;
                overflow-x: hidden; /* Prevent horizontal scrolling */
            }
            img {
                max-width: 100%;
                height: auto; /* Maintain aspect ratio */
            }
            video {
                max-width: 100%;
                height: auto; /* Maintain aspect ratio */
            }
            a {
                color: ${textColor.toCssColor()};
            }
            /* Add responsive design elements for other content as needed */
        </style>
</head>
<body>
    $step3
</body>

    """.trimIndent()
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