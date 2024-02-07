package ani.dantotsu.media.novel.novelreader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.NoPaddingArrayAdapter
import ani.dantotsu.R
import ani.dantotsu.databinding.BottomSheetCurrentNovelReaderSettingsBinding
import ani.dantotsu.settings.CurrentNovelReaderSettings
import ani.dantotsu.settings.CurrentReaderSettings

class NovelReaderSettingsDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetCurrentNovelReaderSettingsBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCurrentNovelReaderSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as NovelReaderActivity
        val settings = activity.defaultSettings
        val themeLabels = activity.themes.map { it.name }
        binding.themeSelect.adapter =
            NoPaddingArrayAdapter(activity, R.layout.item_dropdown, themeLabels)
        binding.themeSelect.setSelection(themeLabels.indexOfFirst { it == settings.currentThemeName })
        binding.themeSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                settings.currentThemeName = themeLabels[position]
                activity.applySettings()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.useOledTheme.isChecked = settings.useOledTheme
        binding.useOledTheme.setOnCheckedChangeListener { _, isChecked ->
            settings.useOledTheme = isChecked
            activity.applySettings()
        }
        val layoutList = listOf(
            binding.paged,
            binding.continuous
        )

        binding.layoutText.text = settings.layout.string
        var selected = layoutList[settings.layout.ordinal]
        selected.alpha = 1f

        layoutList.forEachIndexed { index, imageButton ->
            imageButton.setOnClickListener {
                selected.alpha = 0.33f
                selected = imageButton
                selected.alpha = 1f
                settings.layout = CurrentNovelReaderSettings.Layouts[index]
                    ?: CurrentNovelReaderSettings.Layouts.PAGED
                binding.layoutText.text = settings.layout.string
                activity.applySettings()
            }
        }

        val dualList = listOf(
            binding.dualNo,
            binding.dualAuto,
            binding.dualForce
        )

        binding.dualPageText.text = settings.dualPageMode.toString()
        var selectedDual = dualList[settings.dualPageMode.ordinal]
        selectedDual.alpha = 1f

        dualList.forEachIndexed { index, imageButton ->
            imageButton.setOnClickListener {
                selectedDual.alpha = 0.33f
                selectedDual = imageButton
                selectedDual.alpha = 1f
                settings.dualPageMode = CurrentReaderSettings.DualPageModes[index]
                    ?: CurrentReaderSettings.DualPageModes.Automatic
                binding.dualPageText.text = settings.dualPageMode.toString()
                activity.applySettings()
            }
        }

        binding.lineHeight.setText(settings.lineHeight.toString())
        binding.lineHeight.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = binding.lineHeight.text.toString().toFloatOrNull() ?: 1.4f
                settings.lineHeight = value
                binding.lineHeight.setText(value.toString())
                activity.applySettings()
            }
        }

        binding.incrementLineHeight.setOnClickListener {
            val value = binding.lineHeight.text.toString().toFloatOrNull() ?: 1.4f
            settings.lineHeight = value + 0.1f
            binding.lineHeight.setText(settings.lineHeight.toString())
            activity.applySettings()
        }

        binding.decrementLineHeight.setOnClickListener {
            val value = binding.lineHeight.text.toString().toFloatOrNull() ?: 1.4f
            settings.lineHeight = value - 0.1f
            binding.lineHeight.setText(settings.lineHeight.toString())
            activity.applySettings()
        }

        binding.margin.setText(settings.margin.toString())
        binding.margin.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = binding.margin.text.toString().toFloatOrNull() ?: 0.06f
                settings.margin = value
                binding.margin.setText(value.toString())
                activity.applySettings()
            }
        }

        binding.incrementMargin.setOnClickListener {
            val value = binding.margin.text.toString().toFloatOrNull() ?: 0.06f
            settings.margin = value + 0.01f
            binding.margin.setText(settings.margin.toString())
            activity.applySettings()
        }

        binding.decrementMargin.setOnClickListener {
            val value = binding.margin.text.toString().toFloatOrNull() ?: 0.06f
            settings.margin = value - 0.01f
            binding.margin.setText(settings.margin.toString())
            activity.applySettings()
        }

        binding.maxInlineSize.setText(settings.maxInlineSize.toString())
        binding.maxInlineSize.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = binding.maxInlineSize.text.toString().toIntOrNull() ?: 720
                settings.maxInlineSize = value
                binding.maxInlineSize.setText(value.toString())
                activity.applySettings()
            }
        }

        binding.incrementMaxInlineSize.setOnClickListener {
            val value = binding.maxInlineSize.text.toString().toIntOrNull() ?: 720
            settings.maxInlineSize = value + 10
            binding.maxInlineSize.setText(settings.maxInlineSize.toString())
            activity.applySettings()
        }

        binding.decrementMaxInlineSize.setOnClickListener {
            val value = binding.maxInlineSize.text.toString().toIntOrNull() ?: 720
            settings.maxInlineSize = value - 10
            binding.maxInlineSize.setText(settings.maxInlineSize.toString())
            activity.applySettings()
        }

        binding.maxBlockSize.setText(settings.maxBlockSize.toString())
        binding.maxBlockSize.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = binding.maxBlockSize.text.toString().toIntOrNull() ?: 720
                settings.maxBlockSize = value
                binding.maxBlockSize.setText(value.toString())
                activity.applySettings()
            }

        }
        binding.incrementMaxBlockSize.setOnClickListener {
            val value = binding.maxBlockSize.text.toString().toIntOrNull() ?: 720
            settings.maxBlockSize = value + 10
            binding.maxBlockSize.setText(settings.maxBlockSize.toString())
            activity.applySettings()
        }

        binding.decrementMaxBlockSize.setOnClickListener {
            val value = binding.maxBlockSize.text.toString().toIntOrNull() ?: 720
            settings.maxBlockSize = value - 10
            binding.maxBlockSize.setText(settings.maxBlockSize.toString())
            activity.applySettings()
        }

        binding.useDarkTheme.isChecked = settings.useDarkTheme
        binding.useDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            settings.useDarkTheme = isChecked
            activity.applySettings()
        }

        binding.keepScreenOn.isChecked = settings.keepScreenOn
        binding.keepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            settings.keepScreenOn = isChecked
            activity.applySettings()
        }

        binding.volumeButton.isChecked = settings.volumeButtons
        binding.volumeButton.setOnCheckedChangeListener { _, isChecked ->
            settings.volumeButtons = isChecked
            activity.applySettings()
        }
    }


    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }


    companion object {
        fun newInstance() = NovelReaderSettingsDialogFragment()
        const val TAG = "NovelReaderSettingsDialogFragment"
    }
}