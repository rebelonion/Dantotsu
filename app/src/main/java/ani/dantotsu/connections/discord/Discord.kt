package ani.dantotsu.connections.discord

import android.content.Context
import android.content.Intent
import android.widget.TextView
import ani.dantotsu.R
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.toast
import ani.dantotsu.tryWith
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import java.io.File

object Discord {

    var token: String? = null
    var userid: String? = null
    var avatar: String? = null


    fun getSavedToken(context: Context): Boolean {
        token = PrefManager.getVal(
            PrefName.DiscordToken, null as String?
        )
        return token != null
    }

    fun saveToken(context: Context, token: String) {
        PrefManager.setVal(PrefName.DiscordToken, token)
    }

    fun removeSavedToken(context: Context) {
        PrefManager.removeVal(PrefName.DiscordToken)

        tryWith(true) {
            val dir = File(context.filesDir?.parentFile, "app_webview")
            if (dir.deleteRecursively())
                toast(context.getString(R.string.discord_logout_success))
        }
    }

    private var rpc: RPC? = null


    fun warning(context: Context) = CustomBottomDialog().apply {
        title = context.getString(R.string.warning)
        val md = context.getString(R.string.discord_warning)
        addView(TextView(context).apply {
            val markWon =
                Markwon.builder(context).usePlugin(SoftBreakAddsNewLinePlugin.create()).build()
            markWon.setMarkdown(this, md)
        })

        setNegativeButton(context.getString(R.string.cancel)) {
            dismiss()
        }

        setPositiveButton(context.getString(R.string.login)) {
            dismiss()
            loginIntent(context)
        }
    }

    private fun loginIntent(context: Context) {
        val intent = Intent(context, Login::class.java)
        context.startActivity(intent)
    }

    const val application_Id = "1163925779692912771"
    const val small_Image: String =
        "mp:external/GJEe4hKzr8w56IW6ZKQz43HFVEo8pOtA_C-dJiWwxKo/https/cdn.discordapp.com/app-icons/1163925779692912771/f6b42d41dfdf0b56fcc79d4a12d2ac66.png"
}