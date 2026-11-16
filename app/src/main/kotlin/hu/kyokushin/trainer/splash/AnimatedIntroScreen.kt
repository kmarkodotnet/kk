package hu.kyokushin.trainer.splash

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import hu.kyokushin.trainer.R
import kotlinx.coroutines.delay

/** Dekoratív, offline jelenet; nem karateoktató illusztráció. */
@SuppressLint("SetJavaScriptEnabled") // Csak a csomagolt, saját Canvas-renderelő fut.
@Composable
fun AnimatedIntroScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val finished = rememberUpdatedState(onFinished)
    val reduceMotion = remember(context) {
        val accessibility = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        accessibility.isTouchExplorationEnabled ||
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    // WebView-hiba esetén sem tarthatja fogva az appot az intro.
    LaunchedEffect(reduceMotion) {
        delay(if (reduceMotion) 1200L else 17_000L)
        finished.value()
    }
    if (reduceMotion) {
        SplashScreen(modifier)
        return
    }
    val webView = remember(context) {
        runCatching {
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.WHITE)
                importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                settings.apply {
                    javaScriptEnabled = true
                    blockNetworkLoads = true
                    allowFileAccess = false
                    allowContentAccess = false
                    domStorageEnabled = false
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                }
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun finished() {
                            post { finished.value() }
                        }
                    },
                    "AndroidIntro",
                )
                val html = context.assets.open("intro/intro.html").bufferedReader().use { it.readText() }
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }.getOrNull()
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(webView, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webView?.onPause()
                Lifecycle.Event.ON_RESUME -> webView?.onResume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            webView?.removeJavascriptInterface("AndroidIntro")
            webView?.stopLoading()
            webView?.destroy()
        }
    }
    Box(modifier.fillMaxSize()) {
        if (webView != null) {
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
        } else {
            SplashScreen()
            LaunchedEffect(Unit) {
                delay(1200)
                finished.value()
            }
        }
        Button(
            onClick = { finished.value() },
            modifier = Modifier.align(Alignment.BottomEnd).safeDrawingPadding().padding(16.dp).heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.intro_skip))
        }
    }
}
