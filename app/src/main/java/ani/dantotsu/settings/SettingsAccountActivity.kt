package ani.dantotsu.settings

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.discord.Discord
import ani.dantotsu.connections.mal.MAL
import ani.dantotsu.databinding.ActivitySettingsAccountsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.loadImage
import ani.dantotsu.navBarHeight
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.startMainActivity
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin

class SettingsAccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsAccountsBinding
    private val restartMainActivity = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = startMainActivity(this@SettingsAccountActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this

        binding = ActivitySettingsAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            settingsAccountsLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            accountSettingsBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

            settingsAccountHelp.setOnClickListener {
                CustomBottomDialog.newInstance().apply {
                    setTitleText(context.getString(R.string.account_help))
                    addView(
                        TextView(it.context).apply {
                            val markWon = Markwon.builder(it.context)
                                .usePlugin(SoftBreakAddsNewLinePlugin.create()).build()
                            markWon.setMarkdown(this, context.getString(R.string.full_account_help))
                        }
                    )
                }.show(supportFragmentManager, "dialog")
            }

            fun reload() {
                if (Anilist.token != null) {
                    settingsAnilistLogin.setText(R.string.logout)
                    settingsAnilistLogin.setOnClickListener {
                        Anilist.removeSavedToken()
                        restartMainActivity.isEnabled = true
                        reload()
                    }
                    settingsAnilistUsername.visibility = View.VISIBLE
                    settingsAnilistUsername.text = Anilist.username
                    settingsAnilistAvatar.loadImage(Anilist.avatar)
                    settingsAnilistAvatar.setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        val anilistLink = getString(
                            R.string.anilist_link,
                            PrefManager.getVal<String>(PrefName.AnilistUserName)
                        )
                        openLinkInBrowser(anilistLink)
                    }

                    settingsMALLoginRequired.visibility = View.GONE
                    settingsMALLogin.visibility = View.VISIBLE
                    settingsMALUsername.visibility = View.VISIBLE

                    if (MAL.token != null) {
                        settingsMALLogin.setText(R.string.logout)
                        settingsMALLogin.setOnClickListener {
                            MAL.removeSavedToken()
                            restartMainActivity.isEnabled = true
                            reload()
                        }
                        settingsMALUsername.visibility = View.VISIBLE
                        settingsMALUsername.text = MAL.username
                        settingsMALAvatar.loadImage(MAL.avatar)
                        settingsMALAvatar.setOnClickListener {
                            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            openLinkInBrowser(getString(R.string.myanilist_link, MAL.username))
                        }
                    } else {
                        settingsMALAvatar.setImageResource(R.drawable.ic_round_person_24)
                        settingsMALUsername.visibility = View.GONE
                        settingsMALLogin.setText(R.string.login)
                        settingsMALLogin.setOnClickListener {
                            MAL.loginIntent(context)
                        }
                    }
                } else {
                    settingsAnilistAvatar.setImageResource(R.drawable.ic_round_person_24)
                    settingsAnilistUsername.visibility = View.GONE
                    settingsAnilistLogin.setText(R.string.login)
                    settingsAnilistLogin.setOnClickListener {
                        Anilist.loginIntent(context)
                    }
                    settingsMALLoginRequired.visibility = View.VISIBLE
                    settingsMALLogin.visibility = View.GONE
                    settingsMALUsername.visibility = View.GONE
                }

                if (Discord.token != null) {
                    val id = PrefManager.getVal(PrefName.DiscordId, null as String?)
                    val avatar = PrefManager.getVal(PrefName.DiscordAvatar, null as String?)
                    val username = PrefManager.getVal(PrefName.DiscordUserName, null as String?)
                    if (id != null && avatar != null) {
                        settingsDiscordAvatar.loadImage("https://cdn.discordapp.com/avatars/$id/$avatar.png")
                        settingsDiscordAvatar.setOnClickListener {
                            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            val discordLink = getString(R.string.discord_link, id)
                            openLinkInBrowser(discordLink)
                        }
                    }
                    settingsDiscordUsername.visibility = View.VISIBLE
                    settingsDiscordUsername.text =
                        username ?: Discord.token?.replace(Regex("."), "*")
                    settingsDiscordLogin.setText(R.string.logout)
                    settingsDiscordLogin.setOnClickListener {
                        Discord.removeSavedToken(context)
                        restartMainActivity.isEnabled = true
                        reload()
                    }

                    settingsImageSwitcher.visibility = View.VISIBLE
                    var initialStatus = when (PrefManager.getVal<String>(PrefName.DiscordStatus)) {
                        "online" -> R.drawable.discord_status_online
                        "idle" -> R.drawable.discord_status_idle
                        "dnd" -> R.drawable.discord_status_dnd
                        else -> R.drawable.discord_status_online
                    }
                    settingsImageSwitcher.setImageResource(initialStatus)

                    val zoomInAnimation =
                        AnimationUtils.loadAnimation(context, R.anim.bounce_zoom)
                    settingsImageSwitcher.setOnClickListener {
                        var status = "online"
                        initialStatus = when (initialStatus) {
                            R.drawable.discord_status_online -> {
                                status = "idle"
                                R.drawable.discord_status_idle
                            }

                            R.drawable.discord_status_idle -> {
                                status = "dnd"
                                R.drawable.discord_status_dnd
                            }

                            R.drawable.discord_status_dnd -> {
                                status = "online"
                                R.drawable.discord_status_online
                            }

                            R.drawable.discord_status_dnd -> {
                                status = "invisible"
                                R.drawable.discord_status_invisible
                            }

                            else -> R.drawable.discord_status_online
                        }

                        PrefManager.setVal(PrefName.DiscordStatus, status)
                        settingsImageSwitcher.setImageResource(initialStatus)
                        settingsImageSwitcher.startAnimation(zoomInAnimation)
                    }
                    settingsImageSwitcher.setOnLongClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        DiscordDialogFragment().show(supportFragmentManager, "dialog")
                        true
                    }
                } else {
                    settingsImageSwitcher.visibility = View.GONE
                    settingsDiscordAvatar.setImageResource(R.drawable.ic_round_person_24)
                    settingsDiscordUsername.visibility = View.GONE
                    settingsDiscordLogin.setText(R.string.login)
                    settingsDiscordLogin.setOnClickListener {
                        Discord.warning(context)
                            .show(supportFragmentManager, "dialog")
                    }
                }
            }
            reload()
        }
    }

}
