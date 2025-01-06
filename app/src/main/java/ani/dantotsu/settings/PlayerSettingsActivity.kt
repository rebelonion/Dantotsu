package ani.dantotsu.settings

import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityPlayerSettingsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.media.Media
import ani.dantotsu.navBarHeight
import ani.dantotsu.others.Xpandable
import ani.dantotsu.others.getSerialized
import ani.dantotsu.parsers.Subtitle
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import ani.dantotsu.util.customAlertDialog
import com.google.android.material.slider.Slider.OnChangeListener
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.color.SimpleColorWheelDialog
import kotlin.math.roundToInt

class PlayerSettingsActivity :
    AppCompatActivity(),
    SimpleDialog.OnDialogResultListener {
    interface ColorPickerCallback {
        fun onColorSelected(color: Int)
    }

    private var colorPickerCallback: ColorPickerCallback? = null

    lateinit var binding: ActivityPlayerSettingsBinding
    private val player = "player_settings"

    var media: Media? = null
    var subtitle: Subtitle? = null

    private val Int.toSP
        get() =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                this.toFloat(),
                Resources.getSystem().displayMetrics,
            )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()
        binding = ActivityPlayerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        try {
            media = intent.getSerialized("media")
            subtitle = intent.getSerialized("subtitle")
        } catch (e: Exception) {
            toast(e.toString())
        }

        binding.playerSettingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }

        binding.playerSettingsBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Video

        val speeds =
            arrayOf(
                0.25f,
                0.33f,
                0.5f,
                0.66f,
                0.75f,
                1f,
                1.15f,
                1.25f,
                1.33f,
                1.5f,
                1.66f,
                1.75f,
                2f,
            )
        val cursedSpeeds = arrayOf(1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f, 5f, 10f, 25f, 50f)
        var curSpeedArr = if (PrefManager.getVal(PrefName.CursedSpeeds)) cursedSpeeds else speeds
        var speedsName = curSpeedArr.map { "${it}x" }.toTypedArray()
        binding.playerSettingsSpeed.text =
            getString(
                R.string.default_playback_speed,
                speedsName[PrefManager.getVal(PrefName.DefaultSpeed)],
            )
        binding.playerSettingsSpeed.setOnClickListener {
            customAlertDialog().apply {
                setTitle(getString(R.string.default_speed))
                singleChoiceItems(
                    speedsName,
                    PrefManager.getVal(PrefName.DefaultSpeed),
                ) { i ->
                    PrefManager.setVal(PrefName.DefaultSpeed, i)
                    binding.playerSettingsSpeed.text =
                        getString(R.string.default_playback_speed, speedsName[i])
                }
                show()
            }
        }

        binding.playerSettingsCursedSpeeds.isChecked = PrefManager.getVal(PrefName.CursedSpeeds)
        binding.playerSettingsCursedSpeeds.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.CursedSpeeds, isChecked)
            curSpeedArr = if (isChecked) cursedSpeeds else speeds
            val newDefaultSpeed = if (isChecked) 0 else 5
            PrefManager.setVal(PrefName.DefaultSpeed, newDefaultSpeed)
            speedsName = curSpeedArr.map { "${it}x" }.toTypedArray()
            binding.playerSettingsSpeed.text =
                getString(
                    R.string.default_playback_speed,
                    speedsName[PrefManager.getVal(PrefName.DefaultSpeed)],
                )
        }

        // Time Stamp
        binding.playerSettingsTimeStamps.isChecked = PrefManager.getVal(PrefName.TimeStampsEnabled)
        binding.playerSettingsTimeStamps.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.TimeStampsEnabled, isChecked)
            binding.playerSettingsAutoSkipOpEd.isEnabled = isChecked
        }

        binding.playerSettingsTimeStampsProxy.isChecked =
            PrefManager.getVal(PrefName.UseProxyForTimeStamps)
        binding.playerSettingsTimeStampsProxy.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.UseProxyForTimeStamps, isChecked)
        }

        binding.playerSettingsShowTimeStamp.isChecked =
            PrefManager.getVal(PrefName.ShowTimeStampButton)
        binding.playerSettingsShowTimeStamp.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.ShowTimeStampButton, isChecked)
            binding.playerSettingsTimeStampsAutoHide.isEnabled = isChecked
        }

        binding.playerSettingsTimeStampsAutoHide.isChecked =
            PrefManager.getVal(PrefName.AutoHideTimeStamps)
        binding.playerSettingsTimeStampsAutoHide.isEnabled =
            binding.playerSettingsShowTimeStamp.isChecked
        binding.playerSettingsTimeStampsAutoHide.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.AutoHideTimeStamps, isChecked)
        }

        // Auto
        binding.playerSettingsAutoSkipOpEd.isChecked = PrefManager.getVal(PrefName.AutoSkipOPED)
        binding.playerSettingsAutoSkipOpEd.isEnabled = binding.playerSettingsTimeStamps.isChecked
        binding.playerSettingsAutoSkipOpEd.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.AutoSkipOPED, isChecked)
        }

        binding.playerSettingsAutoSkipRecap.isChecked = PrefManager.getVal(PrefName.AutoSkipRecap)
        binding.playerSettingsAutoSkipRecap.isEnabled = binding.playerSettingsTimeStamps.isChecked
        binding.playerSettingsAutoSkipRecap.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.AutoSkipRecap, isChecked)
        }

        binding.playerSettingsAutoPlay.isChecked = PrefManager.getVal(PrefName.AutoPlay)
        binding.playerSettingsAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.AutoPlay, isChecked)
        }

        binding.playerSettingsAutoSkip.isChecked = PrefManager.getVal(PrefName.AutoSkipFiller)
        binding.playerSettingsAutoSkip.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.AutoSkipFiller, isChecked)
        }

        // Update Progress
        binding.playerSettingsAskUpdateProgress.isChecked =
            PrefManager.getVal(PrefName.AskIndividualPlayer)
        binding.playerSettingsAskUpdateProgress.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.AskIndividualPlayer, isChecked)
            binding.playerSettingsAskChapterZero.isEnabled = !isChecked
        }
        binding.playerSettingsAskChapterZero.isChecked =
            PrefManager.getVal(PrefName.ChapterZeroPlayer)
        binding.playerSettingsAskChapterZero.isEnabled =
            !binding.playerSettingsAskUpdateProgress.isChecked
        binding.playerSettingsAskChapterZero.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.ChapterZeroPlayer, isChecked)
        }
        binding.playerSettingsAskUpdateHentai.isChecked =
            PrefManager.getVal(PrefName.UpdateForHPlayer)
        binding.playerSettingsAskUpdateHentai.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.UpdateForHPlayer, isChecked)
            if (isChecked) snackString(getString(R.string.very_bold))
        }
        binding.playerSettingsCompletePercentage.value =
            (PrefManager.getVal<Float>(PrefName.WatchPercentage) * 100).roundToInt().toFloat()
        binding.playerSettingsCompletePercentage.addOnChangeListener { _, value, _ ->
            PrefManager.setVal(PrefName.WatchPercentage, value / 100)
        }

        // Behaviour
        binding.playerSettingsAlwaysContinue.isChecked = PrefManager.getVal(PrefName.AlwaysContinue)
        binding.playerSettingsAlwaysContinue.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.AlwaysContinue, isChecked)
        }

        binding.playerSettingsPauseVideo.isChecked = PrefManager.getVal(PrefName.FocusPause)
        binding.playerSettingsPauseVideo.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.FocusPause, isChecked)
        }

        binding.playerSettingsVerticalGestures.isChecked = PrefManager.getVal(PrefName.Gestures)
        binding.playerSettingsVerticalGestures.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.Gestures, isChecked)
        }

        binding.playerSettingsDoubleTap.isChecked = PrefManager.getVal(PrefName.DoubleTap)
        binding.playerSettingsDoubleTap.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.DoubleTap, isChecked)
        }
        binding.playerSettingsFastForward.isChecked = PrefManager.getVal(PrefName.FastForward)
        binding.playerSettingsFastForward.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.FastForward, isChecked)
        }
        binding.playerSettingsSeekTime.value = PrefManager.getVal<Int>(PrefName.SeekTime).toFloat()
        binding.playerSettingsSeekTime.addOnChangeListener { _, value, _ ->
            PrefManager.setVal(PrefName.SeekTime, value.toInt())
        }

        binding.exoSkipTime.setText(PrefManager.getVal<Int>(PrefName.SkipTime).toString())
        binding.exoSkipTime.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.exoSkipTime.clearFocus()
            }
            false
        }
        binding.exoSkipTime.addTextChangedListener {
            val time =
                binding.exoSkipTime.text
                    .toString()
                    .toIntOrNull()
            if (time != null) {
                PrefManager.setVal(PrefName.SkipTime, time)
            }
        }

        // Other
        binding.playerSettingsPiP.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                visibility = View.VISIBLE
                isChecked = PrefManager.getVal(PrefName.Pip)
                setOnCheckedChangeListener { _, isChecked ->
                    PrefManager.setVal(PrefName.Pip, isChecked)
                }
            } else {
                visibility = View.GONE
            }
        }

        binding.playerSettingsCast.isChecked = PrefManager.getVal(PrefName.Cast)
        binding.playerSettingsCast.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.Cast, isChecked)
        }

        binding.playerSettingsRotate.isChecked = PrefManager.getVal(PrefName.RotationPlayer)
        binding.playerSettingsRotate.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.RotationPlayer, isChecked)
        }

        binding.playerSettingsInternalCast.isChecked = PrefManager.getVal(PrefName.UseInternalCast)
        binding.playerSettingsInternalCast.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.UseInternalCast, isChecked)
        }

        binding.playerSettingsAdditionalCodec.isChecked =
            PrefManager.getVal(PrefName.UseAdditionalCodec)
        binding.playerSettingsAdditionalCodec.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.UseAdditionalCodec, isChecked)
        }

        val resizeModes = arrayOf("Original", "Zoom", "Stretch")
        binding.playerResizeMode.setOnClickListener {
            customAlertDialog().apply {
                setTitle(getString(R.string.default_resize_mode))
                singleChoiceItems(
                    resizeModes,
                    PrefManager.getVal<Int>(PrefName.Resize),
                ) { count ->
                    PrefManager.setVal(PrefName.Resize, count)
                }
                show()
            }
        }

        fun toggleSubOptions(isChecked: Boolean) {
            arrayOf(
                binding.videoSubColorPrimary,
                binding.videoSubColorSecondary,
                binding.videoSubOutline,
                binding.videoSubColorBackground,
                binding.videoSubAlphaButton,
                binding.videoSubColorWindow,
                binding.videoSubFont,
                binding.videoSubAlpha,
                binding.videoSubStroke,
                binding.subtitleFontSizeText,
                binding.subtitleFontSize,
                binding.videoSubLanguage,
                binding.subTextSwitch,
            ).forEach {
                it.isEnabled = isChecked
                it.alpha =
                    when (isChecked) {
                        true -> 1f
                        false -> 0.5f
                    }
            }
        }

        fun toggleExpSubOptions(isChecked: Boolean) {
            arrayOf(
                binding.videoSubStrokeButton,
                binding.videoSubStroke,
                binding.videoSubBottomMarginButton,
                binding.videoSubBottomMargin,
            ).forEach {
                it.isEnabled = isChecked
                it.alpha =
                    when (isChecked) {
                        true -> 1f
                        false -> 0.5f
                    }
            }
        }

        binding.subSwitch.isChecked = PrefManager.getVal(PrefName.Subtitles)
        binding.subSwitch.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.Subtitles, isChecked)
            toggleSubOptions(isChecked)
            toggleExpSubOptions(binding.subTextSwitch.isChecked && isChecked)
        }
        toggleSubOptions(binding.subSwitch.isChecked)

        binding.subTextSwitch.isChecked = PrefManager.getVal(PrefName.TextviewSubtitles)
        binding.subTextSwitch.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.TextviewSubtitles, isChecked)
            toggleExpSubOptions(isChecked)
        }
        toggleExpSubOptions(binding.subTextSwitch.isChecked)

        val subLanguages =
            arrayOf(
                "Albanian",
                "Arabic",
                "Bosnian",
                "Bulgarian",
                "Chinese",
                "Croatian",
                "Czech",
                "Danish",
                "Dutch",
                "English",
                "Estonian",
                "Finnish",
                "French",
                "Georgian",
                "German",
                "Greek",
                "Hebrew",
                "Hindi",
                "Indonesian",
                "Irish",
                "Italian",
                "Japanese",
                "Korean",
                "Lithuanian",
                "Luxembourgish",
                "Macedonian",
                "Mongolian",
                "Norwegian",
                "Polish",
                "Portuguese",
                "Punjabi",
                "Romanian",
                "Russian",
                "Serbian",
                "Slovak",
                "Slovenian",
                "Spanish",
                "Turkish",
                "Ukrainian",
                "Urdu",
                "Vietnamese",
            )
        binding.videoSubLanguage.setOnClickListener {
            customAlertDialog().apply {
                setTitle(getString(R.string.subtitle_langauge))
                singleChoiceItems(
                    subLanguages,
                    PrefManager.getVal(PrefName.SubLanguage),
                ) { count ->
                    PrefManager.setVal(PrefName.SubLanguage, count)
                }
                show()
            }
        }

        binding.videoSubColorPrimary.setOnClickListener {
            val color = PrefManager.getVal<Int>(PrefName.PrimaryColor)
            val title = getString(R.string.primary_sub_color)
            showColorPicker(
                color,
                title,
                object : ColorPickerCallback {
                    override fun onColorSelected(color: Int) {
                        PrefManager.setVal(PrefName.PrimaryColor, color)
                        updateSubPreview()
                    }
                },
            )
        }

        binding.videoSubColorSecondary.setOnClickListener {
            val color = PrefManager.getVal<Int>(PrefName.SecondaryColor)
            val title = getString(R.string.outline_sub_color)
            showColorPicker(
                color,
                title,
                object : ColorPickerCallback {
                    override fun onColorSelected(color: Int) {
                        PrefManager.setVal(PrefName.SecondaryColor, color)
                        updateSubPreview()
                    }
                },
            )
        }

        val typesOutline = arrayOf("Outline", "Shine", "Drop Shadow", "None")
        binding.videoSubOutline.setOnClickListener {
            customAlertDialog().apply {
                setTitle(getString(R.string.outline_type))
                singleChoiceItems(
                    typesOutline,
                    PrefManager.getVal(PrefName.Outline),
                ) { count ->
                    PrefManager.setVal(PrefName.Outline, count)
                    updateSubPreview()
                }
                show()
            }
        }

        binding.videoSubColorBackground.setOnClickListener {
            val color = PrefManager.getVal<Int>(PrefName.SubBackground)
            val title = getString(R.string.sub_background_color_select)
            showColorPicker(
                color,
                title,
                object : ColorPickerCallback {
                    override fun onColorSelected(color: Int) {
                        PrefManager.setVal(PrefName.SubBackground, color)
                        updateSubPreview()
                    }
                },
            )
        }

        binding.videoSubColorWindow.setOnClickListener {
            val color = PrefManager.getVal<Int>(PrefName.SubWindow)
            val title = getString(R.string.sub_window_color_select)
            showColorPicker(
                color,
                title,
                object : ColorPickerCallback {
                    override fun onColorSelected(color: Int) {
                        PrefManager.setVal(PrefName.SubWindow, color)
                        updateSubPreview()
                    }
                },
            )
        }

        binding.videoSubAlpha.value = PrefManager.getVal(PrefName.SubAlpha)
        binding.videoSubAlpha.addOnChangeListener(
            OnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    PrefManager.setVal(PrefName.SubAlpha, value)
                    updateSubPreview()
                }
            },
        )

        binding.videoSubStroke.value = PrefManager.getVal(PrefName.SubStroke)
        binding.videoSubStroke.addOnChangeListener(
            OnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    PrefManager.setVal(PrefName.SubStroke, value)
                    updateSubPreview()
                }
            },
        )

        binding.videoSubBottomMargin.value = PrefManager.getVal(PrefName.SubBottomMargin)
        binding.videoSubBottomMargin.addOnChangeListener(
            OnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    PrefManager.setVal(PrefName.SubBottomMargin, value)
                    updateSubPreview()
                }
            },
        )

        val fonts =
            arrayOf(
                "Poppins Semi Bold",
                "Poppins Bold",
                "Poppins",
                "Poppins Thin",
                "Century Gothic",
                "Levenim MT Bold",
                "Blocky",
            )
        binding.videoSubFont.setOnClickListener {
            customAlertDialog().apply {
                setTitle(getString(R.string.subtitle_font))
                singleChoiceItems(
                    fonts,
                    PrefManager.getVal(PrefName.Font),
                ) { count ->
                    PrefManager.setVal(PrefName.Font, count)
                    updateSubPreview()
                }
                show()
            }
        }
        binding.subtitleFontSize.setText(PrefManager.getVal<Int>(PrefName.FontSize).toString())
        binding.subtitleFontSize.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.subtitleFontSize.clearFocus()
            }
            false
        }
        binding.subtitleFontSize.addTextChangedListener {
            val size =
                binding.subtitleFontSize.text
                    .toString()
                    .toIntOrNull()
            if (size != null) {
                PrefManager.setVal(PrefName.FontSize, size)
                updateSubPreview()
            }
        }
        binding.subtitleTest.addOnChangeListener(
            object : Xpandable.OnChangeListener {
                override fun onExpand() {
                    updateSubPreview()
                }

                override fun onRetract() {}
            },
        )
        updateSubPreview()
    }

    private fun showColorPicker(
        originalColor: Int,
        title: String,
        callback: ColorPickerCallback,
    ) {
        colorPickerCallback = callback

        SimpleColorWheelDialog()
            .title(title)
            .color(originalColor)
            .alpha(true)
            .neg()
            .theme(R.style.MyPopup)
            .show(this, "colorPicker")
    }

    override fun onResult(
        dialogTag: String,
        which: Int,
        extras: Bundle,
    ): Boolean {
        if (dialogTag == "colorPicker" && which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE) {
            val color = extras.getInt(SimpleColorWheelDialog.COLOR)
            colorPickerCallback?.onColorSelected(color)

            return true
        }
        return false
    }

    private fun updateSubPreview() {
        binding.subtitleTestWindow.run {
            alpha = PrefManager.getVal(PrefName.SubAlpha)
            setBackgroundColor(PrefManager.getVal(PrefName.SubWindow))
        }

        binding.subtitleTestText.run {
            textSize = PrefManager.getVal<Int>(PrefName.FontSize).toSP
            typeface =
                when (PrefManager.getVal<Int>(PrefName.Font)) {
                    0 -> ResourcesCompat.getFont(this.context, R.font.poppins_semi_bold)
                    1 -> ResourcesCompat.getFont(this.context, R.font.poppins_bold)
                    2 -> ResourcesCompat.getFont(this.context, R.font.poppins)
                    3 -> ResourcesCompat.getFont(this.context, R.font.poppins_thin)
                    4 -> ResourcesCompat.getFont(this.context, R.font.century_gothic_regular)
                    5 -> ResourcesCompat.getFont(this.context, R.font.levenim_mt_bold)
                    6 -> ResourcesCompat.getFont(this.context, R.font.blocky)
                    else -> ResourcesCompat.getFont(this.context, R.font.poppins_semi_bold)
                }

            setTextColor(PrefManager.getVal<Int>(PrefName.PrimaryColor))

            setBackgroundColor(PrefManager.getVal<Int>(PrefName.SubBackground))
        }
    }
}
