package ani.dantotsu.media

import android.os.Bundle
import android.text.InputFilter.LengthFilter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.InputFilterMinMax
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.mal.MAL
import ani.dantotsu.databinding.BottomSheetMediaListSmallBinding
import ani.dantotsu.navBarHeight
import ani.dantotsu.others.getSerialized
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.snackString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable


class MediaListDialogSmallFragment : BottomSheetDialogFragment() {

    private lateinit var media: Media

    companion object {
        fun newInstance(m: Media): MediaListDialogSmallFragment =
            MediaListDialogSmallFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("media", m as Serializable)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            media = it.getSerialized("media")!!
        }
    }

    private var _binding: BottomSheetMediaListSmallBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMediaListSmallBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.mediaListContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += navBarHeight }
        val scope = viewLifecycleOwner.lifecycleScope
        binding.mediaListDelete.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                scope.launch {
                    media.deleteFromList(scope, onSuccess = {
                        Refresh.all()
                        snackString(getString(R.string.deleted_from_list))
                        dismissAllowingStateLoss()
                    }, onError = { e ->
                        withContext(Dispatchers.Main) {
                            snackString(
                                getString(
                                    R.string.delete_fail_reason, e.message
                                )
                            )
                        }
                    }, onNotFound = {
                        snackString(getString(R.string.no_list_id))
                    })

                }
            }
        }

        binding.mediaListProgressBar.visibility = View.GONE
        binding.mediaListLayout.visibility = View.VISIBLE
        val statuses: Array<String> = resources.getStringArray(R.array.status)
        val statusStrings =
            if (media.manga == null) resources.getStringArray(R.array.status_anime) else resources.getStringArray(
                R.array.status_manga
            )
        val userStatus =
            if (media.userStatus != null) statusStrings[statuses.indexOf(media.userStatus)] else statusStrings[0]

        binding.mediaListStatus.setText(userStatus)
        binding.mediaListStatus.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.item_dropdown,
                statusStrings
            )
        )


        var total: Int? = null
        binding.mediaListProgress.setText(if (media.userProgress != null) media.userProgress.toString() else "")
        if (media.anime != null) if (media.anime!!.totalEpisodes != null) {
            total = media.anime!!.totalEpisodes!!;binding.mediaListProgress.filters =
                arrayOf(
                    InputFilterMinMax(0.0, total.toDouble(), binding.mediaListStatus),
                    LengthFilter(total.toString().length)
                )
        } else if (media.manga != null) if (media.manga!!.totalChapters != null) {
            total = media.manga!!.totalChapters!!;binding.mediaListProgress.filters =
                arrayOf(
                    InputFilterMinMax(0.0, total.toDouble(), binding.mediaListStatus),
                    LengthFilter(total.toString().length)
                )
        }
        binding.mediaListProgressLayout.suffixText = " / ${total ?: '?'}"
        binding.mediaListProgressLayout.suffixTextView.updateLayoutParams {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.mediaListProgressLayout.suffixTextView.gravity = Gravity.CENTER

        binding.mediaListScore.setText(
            if (media.userScore != 0) media.userScore.div(
                10.0
            ).toString() else ""
        )
        binding.mediaListScore.filters =
            arrayOf(InputFilterMinMax(1.0, 10.0), LengthFilter(10.0.toString().length))
        binding.mediaListScoreLayout.suffixTextView.updateLayoutParams {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.mediaListScoreLayout.suffixTextView.gravity = Gravity.CENTER

        binding.mediaListIncrement.setOnClickListener {
            if (binding.mediaListStatus.text.toString() == statusStrings[0]) binding.mediaListStatus.setText(
                statusStrings[1],
                false
            )
            val init =
                if (binding.mediaListProgress.text.toString() != "") binding.mediaListProgress.text.toString()
                    .toInt() else 0
            if (init < (total ?: 5000)) {
                val progressText = "${init + 1}"
                binding.mediaListProgress.setText(progressText)
            }
            if (init + 1 == (total ?: 5000)) {
                binding.mediaListStatus.setText(statusStrings[2], false)
            }
        }

        binding.mediaListPrivate.isChecked = media.isListPrivate
        binding.mediaListPrivate.setOnCheckedChangeListener { _, checked ->
            media.isListPrivate = checked
        }
        val removeList = PrefManager.getCustomVal("removeList", setOf<Int>())
        var remove: Boolean? = null
        binding.mediaListShow.isChecked = media.id in removeList
        binding.mediaListShow.setOnCheckedChangeListener { _, checked ->
            remove = checked
        }
        binding.mediaListSave.setOnClickListener {
            scope.launch {
                withContext(Dispatchers.IO) {
                    withContext(Dispatchers.IO) {
                        val progress = _binding?.mediaListProgress?.text.toString().toIntOrNull()
                        val score = (_binding?.mediaListScore?.text.toString().toDoubleOrNull()
                            ?.times(10))?.toInt()
                        val status =
                            statuses[statusStrings.indexOf(_binding?.mediaListStatus?.text.toString())]
                        Anilist.mutation.editList(
                            media.id,
                            progress,
                            score,
                            null,
                            null,
                            status,
                            media.isListPrivate
                        )
                        MAL.query.editList(
                            media.idMAL,
                            media.anime != null,
                            progress,
                            score,
                            status
                        )
                    }
                }
                if (remove == true) {
                    PrefManager.setCustomVal("removeList", removeList.plus(media.id))
                } else if (remove == false) {
                    PrefManager.setCustomVal("removeList", removeList.minus(media.id))
                }
                Refresh.all()
                snackString(getString(R.string.list_updated))
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}