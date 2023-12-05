package ani.dantotsu.media.manga.mangareader

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout

class Swipy @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var dragDivider: Int = 5
    var vertical = true

    //public, in case a different sub child needs to be considered
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
        private const val DRAG_RATE = .5f
        private const val INVALID_POINTER = -1
    }

    private var touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var activePointerId = INVALID_POINTER
    private var isBeingDragged = false
    private var initialDown = 0f
    private var initialMotion = 0f

    enum class VerticalPosition {
        Top,
        None,
        Bottom
    }

    enum class HorizontalPosition {
        Left,
        None,
        Right
    }

    private var horizontalPos = HorizontalPosition.None
    private var verticalPos = VerticalPosition.None

    private fun setChildPosition() {
        child?.apply {
            if (vertical) {
                verticalPos = VerticalPosition.None
                if (!canScrollVertically(1)) {
                    verticalPos = VerticalPosition.Bottom
                }
                if (!canScrollVertically(-1)) {
                    verticalPos = VerticalPosition.Top
                }
            } else {
                horizontalPos = HorizontalPosition.None
                if (!canScrollHorizontally(1)) {
                    horizontalPos = HorizontalPosition.Right
                }
                if (!canScrollHorizontally(-1)) {
                    horizontalPos = HorizontalPosition.Left
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
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == activePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            activePointerId = ev.getPointerId(newPointerIndex)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        val pointerIndex: Int
        if (!isEnabled || canChildScroll()) {
            return false
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                isBeingDragged = false
                pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex < 0) {
                    return false
                }
                initialDown = if (vertical) ev.getY(pointerIndex) else ev.getX(pointerIndex)
            }

            MotionEvent.ACTION_MOVE -> {
                if (activePointerId == INVALID_POINTER) {
                    //("Got ACTION_MOVE event but don't have an active pointer id.")
                    return false
                }
                pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex < 0) {
                    return false
                }
                val pos = if (vertical) ev.getY(pointerIndex) else ev.getX(pointerIndex)
                startDragging(pos)
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
        val action = ev.actionMasked
        val pointerIndex: Int
        if (!isEnabled || canChildScroll()) {
            return false
        }
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                isBeingDragged = false
            }

            MotionEvent.ACTION_MOVE -> {
                pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex < 0) {
                    //("Got ACTION_MOVE event but have an invalid active pointer id.")
                    return false
                }
                val pos = if (vertical) ev.getY(pointerIndex) else ev.getX(pointerIndex)
                startDragging(pos)
                if (isBeingDragged) {
                    val overscroll = (
                            if (vertical)
                                if (verticalPos == VerticalPosition.Top) pos - initialMotion else initialMotion - pos
                            else
                                if (horizontalPos == HorizontalPosition.Left) pos - initialMotion else initialMotion - pos
                            ) * DRAG_RATE

                    if (overscroll > 0) {
                        parent.requestDisallowInterceptTouchEvent(true)
                        if (vertical) {
                            val totalDragDistance =
                                Resources.getSystem().displayMetrics.heightPixels / dragDivider
                            if (verticalPos == VerticalPosition.Top)
                                topBeingSwiped.invoke(overscroll * 2 / totalDragDistance)
                            else
                                bottomBeingSwiped.invoke(overscroll * 2 / totalDragDistance)
                        } else {
                            val totalDragDistance =
                                Resources.getSystem().displayMetrics.widthPixels / dragDivider
                            if (horizontalPos == HorizontalPosition.Left)
                                leftBeingSwiped.invoke(overscroll / totalDragDistance)
                            else
                                rightBeingSwiped.invoke(overscroll / totalDragDistance)
                        }
                    } else {
                        return false
                    }
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                pointerIndex = ev.actionIndex
                if (pointerIndex < 0) {
                    //("Got ACTION_POINTER_DOWN event but have an invalid action index.")
                    return false
                }
                activePointerId = ev.getPointerId(pointerIndex)
            }

            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
            MotionEvent.ACTION_UP -> {
                if (vertical) {
                    topBeingSwiped.invoke(0f)
                    bottomBeingSwiped.invoke(0f)
                } else {
                    rightBeingSwiped.invoke(0f)
                    leftBeingSwiped.invoke(0f)
                }
                pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex < 0) {
                    //("Got ACTION_UP event but don't have an active pointer id.")
                    return false
                }
                if (isBeingDragged) {
                    val pos = if (vertical) ev.getY(pointerIndex) else ev.getX(pointerIndex)
                    val overscroll = (
                            if (vertical)
                                if (verticalPos == VerticalPosition.Top) pos - initialMotion else initialMotion - pos
                            else
                                if (horizontalPos == HorizontalPosition.Left) pos - initialMotion else initialMotion - pos
                            ) * DRAG_RATE
                    isBeingDragged = false
                    finishSpinner(overscroll)
                }
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

    private fun finishSpinner(overscrollDistance: Float) {

        if (vertical) {
            val totalDragDistance = Resources.getSystem().displayMetrics.heightPixels / dragDivider
            if (overscrollDistance * 2 > totalDragDistance)
                if (verticalPos == VerticalPosition.Top)
                    onTopSwiped.invoke()
                else
                    onBottomSwiped.invoke()
        } else {
            val totalDragDistance = Resources.getSystem().displayMetrics.widthPixels / dragDivider
            if (overscrollDistance > totalDragDistance)
                if (horizontalPos == HorizontalPosition.Left)
                    onLeftSwiped.invoke()
                else
                    onRightSwiped.invoke()
        }
    }
}