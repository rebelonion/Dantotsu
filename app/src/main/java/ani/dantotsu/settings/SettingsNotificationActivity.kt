package ani.dantotsu.settings

import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.api.NotificationType
import ani.dantotsu.databinding.ActivitySettingsNotificationsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.notifications.TaskScheduler
import ani.dantotsu.notifications.anilist.AnilistNotificationWorker
import ani.dantotsu.notifications.comment.CommentNotificationWorker
import ani.dantotsu.notifications.subscription.SubscriptionNotificationWorker
import ani.dantotsu.openSettings
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager

class SettingsNotificationActivity: AppCompatActivity(){
    private lateinit var binding: ActivitySettingsNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        val context = this
        binding = ActivitySettingsNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            var curTime = PrefManager.getVal<Int>(PrefName.SubscriptionNotificationInterval)
            val timeNames = SubscriptionNotificationWorker.checkIntervals.map {
                val mins = it % 60
                val hours = it / 60
                if (it > 0) "${if (hours > 0) "$hours hrs " else ""}${if (mins > 0) "$mins mins" else ""}"
                else getString(R.string.do_not_update)
            }.toTypedArray()
            settingsNotificationsLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navBarHeight
            }
            notificationSettingsBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            settingsSubscriptionsTime.text =
                getString(R.string.subscriptions_checking_time_s, timeNames[curTime])
            val speedDialog = AlertDialog.Builder(context, R.style.MyPopup)
                .setTitle(R.string.subscriptions_checking_time)
            settingsSubscriptionsTime.setOnClickListener {
                val dialog = speedDialog.setSingleChoiceItems(timeNames, curTime) { dialog, i ->
                    curTime = i
                    settingsSubscriptionsTime.text =
                        getString(R.string.subscriptions_checking_time_s, timeNames[i])
                    PrefManager.setVal(PrefName.SubscriptionNotificationInterval, curTime)
                    dialog.dismiss()
                    TaskScheduler.create(
                        context, PrefManager.getVal(PrefName.UseAlarmManager)
                    ).scheduleAllTasks(context)
                }.show()
                dialog.window?.setDimAmount(0.8f)
            }

            settingsSubscriptionsTime.setOnLongClickListener {
                TaskScheduler.create(
                    context, PrefManager.getVal(PrefName.UseAlarmManager)
                ).scheduleAllTasks(context)
                true
            }

            val aTimeNames = AnilistNotificationWorker.checkIntervals.map { it.toInt() }
            val aItems = aTimeNames.map {
                val mins = it % 60
                val hours = it / 60
                if (it > 0) "${if (hours > 0) "$hours hrs " else ""}${if (mins > 0) "$mins mins" else ""}"
                else getString(R.string.do_not_update)
            }
            settingsAnilistSubscriptionsTime.text = getString(
                R.string.anilist_notifications_checking_time,
                aItems[PrefManager.getVal(PrefName.AnilistNotificationInterval)]
            )
            settingsAnilistSubscriptionsTime.setOnClickListener {

                val selected = PrefManager.getVal<Int>(PrefName.AnilistNotificationInterval)
                val dialog = AlertDialog.Builder(context, R.style.MyPopup)
                    .setTitle(R.string.subscriptions_checking_time)
                    .setSingleChoiceItems(aItems.toTypedArray(), selected) { dialog, i ->
                        PrefManager.setVal(PrefName.AnilistNotificationInterval, i)
                        settingsAnilistSubscriptionsTime.text =
                            getString(R.string.anilist_notifications_checking_time, aItems[i])
                        dialog.dismiss()
                        TaskScheduler.create(
                            context, PrefManager.getVal(PrefName.UseAlarmManager)
                        ).scheduleAllTasks(context)
                    }.create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            settingsAnilistNotifications.setOnClickListener {
                val types = NotificationType.entries.map { it.name }
                val filteredTypes =
                    PrefManager.getVal<Set<String>>(PrefName.AnilistFilteredTypes).toMutableSet()
                val selected = types.map { filteredTypes.contains(it) }.toBooleanArray()
                val dialog = AlertDialog.Builder(context, R.style.MyPopup)
                    .setTitle(R.string.anilist_notification_filters)
                    .setMultiChoiceItems(types.toTypedArray(), selected) { _, which, isChecked ->
                        val type = types[which]
                        if (isChecked) {
                            filteredTypes.add(type)
                        } else {
                            filteredTypes.remove(type)
                        }
                        PrefManager.setVal(PrefName.AnilistFilteredTypes, filteredTypes)
                    }.create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            val cTimeNames = CommentNotificationWorker.checkIntervals.map { it.toInt() }
            val cItems = cTimeNames.map {
                val mins = it % 60
                val hours = it / 60
                if (it > 0) "${if (hours > 0) "$hours hrs " else ""}${if (mins > 0) "$mins mins" else ""}"
                else getString(R.string.do_not_update)
            }

            settingsCommentSubscriptionsTime.text = getString(
                R.string.comment_notification_checking_time,
                cItems[PrefManager.getVal(PrefName.CommentNotificationInterval)]
            )
            settingsCommentSubscriptionsTime.setOnClickListener {
                val selected = PrefManager.getVal<Int>(PrefName.CommentNotificationInterval)
                val dialog = AlertDialog.Builder(context, R.style.MyPopup)
                    .setTitle(R.string.subscriptions_checking_time)
                    .setSingleChoiceItems(cItems.toTypedArray(), selected) { dialog, i ->
                        PrefManager.setVal(PrefName.CommentNotificationInterval, i)
                        settingsCommentSubscriptionsTime.text =
                            getString(R.string.comment_notification_checking_time, cItems[i])
                        dialog.dismiss()
                        TaskScheduler.create(
                            context, PrefManager.getVal(PrefName.UseAlarmManager)
                        ).scheduleAllTasks(context)
                    }.create()
                dialog.window?.setDimAmount(0.8f)
                dialog.show()
            }

            settingsNotificationsCheckingSubscriptions.isChecked =
                PrefManager.getVal(PrefName.SubscriptionCheckingNotifications)
            settingsNotificationsCheckingSubscriptions.setOnCheckedChangeListener { _, isChecked ->
                PrefManager.setVal(PrefName.SubscriptionCheckingNotifications, isChecked)
            }

            settingsNotificationsCheckingSubscriptions.setOnLongClickListener {
                openSettings(context, null)
            }

            settingsNotificationsUseAlarmManager.isChecked =
                PrefManager.getVal(PrefName.UseAlarmManager)

            settingsNotificationsUseAlarmManager.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    val alertDialog = AlertDialog.Builder(context, R.style.MyPopup)
                        .setTitle(R.string.use_alarm_manager)
                        .setMessage(R.string.use_alarm_manager_confirm)
                        .setPositiveButton(R.string.use) { dialog, _ ->
                            PrefManager.setVal(PrefName.UseAlarmManager, true)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (!(getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()) {
                                    val intent =
                                        Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM")
                                    startActivity(intent)
                                    settingsNotificationsCheckingSubscriptions.isChecked = true
                                }
                            }
                            dialog.dismiss()
                        }.setNegativeButton(R.string.cancel) { dialog, _ ->
                            settingsNotificationsCheckingSubscriptions.isChecked = false
                            PrefManager.setVal(PrefName.UseAlarmManager, false)
                            dialog.dismiss()
                        }.create()
                    alertDialog.window?.setDimAmount(0.8f)
                    alertDialog.show()
                } else {
                    PrefManager.setVal(PrefName.UseAlarmManager, false)
                    TaskScheduler.create(context, true).cancelAllTasks()
                    TaskScheduler.create(context, false)
                        .scheduleAllTasks(context)
                }
            }
        }
    }
}