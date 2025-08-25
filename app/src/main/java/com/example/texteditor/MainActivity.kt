package com.example.texteditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.texteditor.ui.TextEditorApp
import com.example.texteditor.utils.SyntaxRules
import com.example.texteditor.ui.theme.TextEditorTheme

class MainActivity : ComponentActivity() {

    private val syntaxConfig = mapOf(
        "kotlin" to SyntaxRules(
            keywords = listOf(
                "fun", "val", "var", "if", "else", "for", "while", "class", "object",
                "package", "import", "return", "when", "try", "catch", "finally",
                "do", "is", "in", "as", "break", "continue", "throw", "super", "this"
            ),
            stringPattern = "\".*?\"".toRegex(),
            commentPattern = "//.*?$".toRegex(RegexOption.MULTILINE)
        ),
        "java" to SyntaxRules(
            keywords = listOf(
                "class", "public", "static", "void", "int", "double", "new", "if", "else",
                "for", "while", "return", "try", "catch", "finally", "import", "package"
            ),
            stringPattern = "\".*?\"".toRegex(),
            commentPattern = "//.*?$".toRegex(RegexOption.MULTILINE)
        ),
        "python" to SyntaxRules(
            keywords = listOf(
                "def", "return", "if", "elif", "else", "for", "while", "import", "as",
                "from", "class", "try", "except", "finally", "with", "lambda", "pass"
            ),
            stringPattern = "\"\"\".*?\"\"\"|'''.*?'''|\".*?\"|'.*?'".toRegex(RegexOption.DOT_MATCHES_ALL),
            commentPattern = "#.*?$".toRegex(RegexOption.MULTILINE)
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TextEditorTheme {
                TextEditorApp(syntaxConfig)
            }
        }
    }
}
