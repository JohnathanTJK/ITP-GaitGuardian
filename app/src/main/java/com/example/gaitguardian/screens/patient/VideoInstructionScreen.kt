package com.example.gaitguardian.screens.patient

import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.gaitguardian.ui.theme.bgColor
import com.example.gaitguardian.ui.theme.ButtonActive
import com.example.gaitguardian.ui.theme.screenPadding

@Composable
fun VideoInstructionScreen(
    navController: NavController,
    assessmentType: String
) {
    val context = LocalContext.current
    val decodedType = Uri.decode(assessmentType)

    // Map assessment types to YouTube video URLs
    val videoUrl = when (decodedType) {
        "Timed Up and Go" -> "https://www.youtube.com/embed/nKboUrNyyjw"
        "Five Times Sit to Stand" -> "https://www.youtube.com/embed/whstRV9jrjM"
        else -> "https://www.youtube.com"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(screenPadding)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "$decodedType Instructions",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            AndroidView(
                factory = {
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            600
                        )
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        loadUrl(videoUrl)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val encoded = Uri.encode(decodedType)
                    navController.navigate("assessment_info_screen/$encoded")
                },
                colors = ButtonDefaults.buttonColors(containerColor = ButtonActive),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Skip to Assessment", fontSize = 18.sp, color = Color.Black)
            }
        }

    }
}
