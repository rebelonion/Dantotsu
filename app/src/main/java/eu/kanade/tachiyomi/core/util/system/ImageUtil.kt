package tachiyomi.core.util.system

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageUtil {


    enum class ImageType(val mime: String, val extension: String) {
        AVIF("image/avif", "avif"),
        GIF("image/gif", "gif"),
        HEIF("image/heif", "heif"),
        JPEG("image/jpeg", "jpg"),
        JXL("image/jxl", "jxl"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp"),
    }

    /**
     * Extract the 'side' part from imageStream and return it as InputStream.
     */
    fun splitInHalf(imageStream: InputStream, side: Side): InputStream {
        val imageBytes = imageStream.readBytes()

        val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val height = imageBitmap.height
        val width = imageBitmap.width

        val singlePage = Rect(0, 0, width / 2, height)

        val half = createBitmap(width / 2, height)
        val part = when (side) {
            Side.RIGHT -> Rect(width - width / 2, 0, width, height)
            Side.LEFT -> Rect(0, 0, width / 2, height)
        }
        half.applyCanvas {
            drawBitmap(imageBitmap, part, singlePage, null)
        }
        val output = ByteArrayOutputStream()
        half.compress(Bitmap.CompressFormat.JPEG, 100, output)

        return ByteArrayInputStream(output.toByteArray())
    }

    fun rotateImage(imageStream: InputStream, degrees: Float): InputStream {
        val imageBytes = imageStream.readBytes()

        val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val rotated = rotateBitMap(imageBitmap, degrees)

        val output = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, 100, output)

        return ByteArrayInputStream(output.toByteArray())
    }

    private fun rotateBitMap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Split the image into left and right parts, then merge them into a new image.
     */
    fun splitAndMerge(imageStream: InputStream, upperSide: Side): InputStream {
        val imageBytes = imageStream.readBytes()

        val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val height = imageBitmap.height
        val width = imageBitmap.width

        val result = createBitmap(width / 2, height * 2)
        result.applyCanvas {
            // right -> upper
            val rightPart = when (upperSide) {
                Side.RIGHT -> Rect(width - width / 2, 0, width, height)
                Side.LEFT -> Rect(0, 0, width / 2, height)
            }
            val upperPart = Rect(0, 0, width / 2, height)
            drawBitmap(imageBitmap, rightPart, upperPart, null)
            // left -> bottom
            val leftPart = when (upperSide) {
                Side.LEFT -> Rect(width - width / 2, 0, width, height)
                Side.RIGHT -> Rect(0, 0, width / 2, height)
            }
            val bottomPart = Rect(0, height, width / 2, height * 2)
            drawBitmap(imageBitmap, leftPart, bottomPart, null)
        }

        val output = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.JPEG, 100, output)
        return ByteArrayInputStream(output.toByteArray())
    }

    enum class Side {
        RIGHT,
        LEFT,
    }

}

val getDisplayMaxHeightInPx: Int
    get() = Resources.getSystem().displayMetrics.let { max(it.heightPixels, it.widthPixels) }
