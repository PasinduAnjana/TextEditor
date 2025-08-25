package com.example.texteditor.utils

object AutoInsert {
    fun processAutoInsert(newText: String, oldText: String): String {
        if (newText.length <= oldText.length) return newText
        return when (val lastChar = newText.last()) {
            '(' -> newText + ")"
            '{' -> newText + "}"
            '[' -> newText + "]"
            '"' -> newText + "\""
            '\'' -> newText + "'"
            '\t' -> oldText + "    "
            else -> newText
        }
    }
}
