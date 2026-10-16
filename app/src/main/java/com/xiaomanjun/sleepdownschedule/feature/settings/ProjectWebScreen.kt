package com.xiaomanjun.sleepdownschedule.feature.settings

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.xiaomanjun.sleepdownschedule.SettingsDetailActivity
import com.xiaomanjun.sleepdownschedule.app.ui.detailContentTopPadding
import com.xiaomanjun.sleepdownschedule.app.ui.enableSleepDownDownloads

/** Project documentation stays inside the app, without the school authentication bridge. */
internal fun Context.openInAppProjectPage(url: String) {
    val uri = Uri.parse(url)
    require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null)
    startActivity(Intent(this, SettingsDetailActivity::class.java)
        .putExtra("settings_page", "ProjectWeb").putExtra("project_url", url))
}

@Composable
internal fun ProjectWebScreen() {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val initial = remember { activity?.intent?.getStringExtra("project_url").orEmpty() }
    var currentUrl by remember { mutableStateOf(initial) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var browser by remember { mutableStateOf<WebView?>(null) }
    val valid = remember(initial) {
        runCatching { Uri.parse(initial).let { it.scheme == "https" && !it.host.isNullOrBlank() && it.userInfo == null } }.getOrDefault(false)
    }
    Column(Modifier.fillMaxSize().padding(top = detailContentTopPadding()).navigationBarsPadding()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(Uri.parse(currentUrl).host.orEmpty(), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsActionButton("上一页", null, onClick = { browser?.let { if (it.canGoBack()) it.goBack() } })
                SettingsActionButton(if (loading) "重新加载" else "刷新", null, onClick = {
                    if (error != null) { error = null; revision++ } else browser?.reload()
                })
            }
        }
        if (!valid || error != null) {
            Text(error ?: "无法打开这个页面", Modifier.padding(24.dp))
        } else key(revision) {
            AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { owner ->
                WebView(owner).apply {
                    browser = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.setSupportMultipleWindows(false)
                    enableSleepDownDownloads()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            return request.url.scheme != "https" || request.url.userInfo != null
                        }
                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            loading = true; currentUrl = url
                        }
                        override fun onPageFinished(view: WebView, url: String) { loading = false }
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                            if (request.isForMainFrame) { loading = false; error = "页面暂时无法加载，请点击重新加载。" }
                        }
                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                            if (request.isForMainFrame) { loading = false; error = "页面返回 ${response.statusCode}，请稍后刷新。" }
                        }
                        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                            (view.parent as? ViewGroup)?.removeView(view)
                            view.destroy(); browser = null; loading = false
                            error = "网页已停止运行，可点击重新加载；应用内数据不受影响。"
                            return true
                        }
                    }
                    loadUrl(initial)
                }
            }, onRelease = { view ->
                if (browser === view) browser = null
                runCatching { view.stopLoading(); view.webViewClient = WebViewClient(); view.destroy() }
            })
        }
    }
}
