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
