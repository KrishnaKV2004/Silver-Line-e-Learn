package com.moonarc.silverline
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // Cover section (bigger + shadow feel)
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(240.dp)
                            .aspectRatio(1f / 1.414f)
                            .clip(RoundedCornerShape(20.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .width(240.dp)
                            .aspectRatio(1f / 1.414f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "$className $subject",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "NCERT Based Content",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Info cards row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    InfoCard(title = "Pages", value = "120+")
                }
                Box(modifier = Modifier.weight(1f)) {
                    InfoCard(title = "Language", value = "English")
                }
                Box(modifier = Modifier.weight(1f)) {
                    InfoCard(title = "Level", value = className)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Description
            Text(
                text = "About this book",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "This book contains structured lessons, diagrams, and exercises designed specifically for $className students studying $subject. It helps build strong fundamentals with easy explanations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Open button (modern)
            Button(
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(40.dp)
            ) {
                Text("Open Book")
            }
        }
    }
}
@Composable
fun InfoCard(title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}