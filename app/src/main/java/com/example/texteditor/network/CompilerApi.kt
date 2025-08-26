package com.example.texteditor.network

import android.content.Context
import android.util.Log
import com.example.texteditor.models.CompileResponse
import com.example.texteditor.models.CompileError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CompilerApi {

    private val errorRegex = Regex("""(.+\.kt):(\d+):(\d+):.*error: (.+)""")

    // Helper to remove ANSI color codes from compiler output
    private fun String.removeAnsiColors(): String {
        return this.replace(Regex("""\u001B\[[;?\d]*[A-Za-z]"""), "")
    }

    /**
     * Compile Kotlin code automatically by waiting for fresh output from PC-side auto compiler.
     */
    suspend fun compileKotlinAuto(context: Context, timeoutMs: Long = 10000L): CompileResponse =
        withContext(Dispatchers.IO) {
            try {
                val outputFile = File(context.getExternalFilesDir(null), "compile_output.txt")

                // Delete old output for fresh compile
                if (outputFile.exists()) outputFile.delete()

                val pollInterval = 100L
                var waited = 0L

                while (!outputFile.exists() && waited < timeoutMs) {
                    Thread.sleep(pollInterval)
                    waited += pollInterval
                }

                if (!outputFile.exists()) {
                    return@withContext CompileResponse(
                        success = false,
                        errors = listOf(
                            CompileError(
                                0,
                                0,
                                "Compilation output not found. Make sure PC auto compiler is running."
                            )
                        ),
                        output = ""
                    )
                }

                val lines = outputFile.readLines()
                val flag = lines.firstOrNull()
                val isCompileError = flag == "COMPILE_ERROR"
                val errors = mutableListOf<CompileError>()
                val outputText = lines.drop(1).joinToString("\n").removeAnsiColors()

                if (isCompileError) {
                    lines.drop(1).forEach { line ->
                        val cleanLine = line.removeAnsiColors()

                        // Skip JVM warnings
                        if (cleanLine.startsWith("WARNING:")) return@forEach

                        val match = errorRegex.matchEntire(cleanLine)
                        if (match != null) {
                            val (_, lineNum, colNum, msg) = match.destructured
                            errors.add(
                                CompileError(
                                    lineNum.toInt(),
                                    colNum.toInt(),
                                    msg
                                )
                            )
                        }
                    }
                }

                CompileResponse(
                    success = !isCompileError,
                    errors = errors,
                    output = if (!isCompileError) outputText else ""
                )

            } catch (e: Exception) {
                Log.e("CompilerApi", "Error reading compilation output", e)
                CompileResponse(
                    success = false,
                    errors = listOf(
                        CompileError(
                            0,
                            0,
                            e.localizedMessage ?: "Unknown error"
                        )
                    ),
                    output = ""
                )
            }
        }
}
