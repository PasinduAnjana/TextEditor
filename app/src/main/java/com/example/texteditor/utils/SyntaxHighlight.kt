package com.example.texteditor.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

data class SyntaxRules(val keywords: List<String>, val stringPattern: Regex, val commentPattern: Regex)

object SyntaxHighlight {
    fun highlightCode(code: String, rules: SyntaxRules): AnnotatedString {
        val keywordPattern = "\\b(${rules.keywords.joinToString("|")})\\b".toRegex()
        return buildAnnotatedString {
            append(code)
            keywordPattern.findAll(code).forEach { addStyle(SpanStyle(color = Color.Cyan), it.range.first, it.range.last + 1) }
            rules.stringPattern.findAll(code).forEach { addStyle(SpanStyle(color = Color(0xFF6A9955)), it.range.first, it.range.last + 1) }
            rules.commentPattern.findAll(code).forEach { addStyle(SpanStyle(color = Color.Gray), it.range.first, it.range.last + 1) }
        }
    }
}
