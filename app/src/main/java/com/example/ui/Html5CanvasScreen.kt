package com.example.ui

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Html5CanvasScreen(
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBgDark)
    ) {
        // --- Sleek Cyberspace Title bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ImmersiveBgHeader)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToMenu,
                modifier = Modifier
                    .size(40.dp)
                    .background(ImmersiveBgDark, RoundedCornerShape(8.dp))
                    .border(1.dp, ImmersiveGreen, RoundedCornerShape(8.dp))
                    .testTag("html5_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Return to Hub Menu",
                    tint = ImmersiveGreen
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "HTML5 ISO-GRID SIMULATOR",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "PURE JS CANVAS RENDERER // STACKED Z-LAYERING",
                    color = ImmersiveSlateMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Divider(color = Color(0x1A00FFCC), thickness = 1.dp)

        // --- WebView container loading isometric_grid.html ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_NO_CACHE
                        }
                        loadUrl("file:///android_asset/isometric_grid.html")
                    }
                },
                modifier = Modifier.fillMaxSize().testTag("html5_webview")
            )
        }
    }
}
