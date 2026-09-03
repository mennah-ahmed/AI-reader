package com.aireader.app

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

data class Book(val name: String, val uri: Uri)

class MainActivity : ComponentActivity() {
    private var selectedUri by mutableStateOf<Uri?>(null)
    private var selectedName by mutableStateOf("")
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { tts.language = Locale.US }
        setContent {
            MaterialTheme {
                AIReaderApp(
                    selectedUri = selectedUri,
                    selectedName = selectedName,
                    onOpenPdf = { selectedUri = it.first; selectedName = it.second },
                    onCloseReader = { selectedUri = null }
                )
            }
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) tts.shutdown()
        super.onDestroy()
    }

    private fun displayName(uri: Uri): String {
        var name = "PDF Book"
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) name = it.getString(0) ?: name
        }
        return name
    }

    @Composable
    fun AIReaderApp(
        selectedUri: Uri?,
        selectedName: String,
        onOpenPdf: (Pair<Uri,String>) -> Unit,
        onCloseReader: () -> Unit
    ) {
        if (selectedUri != null) {
            ReaderScreen(selectedUri, selectedName, onCloseReader)
        } else {
            HomeScreen(onOpenPdf)
        }
    }

    @Composable
    fun HomeScreen(onOpenPdf: (Pair<Uri,String>) -> Unit) {
        var books by remember { mutableStateOf(listOf<Book>()) }
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val b = Book(displayName(it), it)
                books = (books + b).distinctBy { x -> x.uri.toString() }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("AI Reader", fontSize = 24.sp) })
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    launcher.launch(arrayOf("application/pdf"))
                }) { Text("+", fontSize = 28.sp) }
            }
        ) { pad ->
            Column(
                modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp)
            ) {
                Text("Your Library", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                if (books.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📚", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("No books yet")
                            Text("Tap + to add a PDF")
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(books) { book ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onOpenPdf(book.uri to book.name)
                                },
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("📕", fontSize = 42.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(book.name, maxLines = 2)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Tap to read", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ReaderScreen(uri: Uri, title: String, onBack: () -> Unit) {
        var page by remember { mutableIntStateOf(0) }
        var totalPages by remember { mutableIntStateOf(0) }
        var showAi by remember { mutableStateOf(false) }
        var note by remember { mutableStateOf("") }
        var highlights by remember { mutableStateOf(listOf<Color>()) }

        LaunchedEffect(uri) {
            totalPages = try {
                contentResolver.openFileDescriptor(uri, "r")?.use { PdfRenderer(it).pageCount } ?: 0
            } catch (_: Exception) { 0 }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, maxLines = 1) },
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text("←") }
                    }
                )
            },
            bottomBar = {
                Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { showAi = true }) { Text("🤖 AI") }
                        TextButton(onClick = {
                            tts.speak("AI Reader. Page ${page + 1}", TextToSpeech.QUEUE_FLUSH, null, "page")
                        }) { Text("🔊 Read") }
                        TextButton(onClick = { note = "New note..." }) { Text("📝 Note") }
                        TextButton(onClick = {
                            highlights = highlights + Color.Yellow
                        }) { Text("🖍 Highlight") }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { if (page > 0) page-- }) { Text("Previous") }
                        Text("${if (totalPages == 0) 0 else page + 1} / $totalPages")
                        TextButton(onClick = { if (page + 1 < totalPages) page++ }) { Text("Next") }
                    }
                }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📖", fontSize = 70.sp)
                    Text("PDF page ${page + 1}", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text("PDF rendering module is ready for the next build step.")
                    if (note.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note") }
                        )
                    }
                    if (highlights.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Highlights saved: ${highlights.size}")
                    }
                }
            }
        }

        if (showAi) {
            AlertDialog(
                onDismissRequest = { showAi = false },
                title = { Text("AI Tools") },
                text = {
                    Column {
                        TextButton(onClick = { showAi = false }) { Text("✨ Summarize this page") }
                        TextButton(onClick = { showAi = false }) { Text("🧠 Explain this page") }
                        TextButton(onClick = { showAi = false }) { Text("🌍 Translate selected text") }
                        TextButton(onClick = { showAi = false }) { Text("💬 Ask about this book") }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAi = false }) { Text("Close") }
                }
            )
        }
    }
}
