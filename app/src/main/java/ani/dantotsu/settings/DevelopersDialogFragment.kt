package ani.dantotsu.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.databinding.BottomSheetDevelopersBinding

class DevelopersDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetDevelopersBinding? = null
    private val binding get() = _binding!!

    private val developers = arrayOf(
        Developer(
            "rebelonion",
            "https://avatars.githubusercontent.com/u/87634197?v=4",
            "Owner and Maintainer",
            "https://github.com/rebelonion"
        ),
        Developer(
            "Aayush262",
            "https://avatars.githubusercontent.com/u/99584765?v=4",
            "Contributor",
            "https://github.com/aayush2622"
        ),
        Developer(
            "Sadwhy",
            "https://avatars.githubusercontent.com/u/99601717?v=4",
            "Contributor",
            "https://github.com/Sadwhy"
        ),
        Developer(
            "Wai What",
            "https://avatars.githubusercontent.com/u/149729762?v=4",
            "Icon Designer",
            "https://github.com/WaiWhat"
        ),
        Developer(
            "MarshMeadow",
            "https://avatars.githubusercontent.com/u/88599122?v=4",
            "Beta Icon Designer",
            "https://github.com/MarshMeadow?tab=repositories"
        ),
        Developer(
            "Zaxx69",
            "https://avatars.githubusercontent.com/u/138523882?v=4",
            "Telegram Admin",
            "https://github.com/Zaxx69"
        ),
        Developer(
            "Arif Alam",
            "https://avatars.githubusercontent.com/u/70383209?v=4",
            "Head Discord Moderator",
            "https://youtube.com/watch?v=dQw4w9WgXcQ"
        ),
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDevelopersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.devsRecyclerView.adapter = DevelopersAdapter(developers)
        binding.devsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
