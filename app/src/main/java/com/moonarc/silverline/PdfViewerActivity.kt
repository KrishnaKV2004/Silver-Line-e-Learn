package com.moonarc.silverline

import androidx.compose.animation.*
import androidx.compose.animation.core.tween

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.moonarc.silverline.ui.theme.SilverLineTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
class PdfViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pdfName = intent.getStringExtra("pdf") ?: ""

        setContent {
            SilverLineTheme {
                PdfViewerScreen(pdfName = pdfName, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(pdfName: String, onBack: () -> Unit) {

    var currentPage by remember { mutableStateOf(0) }
    var dragOffset by remember { mutableStateOf(0f) }
    var totalPages by remember { mutableStateOf(0) }

    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        if (pdfName.isEmpty()) return@LaunchedEffect

        val file = File("/data/data/${context.packageName}/files/$pdfName")

        if (!file.exists()) {
            val input = context.assets.open(pdfName)
            file.outputStream().use { input.copyTo(it) }
        }

        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(descriptor)
        renderer = pdfRenderer
        totalPages = pdfRenderer.pageCount

        val page = pdfRenderer.openPage(0)
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        pageBitmap = bitmap
    }

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
                                if (dragOffset < -100 && currentPage < totalPages - 1) {
                                    currentPage++
                                } else if (dragOffset > 100 && currentPage > 0) {
                                    currentPage--
                                }
                                dragOffset = 0f

                                renderer?.let { pdf ->
                                    val page = pdf.openPage(currentPage)
                                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                    bitmap.eraseColor(android.graphics.Color.WHITE)
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    page.close()
                                    pageBitmap = bitmap
                                }
                            }
                        ) { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    }
            ) {
                AnimatedContent(
                    targetState = pageBitmap,
                    transitionSpec = {
                        slideInHorizontally(
                            animationSpec = tween(300),
                            initialOffsetX = { it }
                        ) togetherWith slideOutHorizontally(
                            animationSpec = tween(300),
                            targetOffsetX = { -it }
                        )
                    }
                ) { bitmap ->
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ⬅️➡️ Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (currentPage > 0) {
                            currentPage--
                            renderer?.let { pdf ->
                                val page = pdf.openPage(currentPage)
                                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                page.close()
                                pageBitmap = bitmap
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Previous")
                }

                Button(
                    onClick = {
                        if (currentPage < totalPages - 1) {
                            currentPage++
                            renderer?.let { pdf ->
                                val page = pdf.openPage(currentPage)
                                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                page.close()
                                pageBitmap = bitmap
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Next")
                }
            }
        }
    }
}