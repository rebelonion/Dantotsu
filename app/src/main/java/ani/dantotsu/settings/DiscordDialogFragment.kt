package ani.dantotsu.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.BottomSheetDiscordRpcBinding
import ani.dantotsu.profile.activity.UsersAdapter
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import com.google.android.material.bottomsheet.BottomSheetDialog

class DiscordDialogFragment: BottomSheetDialogFragment() {
    private var _binding: BottomSheetDiscordRpcBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDiscordRpcBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (PrefManager.getCustomVal("discord_mode", "dantotsu")) {
            "nothing" -> binding.radioNothing.isChecked= true
            "dantotsu" -> binding.radioDantotsu.isChecked = true
            "anilist" -> binding.radioAnilist.isChecked = true
            else -> binding.radioAnilist.isChecked = true
        }

        binding.anilistLinkPreview.text = getString(R.string.anilist_link, PrefManager.getVal<String>(PrefName.AnilistUserName))

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                binding.radioNothing.id -> "nothing"
                binding.radioDantotsu.id -> "dantotsu"
                binding.radioAnilist.id -> "anilist"
                else -> "dantotsu"
            }
            PrefManager.setCustomVal("discord_mode", mode)
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}