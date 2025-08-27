package com.example.texteditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.texteditor.ui.TextEditorApp
import com.example.texteditor.utils.SyntaxRules
import com.example.texteditor.ui.theme.TextEditorTheme
import com.example.texteditor.utils.parseSyntaxJson
import java.io.File

class MainActivity : ComponentActivity() {

    private var syntaxConfig = mutableMapOf(
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
        ),
        "c" to SyntaxRules(
            keywords = listOf(
                "int", "float", "double", "char", "void", "if", "else", "for", "while",
                "do", "switch", "case", "default", "break", "continue", "return",
                "struct", "union", "enum", "typedef", "const", "sizeof", "volatile"
            ),
            stringPattern = "\".*?\"".toRegex(),
            commentPattern = "//.*?$|/\\*.*?\\*/".toRegex(RegexOption.DOT_MATCHES_ALL)
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load saved custom language JSONs
        filesDir.listFiles { f -> f.extension == "json" }?.forEach { file ->
            val jsonString = file.readText()
            parseSyntaxJson(jsonString)?.let { (name, rules) ->
                syntaxConfig[name] = rules
            }
        }

        setContent {
            TextEditorTheme {
                TextEditorApp(syntaxConfig)
            }
        }
    }
}
