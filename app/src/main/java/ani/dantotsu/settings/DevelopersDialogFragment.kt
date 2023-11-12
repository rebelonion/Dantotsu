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
        Developer("rebelonion","https://avatars.githubusercontent.com/u/87634197?v=4","Owner and Maintainer","https://github.com/rebelonion"),
        Developer("Wai What", "https://avatars.githubusercontent.com/u/149729762?v=4", "Icon Designer", "https://github.com/WaiWhat"),
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
