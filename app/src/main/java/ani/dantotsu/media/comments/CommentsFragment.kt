package ani.dantotsu.media.comments

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.animation.doOnEnd
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.comments.Comment
import ani.dantotsu.connections.comments.CommentResponse
import ani.dantotsu.connections.comments.CommentsAPI
import ani.dantotsu.databinding.FragmentCommentsBinding
import ani.dantotsu.loadImage
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
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
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
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

class CommentsFragment : Fragment() {
    lateinit var binding: FragmentCommentsBinding
    lateinit var activity: AppCompatActivity
    private var interactionState = InteractionState.NONE
    private var commentWithInteraction: CommentItem? = null
    private val section = Section()
    private val adapter = GroupieAdapter()
    private var tag: Int? = null
    private var filterTag: Int? = null
    private var mediaId: Int = -1
    var mediaName: String = ""
    private var backgroundColor: Int = 0
    var pagesLoaded = 1
    var totalPages = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity = requireActivity() as AppCompatActivity
        (activity as? MediaDetailsActivity)?.binding?.mediaAppBar?.setExpanded(false, true)
        //get the media id from the intent
        val mediaId = arguments?.getInt("mediaId") ?: -1
        mediaName = arguments?.getString("mediaName") ?: "unknown"
        if (mediaId == -1) {
            snackString("Invalid Media ID")
            return
        }
        this.mediaId = mediaId
        backgroundColor = (binding.root.background as? ColorDrawable)?.color ?: 0

        val markwon = buildMarkwon()

        binding.commentUserAvatar.loadImage(Anilist.avatar)
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
        binding.commentsList.layoutManager = LinearLayoutManager(activity)

