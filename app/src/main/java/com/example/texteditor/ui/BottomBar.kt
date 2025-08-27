package com.example.texteditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo

@Composable
fun BottomButtonsRow(
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCompile: () -> Unit,
    onTab: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onUndo) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo") }
        IconButton(onClick = onRedo) { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo") }
        IconButton(onClick = onTab) { Icon(Icons.AutoMirrored.Filled.KeyboardTab, contentDescription = "Tab") }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onCompile) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Compile")
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
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Chars: ${codeText.length}  Words: ${codeText.split(Regex("\\s+")).filter { it.isNotEmpty() }.size}",
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = fileName,
            fontSize = 12.sp,
            maxLines = 1
        )

        Spacer(modifier = Modifier.weight(1f))

        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selectedLanguage, fontSize = 12.sp)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                languageList.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang) },
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
fun FindReplaceBar(
    query: String,
    replaceText: String,
    caseSensitive: Boolean,
    onQueryChange: (String) -> Unit,
    onReplaceTextChange: (String) -> Unit,
    onCaseSensitiveChange: (Boolean) -> Unit,
    onFindNext: () -> Unit,
    onReplace: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp)
    ) {
        // First row: Find and Replace text fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Find") },
                modifier = Modifier
                    .weight(0.6f)
                    .padding(end = 4.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = replaceText,
                onValueChange = onReplaceTextChange,
                label = { Text("Replace") },
                modifier = Modifier
                    .weight(0.4f)
                    .padding(start = 4.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Second row: Case-sensitive checkbox and buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Checkbox(
                    checked = caseSensitive,
                    onCheckedChange = onCaseSensitiveChange
                )
                Text("Case Sensitive", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onFindNext) { Icon(Icons.Default.Search, contentDescription = "Find Next") }
            IconButton(onClick = onReplace) { Icon(Icons.Default.Refresh, contentDescription = "Replace") }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
    }
}
