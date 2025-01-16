package ani.dantotsu.others

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.updateLayoutParams
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityCrashBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import eu.kanade.tachiyomi.util.system.copyToClipboard
import java.io.File


class CrashActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCrashBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        binding = ActivityCrashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }
        val stackTrace = intent.getStringExtra("stackTrace") ?: "No stack trace available"

        binding.crashReportView.setText(stackTrace)
        binding.crashReportView.setOnKeyListener(View.OnKeyListener { _, _, _ ->
            true // Blocks input from hardware keyboards.
        })

        binding.copyButton.setOnClickListener {
            copyToClipboard("Crash log", stackTrace)
        }

        binding.shareAsTextFileButton.setOnClickListener {
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