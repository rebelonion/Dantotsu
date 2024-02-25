package ani.dantotsu.media.comments

import android.graphics.drawable.ColorDrawable
import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.comments.Comment
import ani.dantotsu.connections.comments.CommentResponse
import ani.dantotsu.connections.comments.CommentsAPI
import ani.dantotsu.databinding.ActivityCommentsBinding
import ani.dantotsu.initActivity
import ani.dantotsu.loadImage
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.themes.ThemeManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.html.TagHandlerNoOp
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.glide.GlideImagesPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class CommentsActivity : AppCompatActivity() {
    lateinit var binding: ActivityCommentsBinding
    private var interactionState = InteractionState.NONE
    private var commentWithInteraction: CommentItem? = null
    private val section = Section()
    private val adapter = GroupieAdapter()
    private var mediaId: Int = -1
    var mediaName: String = ""
    private var backgroundColor: Int = 0
    var pagesLoaded = 1
    var totalPages = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initActivity(this)
        //get the media id from the intent
        val mediaId = intent.getIntExtra("mediaId", -1)
        mediaName = intent.getStringExtra("mediaName")?:"unknown"
        if (mediaId == -1) {
            snackString("Invalid Media ID")
            finish()
        }
        this.mediaId = mediaId
        backgroundColor = (binding.root.background as? ColorDrawable)?.color ?: 0

        val markwon = buildMarkwon()

        binding.commentUserAvatar.loadImage(Anilist.avatar)
        binding.commentTitle.text = getText(R.string.comments)
        val markwonEditor = MarkwonEditor.create(markwon)
        binding.commentInput.addTextChangedListener(
            MarkwonEditorTextWatcher.withProcess(
                markwonEditor
            )
        )

        binding.commentsRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                loadAndDisplayComments()
                binding.commentsRefresh.isRefreshing = false
            }
            binding.commentReplyToContainer.visibility = View.GONE
        }

        binding.commentsList.adapter = adapter
        binding.commentsList.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            loadAndDisplayComments()
        }

        binding.commentSort.setOnClickListener { view ->
            fun sortComments(sortOrder: String) {
                val groups = section.groups
                when (sortOrder) {
                    "newest" -> groups.sortByDescending { CommentItem.timestampToMillis((it as CommentItem).comment.timestamp) }
                    "oldest" -> groups.sortBy { CommentItem.timestampToMillis((it as CommentItem).comment.timestamp) }
                    "highest_rated" -> groups.sortByDescending { (it as CommentItem).comment.upvotes - it.comment.downvotes }
                    "lowest_rated" -> groups.sortBy { (it as CommentItem).comment.upvotes - it.comment.downvotes }
                }
                section.update(groups)
            }

            val popup = PopupMenu(this, view)
            popup.setOnMenuItemClickListener { item ->
                val sortOrder = when (item.itemId) {
                    R.id.comment_sort_newest -> "newest"
                    R.id.comment_sort_oldest -> "oldest"
                    R.id.comment_sort_highest_rated -> "highest_rated"
                    R.id.comment_sort_lowest_rated -> "lowest_rated"
                    else -> return@setOnMenuItemClickListener false
                }

                PrefManager.setVal(PrefName.CommentSortOrder, sortOrder)
                sortComments(sortOrder)
                binding.commentsList.scrollToPosition(0)
                true
            }
            popup.inflate(R.menu.comments_sort_menu)
            popup.show()
        }

        var isFetching = false
        //if we have scrolled to the bottom of the list, load more comments
        binding.commentsList.addOnScrollListener(object :
            androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                dx: Int,
                dy: Int
            ) {
                super.onScrolled(recyclerView, dx, dy)
                if (shouldLoadMoreComments(recyclerView)) {
                    loadMoreComments()
                }
            }

            private fun shouldLoadMoreComments(recyclerView: androidx.recyclerview.widget.RecyclerView): Boolean {
                return !recyclerView.canScrollVertically(1) && pagesLoaded < totalPages && !isFetching
            }

            private fun loadMoreComments() {
                isFetching = true
                lifecycleScope.launch {
                    val comments = fetchComments()
                    comments?.comments?.forEach { comment ->
                        updateUIWithComment(comment)
                    }
                    totalPages = comments?.totalPages ?: 1
                    pagesLoaded++
                    isFetching = false
                }
            }

            private suspend fun fetchComments(): CommentResponse? {
                return withContext(Dispatchers.IO) {
                    CommentsAPI.getCommentsForId(mediaId, pagesLoaded + 1)
                }
            }

            //adds additional comments to the section
            private suspend fun updateUIWithComment(comment: Comment) {
                withContext(Dispatchers.Main) {
                    section.add(
                        CommentItem(
                            comment,
                            buildMarkwon(),
                            section,
                            this@CommentsActivity,
                            backgroundColor,
                            0
                        )
                    )
                }
            }
        })


        binding.commentInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: android.text.Editable?) {
                if (binding.commentInput.text.length > 300) {
                    binding.commentInput.text.delete(300, binding.commentInput.text.length)
                    snackString("Comment cannot be longer than 300 characters")
                }
            }
        })
        binding.commentSend.setOnClickListener {
            if (CommentsAPI.isBanned) {
                snackString("You are banned from commenting :(")
                return@setOnClickListener
            }

            if (PrefManager.getVal(PrefName.FirstComment)) {
                showCommentRulesDialog()
            } else {
                processComment()
            }
        }
    }

    enum class InteractionState {
        NONE, EDIT, REPLY
    }

    /**
     * Loads and displays the comments
     * Called when the activity is created
     * Or when the user refreshes the comments
     */

    private suspend fun loadAndDisplayComments() {
        binding.commentsProgressBar.visibility = View.VISIBLE
        binding.commentsList.visibility = View.GONE
        adapter.clear()
        section.clear()

        val comments = withContext(Dispatchers.IO) {
            CommentsAPI.getCommentsForId(mediaId)
        }

        val sortedComments = sortComments(comments?.comments)
        sortedComments.forEach {
            withContext(Dispatchers.Main) {
                section.add(
                    CommentItem(
                        it,
                        buildMarkwon(),
                        section,
                        this@CommentsActivity,
                        backgroundColor,
                        0
                    )
                )
            }
        }

        totalPages = comments?.totalPages ?: 1
        binding.commentsProgressBar.visibility = View.GONE
        binding.commentsList.visibility = View.VISIBLE
        adapter.add(section)
    }

    private fun sortComments(comments: List<Comment>?): List<Comment> {
        if (comments == null) return emptyList()
        val sortOrder = PrefManager.getVal(PrefName.CommentSortOrder, "newest")
        return when (sortOrder) {
            "newest" -> comments.sortedByDescending { CommentItem.timestampToMillis(it.timestamp) }
            "oldest" -> comments.sortedBy { CommentItem.timestampToMillis(it.timestamp) }
            "highest_rated" -> comments.sortedByDescending { it.upvotes - it.downvotes }
            "lowest_rated" -> comments.sortedBy { it.upvotes - it.downvotes }
            else -> comments
        }
    }

    /**
     * Resets the old state of the comment input
     * @return the old state
     */
    private fun resetOldState(): InteractionState {
        val oldState = interactionState
        interactionState = InteractionState.NONE
        return when (oldState) {
            InteractionState.EDIT -> {
                binding.commentReplyToContainer.visibility = View.GONE
                binding.commentInput.setText("")
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.commentInput.windowToken, 0)
                commentWithInteraction?.editing(false)
                InteractionState.EDIT
            }

            InteractionState.REPLY -> {
                binding.commentInput.setText("")
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.commentInput.windowToken, 0)
                commentWithInteraction?.replying(false)
                InteractionState.REPLY
            }

            else -> {
                InteractionState.NONE
            }
        }
    }

    /**
     * Callback from the comment item to edit the comment
     * Called every time the edit button is clicked
     * @param comment the comment to edit
     */
    fun editCallback(comment: CommentItem) {
        if (resetOldState() == InteractionState.EDIT) return
        commentWithInteraction = comment
        binding.commentInput.setText(comment.comment.content)
        binding.commentInput.requestFocus()
        binding.commentInput.setSelection(binding.commentInput.text.length)
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.commentInput, InputMethodManager.SHOW_IMPLICIT)
        interactionState = InteractionState.EDIT
    }

    /**
     * Callback from the comment item to reply to the comment
     * Called every time the reply button is clicked
     * @param comment the comment to reply to
     */
    fun replyCallback(comment: CommentItem) {
        if (resetOldState() == InteractionState.REPLY) return
        commentWithInteraction = comment
        binding.commentReplyToContainer.visibility = View.VISIBLE
        binding.commentInput.requestFocus()
        binding.commentInput.setSelection(binding.commentInput.text.length)
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.commentInput, InputMethodManager.SHOW_IMPLICIT)
        interactionState = InteractionState.REPLY

    }
    @SuppressLint("SetTextI18n")
    fun replyTo(comment: CommentItem, username: String) {
        if (comment.isReplying) {
            binding.commentReplyToContainer.visibility = View.VISIBLE
            binding.commentReplyTo.text = "Replying to $username"
            binding.commentReplyToCancel.setOnClickListener {
                comment.replying(false)
                replyCallback(comment)
                binding.commentReplyToContainer.visibility = View.GONE
            }
        } else {
            binding.commentReplyToContainer.visibility = View.GONE
        }
    }

    /**
     * Callback from the comment item to view the replies to the comment
     * @param comment the comment to view the replies of
     */
    fun viewReplyCallback(comment: CommentItem) {
        lifecycleScope.launch {
            val replies = withContext(Dispatchers.IO) {
                CommentsAPI.getRepliesFromId(comment.comment.commentId)
            }

            replies?.comments?.forEach {
                val depth = if (comment.commentDepth + 1 > comment.MAX_DEPTH) comment.commentDepth else comment.commentDepth + 1
                val section = if (comment.commentDepth + 1 > comment.MAX_DEPTH) comment.parentSection else comment.repliesSection
                if (depth >= comment.MAX_DEPTH) comment.registerSubComment(it.commentId)
                val newCommentItem = CommentItem(
                    it,
                    buildMarkwon(),
                    section,
                    this@CommentsActivity,
                    backgroundColor,
                    depth
                )

                section.add(newCommentItem)
            }
        }
    }


    /**
     * Shows the comment rules dialog
     * Called when the user tries to comment for the first time
     */
    private fun showCommentRulesDialog() {
        val alertDialog = android.app.AlertDialog.Builder(this, R.style.MyPopup)
            .setTitle("Commenting Rules")
            .setMessage(
                "I WILL BAN YOU WITHOUT HESITATION\n" +
                        "1. No racism\n" +
                        "2. No hate speech\n" +
                        "3. No spam\n" +
                        "4. No NSFW content\n" +
                        "6. ENGLISH ONLY\n" +
                        "7. No self promotion\n" +
                        "8. No impersonation\n" +
                        "9. No harassment\n" +
                        "10. No illegal content\n" +
                        "11. Anything you know you shouldn't comment\n"
            )
            .setPositiveButton("I Understand") { dialog, _ ->
                dialog.dismiss()
                PrefManager.setVal(PrefName.FirstComment, false)
                processComment()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = alertDialog.show()
        dialog?.window?.setDimAmount(0.8f)
    }

    private fun processComment() {
        val commentText = binding.commentInput.text.toString()
        if (commentText.isEmpty()) {
            snackString("Comment cannot be empty")
            return
        }

        binding.commentInput.text.clear()
        lifecycleScope.launch {
            if (interactionState == InteractionState.EDIT) {
                handleEditComment(commentText)
            } else {
                handleNewComment(commentText)
            }
        }
    }

    private suspend fun handleEditComment(commentText: String) {
        val success = withContext(Dispatchers.IO) {
            CommentsAPI.editComment(
                commentWithInteraction?.comment?.commentId ?: return@withContext false, commentText
            )
        }
        if (success) {
            updateCommentInSection(commentText)
        }
    }

    private fun updateCommentInSection(commentText: String) {
        val groups = section.groups
        groups.forEach { item ->
            if (item is CommentItem && item.comment.commentId == commentWithInteraction?.comment?.commentId) {
                updateCommentItem(item, commentText)
                snackString("Comment edited")
            }
        }
    }

    private fun updateCommentItem(item: CommentItem, commentText: String) {
        item.comment.content = commentText
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        item.comment.timestamp = dateFormat.format(System.currentTimeMillis())
        item.notifyChanged()
    }

    /**
     * Handles the new user-added comment
     * @param commentText the text of the comment
     */

    private suspend fun handleNewComment(commentText: String) {
        val success = withContext(Dispatchers.IO) {
            CommentsAPI.comment(
                mediaId,
                if (interactionState == InteractionState.REPLY) commentWithInteraction?.comment?.commentId else null,
                commentText
            )
        }
        success?.let {
            if (interactionState == InteractionState.REPLY) {
                if (commentWithInteraction == null) return@let
                val section = if (commentWithInteraction!!.commentDepth + 1 > commentWithInteraction!!.MAX_DEPTH) commentWithInteraction?.parentSection else commentWithInteraction?.repliesSection
                val depth = if (commentWithInteraction!!.commentDepth + 1 > commentWithInteraction!!.MAX_DEPTH) commentWithInteraction!!.commentDepth else commentWithInteraction!!.commentDepth + 1
                if (depth >= commentWithInteraction!!.MAX_DEPTH) commentWithInteraction!!.registerSubComment(it.commentId)
                section?.add(
                    if (commentWithInteraction!!.commentDepth + 1 > commentWithInteraction!!.MAX_DEPTH) 0 else section.itemCount,
                    CommentItem(
                        it,
                        buildMarkwon(),
                        section,
                        this@CommentsActivity,
                        backgroundColor,
                        depth
                    )
                )
            } else {
                section.add(
                    0,
                    CommentItem(it, buildMarkwon(), section, this@CommentsActivity, backgroundColor, 0)
                )
            }
        }
    }

    /**
     * Builds the markwon instance with all the plugins
     * @return the markwon instance
     */
    private fun buildMarkwon(): Markwon {
        val markwon = Markwon.builder(this)
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(this))
            .usePlugin(TaskListPlugin.create(this))
            .usePlugin(HtmlPlugin.create { plugin ->
                plugin.addHandler(
                    TagHandlerNoOp.create("h1", "h2", "h3", "h4", "h5", "h6", "hr", "pre")
                )
            })
            .usePlugin(GlideImagesPlugin.create(object : GlideImagesPlugin.GlideStore {

                private val requestManager: RequestManager =
                    Glide.with(this@CommentsActivity).apply {
                        addDefaultRequestListener(object : RequestListener<Any> {
                            override fun onResourceReady(
                                resource: Any,
                                model: Any,
                                target: Target<Any>,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                if (resource is GifDrawable) {
                                    resource.start()
                                }
                                return false
                            }

                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Any>,
                                isFirstResource: Boolean
                            ): Boolean {
                                return false
                            }
                        })
                    }

                override fun load(drawable: AsyncDrawable): RequestBuilder<Drawable> {
                    return requestManager.load(drawable.destination)
                }

                override fun cancel(target: Target<*>) {
                    requestManager.clear(target)
                }
            }))
            .build()
        return markwon
    }
}