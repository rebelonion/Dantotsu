package ani.dantotsu.media.manga.mangareader

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.absoluteValue

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

    enum class ScrollPosition {
        None,
        Start,
        End,
        Both
    }

    private var scrollPos = ScrollPosition.None

    private fun setScrollPosition() = child?.run {
        val (top, bottom) = if (vertical)
            !canScrollVertically(-1) to !canScrollVertically(1)
        else
            !canScrollHorizontally(-1) to !canScrollHorizontally(1)

        scrollPos = when {
            top && !bottom -> ScrollPosition.Start
            !top && bottom -> ScrollPosition.End
            top && bottom -> ScrollPosition.Both
            else -> ScrollPosition.None
        }
    }


    private fun canChildScroll(): Boolean {
        setScrollPosition()
        return scrollPos == ScrollPosition.None
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
                if (pointerIndex < 0) return false

                val pos = if (vertical) ev.getY(pointerIndex) else ev.getX(pointerIndex)
                startDragging(pos)

                if (!isBeingDragged) return false

                val overscroll = getDiff(pos) * DRAG_RATE
                if (overscroll.absoluteValue <= 0) return false

                parent.requestDisallowInterceptTouchEvent(true)

                if (vertical) {
                    val dragDistance =
                        Resources.getSystem().displayMetrics.heightPixels / dragDivider
                    performSwiping(overscroll, dragDistance, topBeingSwiped, bottomBeingSwiped)
                } else {
                    val dragDistance =
                        Resources.getSystem().displayMetrics.widthPixels / dragDivider
                    performSwiping(overscroll, dragDistance, leftBeingSwiped, rightBeingSwiped)
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
                    topBeingSwiped(0f)
                    bottomBeingSwiped(0f)
                } else {
                    rightBeingSwiped(0f)
                    leftBeingSwiped(0f)
                }
                pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex < 0) {
                    //("Got ACTION_UP event but don't have an active pointer id.")
                    return false
                }
                if (isBeingDragged) {
                    val pos = if (vertical) ev.getY(pointerIndex) else ev.getX(pointerIndex)
                    val overscroll = getDiff(pos) * DRAG_RATE
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

    private fun getDiff(pos: Float) = when (scrollPos) {
        ScrollPosition.None -> 0f
        ScrollPosition.Start, ScrollPosition.Both -> pos - initialMotion
        ScrollPosition.End -> initialMotion - pos
    }

    private fun startDragging(pos: Float) {
        val posDiff = getDiff(pos).absoluteValue
        if (posDiff > touchSlop && !isBeingDragged) {
            initialMotion = initialDown + touchSlop
            isBeingDragged = true
        }
    }

    private fun performSwiping(
        overscrollDistance: Float,
        totalDragDistance: Int,
        startBlock: (Float) -> Unit,
        endBlock: (Float) -> Unit
    ) {
        val distance = overscrollDistance * 2 / totalDragDistance
        when (scrollPos) {
            ScrollPosition.Start -> startBlock(distance)
            ScrollPosition.End -> endBlock(distance)
            ScrollPosition.Both -> {
                startBlock(distance)
                endBlock(-distance)
            }
            else -> {}
        }
    }

    private fun performSwipe(
        overscrollDistance: Float,
        totalDragDistance: Int,
        startBlock: () -> Unit,
        endBlock: () -> Unit
    ) {
        fun check(distance: Float, block: () -> Unit) {
            if (distance * 2 > totalDragDistance)
                block.invoke()
        }
        when (scrollPos) {
            ScrollPosition.Start -> check(overscrollDistance) { startBlock() }
            ScrollPosition.End -> check(overscrollDistance) { endBlock() }
            ScrollPosition.Both -> {
                check(overscrollDistance) { startBlock() }
                check(-overscrollDistance) { endBlock() }
            }

            else -> {}
        }
    }

    private fun finishSpinner(overscrollDistance: Float) {
        if (vertical) {
            val totalDragDistance = Resources.getSystem().displayMetrics.heightPixels / dragDivider
            performSwipe(overscrollDistance, totalDragDistance, onTopSwiped, onBottomSwiped)
        } else {
            val totalDragDistance = Resources.getSystem().displayMetrics.widthPixels / dragDivider
            performSwipe(overscrollDistance, totalDragDistance, onLeftSwiped, onRightSwiped)
        }
    }
}