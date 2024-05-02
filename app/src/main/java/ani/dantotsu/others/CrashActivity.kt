package ani.dantotsu.others

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import ani.dantotsu.R
import eu.kanade.tachiyomi.util.system.copyToClipboard


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
    }
}