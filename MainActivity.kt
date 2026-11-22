package com.example.myvideodownloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DownloaderApp() }
    }
}

private enum class Tab { DOWNLOAD, BROWSER, LIBRARY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderApp() {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(Tab.DOWNLOAD) }
    var url by remember { mutableStateOf("") }
    var browserUrl by remember { mutableStateOf("https://www.google.com") }
    var message by remember { mutableStateOf("Paste a direct video link or browse to a website.") }
    var downloads by remember { mutableStateOf(listOf<String>()) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Video Downloader") },
                    actions = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.padding(end = 16.dp)) }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = tab == Tab.DOWNLOAD, onClick = { tab = Tab.DOWNLOAD },
                        icon = { Icon(Icons.Default.Link, null) }, label = { Text("Download") })
                    NavigationBarItem(selected = tab == Tab.BROWSER, onClick = { tab = Tab.BROWSER },
                        icon = { Icon(Icons.Default.Public, null) }, label = { Text("Browser") })
                    NavigationBarItem(selected = tab == Tab.LIBRARY, onClick = { tab = Tab.LIBRARY },
                        icon = { Icon(Icons.Default.VideoLibrary, null) }, label = { Text("Library") })
                }
            }
        ) { padding ->
            when (tab) {
                Tab.DOWNLOAD -> DownloadScreen(
                    modifier = Modifier.padding(padding),
                    url = url, onUrlChange = { url = it }, message = message,
                    onDownload = {
                        val result = enqueueVideoDownload(context, url)
                        message = result.first
                        if (result.second) downloads = downloads + url
                    }
                )
                Tab.BROWSER -> BrowserScreen(
                    modifier = Modifier.padding(padding),
                    initialUrl = browserUrl,
                    onPageChanged = { browserUrl = it },
                    onVideoFound = { found ->
                        url = found
                        message = "Video link detected. Open Download to save it."
                        tab = Tab.DOWNLOAD
                    },
                    onDownload = { found ->
                        val result = enqueueVideoDownload(context, found)
                        message = result.first
                        if (result.second) downloads = downloads + found
                    }
                )
                Tab.LIBRARY -> LibraryScreen(Modifier.padding(padding), downloads)
            }
        }
    }
}

@Composable
private fun DownloadScreen(
    modifier: Modifier, url: String, onUrlChange: (String) -> Unit,
    message: String, onDownload: () -> Unit
) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Download a video", style = MaterialTheme.typography.headlineMedium)
        Text("Paste a direct video file URL (for example, an .mp4 link).")
        OutlinedTextField(
            value = url, onValueChange = onUrlChange, modifier = Modifier.fillMaxWidth(),
            label = { Text("Video URL") }, placeholder = { Text("https://example.com/video.mp4") },
            leadingIcon = { Icon(Icons.Default.Link, null) }, singleLine = false
        )
        Button(onClick = onDownload, enabled = url.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Download, null)
            Spacer(Modifier.width(8.dp))
            Text("Download video")
        }
        Card(Modifier.fillMaxWidth()) {
            Text(message, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        }
        Text("Tip: For websites, use Browser and tap a downloadable video link. This app does not unlock protected or paid streams.")
    }
}

@Composable
private fun BrowserScreen(
    modifier: Modifier, initialUrl: String, onPageChanged: (String) -> Unit,
    onVideoFound: (String) -> Unit, onDownload: (String) -> Unit
) {
    var address by remember { mutableStateOf(initialUrl) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = address, onValueChange = { address = it }, modifier = Modifier.weight(1f),
                singleLine = true, label = { Text("Website address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            IconButton(onClick = {
                val normalized = normalizeWebUrl(address)
                address = normalized
                webView?.loadUrl(normalized)
            }) { Icon(Icons.Default.ArrowForward, "Go") }
        }
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            address = url
                            onPageChanged(url)
                        }
                    }
                    webChromeClient = WebChromeClient()
                    setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, _ ->
                        if (isAllowedVideoUrl(downloadUrl, mimeType)) {
                            onDownload(downloadUrl)
                        } else {
                            onVideoFound(downloadUrl)
                        }
                    }
                    loadUrl(normalizeWebUrl(initialUrl))
                    webView = this
                }
            },
            update = { webView = it }
        )
        Text(
            "Downloads are detected when a website exposes a normal downloadable file. Embedded, segmented, DRM-protected and blob-based streams may not be downloadable.",
            Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun LibraryScreen(modifier: Modifier, downloads: List<String>) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Download history", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        if (downloads.isEmpty()) {
            Text("Your download history will appear here.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(downloads.reversed()) { item ->
                    ListItem(
                        headlineContent = { Text(URLUtil.guessFileName(item, null, null), maxLines = 1) },
                        supportingContent = { Text(item, maxLines = 2) },
                        leadingContent = { Icon(Icons.Default.VideoFile, null) }
                    )
                    HorizontalDivider()
                }
            }
        }
        Text("This is a session history. Completed files are saved by Android in the Downloads folder.")
    }
}

private fun normalizeWebUrl(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return "https://www.google.com"
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
    else "https://$trimmed"
}

private fun isAllowedVideoUrl(url: String, mimeType: String?): Boolean {
    val lower = url.lowercase()
    val type = mimeType?.lowercase().orEmpty()
    return type.startsWith("video/") ||
        listOf(".mp4", ".m4v", ".webm", ".mov", ".3gp", ".mkv").any { lower.substringBefore("?").endsWith(it) }
}

private fun enqueueVideoDownload(context: Context, rawUrl: String): Pair<String, Boolean> {
    val clean = rawUrl.trim()
    val uri = try { Uri.parse(clean) } catch (_: Exception) { null }
    if (uri == null || uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) {
        return "Enter a valid http or https video URL.", false
    }
    return try {
        val request = DownloadManager.Request(uri)
            .setTitle(URLUtil.guessFileName(clean, null, "video/mp4"))
            .setDescription("Downloading video")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(
                android.os.Environment.DIRECTORY_DOWNLOADS,
                URLUtil.guessFileName(clean, null, "video.mp4")
            )
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        "Download queued. Check the Android notification shade for progress.", true
    } catch (e: Exception) {
        "Could not start download: ${e.localizedMessage ?: "unknown error"}", false
    }
}
