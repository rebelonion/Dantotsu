package ani.dantotsu.settings

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import ani.dantotsu.*
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.others.imagesearch.ImageSearchActivity
import ani.dantotsu.databinding.BottomSheetSettingsBinding


class SettingsDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val window = dialog?.window
        window?.statusBarColor = Color.CYAN
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window?.navigationBarColor = typedValue.data

        if (Anilist.token != null) {
            binding.settingsLogin.setText(R.string.logout)
            binding.settingsLogin.setOnClickListener {
                Anilist.removeSavedToken(it.context)
                dismiss()
                startMainActivity(requireActivity(),)
            }
            binding.settingsUsername.text = Anilist.username
            binding.settingsUserAvatar.loadImage(Anilist.avatar)
        } else {
            binding.settingsUsername.visibility = View.GONE
            binding.settingsLogin.setText(R.string.login)
            binding.settingsLogin.setOnClickListener {
                dismiss()
                Anilist.loginIntent(requireActivity())
            }
        }

        binding.settingsExtensionSettings.setSafeOnClickListener {
            startActivity(Intent(activity, ExtensionsActivity::class.java))
            dismiss()
        }
        binding.settingsSettings.setSafeOnClickListener {
            startActivity(Intent(activity, SettingsActivity::class.java))
            dismiss()
        }
        binding.settingsAnilistSettings.setOnClickListener {
            openLinkInBrowser("https://anilist.co/settings/lists")
            dismiss()
        }
        binding.imageSearch.setOnClickListener {
            startActivity(Intent(activity, ImageSearchActivity::class.java))
            dismiss()
        }
        binding.settingsDownloads.setSafeOnClickListener {
            try {
                val arrayOfFiles = ContextCompat.getExternalFilesDirs(requireContext(), null)
                startActivity(
                    if (loadData<Boolean>("sd_dl") == true && arrayOfFiles.size > 1 && arrayOfFiles[0] != null && arrayOfFiles[1] != null) {
                        val parentDirectory = arrayOfFiles[1].toString()
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(Uri.parse(parentDirectory), "resource/folder")
                    } else Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                )
            } catch (e: ActivityNotFoundException) {
                toast(getString(R.string.file_manager_not_found))
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}