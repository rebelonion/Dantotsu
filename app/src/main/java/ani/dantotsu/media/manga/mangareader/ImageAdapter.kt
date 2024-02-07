package ani.dantotsu.media.manga.mangareader

import android.animation.ObjectAnimator
import android.content.res.Resources.getSystem
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.R
import ani.dantotsu.databinding.ItemImageBinding
import ani.dantotsu.media.manga.MangaChapter
import ani.dantotsu.settings.CurrentReaderSettings.Directions.LEFT_TO_RIGHT
import ani.dantotsu.settings.CurrentReaderSettings.Directions.RIGHT_TO_LEFT
import ani.dantotsu.settings.CurrentReaderSettings.Layouts.PAGED
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

open class ImageAdapter(
    activity: MangaReaderActivity,
    chapter: MangaChapter
) : BaseImageAdapter(activity, chapter) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    inner class ImageViewHolder(binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root)

    open suspend fun loadBitmap(position: Int, parent: View): Bitmap? {
        val link = images.getOrNull(position)?.url ?: return null
        if (link.url.isEmpty()) return null

        val transforms = mutableListOf<BitmapTransformation>()
        val parserTransformation = activity.getTransformation(images[position])

        if (parserTransformation != null) transforms.add(parserTransformation)
        if (settings.cropBorders) {
            transforms.add(RemoveBordersTransformation(true, settings.cropBorderThreshold))
            transforms.add(RemoveBordersTransformation(false, settings.cropBorderThreshold))
        }

        return activity.loadBitmap(link, transforms)
    }

    override suspend fun loadImage(position: Int, parent: View): Boolean {
        val imageView = parent.findViewById<SubsamplingScaleImageView>(R.id.imgProgImageNoGestures)
            ?: return false
        val progress = parent.findViewById<View>(R.id.imgProgProgress) ?: return false
        imageView.recycle()
        imageView.visibility = View.GONE

        val bitmap = loadBitmap(position, parent) ?: return false

        var sWidth = getSystem().displayMetrics.widthPixels
        var sHeight = getSystem().displayMetrics.heightPixels

        if (settings.layout != PAGED)
            parent.updateLayoutParams {
                if (settings.direction != LEFT_TO_RIGHT && settings.direction != RIGHT_TO_LEFT) {
                    sHeight =
                        if (settings.wrapImages) bitmap.height else (sWidth * bitmap.height * 1f / bitmap.width).toInt()
                    height = sHeight
                } else {
                    sWidth =
                        if (settings.wrapImages) bitmap.width else (sHeight * bitmap.width * 1f / bitmap.height).toInt()
                    width = sWidth
                }
            }

        imageView.visibility = View.VISIBLE
        imageView.setImage(ImageSource.cachedBitmap(bitmap))

        val parentArea = sWidth * sHeight * 1f
        val bitmapArea = bitmap.width * bitmap.height * 1f
        val scale =
            if (parentArea < bitmapArea) (bitmapArea / parentArea) else (parentArea / bitmapArea)

        imageView.maxScale = scale * 1.1f
        imageView.minScale = scale

        ObjectAnimator.ofFloat(parent, "alpha", 0f, 1f)
            .setDuration((400 * PrefManager.getVal<Float>(PrefName.AnimationSpeed)).toLong())
            .start()
        progress.visibility = View.GONE

        return true
    }

    override fun getItemCount(): Int = images.size

    override fun isZoomed(): Boolean {
        val imageView =
            activity.findViewById<SubsamplingScaleImageView>(R.id.imgProgImageNoGestures)
        return imageView.scale > imageView.minScale
    }

    override fun setZoom(zoom: Float) {
        val imageView =
            activity.findViewById<SubsamplingScaleImageView>(R.id.imgProgImageNoGestures)
        imageView.setScaleAndCenter(zoom, imageView.center)
    }
}
