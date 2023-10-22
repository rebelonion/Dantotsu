package ani.dantotsu.connections.discord

import android.annotation.SuppressLint
import android.app.Application.getProcessName
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import ani.dantotsu.R
import ani.dantotsu.connections.discord.Discord.saveToken
import ani.dantotsu.startMainActivity

class Login : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url != null && url.endsWith("/app")) {
                    webView.stopLoading()
                    webView.evaluateJavascript("""
                        (function() {
                            const wreq = webpackChunkdiscord_app.push([[Symbol()], {}, w => w])
                            webpackChunkdiscord_app.pop()
                            const token = Object.values(wreq.c).find(m => m.exports?.Z?.getToken).exports.Z.getToken();
                            return token;
                        })()
                    """.trimIndent()){
                        login(it.trim('"'))
                    }
                }
            }
        }
        webView.loadUrl("https://discord.com/login")
    }

    private fun login(token: String) {
        finish()
        saveToken(this, token)
        startMainActivity(this@Login)
    }

}
