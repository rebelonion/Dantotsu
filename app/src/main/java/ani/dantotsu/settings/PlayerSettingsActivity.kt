package ani.dantotsu.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityPlayerSettingsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.media.Media
import ani.dantotsu.navBarHeight
import ani.dantotsu.others.getSerialized
import ani.dantotsu.parsers.Subtitle
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import com.google.android.material.snackbar.Snackbar
import kotlin.math.roundToInt


class PlayerSettingsActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlayerSettingsBinding
    private val player = "player_settings"

    var media: Media? = null
    var subtitle: Subtitle? = null


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

        //Video

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
                2f
            )
        val cursedSpeeds = arrayOf(1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f, 5f, 10f, 25f, 50f)
        var curSpeedArr = if (PrefManager.getVal(PrefName.CursedSpeeds)) cursedSpeeds else speeds
        var speedsName = curSpeedArr.map { "${it}x" }.toTypedArray()
        binding.playerSettingsSpeed.text =
            getString(
                R.string.default_playback_speed,
                speedsName[PrefManager.getVal(PrefName.DefaultSpeed)]
            )
        val speedDialog = AlertDialog.Builder(this, R.style.MyPopup)
            .setTitle(getString(R.string.default_speed))
        binding.playerSettingsSpeed.setOnClickListener {
            val dialog =
                speedDialog.setSingleChoiceItems(
                    speedsName,
                    PrefManager.getVal(PrefName.DefaultSpeed)
                ) { dialog, i ->
                    PrefManager.setVal(PrefName.DefaultSpeed, i)
                    binding.playerSettingsSpeed.text =
                        getString(R.string.default_playback_speed, speedsName[i])
                    dialog.dismiss()
                }.show()
            dialog.window?.setDimAmount(0.8f)
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
                    speedsName[PrefManager.getVal(PrefName.DefaultSpeed)]
                )
        }


        // Time Stamp
        binding.playerSettingsTimeStamps.isChecked = PrefManager.getVal(PrefName.TimeStampsEnabled)
        binding.playerSettingsTimeStamps.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.TimeStampsEnabled, isChecked)
        }

        binding.playerSettingsTimeStampsAutoHide.isChecked = PrefManager.getVal(PrefName.AutoHideTimeStamps)
        binding.playerSettingsTimeStampsAutoHide.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.AutoHideTimeStamps, isChecked)
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
        }

        // Auto
        binding.playerSettingsAutoSkipOpEd.isChecked = PrefManager.getVal(PrefName.AutoSkipOPED)
        binding.playerSettingsAutoSkipOpEd.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.AutoSkipOPED, isChecked)
        }

        binding.playerSettingsAutoPlay.isChecked = PrefManager.getVal(PrefName.AutoPlay)
        binding.playerSettingsAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.AutoPlay, isChecked)
        }

        binding.playerSettingsAutoSkip.isChecked = PrefManager.getVal(PrefName.AutoSkipFiller)
        binding.playerSettingsAutoSkip.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.AutoSkipFiller, isChecked)
        }

        //Update Progress
        binding.playerSettingsAskUpdateProgress.isChecked =
            PrefManager.getVal(PrefName.AskIndividualPlayer)
        binding.playerSettingsAskUpdateProgress.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.AskIndividualPlayer, isChecked)
            binding.playerSettingsAskChapterZero.isEnabled = !isChecked
        }
        binding.playerSettingsAskChapterZero.isChecked =
            PrefManager.getVal(PrefName.ChapterZeroPlayer)
        binding.playerSettingsAskChapterZero.isEnabled =
            !PrefManager.getVal<Boolean>(PrefName.AskIndividualPlayer)
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

        //Behaviour
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
            val time = binding.exoSkipTime.text.toString().toIntOrNull()
            if (time != null) {
                PrefManager.setVal(PrefName.SkipTime, time)
            }
        }

        //Other
        binding.playerSettingsPiP.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                visibility = View.VISIBLE
                isChecked = PrefManager.getVal(PrefName.Pip)
                setOnCheckedChangeListener { _, isChecked ->
                    PrefManager.setVal(PrefName.Pip, isChecked)
                }
            } else visibility = View.GONE
        }

        binding.playerSettingsCast.isChecked = PrefManager.getVal(PrefName.Cast)
        binding.playerSettingsCast.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.Cast, isChecked)
        }

        binding.playerSettingsInternalCast.isChecked = PrefManager.getVal(PrefName.UseInternalCast)
        binding.playerSettingsInternalCast.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.UseInternalCast, isChecked)
        }

        binding.playerSettingsRotate.isChecked = PrefManager.getVal(PrefName.RotationPlayer)
        binding.playerSettingsRotate.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.RotationPlayer, isChecked)
        }

        val resizeModes = arrayOf("Original", "Zoom", "Stretch")
        val resizeDialog = AlertDialog.Builder(this, R.style.MyPopup)
            .setTitle(getString(R.string.default_resize_mode))
        binding.playerResizeMode.setOnClickListener {
            val dialog = resizeDialog.setSingleChoiceItems(
                resizeModes,
                PrefManager.getVal<Int>(PrefName.Resize)
            ) { dialog, count ->
                PrefManager.setVal(PrefName.Resize, count)
                dialog.dismiss()
            }.show()
            dialog.window?.setDimAmount(0.8f)
        }

        fun restartApp() {
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

        fun toggleButton(button: android.widget.Button, toggle: Boolean) {
            button.isClickable = toggle
            button.alpha = when (toggle) {
                true -> 1f
                false -> 0.5f
            }
        }

        fun toggleSubOptions(isChecked: Boolean) {
            toggleButton(binding.videoSubColorPrimary, isChecked)
            toggleButton(binding.videoSubColorSecondary, isChecked)
            toggleButton(binding.videoSubOutline, isChecked)
            toggleButton(binding.videoSubFont, isChecked)
            binding.subtitleFontSizeCard.isEnabled = isChecked
            binding.subtitleFontSizeCard.isClickable = isChecked
            binding.subtitleFontSizeCard.alpha = when (isChecked) {
                true -> 1f
                false -> 0.5f
            }
            binding.subtitleFontSize.isEnabled = isChecked
            binding.subtitleFontSize.isClickable = isChecked
            binding.subtitleFontSize.alpha = when (isChecked) {
                true -> 1f
                false -> 0.5f
            }
            ActivityPlayerSettingsBinding.bind(binding.root).subtitleFontSizeText.isEnabled =
                isChecked
            ActivityPlayerSettingsBinding.bind(binding.root).subtitleFontSizeText.isClickable =
                isChecked
            ActivityPlayerSettingsBinding.bind(binding.root).subtitleFontSizeText.alpha =
                when (isChecked) {
                    true -> 1f
                    false -> 0.5f
                }
        }
        binding.subSwitch.isChecked = PrefManager.getVal(PrefName.Subtitles)
        binding.subSwitch.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.Subtitles, isChecked)
            toggleSubOptions(isChecked)
            restartApp()
        }
        val colorsPrimary =
            arrayOf(
                "Black",
                "Dark Gray",
                "Gray",
                "Light Gray",
                "White",
                "Red",
                "Yellow",
                "Green",
                "Cyan",
                "Blue",
                "Magenta"
            )
        val primaryColorDialog = AlertDialog.Builder(this, R.style.MyPopup)
            .setTitle(getString(R.string.primary_sub_color))
        binding.videoSubColorPrimary.setOnClickListener {
            val dialog = primaryColorDialog.setSingleChoiceItems(
                colorsPrimary,
                PrefManager.getVal(PrefName.PrimaryColor)
            ) { dialog, count ->
                PrefManager.setVal(PrefName.PrimaryColor, count)
                dialog.dismiss()
            }.show()
            dialog.window?.setDimAmount(0.8f)
        }
        val colorsSecondary = arrayOf(
            "Black",
            "Dark Gray",
            "Gray",
            "Light Gray",
            "White",
            "Red",
            "Yellow",
            "Green",
            "Cyan",
            "Blue",
            "Magenta",
            "Transparent"
        )
        val secondaryColorDialog = AlertDialog.Builder(this, R.style.MyPopup)
            .setTitle(getString(R.string.outline_sub_color))
        binding.videoSubColorSecondary.setOnClickListener {
            val dialog = secondaryColorDialog.setSingleChoiceItems(
                colorsSecondary,
                PrefManager.getVal(PrefName.SecondaryColor)
            ) { dialog, count ->
                PrefManager.setVal(PrefName.SecondaryColor, count)
                dialog.dismiss()
            }.show()
            dialog.window?.setDimAmount(0.8f)
        }
        val typesOutline = arrayOf("Outline", "Shine", "Drop Shadow", "None")
        val outlineDialog = AlertDialog.Builder(this, R.style.MyPopup)
            .setTitle(getString(R.string.outline_type))
        binding.videoSubOutline.setOnClickListener {
            val dialog = outlineDialog.setSingleChoiceItems(
                typesOutline,
                PrefManager.getVal(PrefName.Outline)
            ) { dialog, count ->
                PrefManager.setVal(PrefName.Outline, count)
                dialog.dismiss()
            }.show()
            dialog.window?.setDimAmount(0.8f)
        }
        val colorsSubBackground = arrayOf(
            "Transparent",
            "Black",
            "Dark Gray",
            "Gray",
            "Light Gray",
            "White",
            "Red",
            "Yellow",
            "Green",
            "Cyan",
            "Blue",
            "Magenta"
        )
        val subBackgroundDialog = AlertDialog.Builder(this, R.style.MyPopup)
            .setTitle(getString(R.string.outline_sub_color))
        binding.videoSubColorBackground.setOnClickListener {
            val dialog = subBackgroundDialog.setSingleChoiceItems(
                colorsSubBackground,
                PrefManager.getVal(PrefName.SubBackground)
            ) { dialog, count ->
                PrefManager.setVal(PrefName.SubBackground, count)
                dialog.dismiss()
            }.show()
            dialog.window?.setDimAmount(0.8f)
        }

        val colorsSubWindow = arrayOf(
            "Transparent",
            "Black",
            "Dark Gray",
            "Gray",
            "Light Gray",
            "White",
            "Red",
            "Yellow",
            "Green",
            "Cyan",
            "Blue",
            "Magenta"
        )
        val subWindowDialog = AlertDialog.Builder(this, R.style.MyPopup)
            .setTitle(getString(R.string.outline_sub_color))
        binding.videoSubColorWindow.setOnClickListener {
            val dialog = subWindowDialog.setSingleChoiceItems(
                colorsSubWindow,
                PrefManager.getVal(PrefName.SubWindow)
            ) { dialog, count ->
                PrefManager.setVal(PrefName.SubWindow, count)
                dialog.dismiss()
            }.show()
            dialog.window?.setDimAmount(0.8f)
        }
        val fonts = arrayOf(
            "Poppins Semi Bold",
            "Poppins Bold",
            "Poppins",
            "Poppins Thin",
            "Century Gothic",
            "Levenim MT Bold",
            "Blocky"
        )
        val fontDialog = AlertDialog.Builder(this, R.style.MyPopup)
            .setTitle(getString(R.string.subtitle_font))
        binding.videoSubFont.setOnClickListener {
            val dialog = fontDialog.setSingleChoiceItems(
                fonts,
                PrefManager.getVal(PrefName.Font)
            ) { dialog, count ->
                PrefManager.setVal(PrefName.Font, count)
                dialog.dismiss()
            }.show()
            dialog.window?.setDimAmount(0.8f)
        }
        binding.subtitleFontSize.setText(PrefManager.getVal<Int>(PrefName.FontSize).toString())
        binding.subtitleFontSize.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.subtitleFontSize.clearFocus()
            }
            false
        }
        binding.subtitleFontSize.addTextChangedListener {
            val size = binding.subtitleFontSize.text.toString().toIntOrNull()
            if (size != null) {
                PrefManager.setVal(PrefName.FontSize, size)
            }
        }
        toggleSubOptions(PrefManager.getVal(PrefName.Subtitles))
    }
}
