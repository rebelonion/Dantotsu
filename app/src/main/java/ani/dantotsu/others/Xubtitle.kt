package ani.dantotsu.others

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class Xubtitle
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {
    private var outlineThickness: Float = 0f
    private var effectColor: Int = currentTextColor
    private var currentEffect: Effect = Effect.NONE

    private val shadowPaint = Paint().apply { isAntiAlias = true }
    private val outlinePaint = Paint().apply { isAntiAlias = true }
    private var shineShader: Shader? = null

    enum class Effect {
        NONE,
        OUTLINE,
        SHINE,
        DROP_SHADOW,
    }

    override fun onDraw(canvas: Canvas) {
        val text = text.toString()
        val textPaint =
            TextPaint(paint).apply {
                color = currentTextColor
            }
        val staticLayout =
            StaticLayout.Builder
                .obtain(text, 0, text.length, textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1f)
                .build()

        when (currentEffect) {
            Effect.OUTLINE -> {
                textPaint.style = Paint.Style.STROKE
                textPaint.strokeWidth = outlineThickness
                textPaint.color = effectColor

                staticLayout.draw(canvas)

                textPaint.style = Paint.Style.FILL
                textPaint.color = currentTextColor
                staticLayout.draw(canvas)
            }

            Effect.DROP_SHADOW -> {
                setLayerType(LAYER_TYPE_SOFTWARE, null)
                textPaint.setShadowLayer(outlineThickness, 4f, 4f, effectColor)

                staticLayout.draw(canvas)

                textPaint.clearShadowLayer()
            }

            Effect.SHINE -> {
                val shadowShader =
                    LinearGradient(
                        0f,
                        0f,
                        width.toFloat(),
                        height.toFloat(),
                        intArrayOf(Color.WHITE, effectColor, Color.BLACK),
                        null,
                        Shader.TileMode.CLAMP,
                    )

                val shadowPaint =
                    Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.FILL
                        textSize = textPaint.textSize
                        typeface = textPaint.typeface
                        shader = shadowShader
                    }

                canvas.drawText(
                    text,
                    x + 4f, // Shadow offset
                    y + 4f,
                    shadowPaint,
                )

                val shader =
                    LinearGradient(
                        0f,
                        0f,
                        width.toFloat(),
                        height.toFloat(),
                        intArrayOf(effectColor, Color.WHITE, Color.WHITE),
                        null,
                        Shader.TileMode.CLAMP,
                    )
                textPaint.shader = shader
                staticLayout.draw(canvas)
                textPaint.shader = null
            }

            Effect.NONE -> {
                staticLayout.draw(canvas)
            }
        }
    }

    fun applyOutline(
        color: Int,
        outlineThickness: Float,
    ) {
        this.effectColor = color
        this.outlineThickness = outlineThickness
        currentEffect = Effect.OUTLINE
    }

    // Too hard for me to figure it out
    fun applyShineEffect(color: Int) {
        this.effectColor = color
        currentEffect = Effect.SHINE
    }

    fun applyDropShadow(
        color: Int,
        outlineThickness: Float,
    ) {
        this.effectColor = color
        this.outlineThickness = outlineThickness
        currentEffect = Effect.DROP_SHADOW
    }
}