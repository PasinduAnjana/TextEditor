package com.example.texteditor.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.texteditor.utils.FileUtils.readTextFromUri
import com.example.texteditor.utils.FileUtils.writeTextToUri
import com.example.texteditor.utils.FileUtils.getFileName
import com.example.texteditor.utils.SyntaxRules
import com.example.texteditor.utils.AutoInsert.processAutoInsert
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.ui.platform.LocalContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorApp(syntaxConfig: Map<String, SyntaxRules>) {
    val context = LocalContext.current // Only inside @Composable

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
                fileUri = it
                fileName = getFileName(context, it)
                codeText = readTextFromUri(context, it)

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
                writeTextToUri(context, it, codeText)
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
                        codeText = ""
                        fileUri = null
                        fileName = "Untitled.kt"
                        currentLanguage = "kotlin"
                    }) { Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "New") }

                    IconButton(onClick = { openFileLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Open")
                    }

                    IconButton(onClick = {
                        if (fileUri != null) writeTextToUri(context, fileUri!!, codeText)
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
                    .imePadding()           // Moves up with keyboard
                    .navigationBarsPadding() // Accounts for gesture/navigation bar
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
            syntaxRules = syntaxConfig[currentLanguage] ?: syntaxConfig["kotlin"]!!,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
