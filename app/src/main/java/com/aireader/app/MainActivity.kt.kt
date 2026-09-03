package com.aireader.app

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AIReaderApp()
            }
        }
    }

    @Composable
    private fun AIReaderApp() {

        var selectedUri by remember {
            mutableStateOf<Uri?>(null)
        }

        var selectedName by remember {
            mutableStateOf("")
        }

        if (selectedUri == null) {

            HomeScreen(
                onPdfSelected = { uri, name ->
                    selectedUri = uri
                    selectedName = name
                }
            )

        } else {

            PdfReaderScreen(
                uri = selectedUri!!,
                fileName = selectedName,
                onBack = {
                    selectedUri = null
                    selectedName = ""
                }
            )
        }
    }

    private fun getFileName(uri: Uri): String {

        var name = "PDF Book"

        try {

            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val index =
                        cursor.getColumnIndex(
                            OpenableColumns.DISPLAY_NAME
                        )

                    if (index >= 0) {
                        name =
                            cursor.getString(index)
                                ?: "PDF Book"
                    }
                }
            }

        } catch (_: Exception) {
        }

        return name
    }

    @Composable
    private fun HomeScreen(
        onPdfSelected: (Uri, String) -> Unit
    ) {

        val launcher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->

                if (uri != null) {

                    try {

                        contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                    } catch (_: Exception) {
                    }

                    onPdfSelected(
                        uri,
                        getFileName(uri)
                    )
                }
            }

        Scaffold(

            topBar = {

                TopAppBar(
                    title = {
                        Text(
                            text = "AI Reader",
                            fontSize = 24.sp
                        )
                    }
                )
            }

        ) { padding ->

            Column(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.Center

            ) {

                Text(
                    text = "📚",
                    fontSize = 80.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                Text(
                    text = "AI Reader",
                    style =
                        MaterialTheme
                            .typography
                            .headlineLarge
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        "Read, translate, summarize and understand your books with AI."
                )

                Spacer(
                    modifier =
                        Modifier.height(30.dp)
                )

                Button(

                    onClick = {

                        launcher.launch(
                            arrayOf("application/pdf")
                        )
                    }

                ) {

                    Text(
                        text = "📖 Open PDF"
                    )
                }
            }
        }
    }

    @Composable
    private fun PdfReaderScreen(
        uri: Uri,
        fileName: String,
        onBack: () -> Unit
    ) {

        var pageNumber by remember {
            mutableIntStateOf(0)
        }

        var pageCount by remember {
            mutableIntStateOf(0)
        }

        var bitmap by remember {
            mutableStateOf<Bitmap?>(null)
        }

        var errorMessage by remember {
            mutableStateOf<String?>(null)
        }

        var scale by remember {
            mutableFloatStateOf(1f)
        }

        LaunchedEffect(
            uri,
            pageNumber
        ) {

            var descriptor:
                    ParcelFileDescriptor? = null

            var renderer:
                    PdfRenderer? = null

            var page:
                    PdfRenderer.Page? = null

            try {

                descriptor =
                    contentResolver.openFileDescriptor(
                        uri,
                        "r"
                    )

                if (descriptor == null) {

                    errorMessage =
                        "Unable to open this PDF."

                    return@LaunchedEffect
                }

                renderer =
                    PdfRenderer(descriptor)

                pageCount =
                    renderer.pageCount

                if (
                    pageNumber >= 0 &&
                    pageNumber < renderer.pageCount
                ) {

                    page =
                        renderer.openPage(
                            pageNumber
                        )

                    val width =
                        page.width * 2

                    val height =
                        page.height * 2

                    val newBitmap =
                        Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.ARGB_8888
                        )

                    newBitmap.eraseColor(
                        android.graphics.Color.WHITE
                    )

                    page.render(
                        newBitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )

                    bitmap = newBitmap

                    errorMessage = null
                }

            } catch (e: Exception) {

                bitmap = null

                errorMessage =
                    e.message
                        ?: "Unable to read this PDF."

            } finally {

                try {
                    page?.close()
                } catch (_: Exception) {
                }

                try {
                    renderer?.close()
                } catch (_: Exception) {
                }

                try {
                    descriptor?.close()
                } catch (_: Exception) {
                }
            }
        }

        Scaffold(

            topBar = {

                TopAppBar(

                    title = {

                        Text(
                            text = fileName,
                            maxLines = 1
                        )
                    },

                    navigationIcon = {

                        TextButton(
                            onClick = onBack
                        ) {

                            Text("← Back")
                        }
                    }
                )
            }

        ) { padding ->

            Column(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)

            ) {

                if (errorMessage != null) {

                    Box(

                        modifier =
                            Modifier
                                .fillMaxSize()
                                .weight(1f),

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "⚠️",
                                fontSize = 50.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(10.dp)
                            )

                            Text(
                                text =
                                    errorMessage!!
                            )
                        }
                    }

                } else {

                    LazyColumn(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                ),

                        horizontalAlignment =
                            Alignment.CenterHorizontally

                    ) {

                        item {

                            bitmap?.let { image ->

                                var offsetX by remember {
                                    mutableFloatStateOf(0f)
                                }

                                var offsetY by remember {
                                    mutableFloatStateOf(0f)
                                }

                                val transformState =
                                    rememberTransformableState {
                                            zoomChange,
                                            panChange,
                                            _ ->

                                        scale =
                                            (scale *
                                                zoomChange)
                                                .coerceIn(
                                                    1f,
                                                    4f
                                                )

                                        offsetX +=
                                            panChange.x

                                        offsetY +=
                                            panChange.y
                                    }

                                Image(

                                    bitmap =
                                        image.asImageBitmap(),

                                    contentDescription =
                                        "PDF page",

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                            .graphicsLayer {

                                                scaleX =
                                                    scale

                                                scaleY =
                                                    scale

                                                translationX =
                                                    offsetX

                                                translationY =
                                                    offsetY
                                            }
                                            .transformable(
                                                transformState
                                            )
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(8.dp)
                    ) {

                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.SpaceEvenly,

                            verticalAlignment =
                                Alignment.CenterVertically

                        ) {

                            TextButton(

                                enabled =
                                    pageNumber > 0,

                                onClick = {

                                    if (pageNumber > 0) {
                                        pageNumber--
                                    }
                                }

                            ) {

                                Text("◀ Previous")
                            }

                            Text(
                                text =
                                    if (pageCount > 0) {
                                        "${pageNumber + 1} / $pageCount"
                                    } else {
                                        "Loading..."
                                    }
                            )

                            TextButton(

                                enabled =
                                    pageNumber <
                                        pageCount - 1,

                                onClick = {

                                    if (
                                        pageNumber <
                                        pageCount - 1
                                    ) {
                                        pageNumber++
                                    }
                                }

                            ) {

                                Text("Next ▶")
                            }
                        }

                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.SpaceEvenly

                        ) {

                            TextButton(

                                onClick = {

                                    scale =
                                        (scale - 0.25f)
                                            .coerceAtLeast(1f)
                                }

                            ) {

                                Text("➖ Zoom")
                            }

                            TextButton(

                                onClick = {

                                    scale = 1f
                                }

                            ) {

                                Text("Reset")
                            }

                            TextButton(

                                onClick = {

                                    scale =
                                        (scale + 0.25f)
                                            .coerceAtMost(4f)
                                }

                            ) {

                                Text("➕ Zoom")
                            }
                        }
                    }
                }
            }
        }
    }
}