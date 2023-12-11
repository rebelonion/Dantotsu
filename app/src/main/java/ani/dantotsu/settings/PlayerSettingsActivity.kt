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
import ani.dantotsu.loadData
import ani.dantotsu.media.Media
import ani.dantotsu.navBarHeight
import ani.dantotsu.others.LangSet
import ani.dantotsu.others.getSerialized
import ani.dantotsu.parsers.Subtitle
import ani.dantotsu.saveData
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
        LangSet.setLocale(this)
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

        val settings = loadData<PlayerSettings>(player, toast = false) ?: PlayerSettings().apply {
            saveData(
                player,
                this
            )
        }

        binding.playerSettingsBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        //Video
        binding.playerSettingsVideoInfo.isChecked = settings.videoInfo
        binding.playerSettingsVideoInfo.setOnCheckedChangeListener { _, isChecked ->
            settings.videoInfo = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsQualityHeight.setText(
            (loadData<Int>("maxHeight", toast = false) ?: 480).toString()
        )
        binding.playerSettingsQualityHeight.addTextChangedListener {
            val height = binding.playerSettingsQualityHeight.text.toString().toIntOrNull()
            saveData("maxHeight", height)
        }
        binding.playerSettingsQualityWidth.setText(
            (loadData<Int>("maxWidth", toast = false) ?: 720).toString()
        )
        binding.playerSettingsQualityWidth.addTextChangedListener {
            val height = binding.playerSettingsQualityWidth.text.toString().toIntOrNull()
            saveData("maxWidth", height)
        }


        val speeds =
            arrayOf(0.25f, 0.33f, 0.5f, 0.66f, 0.75f, 1f, 1.25f, 1.33f, 1.5f, 1.66f, 1.75f, 2f)
        val cursedSpeeds = arrayOf(1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f, 5f, 10f, 25f, 50f)
        var curSpeedArr = if (settings.cursedSpeeds) cursedSpeeds else speeds
        var speedsName = curSpeedArr.map { "${it}x" }.toTypedArray()
        binding.playerSettingsSpeed.text =
            getString(R.string.default_playback_speed, speedsName[settings.defaultSpeed])
        val speedDialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString(R.string.default_speed))
        binding.playerSettingsSpeed.setOnClickListener {
            val dialog = speedDialog.setSingleChoiceItems(speedsName, settings.defaultSpeed) { dialog, i ->
                settings.defaultSpeed = i
                binding.playerSettingsSpeed.text =
                    getString(R.string.default_playback_speed, speedsName[i])
                saveData(player, settings)
                dialog.dismiss()
            }.show()
            dialog.window?.setDimAmount(0.8f)
        }

        binding.playerSettingsCursedSpeeds.isChecked = settings.cursedSpeeds
        binding.playerSettingsCursedSpeeds.setOnCheckedChangeListener { _, isChecked ->
            settings.cursedSpeeds = isChecked
            curSpeedArr = if (settings.cursedSpeeds) cursedSpeeds else speeds
            settings.defaultSpeed = if (settings.cursedSpeeds) 0 else 5
            speedsName = curSpeedArr.map { "${it}x" }.toTypedArray()
            binding.playerSettingsSpeed.text =
                getString(R.string.default_playback_speed, speedsName[settings.defaultSpeed])
            saveData(player, settings)
        }

        //Time Stamp
        binding.playerSettingsTimeStamps.isChecked = settings.timeStampsEnabled
        binding.playerSettingsTimeStamps.setOnCheckedChangeListener { _, isChecked ->
            settings.timeStampsEnabled = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsTimeStampsProxy.isChecked = settings.useProxyForTimeStamps
        binding.playerSettingsTimeStampsProxy.setOnCheckedChangeListener { _, isChecked ->
            settings.useProxyForTimeStamps = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsShowTimeStamp.isChecked = settings.showTimeStampButton
        binding.playerSettingsShowTimeStamp.setOnCheckedChangeListener { _, isChecked ->
            settings.showTimeStampButton = isChecked
            saveData(player, settings)
        }


        //Auto
        binding.playerSettingsAutoSkipOpEd.isChecked = settings.autoSkipOPED
        binding.playerSettingsAutoSkipOpEd.setOnCheckedChangeListener { _, isChecked ->
            settings.autoSkipOPED = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsAutoPlay.isChecked = settings.autoPlay
        binding.playerSettingsAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            settings.autoPlay = isChecked
            saveData(player, settings)
        }
        binding.playerSettingsAutoSkip.isChecked = settings.autoSkipFiller
        binding.playerSettingsAutoSkip.setOnCheckedChangeListener { _, isChecked ->
            settings.autoSkipFiller = isChecked
            saveData(player, settings)
        }

        //Update Progress
        binding.playerSettingsAskUpdateProgress.isChecked = settings.askIndividual
        binding.playerSettingsAskUpdateProgress.setOnCheckedChangeListener { _, isChecked ->
            settings.askIndividual = isChecked
            saveData(player, settings)
        }
        binding.playerSettingsAskUpdateHentai.isChecked = settings.updateForH
        binding.playerSettingsAskUpdateHentai.setOnCheckedChangeListener { _, isChecked ->
            settings.updateForH = isChecked
            if (isChecked) snackString(getString(R.string.very_bold))
            saveData(player, settings)
        }
        binding.playerSettingsCompletePercentage.value =
            (settings.watchPercentage * 100).roundToInt().toFloat()
        binding.playerSettingsCompletePercentage.addOnChangeListener { _, value, _ ->
            settings.watchPercentage = value / 100
            saveData(player, settings)
        }

        //Behaviour
        binding.playerSettingsAlwaysContinue.isChecked = settings.alwaysContinue
        binding.playerSettingsAlwaysContinue.setOnCheckedChangeListener { _, isChecked ->
            settings.alwaysContinue = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsPauseVideo.isChecked = settings.focusPause
        binding.playerSettingsPauseVideo.setOnCheckedChangeListener { _, isChecked ->
            settings.focusPause = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsVerticalGestures.isChecked = settings.gestures
        binding.playerSettingsVerticalGestures.setOnCheckedChangeListener { _, isChecked ->
            settings.gestures = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsDoubleTap.isChecked = settings.doubleTap
        binding.playerSettingsDoubleTap.setOnCheckedChangeListener { _, isChecked ->
            settings.doubleTap = isChecked
            saveData(player, settings)
        }
        binding.playerSettingsFastForward.isChecked = settings.fastforward
        binding.playerSettingsFastForward.setOnCheckedChangeListener { _, isChecked ->
            settings.fastforward = isChecked
            saveData(player, settings)
        }
        binding.playerSettingsSeekTime.value = settings.seekTime.toFloat()
        binding.playerSettingsSeekTime.addOnChangeListener { _, value, _ ->
            settings.seekTime = value.toInt()
            saveData(player, settings)
        }

        binding.exoSkipTime.setText(settings.skipTime.toString())
        binding.exoSkipTime.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.exoSkipTime.clearFocus()
            }
            false
        }
        binding.exoSkipTime.addTextChangedListener {
            val time = binding.exoSkipTime.text.toString().toIntOrNull()
            if (time != null) {
                settings.skipTime = time
                saveData(player, settings)
            }
        }

        //Other
        binding.playerSettingsPiP.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                visibility = View.VISIBLE
                isChecked = settings.pip
                setOnCheckedChangeListener { _, isChecked ->
                    settings.pip = isChecked
                    saveData(player, settings)
                }
            } else visibility = View.GONE
 
        }
        binding.playerSettingsDebug.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                visibility = View.VISIBLE
                isChecked = settings.pip
                setOnCheckedChangeListener { _, isChecked ->
                    settings.pip = isChecked
                    saveData(player, settings)
                }
            } else visibility = View.GONE
        }

        binding.playerSettingsCast.isChecked = settings.cast
        binding.playerSettingsCast.setOnCheckedChangeListener { _, isChecked ->
            settings.cast = isChecked
            saveData(player, settings)
        }

        val resizeModes = arrayOf("Original", "Zoom", "Stretch")
        val resizeDialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString(R.string.default_resize_mode))
        binding.playerResizeMode.setOnClickListener {
            val dialog = resizeDialog.setSingleChoiceItems(resizeModes, settings.resize) { dialog, count ->
                settings.resize = count
                saveData(player, settings)
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
        binding.subSwitch.isChecked = settings.subtitles
        binding.subSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.subtitles = isChecked
            saveData(player, settings)
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
        val primaryColorDialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString(R.string.primary_sub_color))
        binding.videoSubColorPrimary.setOnClickListener {
            val dialog = primaryColorDialog.setSingleChoiceItems(
                colorsPrimary,
                settings.primaryColor
            ) { dialog, count ->
                settings.primaryColor = count
                saveData(player, settings)
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
        val secondaryColorDialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString(R.string.outline_sub_color))
        binding.videoSubColorSecondary.setOnClickListener {
            val dialog = secondaryColorDialog.setSingleChoiceItems(
                colorsSecondary,
                settings.secondaryColor
            ) { dialog, count ->
                settings.secondaryColor = count
                saveData(player, settings)
                dialog.dismiss()
            }.show()
            dialog.window?.setDimAmount(0.8f)
        }
        val typesOutline = arrayOf("Outline", "Shine", "Drop Shadow", "None")
        val outlineDialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString(R.string.outline_type))
        binding.videoSubOutline.setOnClickListener {
            val dialog = outlineDialog.setSingleChoiceItems(typesOutline, settings.outline) { dialog, count ->
                settings.outline = count
                saveData(player, settings)
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
        val subBackgroundDialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString(R.string.outline_sub_color))
        binding.videoSubColorBackground.setOnClickListener {
            val dialog = subBackgroundDialog.setSingleChoiceItems(
                colorsSubBackground,
                settings.subBackground
            ) { dialog, count ->
                settings.subBackground = count
                saveData(player, settings)
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
        val subWindowDialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString(R.string.outline_sub_color))
        binding.videoSubColorWindow.setOnClickListener {
            val dialog = subWindowDialog.setSingleChoiceItems(
                colorsSubWindow,
                settings.subWindow
            ) { dialog, count ->
                settings.subWindow = count
                saveData(player, settings)
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
            "Century Gothic Bold"
        )
        val fontDialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString(R.string.subtitle_font))
        binding.videoSubFont.setOnClickListener {
            val dialog = fontDialog.setSingleChoiceItems(fonts, settings.font) { dialog, count ->
                settings.font = count
                saveData(player, settings)
                dialog.dismiss()
            }.show()
            dialog.window?.setDimAmount(0.8f)
        }
        binding.subtitleFontSize.setText(settings.fontSize.toString())
        binding.subtitleFontSize.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.subtitleFontSize.clearFocus()
            }
            false
        }
        binding.subtitleFontSize.addTextChangedListener {
            val size = binding.subtitleFontSize.text.toString().toIntOrNull()
            if (size != null) {
                settings.fontSize = size
                saveData(player, settings)
            }
        }
        toggleSubOptions(settings.subtitles)
    }
}
