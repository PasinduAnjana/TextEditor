package com.example.texteditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.texteditor.utils.SyntaxRules
import com.example.texteditor.utils.SyntaxHighlight.highlightCode

@Composable
fun EditorScreen(
    codeTextState: TextFieldValue,
    onCodeChange: (TextFieldValue) -> Unit,
    syntaxRules: SyntaxRules,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    BasicTextField(
        value = codeTextState,
        onValueChange = onCodeChange,
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .verticalScroll(scrollState),
        visualTransformation = object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText {
                return TransformedText(
                    highlightCode(text.text, syntaxRules),
                    OffsetMapping.Identity
                )
            }
        },
        decorationBox = { innerTextField ->
            Row(modifier = Modifier.fillMaxSize()) {
                // 📌 Line numbers (drawn in gutter)
                Column(modifier = Modifier.padding(start = 4.dp, end = 8.dp)) {
                    val lines = codeTextState.text.split("\n")
                    lines.forEachIndexed { index, _ ->
                        Text(
                            text = (index + 1).toString(),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 📌 Actual editor
                innerTextField()
            }
        }
    )
}
