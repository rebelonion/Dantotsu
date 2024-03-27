package ani.dantotsu.util

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

class BitmapUtil {
    companion object {
        fun roundCorners(bitmap: Bitmap, cornerRadius: Float = 20f): Bitmap {
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint()
            paint.isAntiAlias = true
            paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

            return output
        }

        fun convertDrawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
    }
}