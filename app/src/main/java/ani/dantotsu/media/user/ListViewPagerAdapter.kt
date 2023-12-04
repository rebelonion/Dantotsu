package ani.dantotsu.media.user

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ListViewPagerAdapter(
    private val size: Int,
    private val calendar: Boolean,
    fragment: FragmentActivity
) :
    FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = size
    override fun createFragment(position: Int): Fragment =
        ListFragment.newInstance(position, calendar)
}