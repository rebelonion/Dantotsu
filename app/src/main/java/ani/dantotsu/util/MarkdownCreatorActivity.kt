package ani.dantotsu.util

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import ani.dantotsu.R
import ani.dantotsu.buildMarkwon
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.ActivityMarkdownCreatorBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import tachiyomi.core.util.lang.launchIO

class MarkdownCreatorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMarkdownCreatorBinding
    private lateinit var type: String
    private var text: String = ""
    var ping: String? = null
    private var parentId: Int = 0
    private var isPreviewMode: Boolean = false

    enum class MarkdownFormat(val syntax: String, val selectionOffset: Int) {
        BOLD("**", 2),
        ITALIC("*", 1),
        STRIKETHROUGH("~~", 2),
        SPOILER("||", 2),
        LINK("[link](url)", 6),
        IMAGE("![alt text](image url)", 11),
        YOUTUBE("[![YouTube](thumbnail)](video url)", 27),
        VIDEO("[video](url)", 7),
        ORDERED_LIST("1. ", 3),
        UNORDERED_LIST("- ", 2),
        HEADING("# ", 2),
        CENTERED("-> <-", 3),
        QUOTE("> ", 2),
        CODE("`", 1);
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityMarkdownCreatorBinding.inflate(layoutInflater)
        binding.markdownCreatorToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
        }
        binding.markdownOptionsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin += navBarHeight
        }
        setContentView(binding.root)

        val params = binding.createButton.layoutParams as ViewGroup.MarginLayoutParams
        params.marginEnd = 16 * resources.displayMetrics.density.toInt()
        binding.createButton.layoutParams = params

        if (intent.hasExtra("type")) {
            type = intent.getStringExtra("type")!!
        } else {
            toast("Error: No type")
            finish()
            return
        }
        binding.markdownCreatorTitle.text = when (type) {
            "activity" -> getString(R.string.create_new_activity)
            "review" -> getString(R.string.create_new_review)
            "replyActivity" -> {
                parentId = intent.getIntExtra("parentId", -1)
                if (parentId == -1) {
                    toast("Error: No parent ID")
                    finish()
                    return
                }
                getString(R.string.create_new_reply)
            }

            else -> ""
        }
        ping = intent.getStringExtra("other")
        text = ping ?: ""
        binding.editText.setText(text)
        binding.editText.addTextChangedListener {
            if (!isPreviewMode) {
                text = it.toString()
            }
        }
        previewMarkdown(false)

        binding.markdownCreatorBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.createButton.setOnClickListener {
            if (text.isBlank()) {
                toast(getString(R.string.cannot_be_empty))
                return@setOnClickListener
            }
            AlertDialog.Builder(this).apply {
                setTitle(R.string.warning)
                setMessage(R.string.post_to_anilist_warning)
                setPositiveButton(R.string.ok) { _, _ ->
                    launchIO {
                        val editId = intent.getIntExtra("edit", -1)
                        val isEdit = editId != -1
                        val success = when (type) {
                            "activity" -> if (isEdit) {
                                Anilist.mutation.postActivity(text, editId)
                            } else {
                                Anilist.mutation.postActivity(text)
                            }
                            //"review" -> Anilist.mutation.postReview(text)
                            "replyActivity" -> if (isEdit) {
                                Anilist.mutation.postReply(parentId, text, editId)
                            } else {
                                Anilist.mutation.postReply(parentId, text)
                            }

                            else -> "Error: Unknown type"
                        }
                        toast(success)
                        finish()
                    }
                }
                setNeutralButton(R.string.open_rules) { _, _ ->
                    openLinkInBrowser("https://anilist.co/forum/thread/14")
                }
                setNegativeButton(R.string.cancel, null)
            }.show()
        }

        binding.createButton.setOnLongClickListener {
            isPreviewMode = !isPreviewMode
            previewMarkdown(isPreviewMode)
            if (isPreviewMode) {
                toast("Preview enabled")
            } else {
                toast("Preview disabled")
            }
            true
        }
        binding.editText.requestFocus()
        setupMarkdownButtons()
    }

    private fun setupMarkdownButtons() {
        binding.formatBold.setOnClickListener { applyMarkdownFormat(MarkdownFormat.BOLD) }
        binding.formatItalic.setOnClickListener { applyMarkdownFormat(MarkdownFormat.ITALIC) }
        binding.formatStrikethrough.setOnClickListener { applyMarkdownFormat(MarkdownFormat.STRIKETHROUGH) }
    }

    private fun applyMarkdownFormat(format: MarkdownFormat) {
        val start = binding.editText.selectionStart
        val end = binding.editText.selectionEnd

        if (start == end) {
            binding.editText.text?.insert(start, format.syntax)
            binding.editText.setSelection(start + format.selectionOffset)
        } else {
            binding.editText.text?.insert(end, format.syntax)
            binding.editText.text?.insert(start, format.syntax)
            binding.editText.setSelection(end + format.syntax.length, end + format.syntax.length)
        }
    }

    private fun previewMarkdown(preview: Boolean) {
        val markwon = buildMarkwon(this, false, anilist = true)
        if (preview) {
            binding.editText.isEnabled = false
            markwon.setMarkdown(binding.editText, text)
        } else {
            binding.editText.setText(text)
            binding.editText.isEnabled = true
            val markwonEditor = MarkwonEditor.create(markwon)
            binding.editText.addTextChangedListener(
                MarkwonEditorTextWatcher.withProcess(markwonEditor)
            )
        }
    }
}