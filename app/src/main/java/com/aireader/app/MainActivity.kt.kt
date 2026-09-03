@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aireader.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Book(
    val name: String,
    val uri: Uri
)

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

        var selectedBook by remember {
            mutableStateOf<Book?>(null)
        }

        if (selectedBook == null) {

            HomeScreen(
                onBookSelected = {
                    selectedBook = it
                }
            )

        } else {

            ReaderScreen(
                book = selectedBook!!,
                onBack = {
                    selectedBook = null
                }
            )
        }
    }

    private fun getFileName(uri: Uri): String {

        var name = "PDF Book"

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

        return name
    }

    @Composable
    private fun HomeScreen(
        onBookSelected: (Book) -> Unit
    ) {

        var books by remember {
            mutableStateOf<List<Book>>(emptyList())
        }

        val pdfLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->

                if (uri != null) {

                    try {

                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                    } catch (_: Exception) {
                    }

                    val book = Book(
                        name = getFileName(uri),
                        uri = uri
                    )

                    books =
                        (books + book)
                            .distinctBy {
                                it.uri.toString()
                            }

                    onBookSelected(book)
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
            },

            floatingActionButton = {

                FloatingActionButton(

                    onClick = {

                        pdfLauncher.launch(
                            arrayOf("application/pdf")
                        )
                    }

                ) {

                    Text(
                        text = "+",
                        fontSize = 28.sp
                    )
                }
            }

        ) { padding ->

            Column(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)

            ) {

                Text(
                    text = "📚 My Library",
                    style =
                        MaterialTheme.typography.headlineSmall
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                if (books.isEmpty()) {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(20.dp)
                    ) {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(30.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally

                        ) {

                            Text(
                                text = "📖",
                                fontSize = 60.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            Text(
                                text = "No books yet",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleLarge
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            Text(
                                text =
                                    "Tap + to add your first PDF"
                            )
                        }
                    }

                } else {

                    LazyVerticalGrid(

                        columns =
                            GridCells.Fixed(2),

                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)

                    ) {

                        items(books) { book ->

                            Card(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onBookSelected(book)
                                        },

                                shape =
                                    RoundedCornerShape(18.dp)

                            ) {

                                Column(
                                    modifier =
                                        Modifier.padding(16.dp)
                                ) {

                                    Text(
                                        text = "📕",
                                        fontSize = 45.sp
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.height(8.dp)
                                    )

                                    Text(
                                        text = book.name,
                                        maxLines = 2
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.height(5.dp)
                                    )

                                    Text(
                                        text = "Tap to read",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ReaderScreen(
        book: Book,
        onBack: () -> Unit
    ) {

        var currentPage by remember {
            mutableIntStateOf(1)
        }

        var totalPages by remember {
            mutableIntStateOf(0)
        }

        var showAiDialog by remember {
            mutableStateOf(false)
        }

        var showNoteDialog by remember {
            mutableStateOf(false)
        }

        var note by remember {
            mutableStateOf("")
        }

        var highlightCount by remember {
            mutableIntStateOf(0)
        }

        Scaffold(

            topBar = {

                TopAppBar(

                    title = {
                        Text(
                            text = book.name,
                            maxLines = 1
                        )
                    },

                    navigationIcon = {

                        TextButton(
                            onClick = onBack
                        ) {

                            Text("←")
                        }
                    }
                )
            },

            bottomBar = {

                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Row(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(4.dp),

                        horizontalArrangement =
                            Arrangement.SpaceEvenly

                    ) {

                        TextButton(
                            onClick = {
                                showAiDialog = true
                            }
                        ) {
                            Text("🤖 AI")
                        }

                        TextButton(
                            onClick = {
                                speakPage(currentPage)
                            }
                        ) {
                            Text("🔊 Read")
                        }

                        TextButton(
                            onClick = {
                                showNoteDialog = true
                            }
                        ) {
                            Text("📝 Note")
                        }

                        TextButton(
                            onClick = {
                                highlightCount++
                            }
                        ) {
                            Text("🖍 Highlight")
                        }
                    }

                    Row(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(8.dp),

                        horizontalArrangement =
                            Arrangement.SpaceBetween,

                        verticalAlignment =
                            Alignment.CenterVertically

                    ) {

                        TextButton(

                            enabled =
                                currentPage > 1,

                            onClick = {

                                if (currentPage > 1) {
                                    currentPage--
                                }
                            }

                        ) {
                            Text("Previous")
                        }

                        Text(
                            text =
                                if (totalPages > 0) {
                                    "$currentPage / $totalPages"
                                } else {
                                    "Page $currentPage"
                                }
                        )

                        TextButton(

                            enabled =
                                totalPages == 0 ||
                                        currentPage < totalPages,

                            onClick = {

                                currentPage++
                            }

                        ) {
                            Text("Next")
                        }
                    }
                }
            }

        ) { padding ->

            Box(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),

                contentAlignment =
                    Alignment.Center

            ) {

                Column(

                    horizontalAlignment =
                        Alignment.CenterHorizontally

                ) {

                    Text(
                        text = "📖",
                        fontSize = 80.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text = "PDF Reader",
                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Page $currentPage"
                    )

                    if (highlightCount > 0) {

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        Text(
                            text =
                                "🖍 Highlights: $highlightCount"
                        )
                    }
                }
            }
        }

        if (showNoteDialog) {

            AlertDialog(

                onDismissRequest = {
                    showNoteDialog = false
                },

                title = {
                    Text("📝 Add Note")
                },

                text = {

                    OutlinedTextField(

                        value = note,

                        onValueChange = {
                            note = it
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        label = {
                            Text("Your note")
                        }
                    )
                },

                confirmButton = {

                    TextButton(
                        onClick = {
                            showNoteDialog = false
                        }
                    ) {

                        Text("Save")
                    }
                },

                dismissButton = {

                    TextButton(
                        onClick = {
                            showNoteDialog = false
                        }
                    ) {

                        Text("Cancel")
                    }
                }
            )
        }

        if (showAiDialog) {

            AlertDialog(

                onDismissRequest = {
                    showAiDialog = false
                },

                title = {
                    Text("🤖 AI Tools")
                },

                text = {

                    Column {

                        TextButton(
                            onClick = {
                                showAiDialog = false
                            }
                        ) {
                            Text(
                                "✨ Summarize this page"
                            )
                        }

                        TextButton(
                            onClick = {
                                showAiDialog = false
                            }
                        ) {
                            Text(
                                "🧠 Explain this page"
                            )
                        }

                        TextButton(
                            onClick = {
                                showAiDialog = false
                            }
                        ) {
                            Text(
                                "🌍 Translate selected text"
                            )
                        }

                        TextButton(
                            onClick = {
                                showAiDialog = false
                            }
                        ) {
                            Text(
                                "💬 Ask about this book"
                            )
                        }
                    }
                },

                confirmButton = {

                    TextButton(
                        onClick = {
                            showAiDialog = false
                        }
                    ) {
                        Text("Close")
                    }
                }
            )
        }
    }

    private fun speakPage(page: Int) {

        val text =
            "You are currently reading page $page in AI Reader."

        val speaker =
            TextToSpeech(
                this
            ) { status ->

                if (status ==
                    TextToSpeech.SUCCESS
                ) {

                    speakerLanguageAndSpeak(
                        speaker,
                        text
                    )
                }
            }
    }

    private fun speakerLanguageAndSpeak(
        speaker: TextToSpeech,
        text: String
    ) {

        speaker.language =
            java.util.Locale.US

        speaker.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "ai_reader_speech"
        )
    }
}