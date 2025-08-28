package com.example.texteditor.ui.state

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.texteditor.models.CompileResponse
import com.example.texteditor.utils.AutoInsert
import com.example.texteditor.utils.SyntaxRules

@Stable
class TextEditorState {
    var codeTextState by mutableStateOf(TextFieldValue(""))
    var fileUri by mutableStateOf<Uri?>(null)
    var fileName by mutableStateOf("Untitled.txt")
    var currentLanguage by mutableStateOf("txt")

    val undoStack = mutableStateListOf<TextFieldValue>()
    val redoStack = mutableStateListOf<TextFieldValue>()
    var isUndoOrRedo by mutableStateOf(false)

    var showFindReplace by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var replaceText by mutableStateOf("")
    var caseSensitive by mutableStateOf(false)

    var compileResult by mutableStateOf<CompileResponse?>(null)
    var showCompileDialog by mutableStateOf(false)
    var isCompiling by mutableStateOf(false)

    var showAddLanguageDialog by mutableStateOf(false)
    var showRemoveLanguageDialog by mutableStateOf(false)

    var languages by mutableStateOf<MutableMap<String, SyntaxRules>>(mutableMapOf())
    var showMenu by mutableStateOf(false)

    var isLoadingFile by mutableStateOf(false)

    fun onCodeChange(newValue: TextFieldValue) {
        if (!isUndoOrRedo && !isLoadingFile) {
            undoStack.add(codeTextState)
            redoStack.clear()

            val (processedText, newCursor) = AutoInsert.processAutoInsert(
                newValue.text,
                codeTextState.text,
                newValue.selection.start
            )

            val changedByAutoInsert = processedText.length != newValue.text.length

            codeTextState = if (changedByAutoInsert) {
                // AutoInsert added something → force cursor where we want it
                newValue.copy(
                    text = processedText,
                    selection = TextRange(newCursor)
                )
            } else {
                // Normal typing or selection → keep user’s selection
                newValue.copy(text = processedText)
            }
        } else {
            codeTextState = newValue
            isUndoOrRedo = false
            isLoadingFile = false
        }
    }



    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(codeTextState)
            isUndoOrRedo = true
            codeTextState = prev
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(codeTextState)
            isUndoOrRedo = true
            codeTextState = next
        }
    }

    fun newFile() {
        codeTextState = TextFieldValue("")
        fileUri = null
        fileName = "Untitled.txt"
        currentLanguage = "txt"
    }
}

@Composable
fun rememberTextEditorState(initialSyntaxConfig: Map<String, SyntaxRules>): TextEditorState {
    return remember {
        TextEditorState().apply {
            languages = initialSyntaxConfig.toMutableMap()
        }
    }
}