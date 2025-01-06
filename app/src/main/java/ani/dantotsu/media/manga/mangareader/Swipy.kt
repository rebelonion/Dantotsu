package ani.dantotsu.media.manga.mangareader

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

class Swipy @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var dragDivider: Int = 5
    var vertical = true

    var child: View? = getChildAt(0)

    var topBeingSwiped: ((Float) -> Unit) = {}
    var onTopSwiped: (() -> Unit) = {}
    var onBottomSwiped: (() -> Unit) = {}
    var bottomBeingSwiped: ((Float) -> Unit) = {}
    var onLeftSwiped: (() -> Unit) = {}
    var leftBeingSwiped: ((Float) -> Unit) = {}
    var onRightSwiped: (() -> Unit) = {}
    var rightBeingSwiped: ((Float) -> Unit) = {}

    companion object {
        private const val DRAG_RATE = 0.5f
        private const val INVALID_POINTER = -1
    }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var activePointerId = INVALID_POINTER
    private var isBeingDragged = false
    private var initialDown = 0f
    private var initialMotion = 0f

    private enum class VerticalPosition { Top, None, Bottom }
    private enum class HorizontalPosition { Left, None, Right }

    private var horizontalPos = HorizontalPosition.None
    private var verticalPos = VerticalPosition.None

    private fun setChildPosition() {
        child?.let {
            if (vertical) {
                verticalPos = when {
                    !it.canScrollVertically(1) && !it.canScrollVertically(-1) -> {
                        if (initialDown > (Resources.getSystem().displayMetrics.heightPixels / 2))
                            VerticalPosition.Bottom
                        else
                            VerticalPosition.Top
                    }

                    !it.canScrollVertically(1) -> VerticalPosition.Bottom
                    !it.canScrollVertically(-1) -> VerticalPosition.Top
                    else -> VerticalPosition.None
                }
            } else {
                horizontalPos = when {
                    !it.canScrollHorizontally(1) && !it.canScrollHorizontally(-1) -> {
                        if (initialDown > (Resources.getSystem().displayMetrics.widthPixels / 2))
                            HorizontalPosition.Right
                        else
                            HorizontalPosition.Left
                    }

                    !it.canScrollHorizontally(1) -> HorizontalPosition.Right
                    !it.canScrollHorizontally(-1) -> HorizontalPosition.Left
                    else -> HorizontalPosition.None
                }
            }
        }
    }

    private fun canChildScroll(): Boolean {
        setChildPosition()
        return if (vertical) verticalPos == VerticalPosition.None
        else horizontalPos == HorizontalPosition.None
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = ev.actionIndex
        if (ev.getPointerId(pointerIndex) == activePointerId) {
            activePointerId = ev.getPointerId(if (pointerIndex == 0) 1 else 0)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled || canChildScroll()) return false

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                initialDown = if (vertical) ev.getY(0) else ev.getX(0)
                isBeingDragged = false
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex >= 0) {
                    startDragging(if (vertical) ev.getY(pointerIndex) else ev.getX(pointerIndex))
                }
            }

            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isBeingDragged = false
                activePointerId = INVALID_POINTER
            }
        }
        return isBeingDragged
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled || canChildScroll()) return false

        val pointerIndex: Int
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                isBeingDragged = false
            }

            MotionEvent.ACTION_MOVE -> {
                pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex >= 0) {
                    val pos = if (vertical) ev.getY(pointerIndex) else ev.getX(pointerIndex)
                    startDragging(pos)
                    if (isBeingDragged) handleDrag(pos)
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                pointerIndex = ev.actionIndex
                if (pointerIndex >= 0) activePointerId = ev.getPointerId(pointerIndex)
            }

            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
            MotionEvent.ACTION_UP -> {
                resetSwipes()
                pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex >= 0) finishSpinner(
                    if (vertical) ev.getY(pointerIndex) else ev.getX(
                        pointerIndex
                    )
                )
                activePointerId = INVALID_POINTER
                return false
            }

            MotionEvent.ACTION_CANCEL -> return false
        }
        return true
    }

    private fun startDragging(pos: Float) {
        val posDiff =
            if ((vertical && verticalPos == VerticalPosition.Top) || (!vertical && horizontalPos == HorizontalPosition.Left))
                pos - initialDown
            else
                initialDown - pos
        if (posDiff > touchSlop && !isBeingDragged) {
            initialMotion = initialDown + touchSlop
            isBeingDragged = true
        }
    }

    private fun handleDrag(pos: Float) {
        val overscroll = abs((pos - initialMotion) * DRAG_RATE)
        parent.requestDisallowInterceptTouchEvent(true)
        if (vertical) {
            val totalDragDistance = Resources.getSystem().displayMetrics.heightPixels / dragDivider
            if (verticalPos == VerticalPosition.Top)
                topBeingSwiped.invoke(overscroll * 2 / totalDragDistance)
            else
                bottomBeingSwiped.invoke(overscroll * 2 / totalDragDistance)
        } else {
            val totalDragDistance = Resources.getSystem().displayMetrics.widthPixels / dragDivider
            if (horizontalPos == HorizontalPosition.Left)
                leftBeingSwiped.invoke(overscroll / totalDragDistance)
            else
                rightBeingSwiped.invoke(overscroll / totalDragDistance)
        }
    }

    private fun resetSwipes() {
        if (vertical) {
            topBeingSwiped.invoke(0f)
            bottomBeingSwiped.invoke(0f)
        } else {
            rightBeingSwiped.invoke(0f)
            leftBeingSwiped.invoke(0f)
        }
    }

    private fun finishSpinner(overscrollDistance: Float) {
        if (vertical) {
            val totalDragDistance = Resources.getSystem().displayMetrics.heightPixels / dragDivider
            val swipeDistance = abs(overscrollDistance - initialMotion)
            if (swipeDistance > totalDragDistance) {
                if (verticalPos == VerticalPosition.Top)
                    onTopSwiped.invoke()
                else
                    onBottomSwiped.invoke()
            }
        } else {
            val totalDragDistance = Resources.getSystem().displayMetrics.widthPixels / dragDivider
            val swipeDistance = abs(overscrollDistance - initialMotion)
            if (swipeDistance > totalDragDistance) {
                if (horizontalPos == HorizontalPosition.Left)
                    onLeftSwiped.invoke()
                else
                    onRightSwiped.invoke()
            }
        }
    }
}
