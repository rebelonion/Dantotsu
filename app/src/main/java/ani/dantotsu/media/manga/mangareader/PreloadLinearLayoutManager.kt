package ani.dantotsu.media.manga.mangareader

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max


class PreloadLinearLayoutManager(context: Context, orientation: Int, reverseLayout: Boolean) :
    LinearLayoutManager(context, orientation, reverseLayout) {
    private val mOrientationHelper: OrientationHelper =
        OrientationHelper.createOrientationHelper(this, orientation)

    /**
     * As [LinearLayoutManager.collectAdjacentPrefetchPositions] will prefetch one view for us,
     * we only need to prefetch additional ones.
     */
    var preloadItemCount = 1
        set(count) {
            require(count >= 1) { "preloadItemCount must not be smaller than 1!" }
            field = count - 1
        }

    override fun collectAdjacentPrefetchPositions(
        dx: Int, dy: Int, state: RecyclerView.State,
        layoutPrefetchRegistry: LayoutPrefetchRegistry
    ) {
        super.collectAdjacentPrefetchPositions(dx, dy, state, layoutPrefetchRegistry)

        val delta = if (orientation == HORIZONTAL) dx else dy
        if (childCount == 0 || delta == 0) {
            return
        }
        val layoutDirection = if (delta > 0) 1 else -1
        val child = getChildClosest(layoutDirection)
        val currentPosition: Int = getPosition(child ?: return) + layoutDirection

        if (layoutDirection == 1) {
            val scrollingOffset =
                (mOrientationHelper.getDecoratedEnd(child) - mOrientationHelper.endAfterPadding)
            ((currentPosition + 1) until (currentPosition + preloadItemCount + 1)).forEach {
                if (it >= 0 && it < state.itemCount) {
                    layoutPrefetchRegistry.addPosition(it, max(0, scrollingOffset))
                }
            }
        }
    }

    private fun getChildClosest(layoutDirection: Int): View? {
        return getChildAt(if (layoutDirection == -1) 0 else childCount - 1)
    }
}