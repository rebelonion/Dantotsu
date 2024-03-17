package ani.dantotsu.settings

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityReaderSettingsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager

class ReaderSettingsActivity : AppCompatActivity() {
    lateinit var binding: ActivityReaderSettingsBinding
    private var defaultSettings = CurrentReaderSettings()
    private var defaultSettingsLN = CurrentNovelReaderSettings()
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

        binding.readerSettingsBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        //Manga Settings
        binding.readerSettingsSourceName.isChecked = PrefManager.getVal(PrefName.ShowSource)
        binding.readerSettingsSourceName.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.ShowSource, isChecked)
        }

        binding.readerSettingsSystemBars.isChecked = PrefManager.getVal(PrefName.ShowSystemBars)
        binding.readerSettingsSystemBars.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.ShowSystemBars, isChecked)
        }
        //Default Manga
        binding.readerSettingsAutoWebToon.isChecked = PrefManager.getVal(PrefName.AutoDetectWebtoon)
        binding.readerSettingsAutoWebToon.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.AutoDetectWebtoon, isChecked)
        }


        val layoutList = listOf(
            binding.readerSettingsPaged,
            binding.readerSettingsContinuousPaged,
            binding.readerSettingsContinuous
        )

        binding.readerSettingsLayoutText.text =
            resources.getStringArray(R.array.manga_layouts)[defaultSettings.layout.ordinal]
        var selectedLayout = layoutList[defaultSettings.layout.ordinal]
        selectedLayout.alpha = 1f

        layoutList.forEachIndexed { index, imageButton ->
            imageButton.setOnClickListener {
                selectedLayout.alpha = 0.33f
                selectedLayout = imageButton
                selectedLayout.alpha = 1f
                defaultSettings.layout =
                    CurrentReaderSettings.Layouts[index] ?: CurrentReaderSettings.Layouts.CONTINUOUS
                binding.readerSettingsLayoutText.text =
                    resources.getStringArray(R.array.manga_layouts)[defaultSettings.layout.ordinal]
                PrefManager.setVal(PrefName.LayoutReader, defaultSettings.layout.ordinal)
            }
        }

        binding.readerSettingsDirectionText.text =
            resources.getStringArray(R.array.manga_directions)[defaultSettings.direction.ordinal]
        binding.readerSettingsDirection.rotation = 90f * (defaultSettings.direction.ordinal)
        binding.readerSettingsDirection.setOnClickListener {
            defaultSettings.direction =
                CurrentReaderSettings.Directions[defaultSettings.direction.ordinal + 1]
                    ?: CurrentReaderSettings.Directions.TOP_TO_BOTTOM
            binding.readerSettingsDirectionText.text =
                resources.getStringArray(R.array.manga_directions)[defaultSettings.direction.ordinal]
            binding.readerSettingsDirection.rotation = 90f * (defaultSettings.direction.ordinal)
            PrefManager.setVal(PrefName.Direction, defaultSettings.direction.ordinal)
        }

        val dualList = listOf(
            binding.readerSettingsDualNo,
            binding.readerSettingsDualAuto,
            binding.readerSettingsDualForce
        )

        binding.readerSettingsDualPageText.text = defaultSettings.dualPageMode.toString()
        var selectedDual = dualList[defaultSettings.dualPageMode.ordinal]
        selectedDual.alpha = 1f

        dualList.forEachIndexed { index, imageButton ->
            imageButton.setOnClickListener {
                selectedDual.alpha = 0.33f
                selectedDual = imageButton
                selectedDual.alpha = 1f
                defaultSettings.dualPageMode = CurrentReaderSettings.DualPageModes[index]
                    ?: CurrentReaderSettings.DualPageModes.Automatic
                binding.readerSettingsDualPageText.text = defaultSettings.dualPageMode.toString()
                PrefManager.setVal(
                    PrefName.DualPageModeReader,
                    defaultSettings.dualPageMode.ordinal
                )
            }
        }
        binding.readerSettingsTrueColors.isChecked = defaultSettings.trueColors
        binding.readerSettingsTrueColors.setOnCheckedChangeListener { _, isChecked ->
            defaultSettings.trueColors = isChecked
            PrefManager.setVal(PrefName.TrueColors, isChecked)
        }

        binding.readerSettingsCropBorders.isChecked = defaultSettings.cropBorders
        binding.readerSettingsCropBorders.setOnCheckedChangeListener { _, isChecked ->
            defaultSettings.cropBorders = isChecked
            PrefManager.setVal(PrefName.CropBorders, isChecked)
        }

        binding.readerSettingsImageRotation.isChecked = defaultSettings.rotation
        binding.readerSettingsImageRotation.setOnCheckedChangeListener { _, isChecked ->
            defaultSettings.rotation = isChecked
            PrefManager.setVal(PrefName.Rotation, isChecked)
        }

        binding.readerSettingsHorizontalScrollBar.isChecked = defaultSettings.horizontalScrollBar
        binding.readerSettingsHorizontalScrollBar.setOnCheckedChangeListener { _, isChecked ->
            defaultSettings.horizontalScrollBar = isChecked
            PrefManager.setVal(PrefName.HorizontalScrollBar, isChecked)
        }
        binding.readerSettingsPadding.isChecked = defaultSettings.padding
        binding.readerSettingsPadding.setOnCheckedChangeListener { _, isChecked ->
            defaultSettings.padding = isChecked
            PrefManager.setVal(PrefName.Padding, isChecked)
        }

        binding.readerSettingsKeepScreenOn.isChecked = defaultSettings.keepScreenOn
        binding.readerSettingsKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            defaultSettings.keepScreenOn = isChecked
            PrefManager.setVal(PrefName.KeepScreenOn, isChecked)
        }

        binding.readerSettingsHideScrollBar.isChecked = defaultSettings.hideScrollBar
        binding.readerSettingsHideScrollBar.setOnCheckedChangeListener { _, isChecked ->
            defaultSettings.hideScrollBar = isChecked
            PrefManager.setVal(PrefName.HideScrollBar, isChecked)
        }

        binding.readerSettingsHidePageNumbers.isChecked = defaultSettings.hidePageNumbers
        binding.readerSettingsHidePageNumbers.setOnCheckedChangeListener { _, isChecked ->
            defaultSettings.hidePageNumbers = isChecked
            PrefManager.setVal(PrefName.HidePageNumbers, isChecked)
        }

        binding.readerSettingsOverscroll.isChecked = defaultSettings.overScrollMode
        binding.readerSettingsOverscroll.setOnCheckedChangeListener { _, isChecked ->
            defaultSettings.overScrollMode = isChecked
            PrefManager.setVal(PrefName.OverScrollMode, isChecked)
        }

        binding.readerSettingsVolumeButton.isChecked = defaultSettings.volumeButtons
        binding.readerSettingsVolumeButton.setOnCheckedChangeListener { _, isChecked ->
            defaultSettings.volumeButtons = isChecked
            PrefManager.setVal(PrefName.VolumeButtonsReader, isChecked)
        }

        binding.readerSettingsWrapImages.isChecked = defaultSettings.wrapImages
        binding.readerSettingsWrapImages.setOnCheckedChangeListener { _, isChecked ->
            defaultSettings.wrapImages = isChecked
            PrefManager.setVal(PrefName.WrapImages, isChecked)
        }

        binding.readerSettingsLongClickImage.isChecked = defaultSettings.longClickImage
        binding.readerSettingsLongClickImage.setOnCheckedChangeListener { _, isChecked ->
            defaultSettings.longClickImage = isChecked
            PrefManager.setVal(PrefName.LongClickImage, isChecked)
        }

        //LN settings
        val layoutListLN = listOf(
            binding.LNpaged,
            binding.LNcontinuous
        )

        binding.LNlayoutText.text = defaultSettingsLN.layout.string
        var selectedLN = layoutListLN[defaultSettingsLN.layout.ordinal]
        selectedLN.alpha = 1f

        layoutListLN.forEachIndexed { index, imageButton ->
            imageButton.setOnClickListener {
                selectedLN.alpha = 0.33f
                selectedLN = imageButton
                selectedLN.alpha = 1f
                defaultSettingsLN.layout = CurrentNovelReaderSettings.Layouts[index]
                    ?: CurrentNovelReaderSettings.Layouts.PAGED
                binding.LNlayoutText.text = defaultSettingsLN.layout.string
                PrefManager.setVal(PrefName.LayoutNovel, defaultSettingsLN.layout.ordinal)
            }
        }

        val dualListLN = listOf(
            binding.LNdualNo,
            binding.LNdualAuto,
            binding.LNdualForce
        )

        binding.LNdualPageText.text = defaultSettingsLN.dualPageMode.toString()
        var selectedDualLN = dualListLN[defaultSettingsLN.dualPageMode.ordinal]
        selectedDualLN.alpha = 1f

        dualListLN.forEachIndexed { index, imageButton ->
            imageButton.setOnClickListener {
                selectedDualLN.alpha = 0.33f
                selectedDualLN = imageButton
                selectedDualLN.alpha = 1f
                defaultSettingsLN.dualPageMode = CurrentReaderSettings.DualPageModes[index]
                    ?: CurrentReaderSettings.DualPageModes.Automatic
                binding.LNdualPageText.text = defaultSettingsLN.dualPageMode.toString()
                PrefManager.setVal(
                    PrefName.DualPageModeNovel,
                    defaultSettingsLN.dualPageMode.ordinal
                )
            }
        }

        binding.LNlineHeight.setText(defaultSettingsLN.lineHeight.toString())
        binding.LNlineHeight.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = binding.LNlineHeight.text.toString().toFloatOrNull() ?: 1.4f
                defaultSettingsLN.lineHeight = value
                binding.LNlineHeight.setText(value.toString())
                PrefManager.setVal(PrefName.LineHeight, value)
            }
        }

        binding.LNincrementLineHeight.setOnClickListener {
            val value = binding.LNlineHeight.text.toString().toFloatOrNull() ?: 1.4f
            defaultSettingsLN.lineHeight = value + 0.1f
            binding.LNlineHeight.setText(defaultSettingsLN.lineHeight.toString())
            PrefManager.setVal(PrefName.LineHeight, defaultSettingsLN.lineHeight)
        }

        binding.LNdecrementLineHeight.setOnClickListener {
            val value = binding.LNlineHeight.text.toString().toFloatOrNull() ?: 1.4f
            defaultSettingsLN.lineHeight = value - 0.1f
            binding.LNlineHeight.setText(defaultSettingsLN.lineHeight.toString())
            PrefManager.setVal(PrefName.LineHeight, defaultSettingsLN.lineHeight)
        }

        binding.LNmargin.setText(defaultSettingsLN.margin.toString())
        binding.LNmargin.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = binding.LNmargin.text.toString().toFloatOrNull() ?: 0.06f
                defaultSettingsLN.margin = value
                binding.LNmargin.setText(value.toString())
                PrefManager.setVal(PrefName.Margin, value)
            }
        }

        binding.LNincrementMargin.setOnClickListener {
            val value = binding.LNmargin.text.toString().toFloatOrNull() ?: 0.06f
            defaultSettingsLN.margin = value + 0.01f
            binding.LNmargin.setText(defaultSettingsLN.margin.toString())
            PrefManager.setVal(PrefName.Margin, defaultSettingsLN.margin)
        }

        binding.LNdecrementMargin.setOnClickListener {
            val value = binding.LNmargin.text.toString().toFloatOrNull() ?: 0.06f
            defaultSettingsLN.margin = value - 0.01f
            binding.LNmargin.setText(defaultSettingsLN.margin.toString())
            PrefManager.setVal(PrefName.Margin, defaultSettingsLN.margin)
        }

        binding.LNmaxInlineSize.setText(defaultSettingsLN.maxInlineSize.toString())
        binding.LNmaxInlineSize.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = binding.LNmaxInlineSize.text.toString().toIntOrNull() ?: 720
                defaultSettingsLN.maxInlineSize = value
                binding.LNmaxInlineSize.setText(value.toString())
                PrefManager.setVal(PrefName.MaxInlineSize, value)
            }
        }

        binding.LNincrementMaxInlineSize.setOnClickListener {
            val value = binding.LNmaxInlineSize.text.toString().toIntOrNull() ?: 720
            defaultSettingsLN.maxInlineSize = value + 10
            binding.LNmaxInlineSize.setText(defaultSettingsLN.maxInlineSize.toString())
            PrefManager.setVal(PrefName.MaxInlineSize, defaultSettingsLN.maxInlineSize)
        }

        binding.LNdecrementMaxInlineSize.setOnClickListener {
            val value = binding.LNmaxInlineSize.text.toString().toIntOrNull() ?: 720
            defaultSettingsLN.maxInlineSize = value - 10
            binding.LNmaxInlineSize.setText(defaultSettingsLN.maxInlineSize.toString())
            PrefManager.setVal(PrefName.MaxInlineSize, defaultSettingsLN.maxInlineSize)
        }

        binding.LNmaxBlockSize.setText(defaultSettingsLN.maxBlockSize.toString())
        binding.LNmaxBlockSize.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = binding.LNmaxBlockSize.text.toString().toIntOrNull() ?: 720
                defaultSettingsLN.maxBlockSize = value
                binding.LNmaxBlockSize.setText(value.toString())
                PrefManager.setVal(PrefName.MaxBlockSize, value)
            }
        }
        binding.LNincrementMaxBlockSize.setOnClickListener {
            val value = binding.LNmaxBlockSize.text.toString().toIntOrNull() ?: 720
            defaultSettingsLN.maxInlineSize = value + 10
            binding.LNmaxBlockSize.setText(defaultSettingsLN.maxInlineSize.toString())
            PrefManager.setVal(PrefName.MaxBlockSize, defaultSettingsLN.maxInlineSize)
        }

        binding.LNdecrementMaxBlockSize.setOnClickListener {
            val value = binding.LNmaxBlockSize.text.toString().toIntOrNull() ?: 720
            defaultSettingsLN.maxBlockSize = value - 10
            binding.LNmaxBlockSize.setText(defaultSettingsLN.maxBlockSize.toString())
            PrefManager.setVal(PrefName.MaxBlockSize, defaultSettingsLN.maxBlockSize)
        }

        binding.LNuseDarkTheme.isChecked = defaultSettingsLN.useDarkTheme
        binding.LNuseDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            defaultSettingsLN.useDarkTheme = isChecked
            PrefManager.setVal(PrefName.UseDarkThemeNovel, isChecked)
        }

        binding.LNuseOledTheme.isChecked = defaultSettingsLN.useOledTheme
        binding.LNuseOledTheme.setOnCheckedChangeListener { _, isChecked ->
            defaultSettingsLN.useOledTheme = isChecked
            PrefManager.setVal(PrefName.UseOledThemeNovel, isChecked)
        }

        binding.LNkeepScreenOn.isChecked = defaultSettingsLN.keepScreenOn
        binding.LNkeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            defaultSettingsLN.keepScreenOn = isChecked
            PrefManager.setVal(PrefName.KeepScreenOnNovel, isChecked)
        }

        binding.LNvolumeButton.isChecked = defaultSettingsLN.volumeButtons
        binding.LNvolumeButton.setOnCheckedChangeListener { _, isChecked ->
            defaultSettingsLN.volumeButtons = isChecked
            PrefManager.setVal(PrefName.VolumeButtonsNovel, isChecked)
        }

        //Update Progress
        binding.readerSettingsAskUpdateProgress.isChecked =
            PrefManager.getVal(PrefName.AskIndividualReader)
        binding.readerSettingsAskUpdateProgress.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.AskIndividualReader, isChecked)
            binding.readerSettingsAskChapterZero.isEnabled = !isChecked
        }
        binding.readerSettingsAskChapterZero.isChecked =
            PrefManager.getVal(PrefName.ChapterZeroReader)
        binding.readerSettingsAskChapterZero.isEnabled =
            !PrefManager.getVal<Boolean>(PrefName.AskIndividualReader)
        binding.readerSettingsAskChapterZero.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.ChapterZeroReader, isChecked)
        }
        binding.readerSettingsAskUpdateDoujins.isChecked =
            PrefManager.getVal(PrefName.UpdateForHReader)
        binding.readerSettingsAskUpdateDoujins.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.UpdateForHReader, isChecked)
            if (isChecked) snackString(getString(R.string.very_bold))
        }

    }
}