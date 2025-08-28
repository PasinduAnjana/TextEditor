package com.example.texteditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.texteditor.ui.actions.rememberTextEditorActions
import com.example.texteditor.ui.state.rememberTextEditorState
import com.example.texteditor.utils.SyntaxRules

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorApp(initialSyntaxConfig: Map<String, SyntaxRules>) {
    val state = rememberTextEditorState(initialSyntaxConfig)
    val snackbarHostState = remember { SnackbarHostState() }
    val actions = rememberTextEditorActions(state, snackbarHostState)
    val launchers = rememberFileLaunchers(state, actions)

    val openFileLauncher = launchers.rememberOpenFileLauncher()
    val saveFileLauncher = launchers.rememberSaveFileLauncher()
    val addLanguageLauncher = launchers.rememberAddLanguageLauncher()

    AutoSaveEffect(state, actions)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Text Editor") },
                actions = {
                    IconButton(onClick = { state.newFile() }) {
                        Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "New")
                    }

                    IconButton(onClick = {
                        openFileLauncher.launch(arrayOf("*/*"))
                    }) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Open")
                    }

                    IconButton(onClick = {
                        if (state.fileUri != null) {
                            actions.saveCurrentFile()
                        } else {
                            val correctFileName = actions.getFileNameWithCorrectExtension()
                            saveFileLauncher.launch(correctFileName)
                        }
                    }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save")
                    }

                    IconButton(onClick = { state.showFindReplace = !state.showFindReplace }) {
                        Icon(Icons.Default.Search, contentDescription = "Find & Replace")
                    }

                    Box {
                        IconButton(onClick = { state.showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }

                        DropdownMenu(
                            expanded = state.showMenu,
                            onDismissRequest = { state.showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add Language") },
                                onClick = {
                                    state.showMenu = false
                                    state.showAddLanguageDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove Language") },
                                onClick = {
                                    state.showMenu = false
                                    state.showRemoveLanguageDialog = true
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
                if (state.isCompiling) {
                    Text(
                        "⏳ Compiling...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.2f))
                            .padding(8.dp)
                    )
                }

                if (state.showFindReplace) {
                    FindReplaceBar(
                        query = state.searchQuery,
                        replaceText = state.replaceText,
                        caseSensitive = state.caseSensitive,
                        onQueryChange = { state.searchQuery = it },
                        onReplaceTextChange = { state.replaceText = it },
                        onCaseSensitiveChange = { state.caseSensitive = it },
                        onFindNext = { actions.findNext() },
                        onReplace = { actions.replaceCurrent() },
                        onClose = { state.showFindReplace = false }
                    )
                }

                BottomButtonsRow(
                    onUndo = { state.undo() },
                    onRedo = { state.redo() },
                    onTab = { actions.addTab() },
                    onCompile = { actions.compile() }
                )

                StatusBarWithLanguageSelect(
                    codeText = state.codeTextState.text,
                    fileName = state.fileName,
                    languageList = state.languages.keys.toList(),
                    selectedLanguage = state.currentLanguage,
                    onLanguageChange = { newLanguage ->
                        // Update filename extension when language changes (only for unsaved files)
                        if (state.fileUri == null) {
                            actions.updateFileNameForLanguage(newLanguage)
                        }
                        state.currentLanguage = newLanguage
                    }
                )
            }
        }
    ) { innerPadding ->
        EditorScreen(
            codeTextState = state.codeTextState,
            onCodeChange = state::onCodeChange,
            syntaxRules = state.languages[state.currentLanguage] ?: state.languages["txt"]!!,
            modifier = Modifier.padding(innerPadding)
        )
    }

    AllDialogs(
        state = state,
        actions = actions,
        onSelectJson = { addLanguageLauncher.launch(arrayOf("application/json")) }
    )
}