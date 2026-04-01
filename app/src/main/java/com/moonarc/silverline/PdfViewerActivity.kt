package com.moonarc.silverline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.moonarc.silverline.ui.theme.SilverLineTheme

@OptIn(ExperimentalMaterial3Api::class)
class PdfViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SilverLineTheme {
                PdfViewerScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(onBack: () -> Unit) {

    var currentPage by remember { mutableStateOf(1) }
    var dragOffset by remember { mutableStateOf(0f) }
    val totalPages = 10 // temp for testing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            // 📄 PDF Page Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.9f))
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragOffset < -100 && currentPage < totalPages) {
                                    currentPage++
                                } else if (dragOffset > 100 && currentPage > 1) {
                                    currentPage--
                                }
                                dragOffset = 0f
                            }
                        ) { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    }
            ) {
                Text(
                    text = "Page $currentPage",
                    color = Color.White,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ⬅️➡️ Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (currentPage > 1) currentPage--
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Previous")
                }

                Button(
                    onClick = {
                        if (currentPage < totalPages) currentPage++
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Next")
                }
            }
        }
    }
}