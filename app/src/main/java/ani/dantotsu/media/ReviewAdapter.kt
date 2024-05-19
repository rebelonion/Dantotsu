package ani.dantotsu.media


import android.content.Context
import android.text.SpannableString
import android.view.View
import androidx.lifecycle.lifecycleScope
import ani.dantotsu.R
import ani.dantotsu.blurImage
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.databinding.ItemFollowerBinding
import ani.dantotsu.databinding.ItemReviewsBinding
import ani.dantotsu.getThemeColor
import ani.dantotsu.loadImage
import ani.dantotsu.profile.activity.ActivityItemBuilder
import ani.dantotsu.toast
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewAdapter(
    private var review: Query.Review,
    val context: ReviewActivity,
    val clickCallback: (Int) -> Unit

) : BindableItem<ItemReviewsBinding>() {
    private lateinit var binding: ItemReviewsBinding

    override fun bind(viewBinding: ItemReviewsBinding, position: Int) {
        binding = viewBinding
        binding.reviewUserName.text = review.user?.name
        binding.reviewUserAvatar.loadImage(review.user?.avatar?.medium)
        binding.reviewText.text = review.summary
        binding.reviewPostTime.text = ActivityItemBuilder.getDateTime(review.createdAt)
        binding.reviewTag.text = "[${review.score}]"
        binding.root.setOnClickListener { clickCallback(review.id) }
        userVote(review.userRating)
        enableVote()
        binding.reviewTotalVotes.text = review.rating.toString()
    }

    override fun getLayout(): Int {
        return R.layout.item_reviews
    }

    override fun initializeViewBinding(view: View): ItemReviewsBinding {
        return ItemReviewsBinding.bind(view)
    }
    private fun userVote(type: String) {
        when (type) {
            "NO_VOTE" -> {
                binding.reviewUpVote.setImageResource(R.drawable.ic_round_upvote_inactive_24)
                binding.reviewDownVote.setImageResource(R.drawable.ic_round_upvote_inactive_24)
                binding.reviewUpVote.alpha = 0.6f
                binding.reviewDownVote.alpha = 0.6f
            }

            "UP_VOTE" -> {
                binding.reviewUpVote.setImageResource(R.drawable.ic_round_upvote_active_24)
                binding.reviewDownVote.setImageResource(R.drawable.ic_round_upvote_inactive_24)
                binding.reviewUpVote.alpha = 1f
                binding.reviewDownVote.alpha = 0.6f
            }

            "DOWN_VOTE" -> {
                binding.reviewUpVote.setImageResource(R.drawable.ic_round_upvote_inactive_24)
                binding.reviewDownVote.setImageResource(R.drawable.ic_round_upvote_active_24)
                binding.reviewDownVote.alpha = 1f
                binding.reviewUpVote.alpha = 0.6f
            }
        }
    }

    private fun rateReview(rating: String) {
        disableVote()
        context.lifecycleScope.launch {
            val result = Anilist.mutation.rateReview(review.id, rating)
            if (result != null) {
                withContext(Dispatchers.Main) {
                    val res = result.data.rateReview
                    review.rating = res.rating
                    review.ratingAmount = res.ratingAmount
                    review.userRating = res.userRating
                    userVote(review.userRating)
                    binding.reviewTotalVotes.text = review.rating.toString()
                    userVote(review.userRating)
                    enableVote()
                }
            } else {
                withContext(Dispatchers.Main) {
                    toast(
                        context.getString(R.string.error_message, "response is null")
                    )
                    enableVote()
                }
            }
        }
    }

    private fun disableVote() {
        binding.reviewUpVote.setOnClickListener(null)
        binding.reviewDownVote.setOnClickListener(null)
        binding.reviewUpVote.isEnabled = false
        binding.reviewDownVote.isEnabled = false
    }

    private fun enableVote() {
        binding.reviewUpVote.setOnClickListener {
            if (review.userRating == "UP_VOTE") {
                rateReview("NO_VOTE")
            } else {
                rateReview("UP_VOTE")
            }
            disableVote()
        }
        binding.reviewDownVote.setOnClickListener {
            if (review.userRating == "DOWN_VOTE") {
                rateReview("NO_VOTE")
            } else {
                rateReview("DOWN_VOTE")
            }
            disableVote()
        }
        binding.reviewUpVote.isEnabled = true
        binding.reviewDownVote.isEnabled = true
    }
}