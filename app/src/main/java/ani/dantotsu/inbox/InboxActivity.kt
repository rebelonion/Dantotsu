package ani.dantotsu.inbox

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.databinding.ActivityInboxBinding
import ani.dantotsu.R
import ani.dantotsu.navBarHeight
import nl.joery.animatedbottombar.AnimatedBottomBar

class InboxActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInboxBinding
    private lateinit var navBar: AnimatedBottomBar
    private var selected: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInboxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Find the ImageButton from activity_inbox.xml
        val imageButton = findViewById<ImageButton>(R.id.imageButton)

        navBar = binding.inboxNavBar
        navBar.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = navBarHeight }

        val activityTab = navBar.createTab(R.drawable.inbox_filled, "Activity")
        val notificationTab =
            navBar.createTab(R.drawable.ic_round_notifications_active_24, "Notification")
        navBar.addTab(activityTab)
        navBar.addTab(notificationTab)

        navBar.visibility = View.GONE

        navBar.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ) {
                selected = newIndex

                val fragmentTransaction = supportFragmentManager.beginTransaction()
                when (newIndex) {
                    0 -> fragmentTransaction.replace(R.id.container, FeedFragment())
                    1 -> fragmentTransaction.replace(R.id.container, NotifsFragment())
                }
                fragmentTransaction.commit()
            }
        })

        navBar.selectTabAt(selected)
        navBar.visibility = View.VISIBLE
        }
    }
