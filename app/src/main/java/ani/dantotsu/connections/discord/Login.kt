package ani.dantotsu.connections.discord

import android.annotation.SuppressLint
import android.app.Application.getProcessName
import android.os.Build
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ani.dantotsu.R
import ani.dantotsu.connections.discord.Discord.saveToken
import ani.dantotsu.startMainActivity
import ani.dantotsu.themes.ThemeManager

class Login : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val process = getProcessName()
            if (packageName != process) WebView.setDataDirectorySuffix(process)
        }
        setContentView(R.layout.activity_discord)

        val webView = findViewById<WebView>(R.id.discordWebview)

        webView.apply {
            settings.javaScriptEnabled = true
            settings.databaseEnabled = true
            settings.domStorageEnabled = true
        }
        WebView.setWebContentsDebuggingEnabled(true)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                // Check if the URL is the one expected after a successful login
                if (request?.url.toString() != "https://discord.com/login") {
                    // Delay the script execution to ensure the page is fully loaded
                    view?.postDelayed({
                        view.evaluateJavascript(
                            """
                    (function() {
                        const wreq = (webpackChunkdiscord_app.push([[''],{},e=>{m=[];for(let c in e.c)m.push(e.c[c])}]),m).find(m=>m?.exports?.default?.getToken!==void 0).exports.default.getToken();
                        return wreq;
                    })()
                """.trimIndent()
                        ) { result ->
                            login(result.trim('"'))
                        }
                    }, 2000)
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }

        webView.loadUrl("https://discord.com/login")
    }

    private fun login(token: String) {
        if (token.isEmpty() || token == "null") {
            Toast.makeText(this, "Failed to retrieve token", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Toast.makeText(this, "Logged in successfully", Toast.LENGTH_SHORT).show()
        finish()
        saveToken(token)
        startMainActivity(this@Login)
    }

}
