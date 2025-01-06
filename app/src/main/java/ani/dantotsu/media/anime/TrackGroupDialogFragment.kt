package ani.dantotsu.media.anime

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.C.TRACK_TYPE_AUDIO
import androidx.media3.common.C.TrackType
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.R
import ani.dantotsu.databinding.BottomSheetSubtitlesBinding
import ani.dantotsu.databinding.ItemSubtitleTextBinding
import java.util.Locale

@OptIn(UnstableApi::class)
class TrackGroupDialogFragment(
    private var instance: ExoplayerView,
    private var trackGroups: ArrayList<Tracks.Group>,
    private var type: @TrackType Int,
    private var overrideTrackNames: List<Pair<String, String>>? = null
) : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSubtitlesBinding? = null
    private val binding get() = _binding!!

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

        if (type == TRACK_TYPE_AUDIO) binding.selectionTitle.text = getString(R.string.audio_tracks)
        binding.subtitlesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.subtitlesRecycler.adapter = TrackGroupAdapter()
    }

    inner class TrackGroupAdapter : RecyclerView.Adapter<TrackGroupAdapter.StreamViewHolder>() {
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

        @SuppressLint("SetTextI18n")
        @OptIn(UnstableApi::class)
        override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
            val binding = holder.binding
            trackGroups[position].let { trackGroup ->
                if (overrideTrackNames?.getOrNull(
                        position - (trackGroups.size - (overrideTrackNames?.size ?: 0))
                    ) != null
                ) {
                    val pair =
                        overrideTrackNames!![position - (trackGroups.size - overrideTrackNames!!.size)]
                    binding.subtitleTitle.text =
                        "[${pair.second}] ${pair.first}"
                } else when (val language = trackGroup.getTrackFormat(0).language?.lowercase()) {
                    null -> {
                        binding.subtitleTitle.text =
                            getString(R.string.unknown_track, "Track $position")
                    }

                    "none" -> {
                        binding.subtitleTitle.text = getString(R.string.disabled_track)
                    }

                    else -> {
                        val locale = if (language.contains("-")) {
                            val parts = language.split("-")
                            try {
                                Locale(parts[0], parts[1])
                            } catch (ignored: Exception) {
                                null
                            }
                        } else {
                            try {
                                Locale(language)
                            } catch (ignored: Exception) {
                                null
                            }
                        }
                        binding.subtitleTitle.text = locale?.let {
                            "[${it.language}] ${it.displayName}"
                        } ?: getString(R.string.unknown_track, language)
                    }
                }
                if (trackGroup.isSelected) {
                    val selected = "✔ ${binding.subtitleTitle.text}"
                    binding.subtitleTitle.text = selected
                }
                binding.root.setOnClickListener {
                    dismiss()
                    instance.onSetTrackGroupOverride(trackGroup, type)
                }
            }
        }

        override fun getItemCount(): Int = trackGroups.size
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}