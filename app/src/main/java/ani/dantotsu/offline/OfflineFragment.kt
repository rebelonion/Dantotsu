package ani.dantotsu.offline

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import ani.dantotsu.App
import ani.dantotsu.R
import ani.dantotsu.currContext
import ani.dantotsu.databinding.FragmentOfflineBinding
import ani.dantotsu.isOnline
import ani.dantotsu.navBarHeight
import ani.dantotsu.startMainActivity
import ani.dantotsu.statusBarHeight

class OfflineFragment : Fragment() {
    private var offline = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentOfflineBinding.inflate(inflater, container, false)
        binding.refreshContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }
        offline = requireContext().getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)
            ?.getBoolean("offlineMode", false) ?: false
        binding.noInternet.text =
            if (!isOnline(requireContext())) getString(R.string.no_internet) else "OFFLINE MODE"
        binding.refreshButton.setOnClickListener {
            println("Offline: $offline")
            println("Online: ${isOnline(requireContext())}")
            if (isOnline(requireContext()) && !offline) {
                startMainActivity(requireActivity())
            }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        offline = requireContext().getSharedPreferences("Dantotsu", Context.MODE_PRIVATE)
            ?.getBoolean("offlineMode", false) ?: false
    }
}