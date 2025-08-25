package com.example.texteditor
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.texteditor.ui.theme.TextEditorTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivity : ComponentActivity() {

    private val syntaxConfig = mapOf(
        "kotlin" to SyntaxRules(
            keywords = listOf(
                "fun", "val", "var", "if", "else", "for", "while", "class", "object",
                "package", "import", "return", "when", "try", "catch", "finally",
                "do", "is", "in", "as", "break", "continue", "throw", "super", "this"
            ),
            stringPattern = "\".*?\"".toRegex(),
            commentPattern = "//.*?$".toRegex(RegexOption.MULTILINE)
        ),
        "java" to SyntaxRules(
            keywords = listOf(
                "class", "public", "static", "void", "int", "double", "new", "if", "else",
                "for", "while", "return", "try", "catch", "finally", "import", "package"
            ),
            stringPattern = "\".*?\"".toRegex(),
            commentPattern = "//.*?$".toRegex(RegexOption.MULTILINE)
        ),
        "python" to SyntaxRules(
            keywords = listOf(
                "def", "return", "if", "elif", "else", "for", "while", "import", "as",
                "from", "class", "try", "except", "finally", "with", "lambda", "pass"
            ),
            stringPattern = "\"\"\".*?\"\"\"|'''.*?'''|\".*?\"|'.*?'".toRegex(RegexOption.DOT_MATCHES_ALL),
            commentPattern = "#.*?$".toRegex(RegexOption.MULTILINE)
        )
    )

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TextEditorTheme {
                var codeText by remember { mutableStateOf("") }
                var fileUri by remember { mutableStateOf<Uri?>(null) }
                var fileName by remember { mutableStateOf("Untitled.kt") }
                var currentLanguage by remember { mutableStateOf("kotlin") }

                val undoStack = remember { mutableStateListOf<String>() }
                val redoStack = remember { mutableStateListOf<String>() }
                var isUndoOrRedo by remember { mutableStateOf(false) }

                val onCodeChange: (String) -> Unit = { newText ->
                    if (!isUndoOrRedo) {
                        undoStack.add(codeText)
                        redoStack.clear()
                    }
                    codeText = newText
                    isUndoOrRedo = false
                }

                fun undo() {
                    if (undoStack.isNotEmpty()) {
                        val previous = undoStack.removeAt(undoStack.size - 1)
                        redoStack.add(codeText)
                        isUndoOrRedo = true
                        codeText = previous
                    }
                }

                fun redo() {
                    if (redoStack.isNotEmpty()) {
                        val next = redoStack.removeAt(redoStack.size - 1)
                        undoStack.add(codeText)
                        isUndoOrRedo = true
                        codeText = next
                    }
                }

                val openFileLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = { uri ->
                        uri?.let {
                            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            fileUri = it
                            fileName = getFileName(this@MainActivity, it)
                            codeText = readTextFromUri(this@MainActivity, it)

                            // ✅ Proper extension detection
                            val extension = fileName.substringAfterLast('.', "").lowercase()
                            currentLanguage = when (extension) {
                                "kt" -> "kotlin"
                                "java" -> "java"
                                "py" -> "python"
                                else -> "kotlin"
                            }
                        }
                    }
                )

                val saveFileLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("*/*"),
                    onResult = { uri ->
                        uri?.let {
                            fileUri = it
                            writeTextToUri(this, it, codeText)
                            fileName = getFileName(this@MainActivity, it)
                        }
                    }
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Text Editor") },
                            actions = {
                                IconButton(onClick = {
                                    codeText = ""
                                    fileUri = null
                                    fileName = "Untitled.kt"
                                    currentLanguage = "kotlin"
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "New", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = { openFileLauncher.launch(arrayOf("*/*")) }) {
                                    Icon(Icons.Filled.FolderOpen, contentDescription = "Open", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = {
                                    if (fileUri != null) {
                                        writeTextToUri(this@MainActivity, fileUri!!, codeText)
                                    } else {
                                        saveFileLauncher.launch(fileName)
                                    }
                                }) {
                                    Icon(Icons.Filled.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        )
                    },
                    bottomBar = {
                        Column(
                            modifier = Modifier.imePadding() // ✅ pushes above keyboard
                        ) {
                            BottomButtonsRow(
                                onUndo = { undo() },
                                onRedo = { redo() },
                                onCompile = { /* TODO */ },
                                onTab = { codeText += "    " }
                            )
                            StatusBarWithLanguageSelect(
                                codeText = codeText,
                                fileName = fileName,
                                languageList = syntaxConfig.keys.toList(),
                                selectedLanguage = currentLanguage,
                                onLanguageChange = { currentLanguage = it }
                            )
                        }
                    }

                ) { innerPadding ->
                    EditorScreen(
                        codeText = codeText,
                        onCodeChange = { input -> onCodeChange(processAutoInsert(input, codeText)) },
                        fileName = fileName,
                        syntaxRules = syntaxConfig[currentLanguage] ?: syntaxConfig["kotlin"]!!,
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun BottomButtonsRow(onUndo: () -> Unit, onRedo: () -> Unit, onCompile: () -> Unit, onTab: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onUndo) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.onSurface)
        }
        IconButton(onClick = onRedo) {
            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = MaterialTheme.colorScheme.onSurface)
        }
        IconButton(onClick = onTab) {
            Icon(Icons.Default.KeyboardTab, contentDescription = "Tab", tint = MaterialTheme.colorScheme.onSurface)
        }
        IconButton(onClick = onCompile) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Compile", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}


@Composable
fun StatusBarWithLanguageSelect(
    codeText: String,
    fileName: String,
    languageList: List<String>,
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = "Chars: ${codeText.length}  Words: ${codeText.split(Regex("\\s+")).filter { it.isNotEmpty() }.size}",
            color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(1f)
        )
        Text(text = fileName, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Box(modifier = Modifier.weight(1f), contentAlignment = androidx.compose.ui.Alignment.CenterEnd) {
            TextButton(onClick = { expanded = true }) {
                Text(selectedLanguage.replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Language", tint = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                languageList.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang.replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            onLanguageChange(lang)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EditorScreen(
    codeText: String,
    onCodeChange: (String) -> Unit,
    fileName: String,
    syntaxRules: SyntaxRules,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)).verticalScroll(scrollState).padding(8.dp)
        ) {
            BasicTextField(
                value = codeText,
                onValueChange = onCodeChange,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = Color.Transparent),
                modifier = Modifier.fillMaxSize().padding(8.dp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    Box {
                        Text(
                            text = highlightCode(codeText, syntaxRules),
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        )
                        innerTextField()
                    }
                }
            )
        }
    }
}

