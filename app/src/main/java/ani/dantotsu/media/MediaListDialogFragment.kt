package ani.dantotsu.media

import android.os.Bundle
import android.text.InputFilter.LengthFilter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.DatePickerFragment
import ani.dantotsu.InputFilterMinMax
import ani.dantotsu.R
import ani.dantotsu.Refresh
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.FuzzyDate
import ani.dantotsu.connections.mal.MAL
import ani.dantotsu.databinding.BottomSheetMediaListBinding
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.snackString
import ani.dantotsu.tryWith
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MediaListDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMediaListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMediaListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.mediaListContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += navBarHeight }
        var media: Media?
        val model: MediaDetailsViewModel by activityViewModels()
        val scope = viewLifecycleOwner.lifecycleScope

        model.getMedia().observe(this) { it ->
            media = it
            if (media != null) {
                binding.mediaListProgressBar.visibility = View.GONE
                binding.mediaListLayout.visibility = View.VISIBLE

                val statuses: Array<String> = resources.getStringArray(R.array.status)
                val statusStrings =
                    if (media?.manga == null) resources.getStringArray(R.array.status_anime) else resources.getStringArray(
                        R.array.status_manga
                    )
                val userStatus =
                    if (media!!.userStatus != null) statusStrings[statuses.indexOf(media!!.userStatus)] else statusStrings[0]

                binding.mediaListStatus.setText(userStatus)
                binding.mediaListStatus.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        R.layout.item_dropdown,
                        statusStrings
                    )
                )


                var total: Int? = null
                binding.mediaListProgress.setText(if (media!!.userProgress != null) media!!.userProgress.toString() else "")
                if (media!!.anime != null) if (media!!.anime!!.totalEpisodes != null) {
                    total = media!!.anime!!.totalEpisodes!!;binding.mediaListProgress.filters =
                        arrayOf(
                            InputFilterMinMax(0.0, total.toDouble(), binding.mediaListStatus),
                            LengthFilter(total.toString().length)
                        )
                } else if (media!!.manga != null) if (media!!.manga!!.totalChapters != null) {
                    total = media!!.manga!!.totalChapters!!;binding.mediaListProgress.filters =
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
                    if (media!!.userScore != 0) media!!.userScore.div(
                        10.0
                    ).toString() else ""
                )
                binding.mediaListScore.filters =
                    arrayOf(InputFilterMinMax(1.0, 10.0), LengthFilter(10.0.toString().length))
                binding.mediaListScoreLayout.suffixTextView.updateLayoutParams {
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
                binding.mediaListScoreLayout.suffixTextView.gravity = Gravity.CENTER

                val start = DatePickerFragment(requireActivity(), media!!.userStartedAt)
                val end = DatePickerFragment(requireActivity(), media!!.userCompletedAt)
                binding.mediaListStart.setText(media!!.userStartedAt.toStringOrEmpty())
                binding.mediaListStart.setOnClickListener {
                    tryWith(false) {
                        if (!start.dialog.isShowing) start.dialog.show()
                    }
                }
                binding.mediaListStart.setOnFocusChangeListener { _, b ->
                    tryWith(false) {
                        if (b && !start.dialog.isShowing) start.dialog.show()
                    }
                }
                binding.mediaListEnd.setText(media!!.userCompletedAt.toStringOrEmpty())
                binding.mediaListEnd.setOnClickListener {
                    tryWith(false) {
                        if (!end.dialog.isShowing) end.dialog.show()
                    }
                }
                binding.mediaListEnd.setOnFocusChangeListener { _, b ->
                    tryWith(false) {
                        if (b && !end.dialog.isShowing) end.dialog.show()
                    }
                }
                start.dialog.setOnDismissListener { _binding?.mediaListStart?.setText(start.date.toStringOrEmpty()) }
                end.dialog.setOnDismissListener { _binding?.mediaListEnd?.setText(end.date.toStringOrEmpty()) }


                fun onComplete() {
                    binding.mediaListProgress.setText(total.toString())
                    if (start.date.year == null) {
                        start.date = FuzzyDate().getToday()
                        binding.mediaListStart.setText(start.date.toString())
                    }
                    end.date = FuzzyDate().getToday()
                    binding.mediaListEnd.setText(end.date.toString())
                }

                var startBackupDate: FuzzyDate? = null
                var endBackupDate: FuzzyDate? = null
                var progressBackup: String? = null
                binding.mediaListStatus.setOnItemClickListener { _, _, i, _ ->
                    if (i == 2 && total != null) {
                        startBackupDate = start.date
                        endBackupDate = end.date
                        progressBackup = binding.mediaListProgress.text.toString()
                        onComplete()
                    } else {
                        if (progressBackup != null) binding.mediaListProgress.setText(progressBackup)
                        if (startBackupDate != null) {
                            binding.mediaListStart.setText(startBackupDate.toString())
                            start.date = startBackupDate!!
                        }
                        if (endBackupDate != null) {
                            binding.mediaListEnd.setText(endBackupDate.toString())
                            end.date = endBackupDate!!
                        }
                    }
                }

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
                        onComplete()
                    }
                }

                binding.mediaListPrivate.isChecked = media?.isListPrivate ?: false
                binding.mediaListPrivate.setOnCheckedChangeListener { _, checked ->
                    media?.isListPrivate = checked
                }
                val removeList = PrefManager.getCustomVal("removeList", setOf<Int>())
                var remove: Boolean? = null
                binding.mediaListShow.isChecked = media?.id in removeList
                binding.mediaListShow.setOnCheckedChangeListener { _, checked ->
                    remove = checked
                }
                media?.userRepeat?.apply {
                    binding.mediaListRewatch.setText(this.toString())
                }

                media?.notes?.apply {
                    binding.mediaListNotes.setText(this)
                }

                if (media?.inCustomListsOf?.isEmpty() != false)
                    binding.mediaListAddCustomList.apply {
                        (parent as? ViewGroup)?.removeView(this)
                    }

                media?.inCustomListsOf?.forEach {
                    MaterialSwitch(requireContext()).apply {
                        isChecked = it.value
                        text = it.key
                        setOnCheckedChangeListener { _, isChecked ->
                            media?.inCustomListsOf?.put(it.key, isChecked)
                        }
                        binding.mediaListCustomListContainer.addView(this)
                    }
                }


                binding.mediaListSave.setOnClickListener {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            if (media != null) {
                                val progress =
                                    _binding?.mediaListProgress?.text.toString().toIntOrNull()
                                val score =
                                    (_binding?.mediaListScore?.text.toString().toDoubleOrNull()
                                        ?.times(10))?.toInt()
                                val status =
                                    statuses[statusStrings.indexOf(_binding?.mediaListStatus?.text.toString())]
                                val rewatch =
                                    _binding?.mediaListRewatch?.text?.toString()?.toIntOrNull()
                                val notes = _binding?.mediaListNotes?.text?.toString()
                                val startD = start.date
                                val endD = end.date
                                Anilist.mutation.editList(
                                    media!!.id,
                                    progress,
                                    score,
                                    rewatch,
                                    notes,
                                    status,
                                    media?.isListPrivate ?: false,
                                    startD,
                                    endD,
                                    media?.inCustomListsOf?.mapNotNull { if (it.value) it.key else null }
                                )
                                MAL.query.editList(
                                    media!!.idMAL,
                                    media!!.anime != null,
                                    progress,
                                    score,
                                    status,
                                    rewatch,
                                    startD,
                                    endD
                                )
                            }
                        }
                        if (remove == true) {
                            PrefManager.setCustomVal("removeList", removeList.plus(media!!.id))
                        } else if (remove == false) {
                            PrefManager.setCustomVal("removeList", removeList.minus(media!!.id))
                        }
                        Refresh.all()
                        snackString(getString(R.string.list_updated))
                        dismissAllowingStateLoss()
                    }
                }

                binding.mediaListDelete.setOnClickListener {
                    scope.launch {
                        media?.deleteFromList(scope, onSuccess = {
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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
