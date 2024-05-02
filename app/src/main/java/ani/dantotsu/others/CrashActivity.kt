package ani.dantotsu.others

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import ani.dantotsu.R
import eu.kanade.tachiyomi.util.system.copyToClipboard
import java.io.File


class CrashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)

        val stackTrace = intent.getStringExtra("stackTrace") ?: "No stack trace available"
        val reportView = findViewById<EditText>(R.id.crashReportView)
        reportView.setText(stackTrace)
        reportView.setOnKeyListener(View.OnKeyListener { _, _, _ ->
            true // Blocks input from hardware keyboards.
        })

        val copyButton = findViewById<Button>(R.id.copyButton)
        copyButton.setOnClickListener {
            copyToClipboard("Crash log", stackTrace)
        }

        val shareAsTextFileButton = findViewById<Button>(R.id.shareAsTextFileButton)
        shareAsTextFileButton.setOnClickListener {
            shareAsTextFile(stackTrace)
        }
    }

    private fun shareAsTextFile(stackTrace: String) {
        val file = File(cacheDir, "crash_log.txt")
        file.writeText(stackTrace)
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }
}