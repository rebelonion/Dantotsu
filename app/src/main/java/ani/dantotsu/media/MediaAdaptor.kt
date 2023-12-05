package ani.dantotsu.media

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.*
import ani.dantotsu.databinding.ItemMediaCompactBinding
import ani.dantotsu.databinding.ItemMediaLargeBinding
import ani.dantotsu.databinding.ItemMediaPageBinding
import ani.dantotsu.databinding.ItemMediaPageSmallBinding
import ani.dantotsu.settings.UserInterfaceSettings
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestOptions
import com.flaviofaria.kenburnsview.RandomTransitionGenerator
import jp.wasabeef.glide.transformations.BlurTransformation
import java.io.Serializable


class MediaAdaptor(
    var type: Int,
    private val mediaList: MutableList<Media>?,
    private val activity: FragmentActivity,
    private val matchParent: Boolean = false,
    private val viewPager: ViewPager2? = null,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val uiSettings =
        loadData<UserInterfaceSettings>("ui_settings") ?: UserInterfaceSettings()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (type) {
            0 -> MediaViewHolder(
                ItemMediaCompactBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            1 -> MediaLargeViewHolder(
                ItemMediaLargeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            2 -> MediaPageViewHolder(
                ItemMediaPageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            3 -> MediaPageSmallViewHolder(
                ItemMediaPageSmallBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            else -> throw IllegalArgumentException()
        }

    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (type) {
            0 -> {
                val b = (holder as MediaViewHolder).binding
                setAnimation(activity, b.root, uiSettings)
                val media = mediaList?.getOrNull(position)
                if (media != null) {
                    b.itemCompactImage.loadImage(media.cover)
                    b.itemCompactOngoing.visibility =
                        if (media.status == currActivity()!!.getString(R.string.status_releasing)) View.VISIBLE else View.GONE
                    b.itemCompactTitle.text = media.userPreferredName
                    b.itemCompactScore.text =
                        ((if (media.userScore == 0) (media.meanScore
                            ?: 0) else media.userScore) / 10.0).toString()
                    b.itemCompactScoreBG.background = ContextCompat.getDrawable(
                        b.root.context,
                        (if (media.userScore != 0) R.drawable.item_user_score else R.drawable.item_score)
                    )
                    b.itemCompactUserProgress.text = (media.userProgress ?: "~").toString()
                    if (media.relation != null) {
                        b.itemCompactRelation.text = "${media.relation}  "
                        b.itemCompactType.visibility = View.VISIBLE
                    } else {
                        b.itemCompactType.visibility = View.GONE
                    }
                    if (media.anime != null) {
                        if (media.relation != null) b.itemCompactTypeImage.setImageDrawable(
                            AppCompatResources.getDrawable(
                                activity,
                                R.drawable.ic_round_movie_filter_24
                            )
                        )
                        b.itemCompactTotal.text =
                            " | ${if (media.anime.nextAiringEpisode != null) (media.anime.nextAiringEpisode.toString() + " | " + (media.anime.totalEpisodes ?: "~").toString()) else (media.anime.totalEpisodes ?: "~").toString()}"
                    } else if (media.manga != null) {
                        if (media.relation != null) b.itemCompactTypeImage.setImageDrawable(
                            AppCompatResources.getDrawable(
                                activity,
                                R.drawable.ic_round_import_contacts_24
                            )
                        )
                        b.itemCompactTotal.text = " | ${media.manga.totalChapters ?: "~"}"
                    }
                }
            }

            1 -> {
                val b = (holder as MediaLargeViewHolder).binding
                setAnimation(activity, b.root, uiSettings)
                val media = mediaList?.get(position)
                if (media != null) {
                    b.itemCompactImage.loadImage(media.cover)
                    b.itemCompactBanner.loadImage(media.banner ?: media.cover, 400)
                    b.itemCompactOngoing.visibility =
                        if (media.status == currActivity()!!.getString(R.string.status_releasing)) View.VISIBLE else View.GONE
                    b.itemCompactTitle.text = media.userPreferredName
                    b.itemCompactScore.text =
                        ((if (media.userScore == 0) (media.meanScore
                            ?: 0) else media.userScore) / 10.0).toString()
                    b.itemCompactScoreBG.background = ContextCompat.getDrawable(
                        b.root.context,
                        (if (media.userScore != 0) R.drawable.item_user_score else R.drawable.item_score)
                    )
                    if (media.anime != null) {
                        b.itemTotal.text = " " + if ((media.anime.totalEpisodes
                                ?: 0) != 1
                        ) currActivity()!!.getString(R.string.episode_plural)
                        else currActivity()!!.getString(R.string.episode_singular)
                        b.itemCompactTotal.text =
                            if (media.anime.nextAiringEpisode != null) (media.anime.nextAiringEpisode.toString() + " / " + (media.anime.totalEpisodes
                                ?: "??").toString()) else (media.anime.totalEpisodes
                                ?: "??").toString()
                    } else if (media.manga != null) {
                        b.itemTotal.text = " " + if ((media.manga.totalChapters
                                ?: 0) != 1
                        ) currActivity()!!.getString(R.string.chapter_plural)
                        else currActivity()!!.getString(R.string.chapter_singular)
                        b.itemCompactTotal.text = "${media.manga.totalChapters ?: "??"}"
                    }
                    @SuppressLint("NotifyDataSetChanged")
                    if (position == mediaList!!.size - 2 && viewPager != null) viewPager.post {
                        mediaList.addAll(mediaList)
                        notifyDataSetChanged()
                    }
                }
            }

            2 -> {
                val b = (holder as MediaPageViewHolder).binding
                val media = mediaList?.get(position)
                if (media != null) {
                    b.itemCompactImage.loadImage(media.cover)
                    if (uiSettings.bannerAnimations)
                        b.itemCompactBanner.setTransitionGenerator(
                            RandomTransitionGenerator(
                                (10000 + 15000 * (uiSettings.animationSpeed)).toLong(),
                                AccelerateDecelerateInterpolator()
                            )
                        )
                    val banner =
                        if (uiSettings.bannerAnimations) b.itemCompactBanner else b.itemCompactBannerNoKen
                    val context = b.itemCompactBanner.context
                    if (!(context as Activity).isDestroyed)
                        Glide.with(context as Context)
                            .load(GlideUrl(media.banner ?: media.cover))
                            .diskCacheStrategy(DiskCacheStrategy.ALL).override(400)
                            .apply(RequestOptions.bitmapTransform(BlurTransformation(2, 3)))
                            .into(banner)
                    b.itemCompactOngoing.visibility =
                        if (media.status == currActivity()!!.getString(R.string.status_releasing)) View.VISIBLE else View.GONE
                    b.itemCompactTitle.text = media.userPreferredName
                    b.itemCompactScore.text =
                        ((if (media.userScore == 0) (media.meanScore
                            ?: 0) else media.userScore) / 10.0).toString()
                    b.itemCompactScoreBG.background = ContextCompat.getDrawable(
                        b.root.context,
                        (if (media.userScore != 0) R.drawable.item_user_score else R.drawable.item_score)
                    )
                    if (media.anime != null) {
                        b.itemTotal.text = " " + if ((media.anime.totalEpisodes
                                ?: 0) != 1
                        ) currActivity()!!.getString(R.string.episode_plural)
                        else currActivity()!!.getString(R.string.episode_singular)
                        b.itemCompactTotal.text =
                            if (media.anime.nextAiringEpisode != null) (media.anime.nextAiringEpisode.toString() + " / " + (media.anime.totalEpisodes
                                ?: "??").toString()) else (media.anime.totalEpisodes
                                ?: "??").toString()
                    } else if (media.manga != null) {
                        b.itemTotal.text = " " + if ((media.manga.totalChapters
                                ?: 0) != 1
                        ) currActivity()!!.getString(R.string.chapter_plural)
                        else currActivity()!!.getString(R.string.chapter_singular)
                        b.itemCompactTotal.text = "${media.manga.totalChapters ?: "??"}"
                    }
                    @SuppressLint("NotifyDataSetChanged")
                    if (position == mediaList!!.size - 2 && viewPager != null) viewPager.post {
                        val size = mediaList.size
                        mediaList.addAll(mediaList)
                        notifyItemRangeInserted(size - 1, mediaList.size)
                    }
                }
            }

            3 -> {
                val b = (holder as MediaPageSmallViewHolder).binding
                val media = mediaList?.get(position)
                if (media != null) {
                    b.itemCompactImage.loadImage(media.cover)
                    if (uiSettings.bannerAnimations)
                        b.itemCompactBanner.setTransitionGenerator(
                            RandomTransitionGenerator(
                                (10000 + 15000 * (uiSettings.animationSpeed)).toLong(),
                                AccelerateDecelerateInterpolator()
                            )
                        )
                    val banner =
                        if (uiSettings.bannerAnimations) b.itemCompactBanner else b.itemCompactBannerNoKen
                    val context = b.itemCompactBanner.context
                    if (!(context as Activity).isDestroyed)
                        Glide.with(context as Context)
                            .load(GlideUrl(media.banner ?: media.cover))
                            .diskCacheStrategy(DiskCacheStrategy.ALL).override(400)
                            .apply(RequestOptions.bitmapTransform(BlurTransformation(2, 3)))
                            .into(banner)
                    b.itemCompactOngoing.visibility =
                        if (media.status == currActivity()!!.getString(R.string.status_releasing)) View.VISIBLE else View.GONE
                    b.itemCompactTitle.text = media.userPreferredName
                    b.itemCompactScore.text =
                        ((if (media.userScore == 0) (media.meanScore
                            ?: 0) else media.userScore) / 10.0).toString()
                    b.itemCompactScoreBG.background = ContextCompat.getDrawable(
                        b.root.context,
                        (if (media.userScore != 0) R.drawable.item_user_score else R.drawable.item_score)
                    )
                    media.genres.apply {
                        if (isNotEmpty()) {
                            var genres = ""
                            forEach { genres += "$it • " }
                            genres = genres.removeSuffix(" • ")
                            b.itemCompactGenres.text = genres
                        }
                    }
                    b.itemCompactStatus.text = media.status ?: ""
                    if (media.anime != null) {
                        b.itemTotal.text = " " + if ((media.anime.totalEpisodes
                                ?: 0) != 1
                        ) currActivity()!!.getString(R.string.episode_plural)
                        else currActivity()!!.getString(R.string.episode_singular)
                        b.itemCompactTotal.text =
                            if (media.anime.nextAiringEpisode != null) (media.anime.nextAiringEpisode.toString() + " / " + (media.anime.totalEpisodes
                                ?: "??").toString()) else (media.anime.totalEpisodes
                                ?: "??").toString()
                    } else if (media.manga != null) {
                        b.itemTotal.text = " " + if ((media.manga.totalChapters
                                ?: 0) != 1
                        ) currActivity()!!.getString(R.string.chapter_plural)
                        else currActivity()!!.getString(R.string.chapter_singular)
                        b.itemCompactTotal.text = "${media.manga.totalChapters ?: "??"}"
                    }
                    @SuppressLint("NotifyDataSetChanged")
                    if (position == mediaList!!.size - 2 && viewPager != null) viewPager.post {
                        val size = mediaList.size
                        mediaList.addAll(mediaList)
                        notifyItemRangeInserted(size - 1, mediaList.size)
                    }
                }
            }
        }
    }

    override fun getItemCount() = mediaList!!.size

    override fun getItemViewType(position: Int): Int {
        return type
    }

    fun randomOptionClick() { //used for user list
        val media = mediaList?.random()
        if (media != null) {
            mediaList?.let {
                clicked(
                    it.indexOf(media),
                    null
                )
            }
        }

    }

    inner class MediaViewHolder(val binding: ItemMediaCompactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            if (matchParent) itemView.updateLayoutParams { width = -1 }
            itemView.setSafeOnClickListener {
                clicked(
                    bindingAdapterPosition,
                    resizeBitmap(getBitmapFromImageView(binding.itemCompactImage), 100)
                )
            }
            itemView.setOnLongClickListener { longClicked(bindingAdapterPosition) }
        }
    }

    inner class MediaLargeViewHolder(val binding: ItemMediaLargeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setSafeOnClickListener {
                clicked(
                    bindingAdapterPosition,
                    resizeBitmap(getBitmapFromImageView(binding.itemCompactImage), 100)
                )
            }
            itemView.setOnLongClickListener { longClicked(bindingAdapterPosition) }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class MediaPageViewHolder(val binding: ItemMediaPageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.itemCompactImage.setSafeOnClickListener {
                clicked(
                    bindingAdapterPosition,
                    resizeBitmap(getBitmapFromImageView(binding.itemCompactImage), 100)
                )
            }
            itemView.setOnTouchListener { _, _ -> true }
            binding.itemCompactImage.setOnLongClickListener { longClicked(bindingAdapterPosition) }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class MediaPageSmallViewHolder(val binding: ItemMediaPageSmallBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.itemCompactImage.setSafeOnClickListener {
                clicked(
                    bindingAdapterPosition,
                    resizeBitmap(getBitmapFromImageView(binding.itemCompactImage), 100)
                )
            }
            binding.itemCompactTitleContainer.setSafeOnClickListener {
                clicked(
                    bindingAdapterPosition,
                    resizeBitmap(getBitmapFromImageView(binding.itemCompactImage), 100)
                )
            }
            itemView.setOnTouchListener { _, _ -> true }
            binding.itemCompactImage.setOnLongClickListener { longClicked(bindingAdapterPosition) }
        }
    }

    fun clicked(position: Int, bitmap: Bitmap? = null) {
        if ((mediaList?.size ?: 0) > position && position != -1) {
            val media = mediaList?.get(position)
            if (bitmap != null) MediaSingleton.bitmap = bitmap
            ContextCompat.startActivity(
                activity,
                Intent(activity, MediaDetailsActivity::class.java).putExtra(
                    "media",
                    media as Serializable
                ), null
            )
        }
    }

    fun longClicked(position: Int): Boolean {
        if ((mediaList?.size ?: 0) > position && position != -1) {
            val media = mediaList?.get(position) ?: return false
            if (activity.supportFragmentManager.findFragmentByTag("list") == null) {
                MediaListDialogSmallFragment.newInstance(media)
                    .show(activity.supportFragmentManager, "list")
                return true
            }
        }
        return false
    }

    fun getBitmapFromImageView(imageView: ImageView): Bitmap? {
        val drawable = imageView.drawable ?: return null

        // If the drawable is a BitmapDrawable, then just get the bitmap
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        // Create a bitmap with the same dimensions as the drawable
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        // Draw the drawable onto the bitmap
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    fun resizeBitmap(source: Bitmap?, maxDimension: Int): Bitmap? {
        if (source == null) return null
        val width = source.width
        val height = source.height
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxDimension
            newHeight = (height * (maxDimension.toFloat() / width)).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (width * (maxDimension.toFloat() / height)).toInt()
        }

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

}