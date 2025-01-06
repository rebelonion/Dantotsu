package ani.dantotsu.util

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import ani.dantotsu.R
import ani.dantotsu.buildMarkwon
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.ActivityMarkdownCreatorBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.openLinkInBrowser
import ani.dantotsu.others.AndroidBug5497Workaround
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import tachiyomi.core.util.lang.launchIO

class ActivityMarkdownCreator : AppCompatActivity() {
    private lateinit var binding: ActivityMarkdownCreatorBinding
    private lateinit var type: String
    private var text: String = ""
    private var ping: String? = null
    private var parentId: Int = 0
    private var isPreviewMode: Boolean = false

    enum class MarkdownFormat(
        val syntax: String,
        val selectionOffset: Int,
        val imageViewId: Int
    ) {
        BOLD("****", 2, R.id.formatBold),
        ITALIC("**", 1, R.id.formatItalic),
        STRIKETHROUGH("~~~~", 2, R.id.formatStrikethrough),
        SPOILER("~!!~", 2, R.id.formatSpoiler),
        LINK("[Placeholder](%s)", 0, R.id.formatLink),
        IMAGE("img(%s)", 0, R.id.formatImage),
        YOUTUBE("youtube(%s)", 0, R.id.formatYoutube),
        VIDEO("webm(%s)", 0, R.id.formatVideo),
        ORDERED_LIST("1. ", 3, R.id.formatListOrdered),
        UNORDERED_LIST("- ", 2, R.id.formatListUnordered),
        HEADING("# ", 2, R.id.formatTitle),
        CENTERED("~~~~~~", 3, R.id.formatCenter),
        QUOTE("> ", 2, R.id.formatQuote),
        CODE("``", 1, R.id.formatCode)
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
        AndroidBug5497Workaround.assistActivity(this) {}

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
        val editId = intent.getIntExtra("edit", -1)
        val userId = intent.getIntExtra("userId", -1)
        parentId = intent.getIntExtra("parentId", -1)
        when (type) {
            "replyActivity" -> if (parentId == -1) {
                toast("Error: No parent ID")
                finish()
                return
            }

            "message" -> {
                if (editId == -1) {
                    binding.privateCheckbox.visibility = ViewGroup.VISIBLE
                }
            }
        }
        var private = false
        binding.privateCheckbox.setOnCheckedChangeListener { _, isChecked ->
            private = isChecked
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
            customAlertDialog().apply {
                setTitle(R.string.warning)
                setMessage(R.string.post_to_anilist_warning)
                setPosButton(R.string.ok) {
                    launchIO {
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

                            "message" -> if (isEdit) {
                                Anilist.mutation.postMessage(userId, text, editId)
                            } else {
                                Anilist.mutation.postMessage(userId, text, isPrivate = private)
                            }

                            else -> "Error: Unknown type"
                        }
                        toast(success)
                        finish()
                    }
                }
                setNeutralButton(R.string.open_rules) {
                    openLinkInBrowser("https://anilist.co/forum/thread/14")
                }
                setNegButton(R.string.cancel)
                show()
            }
        }

        binding.previewCheckbox.setOnClickListener {
            isPreviewMode = !isPreviewMode
            previewMarkdown(isPreviewMode)
            if (isPreviewMode) {
                toast("Preview enabled")
            } else {
                toast("Preview disabled")
            }
        }
        binding.editText.requestFocus()
        setupMarkdownButtons()
    }

    private fun setupMarkdownButtons() {
        MarkdownFormat.entries.forEach { format ->
            findViewById<ImageView>(format.imageViewId)?.setOnClickListener {
                applyMarkdownFormat(format)
            }
        }
    }

    private fun applyMarkdownFormat(format: MarkdownFormat) {
        val start = binding.editText.selectionStart
        val end = binding.editText.selectionEnd

        if (start != end) {
            val selectedText = binding.editText.text?.substring(start, end) ?: ""
            val lines = selectedText.split("\n")

            val newText = when (format) {
                MarkdownFormat.UNORDERED_LIST -> {
                    lines.joinToString("\n") { "- $it" }
                }

                MarkdownFormat.ORDERED_LIST -> {
                    lines.mapIndexed { index, line -> "${index + 1}. $line" }.joinToString("\n")
                }

                else -> {
                    if (format.syntax.contains("%s")) {
                        String.format(format.syntax, selectedText)
                    } else {
                        format.syntax.substring(0, format.selectionOffset) +
                                selectedText +
                                format.syntax.substring(format.selectionOffset)
                    }
                }
            }

            binding.editText.text?.replace(start, end, newText)
            binding.editText.setSelection(start + newText.length)
        } else {
            if (format.syntax.contains("%s")) {
                showInputDialog(format, start)
            } else {
                val newText = format.syntax
                binding.editText.text?.insert(start, newText)
                binding.editText.setSelection(start + format.selectionOffset)
            }
        }
    }


    private fun showInputDialog(format: MarkdownFormat, position: Int) {
        val inputLayout = TextInputLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            isHintEnabled = true
        }

        val inputEditText = TextInputEditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        inputLayout.addView(inputEditText)

        val container = FrameLayout(this).apply {
            addView(inputLayout)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(0, 0, 0, 0)
        }
        customAlertDialog().apply {
            setTitle("Paste your link here")
            setCustomView(container)
            setPosButton(getString(R.string.ok)) {
                val input = inputEditText.text.toString()
                val formattedText = String.format(format.syntax, input)
                binding.editText.text?.insert(position, formattedText)
                binding.editText.setSelection(position + formattedText.length)
            }
            setNegButton(getString(R.string.cancel))
        }.show()

        inputEditText.requestFocus()
    }

    private fun previewMarkdown(preview: Boolean) {
        val markwon = buildMarkwon(this, false, anilist = true)
        if (preview) {
            binding.editText.isVisible = false
            binding.editText.isEnabled = false
            binding.markdownPreview.isVisible = true
            markwon.setMarkdown(binding.markdownPreview, AniMarkdown.getBasicAniHTML(text))
        } else {
            binding.editText.isVisible = true
            binding.markdownPreview.isVisible = false
            binding.editText.setText(text)
            binding.editText.isEnabled = true
            val markwonEditor = MarkwonEditor.create(markwon)
            binding.editText.addTextChangedListener(
                MarkwonEditorTextWatcher.withProcess(markwonEditor)
            )
        }
    }
}