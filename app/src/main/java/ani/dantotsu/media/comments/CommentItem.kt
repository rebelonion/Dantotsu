package ani.dantotsu.media.comments

import android.view.View
import ani.dantotsu.R
import ani.dantotsu.connections.comments.Comment
import ani.dantotsu.connections.comments.CommentsAPI
import ani.dantotsu.databinding.ItemCommentsBinding
import ani.dantotsu.loadImage
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

class CommentItem(val comment: Comment,
                  private val markwon: Markwon,
                  private val section: Section,
                  private val editCallback: (CommentItem) -> Unit
) : BindableItem<ItemCommentsBinding>() {

    override fun bind(viewBinding: ItemCommentsBinding, position: Int) {
        val isUserComment = CommentsAPI.userId == comment.userId
        val node = markwon.parse(comment.content)
        val spanned = markwon.render(node)
        markwon.setParsedMarkdown(viewBinding.commentText, viewBinding.commentText.setSpoilerText(spanned, markwon))
        viewBinding.commentDelete.visibility = if (isUserComment || CommentsAPI.isAdmin || CommentsAPI.isMod) View.VISIBLE else View.GONE
        viewBinding.commentBanUser.visibility = if (CommentsAPI.isAdmin || CommentsAPI.isMod) View.VISIBLE else View.GONE
        viewBinding.commentEdit.visibility = if (isUserComment) View.VISIBLE else View.GONE
        viewBinding.commentReply.visibility = View.GONE //TODO: implement reply
        viewBinding.commentTotalReplies.visibility = View.GONE //TODO: implement reply
        viewBinding.commentReply.setOnClickListener {

        }
        var isEditing = false
        viewBinding.commentEdit.setOnClickListener {
            if (!isEditing) {
                viewBinding.commentEdit.text = "Cancel"
                isEditing = true
                editCallback(this)
            } else {
                viewBinding.commentEdit.text = "Edit"
                isEditing = false
                editCallback(this)
            }

        }
        viewBinding.modBadge.visibility = if (comment.isMod == true) View.VISIBLE else View.GONE
        viewBinding.adminBadge.visibility = if (comment.isAdmin == true) View.VISIBLE else View.GONE
        viewBinding.commentDelete.setOnClickListener {
            val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
            scope.launch {
                val success = CommentsAPI.deleteComment(comment.commentId)
                if (success) {
                    snackString("Comment Deleted")
                    section.remove(this@CommentItem)
                }
            }
        }
        viewBinding.commentBanUser.setOnClickListener {
            val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
            scope.launch {
                val success = CommentsAPI.banUser(comment.userId)
                if (success) {
                    snackString("User Banned")
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
        viewBinding.commentUserTime.text = formatTimestamp(comment.timestamp)
    }

    override fun getLayout(): Int {
        return R.layout.item_comments
    }

    override fun initializeViewBinding(view: View): ItemCommentsBinding {
        return ItemCommentsBinding.bind(view)
    }

    private fun setVoteButtons(viewBinding: ItemCommentsBinding) {
        if (comment.userVoteType == 1) {
            viewBinding.commentUpVote.setImageResource(R.drawable.ic_round_upvote_active_24)
            viewBinding.commentDownVote.setImageResource(R.drawable.ic_round_upvote_inactive_24)
        } else if (comment.userVoteType == -1) {
            viewBinding.commentUpVote.setImageResource(R.drawable.ic_round_upvote_inactive_24)
            viewBinding.commentDownVote.setImageResource(R.drawable.ic_round_upvote_active_24)
        } else {
            viewBinding.commentUpVote.setImageResource(R.drawable.ic_round_upvote_inactive_24)
            viewBinding.commentDownVote.setImageResource(R.drawable.ic_round_upvote_inactive_24)
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
                days > 0 -> "$days days ago"
                hours > 0 -> "$hours hours ago"
                minutes > 0 -> "$minutes minutes ago"
                else -> "just now"
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
}