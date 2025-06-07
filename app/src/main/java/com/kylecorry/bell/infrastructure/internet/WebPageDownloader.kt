package com.kylecorry.bell.infrastructure.internet

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.Gson
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.luna.coroutines.onMain
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


class WebPageDownloader(private val context: Context) {

    private val http = HttpService(context)

    suspend fun download(url: String): String? {
        return tryOrDefault(null) {
            http.get(
                url, headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3",
                    "Accept" to "*/*"
                )
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun downloadAsBrowser(url: String): String? = suspendCancellableCoroutine { cont ->
        runBlocking {
            onMain {
                val webView = WebView(context)

                webView.getSettings().javaScriptEnabled = true

                webView.setWebViewClient(object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        webView.evaluateJavascript(
                            "document.documentElement.outerHTML;",
                            ValueCallback { html: String? ->
                                val formatted = html?.replace("\\u003C", "<")?.replace("\\n", "\n")?.replace("\\\"", "\"")?.replace("\\\\", "\\")
                                cont.resume(formatted)
                            })
                    }
                })

                webView.loadUrl(url)
            }
        }
    }

}