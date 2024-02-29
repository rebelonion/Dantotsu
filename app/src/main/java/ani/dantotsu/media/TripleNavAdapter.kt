package ani.dantotsu.media

import android.graphics.Color
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import ani.dantotsu.R
import ani.dantotsu.navBarHeight
import nl.joery.animatedbottombar.AnimatedBottomBar

class TripleNavAdapter(
    private val nav1: AnimatedBottomBar,
    private val nav2: AnimatedBottomBar,
    private val nav3: AnimatedBottomBar,
    anime: Boolean,
    format: String,
    private val isScreenVertical: Boolean = false
) {
    var selected: Int = 0
    var selectionListener: ((Int, Int) -> Unit)? = null
    init {
        nav1.tabs.clear()
        nav2.tabs.clear()
        nav3.tabs.clear()
        val infoTab = nav1.createTab(R.drawable.ic_round_info_24, R.string.info, R.id.info)
        val  watchTab = if (anime) {
            nav2.createTab(R.drawable.ic_round_movie_filter_24, R.string.watch, R.id.watch)
        } else if (format == "NOVEL") {
            nav2.createTab(R.drawable.ic_round_book_24, R.string.read, R.id.read)
        } else {
            nav2.createTab(R.drawable.ic_round_import_contacts_24, R.string.read, R.id.read)
        }
        val commentTab = nav3.createTab(R.drawable.ic_round_comment_24, R.string.comments, R.id.comment)
        nav1.addTab(infoTab)
        nav1.visibility = ViewGroup.VISIBLE
        if (isScreenVertical) {
            nav2.visibility = ViewGroup.GONE
            nav3.visibility = ViewGroup.GONE
            nav1.addTab(watchTab)
            nav1.addTab(commentTab)
            nav1.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBarHeight
            }
            nav2.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBarHeight
            }
            nav3.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBarHeight
            }
        } else {
            nav1.indicatorColor = Color.TRANSPARENT
            nav2.indicatorColor = Color.TRANSPARENT
            nav3.indicatorColor = Color.TRANSPARENT
            nav2.visibility = ViewGroup.VISIBLE
            nav3.visibility = ViewGroup.VISIBLE
            nav2.addTab(watchTab)
            nav3.addTab(commentTab)
            nav2.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
                override fun onTabSelected(
                    lastIndex: Int,
                    lastTab: AnimatedBottomBar.Tab?,
                    newIndex: Int,
                    newTab: AnimatedBottomBar.Tab
                ) {
                    selected = 1
                    deselectOthers(selected)
                    selectionListener?.invoke(selected, newTab.id)
                }
            })
            nav3.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
                override fun onTabSelected(
                    lastIndex: Int,
                    lastTab: AnimatedBottomBar.Tab?,
                    newIndex: Int,
                    newTab: AnimatedBottomBar.Tab
                ) {
                    selected = 2
                    deselectOthers(selected)
                    selectionListener?.invoke(selected, newTab.id)
                }
            })
        }
        nav1.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ) {
                if (!isScreenVertical) {
                    selected = 0
                    deselectOthers(selected)
                } else selected = newIndex
                selectionListener?.invoke(selected, newTab.id)
            }
        })
    }

    private fun deselectOthers(selected: Int) {
        if (selected == 0) {
            nav2.clearSelection()
            nav3.clearSelection()
        }
        if (selected == 1) {
            nav1.clearSelection()
            nav3.clearSelection()
        }
        if (selected == 2) {
            nav1.clearSelection()
            nav2.clearSelection()
        }
    }

    fun selectTab(tab: Int) {
        selected = tab
        if (!isScreenVertical) {
            when (tab) {
                0 -> nav1.selectTabAt(0)
                1 -> nav2.selectTabAt(0)
                2 -> nav3.selectTabAt(0)
            }
            deselectOthers(selected)
        } else {
            nav1.selectTabAt(selected)
        }
    }

    fun setVisibility(visibility: Int) {
        if (isScreenVertical) {
            nav1.visibility = visibility
            return
        }
        nav1.visibility = visibility
        nav2.visibility = visibility
        nav3.visibility = visibility
    }
}