        lifecycleScope.launch {
            val commentId = arguments?.getInt("commentId")
            if (commentId != null && commentId > 0) {
                loadSingleComment(commentId)
            } else {
                loadAndDisplayComments()
            }
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

            val popup = PopupMenu(activity, view)
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

        binding.commentFilter.setOnClickListener {
            val alertDialog = AlertDialog.Builder(activity, R.style.MyPopup)
                .setTitle("Enter a chapter/episode number tag")
                .setView(R.layout.dialog_edittext)
                .setPositiveButton("OK") { dialog, _ ->
                    val editText =
                        (dialog as AlertDialog).findViewById<EditText>(R.id.dialogEditText)
                    val text = editText?.text.toString()
                    filterTag = text.toIntOrNull()
                    lifecycleScope.launch {
                        loadAndDisplayComments()
                    }

                    dialog.dismiss()
                }
                .setNeutralButton("Clear") { dialog, _ ->
                    filterTag = null
                    lifecycleScope.launch {
                        loadAndDisplayComments()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    filterTag = null
                    dialog.dismiss()
                }
            val dialog = alertDialog.show()
            dialog?.window?.setDimAmount(0.8f)
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
                    CommentsAPI.getCommentsForId(mediaId, pagesLoaded + 1, filterTag)
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
                            this@CommentsFragment,
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

        binding.commentInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val targetWidth = binding.commentInputLayout.width -
                        binding.commentLabel.width -
                        binding.commentSend.width -
                        binding.commentUserAvatar.width - 12 + 16
                val anim = ValueAnimator.ofInt(binding.commentInput.width, targetWidth)
                anim.addUpdateListener { valueAnimator ->
                    val layoutParams = binding.commentInput.layoutParams
                    layoutParams.width = valueAnimator.animatedValue as Int
                    binding.commentInput.layoutParams = layoutParams
                }
                anim.duration = 300

                anim.start()
                anim.doOnEnd {
                    binding.commentLabel.visibility = View.VISIBLE
                    binding.commentSend.visibility = View.VISIBLE
                    binding.commentLabel.animate().translationX(0f).setDuration(300).start()
                    binding.commentSend.animate().translationX(0f).setDuration(300).start()

                }
            }

            binding.commentLabel.setOnClickListener {
                //alert dialog to enter a number, with a cancel and ok button
                val alertDialog = android.app.AlertDialog.Builder(activity, R.style.MyPopup)
                    .setTitle("Enter a chapter/episode number tag")
                    .setView(R.layout.dialog_edittext)
                    .setPositiveButton("OK") { dialog, _ ->
                        val editText =
                            (dialog as AlertDialog).findViewById<EditText>(R.id.dialogEditText)
                        val text = editText?.text.toString()
                        tag = text.toIntOrNull()
                        if (tag == null) {
                            binding.commentLabel.background = ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_label_off_24,
                                null
                            )
                        } else {
                            binding.commentLabel.background = ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.ic_label_24,
                                null
                            )
                        }
                        dialog.dismiss()
                    }
                    .setNeutralButton("Clear") { dialog, _ ->
                        tag = null
                        binding.commentLabel.background = ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.ic_label_off_24,
                            null
                        )
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        tag = null
                        binding.commentLabel.background = ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.ic_label_off_24,
                            null
                        )
                        dialog.dismiss()
                    }
                val dialog = alertDialog.show()
                dialog?.window?.setDimAmount(0.8f)
            }
        }

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

    @SuppressLint("NotifyDataSetChanged")
    override fun onStart() {
        super.onStart()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        tag = null
        adapter.notifyDataSetChanged()
        (activity as? MediaDetailsActivity)?.binding?.mediaAppBar?.setExpanded(false, true)
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
            CommentsAPI.getCommentsForId(mediaId, tag = filterTag)
        }

        val sortedComments = sortComments(comments?.comments)
        sortedComments.forEach {
            withContext(Dispatchers.Main) {
                section.add(
                    CommentItem(
                        it,
                        buildMarkwon(),
                        section,
                        this@CommentsFragment,
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

    private suspend fun loadSingleComment(commentId: Int) {
        binding.commentsProgressBar.visibility = View.VISIBLE
        binding.commentsList.visibility = View.GONE
        adapter.clear()
        section.clear()

        val comment = withContext(Dispatchers.IO) {
            CommentsAPI.getSingleComment(commentId)
        }
        if (comment != null) {
            withContext(Dispatchers.Main) {
                section.add(
                    CommentItem(
                        comment,
                        buildMarkwon(),
                        section,
                        this@CommentsFragment,
                        backgroundColor,
                        0
                    )
                )
            }
        }

        binding.commentsProgressBar.visibility = View.GONE
        binding.commentsList.visibility = View.VISIBLE
        adapter.add(section)
    }

    private fun sortComments(comments: List<Comment>?): List<Comment> {
        if (comments == null) return emptyList()
        return when (PrefManager.getVal(PrefName.CommentSortOrder, "newest")) {
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
                val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.commentInput.windowToken, 0)
                commentWithInteraction?.editing(false)
                InteractionState.EDIT
            }

            InteractionState.REPLY -> {
                binding.commentInput.setText("")
                val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
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
        val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
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
        val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
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
                val depth =
                    if (comment.commentDepth + 1 > comment.MAX_DEPTH) comment.commentDepth else comment.commentDepth + 1
                val section =
                    if (comment.commentDepth + 1 > comment.MAX_DEPTH) comment.parentSection else comment.repliesSection
                if (depth >= comment.MAX_DEPTH) comment.registerSubComment(it.commentId)
                val newCommentItem = CommentItem(
                    it,
                    buildMarkwon(),
                    section,
                    this@CommentsFragment,
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
        val alertDialog = android.app.AlertDialog.Builder(activity, R.style.MyPopup)
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
                tag = null
                binding.commentLabel.background = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_label_off_24,
                    null
                )
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
                commentText,
                tag
            )
        }
        success?.let {
            if (interactionState == InteractionState.REPLY) {
                if (commentWithInteraction == null) return@let
                val section =
                    if (commentWithInteraction!!.commentDepth + 1 > commentWithInteraction!!.MAX_DEPTH) commentWithInteraction?.parentSection else commentWithInteraction?.repliesSection
                val depth =
                    if (commentWithInteraction!!.commentDepth + 1 > commentWithInteraction!!.MAX_DEPTH) commentWithInteraction!!.commentDepth else commentWithInteraction!!.commentDepth + 1
                if (depth >= commentWithInteraction!!.MAX_DEPTH) commentWithInteraction!!.registerSubComment(
                    it.commentId
                )
                section?.add(
                    if (commentWithInteraction!!.commentDepth + 1 > commentWithInteraction!!.MAX_DEPTH) 0 else section.itemCount,
                    CommentItem(
                        it,
                        buildMarkwon(),
                        section,
                        this@CommentsFragment,
                        backgroundColor,
                        depth
                    )
                )
            } else {
                section.add(
                    0,
                    CommentItem(
                        it,
                        buildMarkwon(),
                        section,
                        this@CommentsFragment,
                        backgroundColor,
                        0
                    )
                )
            }
        }
    }

    /**
     * Builds the markwon instance with all the plugins
     * @return the markwon instance
     */
    private fun buildMarkwon(): Markwon {
        val markwon = Markwon.builder(activity)
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.linkResolver { view, link ->
                        // do nothing
                    }
                }
            })

            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(activity))
            .usePlugin(TaskListPlugin.create(activity))
            .usePlugin(HtmlPlugin.create { plugin ->
                plugin.addHandler(
                    TagHandlerNoOp.create("h1", "h2", "h3", "h4", "h5", "h6", "hr", "pre", "a")
                )
            })
            .usePlugin(GlideImagesPlugin.create(object : GlideImagesPlugin.GlideStore {

                private val requestManager: RequestManager =
                    Glide.with(this@CommentsFragment).apply {
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