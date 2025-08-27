package com.example.texteditor.utils

object AutoInsert {
    fun processAutoInsert(newText: String, oldText: String, cursorPos: Int): Pair<String, Int> {
        if (newText.length <= oldText.length) return newText to cursorPos

        val typedChar = newText.getOrNull(cursorPos - 1) ?: return newText to cursorPos
        val nextChar = newText.getOrNull(cursorPos)

        return when (typedChar) {
            '(' -> if (nextChar != ')') insertPair(newText, cursorPos, ")", cursorPos) else TODO()
            '{' -> if (nextChar != '}') insertPair(newText, cursorPos, "}", cursorPos) else TODO()
            '[' -> if (nextChar != ']') insertPair(newText, cursorPos, "]", cursorPos) else TODO()
            '"' -> if (nextChar != '"') insertPair(newText, cursorPos, "\"", cursorPos) else TODO()
            '\'' -> if (nextChar != '\'') insertPair(newText, cursorPos, "'", cursorPos) else TODO()
            '\t' -> (oldText.substring(0, cursorPos - 1) + "    " + oldText.substring(cursorPos)) to cursorPos + 3
            else -> newText to cursorPos
        }
    }

    private fun insertPair(text: String, cursorPos: Int, closing: String, selectionShift: Int): Pair<String, Int> {
        val newText = text.substring(0, cursorPos) + closing + text.substring(cursorPos)
        return newText to (cursorPos) // keep cursor inside
    }
}
