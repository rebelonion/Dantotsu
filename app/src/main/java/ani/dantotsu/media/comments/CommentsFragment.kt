package ani.dantotsu.media.comments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.connections.comments.Comment
import ani.dantotsu.connections.comments.CommentsAPI
import ani.dantotsu.databinding.FragmentCommentsBinding
import ani.dantotsu.databinding.ItemCommentsBinding
import ani.dantotsu.loadImage
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.GroupieViewHolder
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

class CommentsFragment : AppCompatActivity(){
    lateinit var binding: FragmentCommentsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        binding = FragmentCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
            //get the media id from the intent
        val mediaId = intent.getIntExtra("mediaId", -1)
        if (mediaId == -1) {
            snackString("Invalid Media ID")
            finish()
        }

        val adapter = GroupieAdapter()
        val section = Section()
        val markwon = buildMarkwon()

        binding.commentsLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }
        binding.commentUserAvatar.loadImage(Anilist.avatar)
        binding.commentTitle.text = "Work in progress"
        val markwonEditor = MarkwonEditor.create(markwon)
        binding.commentInput.addTextChangedListener(MarkwonEditorTextWatcher.withProcess(markwonEditor))
        binding.commentSend.setOnClickListener {
            if (CommentsAPI.isBanned) {
                snackString("You are banned from commenting :(")
                return@setOnClickListener
            }
            binding.commentInput.text.toString().let {
                if (it.isNotEmpty()) {
                    binding.commentInput.text.clear()
                    lifecycleScope.launch {
                        val success = withContext(Dispatchers.IO) {
                            CommentsAPI.comment(mediaId, null, it)
                        }
                        if (success != null)
                            section.add(CommentItem(success, buildMarkwon(), section))
                    }
                } else {
                    snackString("Comment cannot be empty")
                }
            }
        }
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

        binding.commentsList.adapter = adapter
        binding.commentsList.layoutManager = LinearLayoutManager(this)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val comments = CommentsAPI.getCommentsForId(mediaId)
                comments?.comments?.forEach {
                    withContext(Dispatchers.Main) {
                        section.add(CommentItem(it, buildMarkwon(), section))
                    }
                }
            }
            adapter.add(section)
        }
        binding.commentSort.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.setOnMenuItemClickListener { item ->
                true
            }
            popup.inflate(R.menu.comments_sort_menu)
            popup.show()
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

                private val requestManager: RequestManager = Glide.with(this@CommentsFragment).apply {
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