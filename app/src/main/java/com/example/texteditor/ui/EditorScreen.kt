package com.example.texteditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import com.example.texteditor.utils.SyntaxRules
import com.example.texteditor.utils.SyntaxHighlight.highlightCode

@Composable
fun EditorScreen(
    codeText: String,
    onCodeChange: (String) -> Unit,
    syntaxRules: SyntaxRules,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    BasicTextField(
        value = codeText,
        onValueChange = onCodeChange,
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .verticalScroll(scrollState),
        visualTransformation = object : VisualTransformation {
            override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
                // Wrap the highlighted code in a TransformedText
                return TransformedText(highlightCode(text.text, syntaxRules), OffsetMapping.Identity)
            }
        }
    )
}
