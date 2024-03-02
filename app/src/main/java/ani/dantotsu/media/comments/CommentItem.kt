package ani.dantotsu.media.comments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.comments.Comment
import ani.dantotsu.connections.comments.CommentsAPI
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ItemCommentsBinding
import ani.dantotsu.loadImage
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.snackString
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem
import io.noties.markwon.Markwon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class CommentItem(val comment: Comment,
                  private val markwon: Markwon,
                  val parentSection: Section,
                  private val commentsFragment: CommentsFragment,
                  private val backgroundColor: Int,
                  val commentDepth: Int
) : BindableItem<ItemCommentsBinding>() {
    var binding: ItemCommentsBinding? = null
    val adapter = GroupieAdapter()
    private var subCommentIds: MutableList<Int> = mutableListOf()
    val repliesSection = Section()
    private var isEditing = false
    var isReplying = false
    private var repliesVisible = false
    var MAX_DEPTH = 3

    init {
        adapter.add(repliesSection)
    }

    @SuppressLint("SetTextI18n")
    override fun bind(viewBinding: ItemCommentsBinding, position: Int) {
        binding = viewBinding
        viewBinding.commentRepliesList.layoutManager = LinearLayoutManager(commentsFragment.activity)
        viewBinding.commentRepliesList.adapter = adapter
        val isUserComment = CommentsAPI.userId == comment.userId
        val node = markwon.parse(comment.content)
        val spanned = markwon.render(node)
        val levelColor = getAvatarColor(comment.totalVotes, backgroundColor)
        markwon.setParsedMarkdown(viewBinding.commentText, viewBinding.commentText.setSpoilerText(spanned, markwon))
        viewBinding.commentDelete.visibility = if (isUserComment || CommentsAPI.isAdmin || CommentsAPI.isMod) View.VISIBLE else View.GONE
        viewBinding.commentBanUser.visibility = if ((CommentsAPI.isAdmin || CommentsAPI.isMod) && !isUserComment) View.VISIBLE else View.GONE
        viewBinding.commentReport.visibility = if (!isUserComment) View.VISIBLE else View.GONE
        viewBinding.commentEdit.visibility = if (isUserComment) View.VISIBLE else View.GONE
        if (comment.tag == null) {
            viewBinding.commentUserTagLayout.visibility = View.GONE
        } else {
            viewBinding.commentUserTagLayout.visibility = View.VISIBLE
            viewBinding.commentUserTag.text = comment.tag.toString()
        }
        replying(isReplying) //sets default text
        editing(isEditing)
        if ((comment.replyCount ?: 0) > 0) {
            viewBinding.commentTotalReplies.visibility = View.VISIBLE
            viewBinding.commentRepliesDivider.visibility = View.VISIBLE
            viewBinding.commentTotalReplies.text = if(repliesVisible) "Hide Replies" else
                "View ${comment.replyCount} repl${if (comment.replyCount == 1) "y" else "ies"}"
        } else {
            viewBinding.commentTotalReplies.visibility = View.GONE
            viewBinding.commentRepliesDivider.visibility = View.GONE
        }
        viewBinding.commentReply.visibility = View.VISIBLE
        viewBinding.commentTotalReplies.setOnClickListener {
            if (repliesVisible) {
                repliesSection.clear()
                removeSubCommentIds()
                viewBinding.commentTotalReplies.text = "View ${comment.replyCount} repl${if (comment.replyCount == 1) "y" else "ies"}"
                repliesVisible = false
            } else {
                viewBinding.commentTotalReplies.text = "Hide Replies"
                repliesSection.clear()
                commentsFragment.viewReplyCallback(this)
                repliesVisible = true
            }
        }

        viewBinding.commentUserName.setOnClickListener {
            ContextCompat.startActivity(
                commentsFragment.activity, Intent(commentsFragment.activity, ProfileActivity::class.java)
                    .putExtra("userId", comment.userId.toInt())
                    .putExtra("username","[${levelColor.second}]"), null
            )

        }
        viewBinding.commentText.setOnLongClickListener {
            copyToClipboard(comment.content)
            true
        }

        viewBinding.commentEdit.setOnClickListener {
            editing(!isEditing)
            commentsFragment.editCallback(this)
        }
        viewBinding.commentReply.setOnClickListener {
            replying(!isReplying)
            commentsFragment.replyTo(this, comment.username)
            commentsFragment.replyCallback(this)
        }
        viewBinding.modBadge.visibility = if (comment.isMod == true) View.VISIBLE else View.GONE
        viewBinding.adminBadge.visibility = if (comment.isAdmin == true) View.VISIBLE else View.GONE
        viewBinding.commentDelete.setOnClickListener {
            dialogBuilder("Delete Comment", "Are you sure you want to delete this comment?") {
                val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
                scope.launch {
                    val success = CommentsAPI.deleteComment(comment.commentId)
                    if (success) {
                        snackString("Comment Deleted")
                        parentSection.remove(this@CommentItem)
                    }
                }
            }
        }
        viewBinding.commentBanUser.setOnClickListener {
            dialogBuilder("Ban User", "Are you sure you want to ban this user?") {
                val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
                scope.launch {
                    val success = CommentsAPI.banUser(comment.userId)
                    if (success) {
                        snackString("User Banned")
                    }
                }
            }
        }
        viewBinding.commentReport.setOnClickListener {
            dialogBuilder("Report Comment", "Only report comments that violate the rules. Are you sure you want to report this comment?") {
                val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
                scope.launch {
                    val success = CommentsAPI.reportComment(comment.commentId, comment.username, commentsFragment.mediaName, comment.userId)
                    if (success) {
                        snackString("Comment Reported")
                    }
                }
            }
        }
        //fill the icon if the user has liked the comment
        setVoteButtons(viewBinding)
        viewBinding.commentUpVote.setOnClickListener {
            val voteType = if (comment.userVoteType == 1) 0 else 1
            val previousVoteType = comment.userVoteType
            val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
            scope.launch {
                val success = CommentsAPI.vote(comment.commentId, voteType)
                if (success) {
                    comment.userVoteType = voteType

                    if (previousVoteType == -1) {
                        comment.downvotes -= 1
                    }
                    comment.upvotes += if (voteType == 1) 1 else -1

                    notifyChanged()
                }
            }
        }

        viewBinding.commentDownVote.setOnClickListener {
            val voteType = if (comment.userVoteType == -1) 0 else -1
            val previousVoteType = comment.userVoteType
            val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
            scope.launch {
                val success = CommentsAPI.vote(comment.commentId, voteType)
                if (success) {
                    comment.userVoteType = voteType

                    if (previousVoteType == 1) {
                        comment.upvotes -= 1
                    }
                    comment.downvotes += if (voteType == -1) 1 else -1

                    notifyChanged()
                }
            }
        }
        viewBinding.commentTotalVotes.text = (comment.upvotes - comment.downvotes).toString()
        viewBinding.commentUserAvatar
        comment.profilePictureUrl?.let { viewBinding.commentUserAvatar.loadImage(it) }
        viewBinding.commentUserName.text = comment.username

        viewBinding.commentUserLevel.text = "[${levelColor.second}]"
        viewBinding.commentUserLevel.setTextColor(levelColor.first)
        viewBinding.commentUserTime.text = formatTimestamp(comment.timestamp)
    }

    override fun getLayout(): Int {
        return R.layout.item_comments
    }

    override fun initializeViewBinding(view: View): ItemCommentsBinding {
        return ItemCommentsBinding.bind(view)
    }

    fun replying(isReplying: Boolean) {
        binding?.commentReply?.text = if (isReplying) commentsFragment.activity.getString(R.string.cancel) else "Reply"
        this.isReplying = isReplying
    }

    fun editing(isEditing: Boolean) {
        binding?.commentEdit?.text = if (isEditing) commentsFragment.activity.getString(R.string.cancel) else commentsFragment.activity.getString(R.string.edit)
        this.isEditing = isEditing
    }

    fun registerSubComment(id: Int) {
        subCommentIds.add(id)
    }

    private fun removeSubCommentIds(){
        subCommentIds.forEach { id ->
            val parentComments = parentSection.groups as? List<CommentItem> ?: emptyList()
            val commentToRemove = parentComments.find { it.comment.commentId == id }
            commentToRemove?.let {
                it.removeSubCommentIds()
                parentSection.remove(it)
            }
        }
        subCommentIds.clear()
    }

    private fun setVoteButtons(viewBinding: ItemCommentsBinding) {
        when (comment.userVoteType) {
            1 -> {
                viewBinding.commentUpVote.setImageResource(R.drawable.ic_round_upvote_active_24)
                viewBinding.commentUpVote.alpha = 1f
                viewBinding.commentDownVote.setImageResource(R.drawable.ic_round_upvote_inactive_24)
            }
            -1 -> {
                viewBinding.commentUpVote.setImageResource(R.drawable.ic_round_upvote_inactive_24)
                viewBinding.commentDownVote.setImageResource(R.drawable.ic_round_upvote_active_24)
                viewBinding.commentDownVote.alpha = 1f
            }
            else -> {
                viewBinding.commentUpVote.setImageResource(R.drawable.ic_round_upvote_inactive_24)
                viewBinding.commentDownVote.setImageResource(R.drawable.ic_round_upvote_inactive_24)
            }
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val parsedDate = dateFormat.parse(timestamp)
            val currentDate = Date()

            val diff = currentDate.time - (parsedDate?.time ?: 0)

            val days = diff / (24 * 60 * 60 * 1000)
            val hours = diff / (60 * 60 * 1000) % 24
            val minutes = diff / (60 * 1000) % 60

            return when {
                days > 0 -> "${days}d"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                else -> "now"
            }
        } catch (e: Exception) {
            "now"
        }
    }

    companion object {
        fun timestampToMillis(timestamp: String): Long {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val parsedDate = dateFormat.parse(timestamp)
            return parsedDate?.time ?: 0
        }
    }

    private fun getLuminance(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0

        val rL = if (r <= 0.03928) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
        val gL = if (g <= 0.03928) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
        val bL = if (b <= 0.03928) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)

        return 0.2126 * rL + 0.7152 * gL + 0.0722 * bL
    }

    private fun getContrastRatio(color1: Int, color2: Int): Double {
        val l1 = getLuminance(color1)
        val l2 = getLuminance(color2)

        return if (l1 > l2) (l1 + 0.05) / (l2 + 0.05) else (l2 + 0.05) / (l1 + 0.05)
    }

    private fun getAvatarColor(voteCount: Int, backgroundColor: Int): Pair<Int, Int> {
        val level = if (voteCount < 0) 0 else sqrt(abs(voteCount.toDouble()) / 0.8).toInt()
        val colorString = if (level > usernameColors.size - 1) usernameColors[usernameColors.size - 1] else usernameColors[level]
        var color = Color.parseColor(colorString)
        val ratio = getContrastRatio(color, backgroundColor)
        if (ratio < 4.5) {
            color = adjustColorForContrast(color, backgroundColor)
        }

        return Pair(color, level)
    }

    private fun adjustColorForContrast(originalColor: Int, backgroundColor: Int): Int {
        var adjustedColor = originalColor
        var contrastRatio = getContrastRatio(adjustedColor, backgroundColor)
        val isBackgroundDark = getLuminance(backgroundColor) < 0.5

        while (contrastRatio < 4.5) {
            // Adjust brightness by modifying the RGB values
            val r = Color.red(adjustedColor)
            val g = Color.green(adjustedColor)
            val b = Color.blue(adjustedColor)

            // Calculate the amount to adjust
            val adjustment = if (isBackgroundDark) 10 else -10

            // Adjust the color
            val newR = (r + adjustment).coerceIn(0, 255)
            val newG = (g + adjustment).coerceIn(0, 255)
            val newB = (b + adjustment).coerceIn(0, 255)

            adjustedColor = Color.rgb(newR, newG, newB)
            contrastRatio = getContrastRatio(adjustedColor, backgroundColor)

            // Break the loop if the color adjustment does not change (to avoid infinite loop)
            if (newR == r && newG == g && newB == b) {
                break
            }
        }
        return adjustedColor
    }


    /**
     * Builds the dialog for yes/no confirmation
     * no doesn't do anything, yes calls the callback
     * @param title the title of the dialog
     * @param message the message of the dialog
     * @param callback the callback to call when the user clicks yes
     */
    private fun dialogBuilder(title: String, message: String, callback: () -> Unit) {
        val alertDialog = android.app.AlertDialog.Builder(commentsFragment.activity, R.style.MyPopup)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { dialog, _ ->
                callback()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = alertDialog.show()
        dialog?.window?.setDimAmount(0.8f)
    }

    private val usernameColors: Array<String> = arrayOf(
        "#9932cc",
        "#a020f0",
        "#8b008b",
        "#7b68ee",
        "#da70d6",
        "#dda0dd",
        "#ffe4b5",
        "#f0e68c",
        "#ffb6c1",
        "#fa8072",
        "#b03060",
        "#ff1493",
        "#ff00ff",
        "#ff69b4",
        "#dc143c",
        "#8b0000",
        "#ff0000",
        "#a0522d",
        "#f4a460",
        "#b8860b",
        "#ffa500",
        "#d2691e",
        "#ff6347",
        "#808000",
        "#ffd700",
        "#ffff54",
        "#8fbc8f",
        "#3cb371",
        "#008000",
        "#00fa9a",
        "#98fb98",
        "#00ff00",
        "#adff2f",
        "#32cd32",
        "#556b2f",
        "#9acd32",
        "#7fffd4",
        "#2f4f4f",
        "#5f9ea0",
        "#87ceeb",
        "#00bfff",
        "#00ffff",
        "#1e90ff",
        "#4682b4",
        "#0000ff",
        "#0000cd",
        "#00008b",
        "#191970",
        "#ffffff",
    )

}