package ani.dantotsu.media.manga.mangareader

import android.graphics.Bitmap
import android.view.View
import ani.dantotsu.media.manga.MangaChapter
import ani.dantotsu.settings.CurrentReaderSettings.Directions.LEFT_TO_RIGHT
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation

class DualPageAdapter(
    activity: MangaReaderActivity,
    val chapter: MangaChapter
) : ImageAdapter(activity, chapter) {

    private val pages = chapter.dualPages()

    override suspend fun loadBitmap(position: Int, parent: View): Bitmap? {
        val img1 = pages[position].first
        val link1 = img1.url
        if (link1.url.isEmpty()) return null

        val img2 = pages[position].second
        val link2 = img2?.url
        if (link2?.url?.isEmpty() == true) return null

        val transforms1 = mutableListOf<BitmapTransformation>()
        val parserTransformation1 = activity.getTransformation(img1)
        if (parserTransformation1 != null) transforms1.add(parserTransformation1)
        val transforms2 = mutableListOf<BitmapTransformation>()
        if (img2 != null) {
            val parserTransformation2 = activity.getTransformation(img2)
            if (parserTransformation2 != null) transforms2.add(parserTransformation2)
        }

        if (settings.cropBorders) {
            transforms1.add(RemoveBordersTransformation(true, settings.cropBorderThreshold))
            transforms1.add(RemoveBordersTransformation(false, settings.cropBorderThreshold))
            if (img2 != null) {
                transforms2.add(RemoveBordersTransformation(true, settings.cropBorderThreshold))
                transforms2.add(RemoveBordersTransformation(false, settings.cropBorderThreshold))
            }
        }

        val bitmap1 = activity.loadBitmap(link1, transforms1) ?: return null
        val bitmap2 = link2?.let { activity.loadBitmap(it, transforms2) ?: return null }

        return if (bitmap2 != null) {
            if (settings.direction != LEFT_TO_RIGHT)
                mergeBitmap(bitmap2, bitmap1)
            else mergeBitmap(bitmap1, bitmap2)
        } else bitmap1
    }

    override fun getItemCount(): Int = pages.size
}