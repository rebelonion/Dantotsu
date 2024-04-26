package ani.dantotsu.home.status

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class CircleView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var parts: Int = 3
    private var gapAngle: Float = 9f
    private val path = Path()
    private var booleanList = listOf<Boolean>()
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = centerX.coerceAtMost(centerY) - paint.strokeWidth / 2

        val totalGapAngle = gapAngle * (parts)
        val totalAngle = 360f - totalGapAngle
        val typedValue = TypedValue()
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        fun setColor(int: Int) {
            if (int < booleanList.size && booleanList[int]) {

                paint.color = Color.GRAY
            } else {

                paint.color = primaryColor
            }
            canvas.drawPath(path, paint)
        }

        if (parts == 1) {
            // Draw a single arc covering the entire circle
            path.addArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                0f,
                360f
            )
            setColor(0)
        } else {
            val effectiveAngle = totalAngle / parts

            for (i in 0 until parts) {
                val startAngle = i * (effectiveAngle + gapAngle)
                path.reset()
                path.addArc(
                    centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius,
                    startAngle,
                    effectiveAngle
                )
                setColor(i)
            }
        }

    }

    fun setParts(parts: Int, list : List<Boolean> = mutableListOf())  {
        this.parts = parts
        this.booleanList = list
        invalidate()
    }
}