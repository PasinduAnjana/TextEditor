package com.example.texteditor.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.example.texteditor.ui.actions.TextEditorActions
import com.example.texteditor.ui.state.TextEditorState

class FileLaunchers(
    private val state: TextEditorState,
    private val actions: TextEditorActions
) {
    @Composable
    fun rememberOpenFileLauncher() = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { actions.onFileOpened(it) } }
    )

    @Composable
    fun rememberSaveFileLauncher() = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = { uri -> uri?.let { actions.onFileSaved(it) } }
    )

    @Composable
    fun rememberAddLanguageLauncher() = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { actions.onLanguageAdded(it) } }
    )
}

@Composable
fun rememberFileLaunchers(
    state: TextEditorState,
    actions: TextEditorActions
): FileLaunchers {
    return FileLaunchers(state, actions)
}