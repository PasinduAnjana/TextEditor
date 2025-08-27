package com.example.texteditor.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import org.json.JSONObject


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

// Parse JSON for custom language
fun parseSyntaxJson(jsonString: String): Pair<String, SyntaxRules>? {
    return try {
        val obj = JSONObject(jsonString)
        val name = obj.getString("name")
        val keywords = obj.getJSONArray("keywords").let { arr ->
            List(arr.length()) { arr.getString(it) }
        }
        val stringPattern = obj.getString("stringPattern").toRegex()
        val commentPattern = obj.getString("commentPattern").toRegex(RegexOption.MULTILINE)
        name to SyntaxRules(keywords, stringPattern, commentPattern)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
