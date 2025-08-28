package com.example.texteditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.example.texteditor.ui.actions.TextEditorActions
import com.example.texteditor.ui.state.TextEditorState
import kotlinx.coroutines.delay

@Composable
fun AutoSaveEffect(
    state: TextEditorState,
    actions: TextEditorActions
) {
    LaunchedEffect(state.codeTextState.text, state.fileUri) {
        snapshotFlow { state.codeTextState.text }
            .collect { newText ->
                if (state.fileUri != null) {
                    delay(1500) // Wait for 1.5 seconds of inactivity
                    actions.saveTextToFile(state.fileUri, newText)
                    println("Auto-saved: ${state.fileName}")
                }
            }
    }
}