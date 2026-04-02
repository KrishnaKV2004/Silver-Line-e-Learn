package com.moonarc.silverline

import android.content.Context
import android.view.View

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
import androidx.compose.foundation.layout.*
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
import java.io.File

class PdfViewerActivity : ComponentActivity() {

    private var pdfRenderer: PdfRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pdfName = intent.getStringExtra("pdf") ?: return
        val file = File(filesDir, pdfName)

        if (!file.exists()) {
            assets.open(pdfName).use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
        }

        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(descriptor)

        setContent {
            PdfScreen(pdfRenderer!!) {
                finish()
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

    fun renderPage(index: Int) {
        val page = pdf.openPage(index)
        val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.WHITE)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        bitmap = bmp
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
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragOffset < -100 && currentPage < pdf.pageCount - 1) {
                                    currentPage++
                                    renderPage(currentPage)
                                } else if (dragOffset > 100 && currentPage > 0) {
                                    currentPage--
                                    renderPage(currentPage)
                                }
                                dragOffset = 0f
                            }
                        ) { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    }
            ) {
                bitmap?.let { bmp ->
                    AndroidView(
                        factory = { context ->
                            CurlGLView(context)
                        },
                        update = { view ->
                            view.setBitmap(bmp)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(70.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {

                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    // Previous Button
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .background(Color.Gray.copy(alpha = 0.3f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = {
                            if (currentPage > 0) {
                                currentPage--
                                renderPage(currentPage)
                            }
                        }) {
                            Icon(
                                Icons.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous",
                                tint = Color.White
                            )
                        }
                    }

                    // Next Button
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .background(Color.Gray.copy(alpha = 0.3f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = {
                            if (currentPage < pdf.pageCount - 1) {
                                currentPage++
                                renderPage(currentPage)
                            }
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
            Spacer(modifier = Modifier.weight(0.1f))
        }

    }
}


class CurlGLView(context: Context) : GLSurfaceView(context) {

    private val renderer: CurlRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = CurlRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY

        // Allow Compose gestures to work
        setOnTouchListener { _, _ -> false }
    }

    fun setBitmap(bitmap: Bitmap) {
        renderer.setBitmap(bitmap)
        requestRender()
    }
}

class CurlRenderer : GLSurfaceView.Renderer {

    private var bitmap: Bitmap? = null
    private var textureId = IntArray(1)

    private var program = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureHandle = 0

    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = aPosition;
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """.trimIndent()

    private val vertices = floatArrayOf(
        -1f, 1f, 0f,   // top-left
        -1f, -1f, 0f,  // bottom-left
        1f, 1f, 0f,    // top-right
        1f, -1f, 0f    // bottom-right
    )

    private val texCoords = floatArrayOf(
        0f, 0f,
        0f, 1f,
        1f, 0f,
        1f, 1f
    )

    private lateinit var vertexBuffer: java.nio.FloatBuffer
    private lateinit var texBuffer: java.nio.FloatBuffer

    fun setBitmap(bmp: Bitmap) {
        bitmap = bmp
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        vertexBuffer = java.nio.ByteBuffer.allocateDirect(vertices.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertices).position(0)

        texBuffer = java.nio.ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        texBuffer.put(texCoords).position(0)

        GLES20.glGenTextures(1, textureId, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val bmp = bitmap ?: return

        GLES20.glUseProgram(program)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texBuffer)

        GLES20.glUniform1i(textureHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }
}