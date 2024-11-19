package ani.dantotsu.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.databinding.BottomSheetProxyBinding
import ani.dantotsu.snackString
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.restartApp

class ProxyDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetProxyBinding? = null
    private val binding get() = _binding!!

    private var proxyHost: String? = PrefManager.getVal<String>(PrefName.Socks5ProxyHost) ?: ""
    private var proxyPort: String? = PrefManager.getVal<String>(PrefName.Socks5ProxyPort) ?: ""
    private var proxyUsername: String? = PrefManager.getVal<String>(PrefName.Socks5ProxyUsername) ?: ""
    private var proxyPassword: String? = PrefManager.getVal<String>(PrefName.Socks5ProxyPassword) ?: ""
    private var authEnabled: Boolean = PrefManager.getVal<Boolean>(PrefName.ProxyAuthEnabled)
    private val proxyEnabled: Boolean = PrefManager.getVal<Boolean>(PrefName.EnableSocks5Proxy)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetProxyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.proxyHost.setText(proxyHost)
        binding.proxyPort.setText(proxyPort)
        binding.proxyUsername.setText(proxyUsername)
        binding.proxyPassword.setText(proxyPassword)
        binding.proxyAuthentication.isChecked = authEnabled

        binding.proxySave.setOnClickListener {
            proxyHost = binding.proxyHost.text.toString() ?: ""
            proxyPort = binding.proxyPort.text.toString() ?: ""
            proxyUsername = binding.proxyUsername.text.toString() ?: ""
            proxyPassword = binding.proxyPassword.text.toString() ?: ""

            PrefManager.setVal(PrefName.Socks5ProxyHost, proxyHost)
            PrefManager.setVal(PrefName.Socks5ProxyPort, proxyPort)
            PrefManager.setVal(PrefName.Socks5ProxyUsername, proxyUsername)
            PrefManager.setVal(PrefName.Socks5ProxyPassword, proxyPassword)

            dismiss()
            if (proxyEnabled) activity?.restartApp()
        }

        binding.proxyAuthentication.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.ProxyAuthEnabled, isChecked)
            binding.proxyUsername.isEnabled = isChecked
            binding.proxyPassword.isEnabled = isChecked
            binding.proxyUsernameLayout.isEnabled = isChecked
            binding.proxyPasswordLayout.isEnabled = isChecked
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}