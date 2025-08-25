// FileUtils.kt
package com.example.texteditor.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object FileUtils {

    fun readTextFromUri(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.readText()
    }

    fun writeTextToUri(context: Context, uri: Uri, text: String) {
        val outputStream = context.contentResolver.openOutputStream(uri)
        val writer = OutputStreamWriter(outputStream)
        writer.write(text)
        writer.flush()
        writer.close()
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = "Untitled"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) name = it.getString(nameIndex)
            }
        }
        return name
    }
}
