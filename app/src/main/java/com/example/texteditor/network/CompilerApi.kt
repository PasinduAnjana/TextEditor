package com.example.texteditor.network

import android.content.Context
import android.util.Log
import com.example.texteditor.models.CompileResponse
import com.example.texteditor.models.CompileError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CompilerApi {

    private val errorRegex = Regex("""(.+):(\d+):(\d+): error: (.+)""")

    /**
     * Compile Kotlin code automatically by waiting for fresh output from PC-side auto compiler.
     */
    suspend fun compileKotlinAuto(context: Context, timeoutMs: Long = 10000L): CompileResponse = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(context.getExternalFilesDir(null), "compile_output.txt")

            // ⚡ Delete old output before compiling to ensure fresh result
            if (outputFile.exists()) outputFile.delete()

            val pollInterval = 100L // faster polling
            var waited = 0L

            // Wait until new output file appears
            while (!outputFile.exists() && waited < timeoutMs) {
                Thread.sleep(pollInterval)
                waited += pollInterval
            }

            if (!outputFile.exists()) {
                return@withContext CompileResponse(
                    success = false,
                    errors = listOf(
                        CompileError(0, 0, "Compilation output not found. Make sure PC auto compiler is running.")
                    ),
                    output = ""
                )
            }

            val outputText = outputFile.readText()
            val errors = mutableListOf<CompileError>()

            outputText.lines().forEach { line ->
                val match = errorRegex.matchEntire(line)
                if (match != null) {
                    val (_, lineNum, colNum, msg) = match.destructured
                    errors.add(CompileError(lineNum.toInt(), colNum.toInt(), msg))
                }
            }

            CompileResponse(
                success = errors.isEmpty(),
                errors = errors,
                output = outputText
            )

        } catch (e: Exception) {
            Log.e("CompilerApi", "Error reading compilation output", e)
            CompileResponse(
                success = false,
                errors = listOf(CompileError(0, 0, e.localizedMessage ?: "Unknown error")),
                output = ""
            )
        }
    }
}
