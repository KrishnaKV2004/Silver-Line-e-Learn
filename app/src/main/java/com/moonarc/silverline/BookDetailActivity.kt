package com.moonarc.silverline

import android.content.Intent

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import com.moonarc.silverline.ui.theme.SilverLineTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
class BookDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val subject = intent.getStringExtra("subject") ?: "Subject"
        val className = intent.getStringExtra("class") ?: "Class"

        setContent {
            SilverLineTheme {
                BookDetailScreen(
                    subject = subject,
                    className = className,
                    onBack = { finish() },
                    onOpen = {
                        val intent = Intent(this, PdfViewerActivity::class.java)

                        val pdfName = if (subject == "Science" && className == "Class 1") {
                            "science_class1.pdf"
                        } else {
                            ""
                        }

                        intent.putExtra("pdf", pdfName)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    subject: String,
    className: String,
    onBack: () -> Unit,
    onOpen: () -> Unit
) {

    val imageRes = if (subject == "Science" && className == "Class 1") {
        R.drawable.science_class1_cover
    } else {
        null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$subject - $className") },
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

            if (imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(220.dp)
                        .aspectRatio(1f / 1.414f)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(20.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .aspectRatio(1f / 1.414f)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "$className Book",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "This book contains lessons and content for $className $subject.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Open Book")
            }
        }
    }
}