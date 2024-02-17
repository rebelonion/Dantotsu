package ani.dantotsu.media.comments

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
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.glide.GlideImagesPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.TimeZone

class CommentsActivity : AppCompatActivity() {
    lateinit var binding: ActivityCommentsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initActivity(this)
        //get the media id from the intent
        val mediaId = intent.getIntExtra("mediaId", -1)
        if (mediaId == -1) {
            snackString("Invalid Media ID")
            finish()
        }

        val adapter = GroupieAdapter()
        val section = Section()
        val markwon = buildMarkwon()

        binding.commentUserAvatar.loadImage(Anilist.avatar)
        binding.commentTitle.text = getText(R.string.comments)
        val markwonEditor = MarkwonEditor.create(markwon)
        binding.commentInput.addTextChangedListener(
            MarkwonEditorTextWatcher.withProcess(
                markwonEditor
            )
        )
        binding.commentReplyToContainer.visibility = View.GONE //TODO: implement reply
        var editing = false
        var editingCommentId = -1
        fun editCallback(comment: CommentItem) {
            if (editingCommentId == comment.comment.commentId) {
                editing = false
                editingCommentId = -1
                binding.commentInput.setText("")
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.commentInput.windowToken, 0)
            } else {
                editing = true
                editingCommentId = comment.comment.commentId
                binding.commentInput.setText(comment.comment.content)
                binding.commentInput.requestFocus()
                binding.commentInput.setSelection(binding.commentInput.text.length)
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.commentInput, InputMethodManager.SHOW_IMPLICIT)
            }

        }

        binding.commentsRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                binding.commentsList.visibility = View.GONE
                adapter.clear()
                section.clear()
                withContext(Dispatchers.IO) {
                    val comments = CommentsAPI.getCommentsForId(mediaId)
                    val sorted = when (PrefManager.getVal(PrefName.CommentSortOrder, "newest")) {
                        "newest" -> comments?.comments?.sortedByDescending { comment ->
                            CommentItem.timestampToMillis(comment.timestamp)
                        }

                        "oldest" -> comments?.comments?.sortedBy { comment ->
                            CommentItem.timestampToMillis(comment.timestamp)
                        }

                        "highest_rated" -> comments?.comments?.sortedByDescending { comment ->
                            comment.upvotes - comment.downvotes
                        }

                        "lowest_rated" -> comments?.comments?.sortedBy { comment ->
                            comment.upvotes - comment.downvotes
                        }

                        else -> comments?.comments
                    }
                    sorted?.forEach {
                        withContext(Dispatchers.Main) {
                            section.add(CommentItem(it, buildMarkwon(), section) { comment ->
                                editCallback(comment)
                            })
                        }
                    }
                }
                adapter.add(section)
                binding.commentsList.visibility = View.VISIBLE
                binding.commentsRefresh.isRefreshing = false
            }
        }

        var pagesLoaded = 1
        var totalPages = 1
        binding.commentsList.adapter = adapter
        binding.commentsList.layoutManager = LinearLayoutManager(this)
        lifecycleScope.launch {
            binding.commentsProgressBar.visibility = View.VISIBLE
            binding.commentsList.visibility = View.GONE
            withContext(Dispatchers.IO) {
                val comments = CommentsAPI.getCommentsForId(mediaId)
                val sorted = when (PrefManager.getVal(PrefName.CommentSortOrder, "newest")) {
                    "newest" -> comments?.comments?.sortedByDescending { comment ->
                        CommentItem.timestampToMillis(comment.timestamp)
                    }

                    "oldest" -> comments?.comments?.sortedBy { comment ->
                        CommentItem.timestampToMillis(comment.timestamp)
                    }

                    "highest_rated" -> comments?.comments?.sortedByDescending { comment ->
                        comment.upvotes - comment.downvotes
                    }

                    "lowest_rated" -> comments?.comments?.sortedBy { comment ->
                        comment.upvotes - comment.downvotes
                    }

                    else -> comments?.comments
                }
                sorted?.forEach {
                    withContext(Dispatchers.Main) {
                        section.add(CommentItem(it, buildMarkwon(), section) { comment ->
                            editCallback(comment)
                        })
                    }
                }
                totalPages = comments?.totalPages ?: 1
            }
            binding.commentsProgressBar.visibility = View.GONE
            binding.commentsList.visibility = View.VISIBLE
            adapter.add(section)
        }

        binding.commentSort.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.comment_sort_newest -> {
                        PrefManager.setVal(PrefName.CommentSortOrder, "newest")
                        val groups = section.groups
                        groups.sortByDescending { comment ->
                            comment as CommentItem
                            CommentItem.timestampToMillis(comment.comment.timestamp)
                        }
                        section.update(groups)
                        binding.commentsList.scrollToPosition(0)
                    }

                    R.id.comment_sort_oldest -> {
                        PrefManager.setVal(PrefName.CommentSortOrder, "oldest")
                        val groups = section.groups
                        groups.sortBy { comment ->
                            comment as CommentItem
                            CommentItem.timestampToMillis(comment.comment.timestamp)
                        }
                        section.update(groups)
                        binding.commentsList.scrollToPosition(0)
                    }

                    R.id.comment_sort_highest_rated -> {
                        PrefManager.setVal(PrefName.CommentSortOrder, "highest_rated")
                        val groups = section.groups
                        groups.sortByDescending { comment ->
                            comment as CommentItem
                            comment.comment.upvotes - comment.comment.downvotes
                        }
                        section.update(groups)
                        binding.commentsList.scrollToPosition(0)
                    }

                    R.id.comment_sort_lowest_rated -> {
                        PrefManager.setVal(PrefName.CommentSortOrder, "lowest_rated")
                        val groups = section.groups
                        groups.sortBy { comment ->
                            comment as CommentItem
                            comment.comment.upvotes - comment.comment.downvotes
                        }
                        section.update(groups)
                        binding.commentsList.scrollToPosition(0)
                    }
                }
                true
            }
            popup.inflate(R.menu.comments_sort_menu)
            popup.show()
        }

        var fetching = false
        //if we have scrolled to the bottom of the list, load more comments
        binding.commentsList.addOnScrollListener(object :
            androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                dx: Int,
                dy: Int
            ) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    if (pagesLoaded < totalPages && !fetching) {
                        fetching = true
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                val comments =
                                    CommentsAPI.getCommentsForId(mediaId, pagesLoaded + 1)
                                comments?.comments?.forEach {
                                    withContext(Dispatchers.Main) {
                                        section.add(
                                            CommentItem(
                                                it,
                                                buildMarkwon(),
                                                section
                                            ) { comment ->
                                                editCallback(comment)
                                            })
                                    }
                                }
                                totalPages = comments?.totalPages ?: 1
                            }
                            pagesLoaded++
                            fetching = false
                        }
                    }
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
            val firstComment = PrefManager.getVal<Boolean>(PrefName.FirstComment)
            if (firstComment) {
                //show a dialog explaining the rules
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
                        binding.commentInput.text.toString().let {
                            if (it.isNotEmpty()) {
                                binding.commentInput.text.clear()
                                lifecycleScope.launch {
                                    if (editing) {
                                        val success = withContext(Dispatchers.IO) {
                                            CommentsAPI.editComment(editingCommentId, it)
                                        }
                                        if (success) {
                                            val groups = section.groups
                                            groups.forEach { item ->
                                                if (item is CommentItem) {
                                                    if (item.comment.commentId == editingCommentId) {
                                                        item.comment.content = it
                                                        val dateFormat =
                                                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                                        dateFormat.timeZone =
                                                            TimeZone.getTimeZone("UTC")
                                                        item.comment.timestamp =
                                                            dateFormat.format(System.currentTimeMillis())
                                                        item.notifyChanged()
                                                        snackString("Comment edited")
                                                    }
                                                }

                                            }
                                        }
                                    } else {
                                        val success = withContext(Dispatchers.IO) {
                                            CommentsAPI.comment(mediaId, null, it)
                                        }
                                        if (success != null)
                                            section.add(
                                                0,
                                                CommentItem(
                                                    success,
                                                    buildMarkwon(),
                                                    section
                                                ) { comment ->
                                                    editCallback(comment)
                                                })
                                    }
                                }
                            } else {
                                snackString("Comment cannot be empty")
                            }
                        }
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                val dialog = alertDialog.show()
                dialog?.window?.setDimAmount(0.8f)

            }
        }
    }

    private fun buildMarkwon(): Markwon {
        val markwon = Markwon.builder(this)
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(HtmlPlugin.create())
            .usePlugin(TablePlugin.create(this))
            .usePlugin(TaskListPlugin.create(this))
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