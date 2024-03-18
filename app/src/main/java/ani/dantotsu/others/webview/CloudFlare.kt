package ani.dantotsu.others.webview

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import ani.dantotsu.FileUrl

class CloudFlare(override val location: FileUrl) : WebViewBottomDialog() {
    val cfTag = "cf_clearance"

    override var title = "Cloudflare Bypass"
    override val webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            val cookie = cookies?.getCookie(url.toString())
            if (cookie?.contains(cfTag) == true) {
                val clearance = cookie.substringAfter("$cfTag=").substringBefore(";")
                privateCallback.invoke(mapOf(cfTag to clearance))
            }
            super.onPageStarted(view, url, favicon)
        }
    }

    companion object {
        fun newInstance(url: FileUrl) = CloudFlare(url)
        fun newInstance(url: String) = CloudFlare(FileUrl(url))
    }
}