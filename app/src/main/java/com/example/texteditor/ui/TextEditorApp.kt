package com.example.texteditor.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.texteditor.models.CompileError
import com.example.texteditor.models.CompileResponse
import com.example.texteditor.network.CompilerApi
import com.example.texteditor.utils.AutoInsert
import com.example.texteditor.utils.FileUtils.getFileName
import com.example.texteditor.utils.FileUtils.readTextFromUri
import com.example.texteditor.utils.FileUtils.writeTextToUri
import com.example.texteditor.utils.SyntaxRules
import com.example.texteditor.utils.AutoInsert.processAutoInsert
import kotlinx.coroutines.launch
import java.io.File
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorApp(initialSyntaxConfig: Map<String, SyntaxRules>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ---------------- States ----------------
    var codeTextState by remember { mutableStateOf(TextFieldValue("")) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("Untitled.kt") }
    var currentLanguage by remember { mutableStateOf("kotlin") }

    var undoStack = remember { mutableStateListOf<TextFieldValue>() }
    var redoStack = remember { mutableStateListOf<TextFieldValue>() }
    var isUndoOrRedo by remember { mutableStateOf(false) }

    var showFindReplace by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }

    var compileResult by remember { mutableStateOf<CompileResponse?>(null) }
    var showCompileDialog by remember { mutableStateOf(false) }
    var isCompiling by remember { mutableStateOf(false) }

    var showAddLanguageDialog by remember { mutableStateOf(false) }
    var showRemoveLanguageDialog by remember { mutableStateOf(false) }

    var languages by remember { mutableStateOf(initialSyntaxConfig.toMutableMap()) }
    var showMenu by remember { mutableStateOf(false) }

    var isLoadingFile by remember { mutableStateOf(false) } // <-- prevents auto-insert on load
    val snackbarHostState = remember { SnackbarHostState() }

    // ---------------- Undo / Redo ----------------
    val onCodeChange: (TextFieldValue) -> Unit = { newValue ->
        if (!isUndoOrRedo && !isLoadingFile) {
            undoStack.add(codeTextState)
            redoStack.clear()

            val (processedText, newCursor) = AutoInsert.processAutoInsert(
                newValue.text,
                codeTextState.text,
                newValue.selection.start
            )

            codeTextState = TextFieldValue(
                text = processedText,
                selection = TextRange(newCursor)
            )
        } else {
            codeTextState = newValue
            isUndoOrRedo = false
            isLoadingFile = false
        }
    }



    @RequiresApi(Build.VERSION_CODES.N)
    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(codeTextState)
            isUndoOrRedo = true
            codeTextState = prev
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(codeTextState)
            isUndoOrRedo = true
            codeTextState = next
        }
    }

    // ---------------- Find / Replace ----------------
    fun findNext() {
        if (searchQuery.isEmpty()) return
        val startIndex = codeTextState.selection.end
        val textToSearch = if (caseSensitive) codeTextState.text else codeTextState.text.lowercase()
        val queryToSearch = if (caseSensitive) searchQuery else searchQuery.lowercase()
        val index = textToSearch.indexOf(queryToSearch, startIndex).takeIf { it >= 0 }
            ?: textToSearch.indexOf(queryToSearch) // wrap around
        if (index >= 0) {
            codeTextState = codeTextState.copy(
                selection = TextRange(index, index + searchQuery.length)
            )
        }
    }

    fun replaceCurrent() {
        val sel = codeTextState.selection
        if (sel.start < sel.end) {
            val before = codeTextState.text.substring(0, sel.start)
            val after = codeTextState.text.substring(sel.end)
            val newText = before + replaceText + after
            codeTextState = TextFieldValue(
                text = newText,
                selection = TextRange(sel.start + replaceText.length)
            )
        } else {
            findNext()
        }
    }

    fun saveTextToFile(context: Context, uri: Uri?, text: String) {
        if (uri == null) return

        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(text.toByteArray())
                output.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    LaunchedEffect(codeTextState.text, fileUri) {
        snapshotFlow { codeTextState.text }
            .collect { newText ->
                if (fileUri != null) {
                    // ⏳ wait for 1.5 seconds of inactivity
                    kotlinx.coroutines.delay(1500)

                    // Save latest content to file
                    saveTextToFile(context, fileUri, newText)

                    // ✅ Optional: Show confirmation in logs or snackbar
                    println("Auto-saved: $fileName")
                }
            }
    }


    // ---------------- File Launchers ----------------
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                fileUri = it
                fileName = getFileName(context, it)
                val text = readTextFromUri(context, it)
                isLoadingFile = true // disable auto-insert when loading
                codeTextState = TextFieldValue(text)

                val ext = fileName.substringAfterLast('.', "").lowercase()
                currentLanguage = when (ext) {
                    "kt" -> "kotlin"
                    "java" -> "java"
                    "py" -> "python"
                    "c" -> "c"
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
                writeTextToUri(context, it, codeTextState.text)
                fileName = getFileName(context, it)
                scope.launch { snackbarHostState.showSnackbar("💾 File saved as $fileName") }
            }
        }
    )


    // ---------------- JSON Language Launcher ----------------
    val addLanguageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val jsonText = readTextFromUri(context, it)
                parseSyntaxJson(jsonText)?.let { (name, rules) ->
                    languages[name] = rules
                    context.openFileOutput("$name.json", 0).use { it.write(jsonText.toByteArray()) }
                    scope.launch { snackbarHostState.showSnackbar("✅ Language '$name' added") }
                }
            }
        }
    )

    // ---------------- UI ----------------
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Text Editor") },
                actions = {
                    IconButton(onClick = {
                        codeTextState = TextFieldValue("")
                        fileUri = null
                        fileName = "Untitled.kt"
                        currentLanguage = "kotlin"
                    }) { Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "New") }

                    IconButton(onClick = { openFileLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Open")
                    }

                    IconButton(onClick = {
                        if (fileUri != null) {
                            try {
                                saveTextToFile(context, fileUri, codeTextState.text)
                                scope.launch {
                                    snackbarHostState.showSnackbar("💾 File saved as $fileName")
                                }
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("⚠️ Error saving file: ${e.message}")
                                }
                            }
                        } else {
                            saveFileLauncher.launch(fileName)
                        }
                    }) { Icon(Icons.Filled.Save, contentDescription = "Save") }



                    IconButton(onClick = { showFindReplace = !showFindReplace }) {
                        Icon(Icons.Default.Search, contentDescription = "Find & Replace")
                    }

                    // Overflow menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add Language") },
                                onClick = {
                                    showMenu = false
                                    showAddLanguageDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove Language") },
                                onClick = {
                                    showMenu = false
                                    showRemoveLanguageDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                if (isCompiling) {
                    Text(
                        "⏳ Compiling...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.2f))
                            .padding(8.dp)
                    )
                }

                if (showFindReplace) {
                    FindReplaceBar(
                        query = searchQuery,
                        replaceText = replaceText,
                        caseSensitive = caseSensitive,
                        onQueryChange = { searchQuery = it },
                        onReplaceTextChange = { replaceText = it },
                        onCaseSensitiveChange = { caseSensitive = it },
                        onFindNext = { findNext() },
                        onReplace = { replaceCurrent() },
                        onClose = { showFindReplace = false }
                    )
                }

                BottomButtonsRow(
                    onUndo = { undo() },
                    onRedo = { redo() },
                    onTab = {
                        val cursor = codeTextState.selection.start
                        val newText = codeTextState.text.substring(0, cursor) + "    " +
                                codeTextState.text.substring(cursor)
                        codeTextState = codeTextState.copy(
                            text = newText,
                            selection = TextRange(cursor + 4)
                        )
                    },
                    onCompile = {
                        if (fileUri == null) {
                            compileResult = CompileResponse(false, listOf(CompileError(0,0,"Please save before compiling")), "")
                            showCompileDialog = true
                            return@BottomButtonsRow
                        }
                        scope.launch {
                            try {
                                isCompiling = true
                                val ext = fileName.substringAfterLast('.', "").lowercase()
                                val dir = context.getExternalFilesDir(null)
                                val tmpFile = File(dir, "tmp_code_${System.currentTimeMillis()}.$ext")
                                tmpFile.writeText(codeTextState.text)

                                compileResult = when (ext) {
                                    "kt" -> CompilerApi.compileKotlinAuto(context)
                                    "py" -> CompilerApi.compilePythonAuto(context)
                                    "c" -> CompilerApi.compileCAuto(context)
                                    else -> CompileResponse(false, listOf(CompileError(0,0,"Unsupported file type")), "")
                                }
                            } catch (e: Exception) {
                                compileResult = CompileResponse(false, listOf(CompileError(0,0,e.message ?: "Unknown error")), "")
                            } finally {
                                isCompiling = false
                                showCompileDialog = true
                            }
                        }
                    }
                )

                StatusBarWithLanguageSelect(
                    codeText = codeTextState.text,
                    fileName = fileName,
                    languageList = languages.keys.toList(),
                    selectedLanguage = currentLanguage,
                    onLanguageChange = { currentLanguage = it }
                )
            }
        }
    ) { innerPadding ->
        EditorScreen(
            codeTextState = codeTextState,
            onCodeChange = onCodeChange,
            syntaxRules = languages[currentLanguage] ?: languages["kotlin"]!!,
            modifier = Modifier.padding(innerPadding)
        )
    }

    // Compile dialog
    if (showCompileDialog) {
        AlertDialog(
            onDismissRequest = { showCompileDialog = false },
            title = { Text("Compilation Result") },
            text = {
                val msg = compileResult?.let { res ->
                    buildString {
                        if (res.success) append("✅ Compilation Successful\n")
                        else append("❌ Compilation Failed\n")
                        res.errors.forEach { e ->
                            appendLine("Line ${e.line}, Col ${e.column}: ${e.message}")
                        }
                        if (!res.output.isNullOrEmpty()) appendLine("\nOutput:\n${res.output}")
                    }
                } ?: "Unknown error"
                Text(msg)
            },
            confirmButton = {
                TextButton(onClick = { showCompileDialog = false }) { Text("OK") }
            }
        )
    }

    // Add language dialog
    if (showAddLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showAddLanguageDialog = false },
            title = { Text("Add Custom Language") },
            text = {
                Column {
                    Text("Select a JSON file with syntax rules")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { addLanguageLauncher.launch(arrayOf("application/json")) }) {
                        Text("Select JSON")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddLanguageDialog = false }) { Text("Close") }
            }
        )
    }

    // Remove language dialog
    if (showRemoveLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveLanguageDialog = false },
            title = { Text("Remove Language") },
            text = {
                Column {
                    languages.keys.filter { it !in listOf("kotlin","java","python","c") }.forEach { lang ->
                        Button(onClick = {
                            languages.remove(lang)
                            File(context.filesDir, "$lang.json").delete()
                            showRemoveLanguageDialog = false
                            scope.launch { snackbarHostState.showSnackbar("🗑 Language '$lang' removed") }
                        }) { Text(lang) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRemoveLanguageDialog = false }) { Text("Close") }
            }
        )
    }
}

// ---------------- Parse JSON ----------------
fun parseSyntaxJson(jsonString: String): Pair<String, SyntaxRules>? {
    return try {
        val obj = JSONObject(jsonString)
        val name = obj.getString("name")
        val keywords = obj.getJSONArray("keywords").let { arr -> List(arr.length()) { arr.getString(it) } }
        val stringPattern = obj.getString("stringPattern").toRegex(RegexOption.DOT_MATCHES_ALL)
        val commentPattern = obj.getString("commentPattern").toRegex(RegexOption.MULTILINE)
        name to SyntaxRules(keywords, stringPattern, commentPattern)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
