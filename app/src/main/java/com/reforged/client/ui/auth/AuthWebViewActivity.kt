package com.reforged.client.ui.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.reforged.client.R
import org.json.JSONObject

class AuthWebViewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appId = resources.getInteger(R.integer.com_vk_sdk_AppId)
        // Scopes for full access
        val scope = "friends,photos,audio,video,status,notes,messages,wall,ads,offline,docs,groups,notifications,stats,email,market"
        
        val intentUrl = intent.getStringExtra("url")
        val defaultAuthUrl = "https://oauth.vk.ru/authorize?" +
                "client_id=$appId&" +
                "display=mobile&" +
                "redirect_uri=https://oauth.vk.ru/blank.html&" +
                "scope=$scope&" +
                "response_type=token&" +
                "v=5.199&" +
                "state=reforged"
        
        val authUrl = intentUrl ?: defaultAuthUrl

        setContent {
            var isLoading by remember { mutableStateOf(true) }
            
            Box(modifier = Modifier.fillMaxSize()) {
                AuthWebView(
                    url = authUrl,
                    onLoadingChanged = { isLoading = it },
                    onCaptured = { data ->
                        val result = Intent().apply {
                            data.forEach { (k, v) -> putExtra(k, v) }
                        }
                        setResult(Activity.RESULT_OK, result)
                        finish()
                    },
                    onClose = { finish() }
                )
                
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AuthWebView(
    url: String,
    onLoadingChanged: (Boolean) -> Unit,
    onCaptured: (Map<String, String>) -> Unit,
    onClose: () -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                
                // Important: VK ID Captcha communicates via "AndroidBridge"
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun VKCaptchaGetResult(data: String) {
                        try {
                            val json = JSONObject(data)
                            val token = json.optString("token")
                            if (token.isNotEmpty()) {
                                post { onCaptured(mapOf("success_token" to token)) }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    @JavascriptInterface
                    fun VKCaptchaCloseCaptcha(data: String) {
                        post { onClose() }
                    }

                    @JavascriptInterface
                    fun VKCaptchaListenSensorsStart(data: String) {}

                    @JavascriptInterface
                    fun VKCaptchaListenSensorsStop(data: String) {}
                }, "AndroidBridge")
                
                // Clear cookies to ensure fresh login if it's the main auth url
                if (url.contains("authorize")) {
                    android.webkit.CookieManager.getInstance().removeAllCookies(null)
                }
                
                // User-Agent like VK Android App
                settings.userAgentString = "VKAndroidApp/8.191-56796 (Android 13; SDK 33; arm64-v8a; Google Pixel 4; ru; 1080x1920)"
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onLoadingChanged(true)
                        if (checkUrl(url)) return
                        super.onPageStarted(view, url, favicon)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onLoadingChanged(false)
                        checkUrl(url)
                        super.onPageFinished(view, url)
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return checkUrl(request?.url.toString())
                    }

                    private fun checkUrl(url: String?): Boolean {
                        url ?: return false
                        if (url.contains("access_token=")) {
                            val fragment = url.substringAfter("#")
                            val params = fragment.split("&").associate {
                                it.substringBefore("=") to it.substringAfter("=")
                            }
                            onCaptured(params)
                            return true
                        }
                        if (url.contains("success_token=")) {
                            val token = url.substringAfter("success_token=").substringBefore("&")
                            onCaptured(mapOf("success_token" to token))
                            return true
                        }
                        return false
                    }
                }
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
