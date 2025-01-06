package ani.dantotsu.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.BuildConfig
import ani.dantotsu.R
import ani.dantotsu.buildMarkwon
import ani.dantotsu.client
import ani.dantotsu.databinding.ActivitySettingsAboutBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.restartApp
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsAboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this

        binding = ActivitySettingsAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            settingsAboutLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            aboutSettingsBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

            settingsRecyclerView.adapter = SettingsAdapter(
                arrayListOf(
                    Settings(
                        type = 1,
                        name = getString(R.string.faq),
                        desc = getString(R.string.faq_desc),
                        icon = R.drawable.ic_round_help_24,
                        onClick = {
                            startActivity(Intent(context, FAQActivity::class.java))
                        },
                        isActivity = true
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.check_app_updates),
                        desc = getString(R.string.check_app_updates_desc),
                        icon = R.drawable.ic_round_new_releases_24,
                        isChecked = PrefManager.getVal(PrefName.CheckUpdate),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.CheckUpdate, isChecked)
                        },
                        isVisible = !BuildConfig.FLAVOR.contains("fdroid")
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.share_username_in_crash_reports),
                        desc = getString(R.string.share_username_in_crash_reports_desc),
                        icon = R.drawable.ic_round_search_24,
                        isChecked = PrefManager.getVal(PrefName.SharedUserID),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.SharedUserID, isChecked)
                        },
                        isVisible = !BuildConfig.FLAVOR.contains("fdroid")
                    ),
                    Settings(
                        type = 2,
                        name = getString(R.string.log_to_file),
                        desc = getString(R.string.logging_warning),
                        icon = R.drawable.ic_round_edit_note_24,
                        isChecked = PrefManager.getVal(PrefName.LogToFile),
                        switch = { isChecked, _ ->
                            PrefManager.setVal(PrefName.LogToFile, isChecked)
                            Logger.clearLog()
                            restartApp()
                        },
                        attachToSwitch = {
                            it.settingsExtraIcon.visibility = View.VISIBLE
                            it.settingsExtraIcon.setImageResource(R.drawable.ic_round_share_24)
                            it.settingsExtraIcon.setOnClickListener {
                                Logger.shareLog(context)
                            }

                        }
                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.devs),
                        desc = getString(R.string.devs_desc),
                        icon = R.drawable.ic_round_accessible_forward_24,
                        onClick = {
                            DevelopersDialogFragment().show(supportFragmentManager, "dialog")
                        }
                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.forks),
                        desc = getString(R.string.forks_desc),
                        icon = R.drawable.ic_round_restaurant_24,
                        onClick = {
                            ForksDialogFragment().show(supportFragmentManager, "dialog")
                        }
                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.disclaimer),
                        desc = getString(R.string.disclaimer_desc),
                        icon = R.drawable.ic_round_info_24,
                        onClick = {
                            val text = TextView(context)
                            text.setText(R.string.full_disclaimer)

                            CustomBottomDialog.newInstance().apply {
                                setTitleText(context.getString(R.string.disclaimer))
                                addView(text)
                                setNegativeButton(context.getString(R.string.close)) {
                                    dismiss()
                                }
                                show(supportFragmentManager, "dialog")
                            }
                        }
                    ),
                    Settings(
                        type = 1,
                        name = getString(R.string.privacy_policy),
                        desc = getString(R.string.privacy_policy_desc),
                        icon = R.drawable.ic_incognito_24,
                        onClick = {
                            val text = TextView(context)
                            val pPLink =
                                "https://raw.githubusercontent.com/rebelonion/Dantotsu/main/privacy_policy.md"
                            val backup =
                                "https://gcore.jsdelivr.net/gh/rebelonion/dantotsu/privacy_policy.md"
                            text.text = getString(R.string.loading)
                            val markWon = try {
                                buildMarkwon(this@SettingsAboutActivity, false)
                            } catch (e: IllegalArgumentException) {
                                return@Settings
                            }
                            CoroutineScope(Dispatchers.IO).launch {
                                val res = try {
                                    val out = client.get(pPLink)
                                    if (out.code != 200) {
                                        client.get(backup)
                                    } else {
                                        out
                                    }.text
                                } catch (e: Exception) {
                                    getString(R.string.failed_to_load)
                                }
                                runOnUiThread {
                                    markWon.setMarkdown(text, res)
                                }
                            }

                            CustomBottomDialog.newInstance().apply {
                                setTitleText(context.getString(R.string.privacy_policy))
                                addView(text)
                                setNegativeButton(context.getString(R.string.close)) {
                                    dismiss()
                                }
                                show(supportFragmentManager, "dialog")
                            }
                        }
                    ),

                    )
            )
            binding.settingsRecyclerView.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }
    }
}