package ani.dantotsu.settings

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.BottomSheetSettingsBinding
import ani.dantotsu.download.DownloadContainerActivity
import ani.dantotsu.download.manga.OfflineMangaFragment
import ani.dantotsu.loadData
import ani.dantotsu.loadImage
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.others.imagesearch.ImageSearchActivity
import ani.dantotsu.setSafeOnClickListener
import ani.dantotsu.startMainActivity
import ani.dantotsu.toast


class SettingsDialogFragment() : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var pageType: PageType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageType = arguments?.getSerializable("pageType") as? PageType ?: PageType.HOME
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
                startMainActivity(requireActivity())
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

        binding.settingsIncognito.isChecked =
            getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).getBoolean(
                "incognito",
                false
            )
        binding.settingsIncognito.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("Dantotsu", Context.MODE_PRIVATE).edit()
                .putBoolean("incognito", isChecked).apply()
            restartApp()
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
            when (pageType) {
                PageType.MANGA -> {
                    val intent = Intent(activity, DownloadContainerActivity::class.java)
                    intent.putExtra("FRAGMENT_CLASS_NAME", OfflineMangaFragment::class.java.name)
                    startActivity(intent)
                }

                PageType.ANIME -> {
                    try {
                        val arrayOfFiles =
                            ContextCompat.getExternalFilesDirs(requireContext(), null)
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
                }

                PageType.HOME -> {
                    val intent = Intent(activity, DownloadContainerActivity::class.java)
                    intent.putExtra("FRAGMENT_CLASS_NAME", OfflineMangaFragment::class.java.name)
                    startActivity(intent)
                }
            }

            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun restartApp() {
        Snackbar.make(
            binding.root,
            R.string.restart_app, Snackbar.LENGTH_SHORT
        ).apply {
            val mainIntent =
                Intent.makeRestartActivityTask(
                    context.packageManager.getLaunchIntentForPackage(
                        context.packageName
                    )!!.component
                )
            setAction("Do it!") {
                context.startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }
            show()
        }
    }

    companion object {
        enum class PageType {
            MANGA, ANIME, HOME
        }

        fun newInstance(pageType: PageType): SettingsDialogFragment {
            val fragment = SettingsDialogFragment()
            val args = Bundle()
            args.putSerializable("pageType", pageType)
            fragment.arguments = args
            return fragment
        }
    }
}