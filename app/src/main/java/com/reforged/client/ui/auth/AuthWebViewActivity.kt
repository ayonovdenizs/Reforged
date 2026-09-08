package com.reforged.client.ui.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
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

class AuthWebViewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appId = resources.getInteger(R.integer.com_vk_sdk_AppId)
        val authUrl = "https://oauth.vk.ru/authorize?" +
                "client_id=$appId&" +
                "display=mobile&" +
                "redirect_uri=https://oauth.vk.ru/blank.html&" +
                "scope=notify,friends,photos,audio,video,stories,pages,status,notes,messages,wall,ads,offline,docs,groups,notifications,stats,email,market&" +
                "response_type=token&" +
                "v=5.199"

        setContent {
            var isLoading by remember { mutableStateOf(true) }
            
            Box(modifier = Modifier.fillMaxSize()) {
                AuthWebView(
                    url = authUrl,
                    onLoadingChanged = { isLoading = it },
                    onTokenCaptured = { token, userId ->
                        val result = Intent().apply {
                            putExtra("access_token", token)
                            putExtra("user_id", userId)
                        }
                        setResult(Activity.RESULT_OK, result)
                        finish()
                    }
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
    onTokenCaptured: (String, Long) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                
                // Masking as mobile browser
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onLoadingChanged(true)
                        super.onPageStarted(view, url, favicon)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onLoadingChanged(false)
                        super.onPageFinished(view, url)
                        
                        url?.let {
                            if (it.contains("access_token=")) {
                                val token = it.substringAfter("access_token=").substringBefore("&")
                                val userId = it.substringAfter("user_id=").substringBefore("&").toLongOrNull() ?: 0L
                                onTokenCaptured(token, userId)
                            }
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val newUrl = request?.url.toString()
                        if (newUrl.contains("access_token=")) {
                            val token = newUrl.substringAfter("access_token=").substringBefore("&")
                            val userId = newUrl.substringAfter("user_id=").substringBefore("&").toLongOrNull() ?: 0L
                            onTokenCaptured(token, userId)
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
