package ani.dantotsu.settings

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityReaderSettingsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.loadData
import ani.dantotsu.navBarHeight
import ani.dantotsu.saveData
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager

class ReaderSettingsActivity : AppCompatActivity() {
    lateinit var binding: ActivityReaderSettingsBinding
    private val reader = "reader_settings"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        ThemeManager(this).applyTheme()
        binding = ActivityReaderSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)
        binding.readerSettingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }

        val settings = loadData<ReaderSettings>(reader, toast = false) ?: ReaderSettings().apply {
            saveData(
                reader,
                this
            )
        }

        binding.readerSettingsBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        //Manga Settings
        binding.readerSettingsSourceName.isChecked = settings.showSource
        binding.readerSettingsSourceName.setOnCheckedChangeListener { _, isChecked ->
            settings.showSource = isChecked
            saveData(reader, settings)
        }

        binding.readerSettingsSystemBars.isChecked = settings.showSystemBars
        binding.readerSettingsSystemBars.setOnCheckedChangeListener { _, isChecked ->
            settings.showSystemBars = isChecked
            saveData(reader, settings)
        }
        //Default Manga
        binding.readerSettingsAutoWebToon.isChecked = settings.autoDetectWebtoon
        binding.readerSettingsAutoWebToon.setOnCheckedChangeListener { _, isChecked ->
            settings.autoDetectWebtoon = isChecked
            saveData(reader, settings)
        }


        val layoutList = listOf(
            binding.readerSettingsPaged,
            binding.readerSettingsContinuousPaged,
            binding.readerSettingsContinuous
        )

        binding.readerSettingsLayoutText.text =
            resources.getStringArray(R.array.manga_layouts)[settings.default.layout.ordinal]
        var selectedLayout = layoutList[settings.default.layout.ordinal]
        selectedLayout.alpha = 1f

        layoutList.forEachIndexed { index, imageButton ->
            imageButton.setOnClickListener {
                selectedLayout.alpha = 0.33f
                selectedLayout = imageButton
                selectedLayout.alpha = 1f
                settings.default.layout =
                    CurrentReaderSettings.Layouts[index] ?: CurrentReaderSettings.Layouts.CONTINUOUS
                binding.readerSettingsLayoutText.text =
                    resources.getStringArray(R.array.manga_layouts)[settings.default.layout.ordinal]
                saveData(reader, settings)
            }
        }

        binding.readerSettingsDirectionText.text =
            resources.getStringArray(R.array.manga_directions)[settings.default.direction.ordinal]
        binding.readerSettingsDirection.rotation = 90f * (settings.default.direction.ordinal)
        binding.readerSettingsDirection.setOnClickListener {
            settings.default.direction =
                CurrentReaderSettings.Directions[settings.default.direction.ordinal + 1]
                    ?: CurrentReaderSettings.Directions.TOP_TO_BOTTOM
            binding.readerSettingsDirectionText.text =
                resources.getStringArray(R.array.manga_directions)[settings.default.direction.ordinal]
            binding.readerSettingsDirection.rotation = 90f * (settings.default.direction.ordinal)
            saveData(reader, settings)
        }

        val dualList = listOf(
            binding.readerSettingsDualNo,
            binding.readerSettingsDualAuto,
            binding.readerSettingsDualForce
        )

        binding.readerSettingsDualPageText.text = settings.default.dualPageMode.toString()
        var selectedDual = dualList[settings.default.dualPageMode.ordinal]
        selectedDual.alpha = 1f

        dualList.forEachIndexed { index, imageButton ->
            imageButton.setOnClickListener {
                selectedDual.alpha = 0.33f
                selectedDual = imageButton
                selectedDual.alpha = 1f
                settings.default.dualPageMode = CurrentReaderSettings.DualPageModes[index]
                    ?: CurrentReaderSettings.DualPageModes.Automatic
                binding.readerSettingsDualPageText.text = settings.default.dualPageMode.toString()
                saveData(reader, settings)
            }
        }
        binding.readerSettingsTrueColors.isChecked = settings.default.trueColors
        binding.readerSettingsTrueColors.setOnCheckedChangeListener { _, isChecked ->
            settings.default.trueColors = isChecked
            saveData(reader, settings)
        }

        binding.readerSettingsCropBorders.isChecked = settings.default.cropBorders
        binding.readerSettingsCropBorders.setOnCheckedChangeListener { _, isChecked ->
            settings.default.cropBorders = isChecked
            saveData(reader, settings)
        }

        binding.readerSettingsImageRotation.isChecked = settings.default.rotation
        binding.readerSettingsImageRotation.setOnCheckedChangeListener { _, isChecked ->
            settings.default.rotation = isChecked
            saveData(reader, settings)
        }

        binding.readerSettingsHorizontalScrollBar.isChecked = settings.default.horizontalScrollBar
        binding.readerSettingsHorizontalScrollBar.setOnCheckedChangeListener { _, isChecked ->
            settings.default.horizontalScrollBar = isChecked
            saveData(reader, settings)
        }
        binding.readerSettingsPadding.isChecked = settings.default.padding
        binding.readerSettingsPadding.setOnCheckedChangeListener { _, isChecked ->
            settings.default.padding = isChecked
            saveData(reader, settings)
        }

        binding.readerSettingsKeepScreenOn.isChecked = settings.default.keepScreenOn
        binding.readerSettingsKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            settings.default.keepScreenOn = isChecked
            saveData(reader, settings)
        }

        binding.readerSettingsHidePageNumbers.isChecked = settings.default.hidePageNumbers
        binding.readerSettingsHidePageNumbers.setOnCheckedChangeListener { _, isChecked ->
            settings.default.hidePageNumbers = isChecked
            saveData(reader, settings)
        }

        binding.readerSettingsOverscroll.isChecked = settings.default.overScrollMode
        binding.readerSettingsOverscroll.setOnCheckedChangeListener { _, isChecked ->
            settings.default.overScrollMode = isChecked
            saveData(reader, settings)
        }

        binding.readerSettingsVolumeButton.isChecked = settings.default.volumeButtons
        binding.readerSettingsVolumeButton.setOnCheckedChangeListener { _, isChecked ->
            settings.default.volumeButtons = isChecked
            saveData(reader, settings)
        }

        binding.readerSettingsWrapImages.isChecked = settings.default.wrapImages
        binding.readerSettingsWrapImages.setOnCheckedChangeListener { _, isChecked ->
            settings.default.wrapImages = isChecked
            saveData(reader, settings)
        }

        binding.readerSettingsLongClickImage.isChecked = settings.default.longClickImage
        binding.readerSettingsLongClickImage.setOnCheckedChangeListener { _, isChecked ->
            settings.default.longClickImage = isChecked
            saveData(reader, settings)
        }

        //LN settings
        val layoutListLN = listOf(
            binding.LNpaged,
            binding.LNcontinuous
        )

        binding.LNlayoutText.text = settings.defaultLN.layout.string
        var selectedLN = layoutListLN[settings.defaultLN.layout.ordinal]
        selectedLN.alpha = 1f

        layoutListLN.forEachIndexed { index, imageButton ->
            imageButton.setOnClickListener {
                selectedLN.alpha = 0.33f
                selectedLN = imageButton
                selectedLN.alpha = 1f
                settings.defaultLN.layout = CurrentNovelReaderSettings.Layouts[index]
                    ?: CurrentNovelReaderSettings.Layouts.PAGED
                binding.LNlayoutText.text = settings.defaultLN.layout.string
                saveData(reader, settings)
            }
        }

        val dualListLN = listOf(
            binding.LNdualNo,
            binding.LNdualAuto,
            binding.LNdualForce
        )

        binding.LNdualPageText.text = settings.defaultLN.dualPageMode.toString()
        var selectedDualLN = dualListLN[settings.defaultLN.dualPageMode.ordinal]
        selectedDualLN.alpha = 1f

        dualListLN.forEachIndexed { index, imageButton ->
            imageButton.setOnClickListener {
                selectedDualLN.alpha = 0.33f
                selectedDualLN = imageButton
                selectedDualLN.alpha = 1f
                settings.defaultLN.dualPageMode = CurrentReaderSettings.DualPageModes[index]
                    ?: CurrentReaderSettings.DualPageModes.Automatic
                binding.LNdualPageText.text = settings.defaultLN.dualPageMode.toString()
                saveData(reader, settings)
            }
        }

        binding.LNlineHeight.setText(settings.defaultLN.lineHeight.toString())
        binding.LNlineHeight.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = binding.LNlineHeight.text.toString().toFloatOrNull() ?: 1.4f
                settings.defaultLN.lineHeight = value
                binding.LNlineHeight.setText(value.toString())
                saveData(reader, settings)
            }
        }

        binding.LNincrementLineHeight.setOnClickListener {
            val value = binding.LNlineHeight.text.toString().toFloatOrNull() ?: 1.4f
            settings.defaultLN.lineHeight = value + 0.1f
            binding.LNlineHeight.setText(settings.defaultLN.lineHeight.toString())
            saveData(reader, settings)
        }

        binding.LNdecrementLineHeight.setOnClickListener {
            val value = binding.LNlineHeight.text.toString().toFloatOrNull() ?: 1.4f
            settings.defaultLN.lineHeight = value - 0.1f
            binding.LNlineHeight.setText(settings.defaultLN.lineHeight.toString())
            saveData(reader, settings)
        }

        binding.LNmargin.setText(settings.defaultLN.margin.toString())
        binding.LNmargin.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = binding.LNmargin.text.toString().toFloatOrNull() ?: 0.06f
                settings.defaultLN.margin = value
                binding.LNmargin.setText(value.toString())
                saveData(reader, settings)
            }
        }

        binding.LNincrementMargin.setOnClickListener {
            val value = binding.LNmargin.text.toString().toFloatOrNull() ?: 0.06f
            settings.defaultLN.margin = value + 0.01f
            binding.LNmargin.setText(settings.defaultLN.margin.toString())
            saveData(reader, settings)
        }

        binding.LNdecrementMargin.setOnClickListener {
            val value = binding.LNmargin.text.toString().toFloatOrNull() ?: 0.06f
            settings.defaultLN.margin = value - 0.01f
            binding.LNmargin.setText(settings.defaultLN.margin.toString())
            saveData(reader, settings)
        }

        binding.LNmaxInlineSize.setText(settings.defaultLN.maxInlineSize.toString())
        binding.LNmaxInlineSize.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = binding.LNmaxInlineSize.text.toString().toIntOrNull() ?: 720
                settings.defaultLN.maxInlineSize = value
                binding.LNmaxInlineSize.setText(value.toString())
                saveData(reader, settings)
            }
        }

        binding.LNincrementMaxInlineSize.setOnClickListener {
            val value = binding.LNmaxInlineSize.text.toString().toIntOrNull() ?: 720
            settings.defaultLN.maxInlineSize = value + 10
            binding.LNmaxInlineSize.setText(settings.defaultLN.maxInlineSize.toString())
            saveData(reader, settings)
        }

        binding.LNdecrementMaxInlineSize.setOnClickListener {
            val value = binding.LNmaxInlineSize.text.toString().toIntOrNull() ?: 720
            settings.defaultLN.maxInlineSize = value - 10
            binding.LNmaxInlineSize.setText(settings.defaultLN.maxInlineSize.toString())
            saveData(reader, settings)
        }

        binding.LNmaxBlockSize.setText(settings.defaultLN.maxBlockSize.toString())
        binding.LNmaxBlockSize.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = binding.LNmaxBlockSize.text.toString().toIntOrNull() ?: 720
                settings.defaultLN.maxBlockSize = value
                binding.LNmaxBlockSize.setText(value.toString())
                saveData(reader, settings)
            }
        }
        binding.LNincrementMaxBlockSize.setOnClickListener {
            val value = binding.LNmaxBlockSize.text.toString().toIntOrNull() ?: 720
            settings.defaultLN.maxInlineSize = value + 10
            binding.LNmaxBlockSize.setText(settings.defaultLN.maxInlineSize.toString())
            saveData(reader, settings)
        }

        binding.LNdecrementMaxBlockSize.setOnClickListener {
            val value = binding.LNmaxBlockSize.text.toString().toIntOrNull() ?: 720
            settings.defaultLN.maxBlockSize = value - 10
            binding.LNmaxBlockSize.setText(settings.defaultLN.maxBlockSize.toString())
            saveData(reader, settings)
        }

        binding.LNuseDarkTheme.isChecked = settings.defaultLN.useDarkTheme
        binding.LNuseDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            settings.defaultLN.useDarkTheme = isChecked
            saveData(reader, settings)
        }

        binding.LNuseOledTheme.isChecked = settings.defaultLN.useOledTheme
        binding.LNuseOledTheme.setOnCheckedChangeListener { _, isChecked ->
            settings.defaultLN.useOledTheme = isChecked
            saveData(reader, settings)
        }

        binding.LNkeepScreenOn.isChecked = settings.defaultLN.keepScreenOn
        binding.LNkeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            settings.defaultLN.keepScreenOn = isChecked
            saveData(reader, settings)
        }

        binding.LNvolumeButton.isChecked = settings.defaultLN.volumeButtons
        binding.LNvolumeButton.setOnCheckedChangeListener { _, isChecked ->
            settings.defaultLN.volumeButtons = isChecked
            saveData(reader, settings)
        }

        //Update Progress
        binding.readerSettingsAskUpdateProgress.isChecked = settings.askIndividual
        binding.readerSettingsAskUpdateProgress.setOnCheckedChangeListener { _, isChecked ->
            settings.askIndividual = isChecked
            saveData(reader, settings)
        }
        binding.readerSettingsAskUpdateDoujins.isChecked = settings.updateForH
        binding.readerSettingsAskUpdateDoujins.setOnCheckedChangeListener { _, isChecked ->
            settings.updateForH = isChecked
            if (isChecked) snackString(getString(R.string.very_bold))
            saveData(reader, settings)
        }

    }
}