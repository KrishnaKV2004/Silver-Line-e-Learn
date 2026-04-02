package com.moonarc.silverline

import android.content.Context
import android.view.View
import android.view.WindowManager

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.spring
import java.io.File
import androidx.compose.ui.draw.clip

class PdfViewerActivity : ComponentActivity() {

    private var pdfRenderer: PdfRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        val pdfName = intent.getStringExtra("pdf")
        if (pdfName != null) {
            try {
                val file = File(filesDir, pdfName)

                if (!file.exists()) {
                    try {
                        assets.open(pdfName).use { input ->
                            file.outputStream().use { input.copyTo(it) }
                        }
                    } catch (e: Exception) {
                        // file not found in assets
                    }
                }

                if (file.exists()) {
                    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    pdfRenderer = PdfRenderer(descriptor)

                    setContent {
                        PdfScreen(pdfRenderer!!) {
                            finish()
                        }
                    }
                } else {
                    setContent {
                        NotAvailableScreen {
                            finish()
                        }
                    }
                }
            } catch (e: Exception) {
                setContent {
                    NotAvailableScreen {
                        finish()
                    }
                }
            }
        } else {
            setContent {
                NotAvailableScreen {
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfScreen(pdf: PdfRenderer, onBack: () -> Unit) {

    var currentPage by remember { mutableStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var curl by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var nextBitmap by remember { mutableStateOf<Bitmap?>(null) }

    fun renderPage(index: Int) {
        val page = pdf.openPage(index)
        val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.WHITE)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        bitmap = bmp

        // preload next page
        if (index < pdf.pageCount - 1) {
            try {
                val nextPage = pdf.openPage(index + 1)
                val nextBmp = Bitmap.createBitmap(nextPage.width, nextPage.height, Bitmap.Config.ARGB_8888)
                nextBmp.eraseColor(android.graphics.Color.WHITE)
                nextPage.render(nextBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                nextPage.close()
                nextBitmap = nextBmp
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        renderPage(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
                .padding(16.dp)
                .padding(top = 8.dp)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 3f)

                            // Apply zoom
                            scale = newScale

                            // Only allow panning when zoomed in
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragOffset < -150 && currentPage < pdf.pageCount - 1) {
                                    currentPage++
                                    renderPage(currentPage)
                                } else if (dragOffset > 150 && currentPage > 0) {
                                    currentPage--
                                    renderPage(currentPage)
                                }

                                curl = 0f
                                dragOffset = 0f
                            }
                        ) { _, dragAmount ->
                            if (scale <= 1.05f) {
                                dragOffset += dragAmount
                                curl = (-dragOffset / 800f).coerceIn(0f, 1f)
                            }
                        }
                    }
            ) {
                bitmap?.let { bmp ->

                    val animatedOffset by animateFloatAsState(
                        targetValue = dragOffset,
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = 300f
                        ),
                        label = "slide"
                    )

                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f / 1.414f)
                            .align(Alignment.Center)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .graphicsLayer {
                                val maxX = (scale - 1f) * 300f
                                val maxY = (scale - 1f) * 500f

                                val clampedX = offsetX.coerceIn(-maxX, maxX)
                                val clampedY = offsetY.coerceIn(-maxY, maxY)

                                translationX = animatedOffset + clampedX
                                translationY = clampedY
                                scaleX = scale
                                scaleY = scale
                                clip = true
                            }
                    )
                }

                Text(
                    text = "Page ${currentPage + 1} / ${pdf.pageCount}",
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            }

            Spacer(Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Previous Button
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentPage > 0,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInHorizontally { -it },
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutHorizontally { -it }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .background(Color.Gray.copy(alpha = 0.3f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = {
                                currentPage--
                                renderPage(currentPage)
                            }) {
                                Icon(
                                    Icons.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Next Button
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentPage < pdf.pageCount - 1,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInHorizontally { it },
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutHorizontally { it }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .background(Color.Gray.copy(alpha = 0.3f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = {
                                currentPage++
                                renderPage(currentPage)
                            }) {
                                Icon(
                                    Icons.Filled.KeyboardArrowRight,
                                    contentDescription = "Next",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(0.1f))
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotAvailableScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Not Available",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}