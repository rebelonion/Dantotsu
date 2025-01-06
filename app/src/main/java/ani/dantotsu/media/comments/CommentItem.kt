package ani.dantotsu.media.comments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.comments.Comment
import ani.dantotsu.connections.comments.CommentsAPI
import ani.dantotsu.copyToClipboard
import ani.dantotsu.databinding.ItemCommentsBinding
import ani.dantotsu.getAppString
import ani.dantotsu.loadImage
import ani.dantotsu.openImage
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.setAnimation
import ani.dantotsu.snackString
import ani.dantotsu.util.ColorEditor.Companion.adjustColorForContrast
import ani.dantotsu.util.ColorEditor.Companion.getContrastRatio
import ani.dantotsu.util.customAlertDialog
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
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.sqrt

class CommentItem(
    val comment: Comment,
    private val markwon: Markwon,
    val parentSection: Section,
    private val commentsFragment: CommentsFragment,
    private val backgroundColor: Int,
    val commentDepth: Int
) :
    BindableItem<ItemCommentsBinding>() {
    lateinit var binding: ItemCommentsBinding
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

    override fun bind(viewBinding: ItemCommentsBinding, position: Int) {
        binding = viewBinding
        setAnimation(binding.root.context, binding.root)
        val item = this
        viewBinding.apply {
            commentRepliesList.layoutManager =
                LinearLayoutManager(commentsFragment.activity)
            commentRepliesList.adapter = adapter
            val isUserComment = CommentsAPI.userId == comment.userId
            val levelColor = getAvatarColor(comment.totalVotes, backgroundColor)
            markwon.setMarkdown(commentText, comment.content)
            commentEdit.visibility = if (isUserComment) View.VISIBLE else View.GONE
            if (comment.tag == null) {
                commentUserTagLayout.visibility = View.GONE
            } else {
                commentUserTagLayout.visibility = View.VISIBLE
                commentUserTag.text = comment.tag.toString()
            }
            replying(isReplying) //sets default text
            editing(isEditing)
            if ((comment.replyCount ?: 0) > 0) {
                commentTotalReplies.visibility = View.VISIBLE
                commentRepliesDivider.visibility = View.VISIBLE
                commentTotalReplies.context.run {
                    commentTotalReplies.text = if (repliesVisible)
                        getString(R.string.hide_replies)
                    else
                        if (comment.replyCount == 1)
                            getString(R.string.view_reply)
                        else
                            getString(R.string.view_replies_count, comment.replyCount)
                }
            } else {
                commentTotalReplies.visibility = View.GONE
                commentRepliesDivider.visibility = View.GONE
            }
            commentReply.visibility = View.VISIBLE
            commentTotalReplies.setOnClickListener {
                if (repliesVisible) {
                    repliesSection.clear()
                    removeSubCommentIds()
                    commentTotalReplies.context.run {
                        commentTotalReplies.text = if (comment.replyCount == 1)
                            getString(R.string.view_reply)
                        else
                            getString(R.string.view_replies_count, comment.replyCount)
                    }
                    repliesVisible = false
                } else {
                    commentTotalReplies.setText(R.string.hide_replies)
                    repliesSection.clear()
                    commentsFragment.viewReplyCallback(item)
                    repliesVisible = true
                }
            }

            commentUserName.setOnClickListener {
                ContextCompat.startActivity(
                    commentsFragment.activity,
                    Intent(commentsFragment.activity, ProfileActivity::class.java)
                        .putExtra("userId", comment.userId.toInt()),
                    null
                )
            }
            commentUserAvatar.setOnClickListener {
                ContextCompat.startActivity(
                    commentsFragment.activity,
                    Intent(commentsFragment.activity, ProfileActivity::class.java)
                        .putExtra("userId", comment.userId.toInt()),
                    null
                )
            }
            commentText.setOnLongClickListener {
                copyToClipboard(comment.content)
                true
            }

            commentEdit.setOnClickListener {
                editing(!isEditing)
                commentsFragment.editCallback(item)
            }
            commentReply.setOnClickListener {
                replying(!isReplying)
                commentsFragment.replyTo(item, comment.username)
                commentsFragment.replyCallback(item)
            }
            modBadge.visibility = if (comment.isMod == true) View.VISIBLE else View.GONE
            adminBadge.visibility =
                if (comment.isAdmin == true) View.VISIBLE else View.GONE
            commentInfo.setOnClickListener {
                val popup = PopupMenu(commentsFragment.requireContext(), commentInfo)
                popup.menuInflater.inflate(R.menu.profile_details_menu, popup.menu)
                popup.menu.findItem(R.id.commentDelete)?.isVisible =
                    isUserComment || CommentsAPI.isAdmin || CommentsAPI.isMod
                popup.menu.findItem(R.id.commentBanUser)?.isVisible =
                    (CommentsAPI.isAdmin || CommentsAPI.isMod) && !isUserComment
                popup.menu.findItem(R.id.commentReport)?.isVisible = !isUserComment
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.commentReport -> {
                            dialogBuilder(
                                getAppString(R.string.report_comment),
                                getAppString(R.string.report_comment_confirm)
                            ) {
                                CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                                    val success = CommentsAPI.reportComment(
                                        comment.commentId,
                                        comment.username,
                                        commentsFragment.mediaName,
                                        comment.userId
                                    )
                                    if (success) {
                                        snackString(R.string.comment_reported)
                                    }
                                }
                            }
                            true
                        }

                        R.id.commentDelete -> {
                            dialogBuilder(
                                getAppString(R.string.delete_comment),
                                getAppString(R.string.delete_comment_confirm)
                            ) {
                                CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                                    val success = CommentsAPI.deleteComment(comment.commentId)
                                    if (success) {
                                        snackString(R.string.comment_deleted)
                                        parentSection.remove(this@CommentItem)
                                    }
                                }
                            }
                            true
                        }

                        R.id.commentBanUser -> {
                            dialogBuilder(
                                getAppString(R.string.ban_user),
                                getAppString(R.string.ban_user_confirm)
                            ) {
                                CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                                    val success = CommentsAPI.banUser(comment.userId)
                                    if (success) {
                                        snackString(R.string.user_banned)
                                    }
                                }
                            }
                            true
                        }

                        else -> {
                            false
                        }
                    }
                }
                popup.show()
            }
            //fill the icon if the user has liked the comment
            setVoteButtons(viewBinding)
            commentUpVote.setOnClickListener {
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

            commentDownVote.setOnClickListener {
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
            commentTotalVotes.text = (comment.upvotes - comment.downvotes).toString()
            commentUserAvatar.openImage(
                commentsFragment.activity.getString(R.string.avatar, comment.username),
                comment.profilePictureUrl ?: ""
            )
            comment.profilePictureUrl?.let { commentUserAvatar.loadImage(it) }
            commentUserName.text = comment.username
            val userColor = "[${levelColor.second}]"
            commentUserLevel.text = userColor
            commentUserLevel.setTextColor(levelColor.first)
            commentUserTime.text = formatTimestamp(comment.timestamp)
        }
    }

    override fun getLayout(): Int {
        return R.layout.item_comments
    }

    fun containsGif(): Boolean {
        return comment.content.contains(".gif")
    }

    override fun initializeViewBinding(view: View): ItemCommentsBinding {
        return ItemCommentsBinding.bind(view)
    }

    fun replying(isReplying: Boolean) {
        binding.commentReply.text =
            if (isReplying) commentsFragment.activity.getString(R.string.cancel) else "Reply"
        this.isReplying = isReplying
    }

    fun editing(isEditing: Boolean) {
        binding.commentEdit.text =
            if (isEditing) commentsFragment.activity.getString(R.string.cancel) else commentsFragment.activity.getString(
                R.string.edit
            )
        this.isEditing = isEditing
    }

    fun registerSubComment(id: Int) {
        subCommentIds.add(id)
    }

    private fun removeSubCommentIds() {
        subCommentIds.forEach { id ->
            @Suppress("UNCHECKED_CAST")
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

    @SuppressLint("SimpleDateFormat")
    private fun formatTimestamp(timestamp: String): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
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
        @SuppressLint("SimpleDateFormat")
        fun timestampToMillis(timestamp: String): Long {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val parsedDate = dateFormat.parse(timestamp)
            return parsedDate?.time ?: 0
        }
    }

    private fun getAvatarColor(voteCount: Int, backgroundColor: Int): Pair<Int, Int> {
        val level = if (voteCount < 0) 0 else sqrt(abs(voteCount.toDouble()) / 0.8).toInt()
        val colorString =
            if (level > usernameColors.size - 1) usernameColors[usernameColors.size - 1] else usernameColors[level]
        var color = Color.parseColor(colorString)
        val ratio = getContrastRatio(color, backgroundColor)
        if (ratio < 4.5) {
            color = adjustColorForContrast(color, backgroundColor)
        }

        return Pair(color, level)
    }

    /**
     * Builds the dialog for yes/no confirmation
     * no doesn't do anything, yes calls the callback
     * @param title the title of the dialog
     * @param message the message of the dialog
     * @param callback the callback to call when the user clicks yes
     */
    private fun dialogBuilder(title: String, message: String, callback: () -> Unit) {
        commentsFragment.activity.customAlertDialog().apply {
            setTitle(title)
            setMessage(message)
            setPosButton("Yes") {
                callback()
            }
            setNegButton("No") {}
        }.show()
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