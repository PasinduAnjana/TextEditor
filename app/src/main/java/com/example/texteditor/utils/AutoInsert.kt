package com.example.texteditor.utils

object AutoInsert {
    fun processAutoInsert(newText: String, oldText: String, cursorPos: Int): Pair<String, Int> {
        if (newText.length <= oldText.length) return newText to cursorPos

        val typedChar = newText.getOrNull(cursorPos - 1) ?: return newText to cursorPos
        val nextChar = newText.getOrNull(cursorPos)

        return when (typedChar) {
            // --- Opening brackets/quotes: auto insert ---
            '(' -> insertPair(newText, cursorPos, ")")
            '{' -> insertPair(newText, cursorPos, "}")
            '[' -> insertPair(newText, cursorPos, "]")
            '"' -> insertPair(newText, cursorPos, "\"")
            '\'' -> insertPair(newText, cursorPos, "'")

            // --- Closing brackets/quotes: skip-over ---
            ')' -> if (nextChar == ')') oldText to cursorPos + 1 else newText to cursorPos
            '}' -> if (nextChar == '}') oldText to cursorPos + 1 else newText to cursorPos
            ']' -> if (nextChar == ']') oldText to cursorPos + 1 else newText to cursorPos
            '"' -> if (nextChar == '"') oldText to cursorPos + 1 else newText to cursorPos
            '\'' -> if (nextChar == '\'') oldText to cursorPos + 1 else newText to cursorPos

            // --- Tab handling ---
            '\t' -> (oldText.substring(0, cursorPos - 1) + "    " + oldText.substring(cursorPos)) to cursorPos + 3

            else -> newText to cursorPos
        }
    }

    private fun insertPair(text: String, cursorPos: Int, closing: String): Pair<String, Int> {
        val newText = text.substring(0, cursorPos) + closing + text.substring(cursorPos)
        return newText to cursorPos // keep cursor inside pair
    }
}
