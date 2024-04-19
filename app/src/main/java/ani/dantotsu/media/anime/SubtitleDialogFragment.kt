package ani.dantotsu.media.anime

import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.R
import ani.dantotsu.databinding.BottomSheetSubtitlesBinding
import ani.dantotsu.databinding.ItemSubtitleTextBinding
import ani.dantotsu.media.MediaDetailsViewModel
import ani.dantotsu.parsers.Subtitle
import ani.dantotsu.settings.saving.PrefManager

class SubtitleDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSubtitlesBinding? = null
    private val binding get() = _binding!!
    val model: MediaDetailsViewModel by activityViewModels()
    private lateinit var episode: Episode

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSubtitlesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.getMedia().observe(viewLifecycleOwner) { media ->
            episode = media?.anime?.episodes?.get(media.anime.selectedEpisode) ?: return@observe
            val currentExtractor =
                episode.extractors?.find { it.server.name == episode.selectedExtractor }
                    ?: return@observe
            binding.subtitlesRecycler.layoutManager = LinearLayoutManager(requireContext())
            binding.subtitlesRecycler.adapter = SubtitleAdapter(currentExtractor.subtitles)
        }
    }

    inner class SubtitleAdapter(val subtitles: List<Subtitle>) :
        RecyclerView.Adapter<SubtitleAdapter.StreamViewHolder>() {
        inner class StreamViewHolder(val binding: ItemSubtitleTextBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder =
            StreamViewHolder(
                ItemSubtitleTextBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        @OptIn(UnstableApi::class)
        override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
            val binding = holder.binding
            if (position == 0) {
                binding.subtitleTitle.setText(R.string.none)
                model.getMedia().observe(viewLifecycleOwner) { media ->
                    val mediaID: Int = media.id
                    val selSubs = PrefManager.getNullableCustomVal(
                        "subLang_${mediaID}",
                        null,
                        String::class.java
                    )
                    if (episode.selectedSubtitle != null && selSubs != "None") {
                        binding.root.setCardBackgroundColor(TRANSPARENT)
                    }
                }
                binding.root.setOnClickListener {
                    episode.selectedSubtitle = null
                    model.setEpisode(episode, "Subtitle")
                    model.getMedia().observe(viewLifecycleOwner) { media ->
                        val mediaID: Int = media.id
                        PrefManager.setCustomVal("subLang_${mediaID}", "None")
                    }
                    dismiss()
                }
            } else {
                binding.subtitleTitle.text = when (subtitles[position - 1].language) {
                    "ja-JP" -> "[ja-JP] Japanese"
                    "en-US" -> "[en-US] English"
                    "de-DE" -> "[de-DE] German"
                    "es-ES" -> "[es-ES] Spanish"
                    "es-419" -> "[es-419] Spanish"
                    "fr-FR" -> "[fr-FR] French"
                    "it-IT" -> "[it-IT] Italian"
                    "pt-BR" -> "[pt-BR] Portuguese (Brazil)"
                    "pt-PT" -> "[pt-PT] Portuguese (Portugal)"
                    "ru-RU" -> "[ru-RU] Russian"
                    "zh-CN" -> "[zh-CN] Chinese (Simplified)"
                    "tr-TR" -> "[tr-TR] Turkish"
                    "ar-ME" -> "[ar-ME] Arabic"
                    "ar-SA" -> "[ar-SA] Arabic (Saudi Arabia)"
                    "uk-UK" -> "[uk-UK] Ukrainian"
                    "he-IL" -> "[he-IL] Hebrew"
                    "pl-PL" -> "[pl-PL] Polish"
                    "ro-RO" -> "[ro-RO] Romanian"
                    "sv-SE" -> "[sv-SE] Swedish"
                    else -> if (subtitles[position - 1].language matches Regex("([a-z]{2})-([A-Z]{2}|\\d{3})")) "[${subtitles[position - 1].language}]" else subtitles[position - 1].language
                }
                model.getMedia().observe(viewLifecycleOwner) { media ->
                    val mediaID: Int = media.id
                    val selSubs: String? =
                        PrefManager.getNullableCustomVal(
                            "subLang_${mediaID}",
                            null,
                            String::class.java
                        )
                    if (episode.selectedSubtitle != position - 1 && selSubs != subtitles[position - 1].language) {
                        binding.root.setCardBackgroundColor(TRANSPARENT)
                    }
                }
                binding.root.setOnClickListener {
                    episode.selectedSubtitle = position - 1
                    model.setEpisode(episode, "Subtitle")
                    model.getMedia().observe(viewLifecycleOwner) { media ->
                        val mediaID: Int = media.id
                        PrefManager.setCustomVal(
                            "subLang_${mediaID}",
                            subtitles[position - 1].language
                        )
                    }
                    dismiss()
                }
            }
        }

        override fun getItemCount(): Int = subtitles.size + 1
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}