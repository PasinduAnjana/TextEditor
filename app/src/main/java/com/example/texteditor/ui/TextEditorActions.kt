package com.example.texteditor.ui.actions

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.texteditor.models.CompileError
import com.example.texteditor.models.CompileResponse
import com.example.texteditor.network.CompilerApi
import com.example.texteditor.ui.state.TextEditorState
import com.example.texteditor.utils.FileUtils
import com.example.texteditor.utils.SyntaxRules
import com.example.texteditor.utils.parseSyntaxJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class TextEditorActions(
    private val state: TextEditorState,
    private val context: Context,
    private val scope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState
) {

    // Language to extension mapping
    private val languageExtensions = mapOf(
        "txt" to "txt",
        "kotlin" to "kt",
        "java" to "java",
        "python" to "py",
        "c" to "c"
    )

    fun findNext() {
        if (state.searchQuery.isEmpty()) return
        val startIndex = state.codeTextState.selection.end
        val textToSearch = if (state.caseSensitive) state.codeTextState.text else state.codeTextState.text.lowercase()
        val queryToSearch = if (state.caseSensitive) state.searchQuery else state.searchQuery.lowercase()
        val index = textToSearch.indexOf(queryToSearch, startIndex).takeIf { it >= 0 }
            ?: textToSearch.indexOf(queryToSearch)
        if (index >= 0) {
            state.codeTextState = state.codeTextState.copy(
                selection = TextRange(index, index + state.searchQuery.length)
            )
        }
    }

    fun replaceCurrent() {
        val sel = state.codeTextState.selection
        if (sel.start < sel.end) {
            val before = state.codeTextState.text.substring(0, sel.start)
            val after = state.codeTextState.text.substring(sel.end)
            val newText = before + state.replaceText + after
            state.codeTextState = TextFieldValue(
                text = newText,
                selection = TextRange(sel.start + state.replaceText.length)
            )
        } else {
            findNext()
        }
    }

    fun saveTextToFile(uri: Uri?, text: String) {
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

    fun onFileOpened(uri: Uri) {
        state.fileUri = uri
        state.fileName = FileUtils.getFileName(context, uri)
        val text = FileUtils.readTextFromUri(context, uri)
        state.isLoadingFile = true
        state.codeTextState = TextFieldValue(text)

        val ext = state.fileName.substringAfterLast('.', "").lowercase()
        state.currentLanguage = when (ext) {
            "kt" -> "kotlin"
            "java" -> "java"
            "py" -> "python"
            "c" -> "c"
            else -> "txt" // Default to txt instead of kotlin
        }
    }

    fun onFileSaved(uri: Uri) {
        state.fileUri = uri
        FileUtils.writeTextToUri(context, uri, state.codeTextState.text)
        state.fileName = FileUtils.getFileName(context, uri)
        scope.launch { snackbarHostState.showSnackbar("💾 File saved as ${state.fileName}") }
    }

    fun saveCurrentFile() {
        try {
            saveTextToFile(state.fileUri, state.codeTextState.text)
            scope.launch {
                snackbarHostState.showSnackbar("💾 File saved as ${state.fileName}")
            }
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("⚠️ Error saving file: ${e.message}")
            }
        }
    }

    fun addTab() {
        val cursor = state.codeTextState.selection.start
        val newText = state.codeTextState.text.substring(0, cursor) + "    " +
                state.codeTextState.text.substring(cursor)
        state.codeTextState = state.codeTextState.copy(
            text = newText,
            selection = TextRange(cursor + 4)
        )
    }

    fun compile() {
        if (state.fileUri == null) {
            state.compileResult = CompileResponse(false, listOf(CompileError(0,0,"Please save before compiling")), "")
            state.showCompileDialog = true
            return
        }

        scope.launch {
            try {
                state.isCompiling = true
                val ext = state.fileName.substringAfterLast('.', "").lowercase()
                val dir = context.getExternalFilesDir(null)
                val tmpFile = File(dir, "tmp_code_${System.currentTimeMillis()}.$ext")
                tmpFile.writeText(state.codeTextState.text)

                state.compileResult = when (ext) {
                    "kt" -> CompilerApi.compileKotlinAuto(context)
                    "py" -> CompilerApi.compilePythonAuto(context)
                    "c" -> CompilerApi.compileCAuto(context)
                    else -> CompileResponse(false, listOf(CompileError(0,0,"Unsupported file type")), "")
                }
            } catch (e: Exception) {
                state.compileResult = CompileResponse(false, listOf(CompileError(0,0,e.message ?: "Unknown error")), "")
            } finally {
                state.isCompiling = false
                state.showCompileDialog = true
            }
        }
    }

    fun onLanguageAdded(uri: Uri) {
        val jsonText = FileUtils.readTextFromUri(context, uri)
        parseSyntaxJson(jsonText)?.let { (name, rules) ->
            state.languages[name] = rules
            context.openFileOutput("$name.json", 0).use { it.write(jsonText.toByteArray()) }
            scope.launch { snackbarHostState.showSnackbar("✅ Language '$name' added") }
        }
    }

    fun removeLanguage(language: String) {
        state.languages.remove(language)
        File(context.filesDir, "$language.json").delete()
        scope.launch { snackbarHostState.showSnackbar("🗑 Language '$language' removed") }
    }

    fun getFileNameWithCorrectExtension(): String {
        val baseName = state.fileName.substringBeforeLast('.', state.fileName)
        val correctExtension = languageExtensions[state.currentLanguage] ?: "txt"
        return "$baseName.$correctExtension"
    }

    fun updateFileNameForLanguage(language: String) {
        val baseName = state.fileName.substringBeforeLast('.', state.fileName)
        val extension = languageExtensions[language] ?: "txt"
        state.fileName = "$baseName.$extension"
    }
}

@Composable
fun rememberTextEditorActions(
    state: TextEditorState,
    snackbarHostState: SnackbarHostState
): TextEditorActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    return remember(state, context, scope, snackbarHostState) {
        TextEditorActions(state, context, scope, snackbarHostState)
    }
}