data class SyntaxRules(val keywords: List<String>, val stringPattern: Regex, val commentPattern: Regex)

fun highlightCode(code: String, rules: SyntaxRules): AnnotatedString {
    val keywordPattern = "\\b(${rules.keywords.joinToString("|")})\\b".toRegex()
    return buildAnnotatedString {
        append(code)
        keywordPattern.findAll(code).forEach {
            addStyle(SpanStyle(color = Color.Cyan), it.range.first, it.range.last + 1)
        }
        rules.stringPattern.findAll(code).forEach {
            addStyle(SpanStyle(color = Color(0xFF6A9955)), it.range.first, it.range.last + 1)
        }
        rules.commentPattern.findAll(code).forEach {
            addStyle(SpanStyle(color = Color.Gray), it.range.first, it.range.last + 1)
        }
    }
}

/* -------- File Utils -------- */
fun readTextFromUri(context: Context, uri: Uri): String {
    val inputStream = context.contentResolver.openInputStream(uri)
    val reader = BufferedReader(InputStreamReader(inputStream))
    return reader.readText()
}
fun writeTextToUri(context: Context, uri: Uri, text: String) {
    val outputStream = context.contentResolver.openOutputStream(uri)
    val writer = OutputStreamWriter(outputStream)
    writer.write(text); writer.flush(); writer.close()
}
fun getFileName(context: Context, uri: Uri): String {
    var name = "Untitled"
    val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) name = it.getString(nameIndex)
        }
    }
    return name
}

/* -------- Auto Insert (Brackets, Quotes, Tabs) -------- */
fun processAutoInsert(newText: String, oldText: String): String {
    if (newText.length <= oldText.length) return newText
    val lastChar = newText.last()
    return when (lastChar) {
        '(' -> newText + ")"
        '{' -> newText + "}"
        '[' -> newText + "]"
        '"' -> newText + "\""
        '\'' -> newText + "'"
        '\t' -> oldText + "    "
        else -> newText
    }
}
