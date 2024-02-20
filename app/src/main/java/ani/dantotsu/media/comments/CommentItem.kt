package ani.dantotsu.media.comments

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.comments.Comment
import ani.dantotsu.connections.comments.CommentsAPI
import ani.dantotsu.copyToClipboard
import ani.dantotsu.currActivity
import ani.dantotsu.databinding.ItemCommentsBinding
import ani.dantotsu.loadImage
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.others.ImageViewDialog
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
                  private val editCallback: (CommentItem) -> Unit,
                  private val viewReplyCallback: (CommentItem) -> Unit,
                    private val replyCallback: (CommentItem) -> Unit
) : BindableItem<ItemCommentsBinding>() {
    var binding: ItemCommentsBinding? = null
    val adapter = GroupieAdapter()
    val repliesSection = Section()

    init {
        adapter.add(repliesSection)
    }

    @SuppressLint("SetTextI18n")
    override fun bind(viewBinding: ItemCommentsBinding, position: Int) {
        binding = viewBinding
        viewBinding.commentRepliesList.layoutManager = LinearLayoutManager(currActivity())
        viewBinding.commentRepliesList.adapter = adapter
        val isUserComment = CommentsAPI.userId == comment.userId
        val node = markwon.parse(comment.content)
        val spanned = markwon.render(node)
        markwon.setParsedMarkdown(viewBinding.commentText, viewBinding.commentText.setSpoilerText(spanned, markwon))
        viewBinding.commentDelete.visibility = if (isUserComment || CommentsAPI.isAdmin || CommentsAPI.isMod) View.VISIBLE else View.GONE
        viewBinding.commentBanUser.visibility = if ((CommentsAPI.isAdmin || CommentsAPI.isMod) && !isUserComment) View.VISIBLE else View.GONE
        viewBinding.commentEdit.visibility = if (isUserComment) View.VISIBLE else View.GONE
        if ((comment.replyCount ?: 0) > 0) {
            viewBinding.commentTotalReplies.visibility = View.VISIBLE
            viewBinding.commentRepliesDivider.visibility = View.VISIBLE
            viewBinding.commentTotalReplies.text = "View ${comment.replyCount} repl${if (comment.replyCount == 1) "y" else "ies"}"
        } else {
            viewBinding.commentTotalReplies.visibility = View.GONE
            viewBinding.commentRepliesDivider.visibility = View.GONE
        }
        viewBinding.commentReply.visibility = View.VISIBLE
        viewBinding.commentTotalReplies.setOnClickListener {
            viewBinding.commentTotalReplies.visibility = View.GONE
            viewBinding.commentRepliesDivider.visibility = View.GONE
            viewReplyCallback(this)
        }
        viewBinding.commentUserName.setOnClickListener {
            openLinkInBrowser("https://anilist.co/user/${comment.username}")
        }
        viewBinding.commentText.setOnLongClickListener {
            copyToClipboard(comment.content)
            true
        }
        var isEditing = false
        var isReplying = false
        viewBinding.commentEdit.setOnClickListener {
            if (!isEditing) {
                viewBinding.commentEdit.text = currActivity()!!.getString(R.string.cancel)
                isEditing = true
                isReplying = false
                viewBinding.commentReply.text = "Reply"
                editCallback(this)
            } else {
                viewBinding.commentEdit.text = currActivity()!!.getString(R.string.edit)
                isEditing = false
                editCallback(this)
            }

        }
        viewBinding.commentReply.setOnClickListener {
            if (!isReplying) {
                viewBinding.commentReply.text = currActivity()!!.getString(R.string.cancel)
                isReplying = true
                isEditing = false
                viewBinding.commentEdit.text = currActivity()!!.getString(R.string.edit)
                replyCallback(this)
            } else {
                viewBinding.commentReply.text = "Reply"
                isReplying = false
                replyCallback(this)
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
        viewBinding.commentUserTime.text = "● ${formatTimestamp(comment.timestamp)}"
    }

    override fun getLayout(): Int {
        return R.layout.item_comments
    }

    override fun initializeViewBinding(view: View): ItemCommentsBinding {
        return ItemCommentsBinding.bind(view)
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