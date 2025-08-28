package com.example.texteditor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.texteditor.ui.actions.TextEditorActions
import com.example.texteditor.ui.state.TextEditorState

@Composable
fun CompileDialog(
    state: TextEditorState,
    onDismiss: () -> Unit
) {
    if (state.showCompileDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Compilation Result") },
            text = {
                val msg = state.compileResult?.let { res ->
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
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        )
    }
}

@Composable
fun AddLanguageDialog(
    state: TextEditorState,
    onSelectJson: () -> Unit,
    onDismiss: () -> Unit
) {
    if (state.showAddLanguageDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Custom Language") },
            text = {
                Column {
                    Text("Select a JSON file with syntax rules")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onSelectJson) {
                        Text("Select JSON")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        )
    }
}

@Composable
fun RemoveLanguageDialog(
    state: TextEditorState,
    actions: TextEditorActions,
    onDismiss: () -> Unit
) {
    if (state.showRemoveLanguageDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Remove Language") },
            text = {
                Column {
                    state.languages.keys.filter {
                        it !in listOf("kotlin","java","python","c")
                    }.forEach { lang ->
                        Button(onClick = {
                            actions.removeLanguage(lang)
                            onDismiss()
                        }) {
                            Text("Remove $lang")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        )
    }
}

@Composable
fun AllDialogs(
    state: TextEditorState,
    actions: TextEditorActions,
    onSelectJson: () -> Unit
) {
    CompileDialog(
        state = state,
        onDismiss = { state.showCompileDialog = false }
    )

    AddLanguageDialog(
        state = state,
        onSelectJson = onSelectJson,
        onDismiss = { state.showAddLanguageDialog = false }
    )

    RemoveLanguageDialog(
        state = state,
        actions = actions,
        onDismiss = { state.showRemoveLanguageDialog = false }
    )
}