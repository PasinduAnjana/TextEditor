package com.example.texteditor.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.texteditor.utils.FileUtils.getFileName
import com.example.texteditor.utils.FileUtils.readTextFromUri
import com.example.texteditor.utils.FileUtils.writeTextToUri
import com.example.texteditor.utils.SyntaxRules
import com.example.texteditor.utils.AutoInsert.processAutoInsert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorApp(syntaxConfig: Map<String, SyntaxRules>) {
    val context = LocalContext.current

    var codeTextState by remember { mutableStateOf(TextFieldValue("")) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("Untitled.kt") }
    var currentLanguage by remember { mutableStateOf("kotlin") }

    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }
    var isUndoOrRedo by remember { mutableStateOf(false) }

    var showFindReplace by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }


    val onCodeChange: (TextFieldValue) -> Unit = { newValue ->
        if (!isUndoOrRedo) {
            undoStack.add(codeTextState)
            redoStack.clear()
        }
        codeTextState = newValue
        isUndoOrRedo = false
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(codeTextState)
            isUndoOrRedo = true
            codeTextState = previous
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(codeTextState)
            isUndoOrRedo = true
            codeTextState = next
        }
    }

    fun findNext() {
        if (searchQuery.isNotEmpty()) {
            val startIndex = codeTextState.selection.end
            val index = codeTextState.text.indexOf(searchQuery, startIndex, ignoreCase = true)
                .takeIf { it >= 0 }
                ?: codeTextState.text.indexOf(searchQuery, ignoreCase = true) // wrap around

            if (index >= 0) {
                codeTextState = codeTextState.copy(
                    selection = TextRange(index, index + searchQuery.length)
                )
            }
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
            findNext() // if no selection, just find
        }
    }


    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                fileUri = it
                fileName = getFileName(context, it)
                val text = readTextFromUri(context, it)
                codeTextState = TextFieldValue(text)

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
                writeTextToUri(context, it, codeTextState.text)
                fileName = getFileName(context, it)
            }
        }
    )

    Scaffold(
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
                        if (fileUri != null) writeTextToUri(context, fileUri!!, codeTextState.text)
                        else saveFileLauncher.launch(fileName)
                    }) { Icon(Icons.Filled.Save, contentDescription = "Save") }

                    IconButton(onClick = { showFindReplace = !showFindReplace }) {
                        Icon(Icons.Default.Search, contentDescription = "Find & Replace")
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
                if (showFindReplace) {
                    FindReplaceBar(
                        query = searchQuery,
                        replaceText = replaceText,
                        onQueryChange = { searchQuery = it },
                        onReplaceTextChange = { replaceText = it },
                        onFindNext = { findNext() },
                        onReplace = { replaceCurrent() },
                        onClose = { showFindReplace = false }
                    )
                }
                BottomButtonsRow(
                    onUndo = { undo() },
                    onRedo = { redo() },
                    onCompile = { /* TODO */ },
                    onTab = {
                        val cursor = codeTextState.selection.start
                        val newText = codeTextState.text.substring(0, cursor) +
                                "    " +
                                codeTextState.text.substring(cursor)
                        codeTextState = codeTextState.copy(
                            text = newText,
                            selection = TextRange(cursor + 4)
                        )
                    }
                )

                StatusBarWithLanguageSelect(
                    codeText = codeTextState.text,
                    fileName = fileName,
                    languageList = syntaxConfig.keys.toList(),
                    selectedLanguage = currentLanguage,
                    onLanguageChange = { currentLanguage = it }
                )
            }
        }
    ) { innerPadding ->
        EditorScreen(
            codeTextState = codeTextState,
            onCodeChange = { newValue: TextFieldValue ->
                val newText: String = processAutoInsert(newValue.text, codeTextState.text) // Returns String
                onCodeChange(
                    TextFieldValue(
                        text = newText,
                        selection = newValue.selection // preserve cursor
                    )
                )
            },

            syntaxRules = syntaxConfig[currentLanguage] ?: syntaxConfig["kotlin"]!!,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
