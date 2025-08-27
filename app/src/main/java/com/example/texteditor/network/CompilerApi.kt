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

    private fun String.removeAnsiColors(): String =
        this.replace(Regex("""\u001B\[[;?\d]*[A-Za-z]"""), "")

    private suspend fun compileGeneric(context: Context, timeoutMs: Long): CompileResponse =
        withContext(Dispatchers.IO) {
            try {
                val outputFile = File(context.getExternalFilesDir(null), "compile_output.txt")
                if (outputFile.exists()) outputFile.delete()

                val pollInterval = 100L
                var waited = 0L
                while (!outputFile.exists() && waited < timeoutMs) {
                    Thread.sleep(pollInterval)
                    waited += pollInterval
                }

                if (!outputFile.exists()) {
                    return@withContext CompileResponse(false,
                        listOf(CompileError(0,0,"Compilation output not found. Ensure PC compiler is running.")),
                        "")
                }

                val lines = outputFile.readLines()
                val flag = lines.firstOrNull()
                val isCompileError = flag == "COMPILE_ERROR"
                val outputText = lines.drop(1).joinToString("\n").removeAnsiColors()

                CompileResponse(
                    success = !isCompileError,
                    errors = if (isCompileError) listOf(CompileError(0,0,outputText)) else emptyList(),
                    output = if (!isCompileError) outputText else ""
                )
            } catch (e: Exception) {
                CompileResponse(false, listOf(CompileError(0,0,e.localizedMessage ?: "Unknown error")), "")
            }
        }

    suspend fun compilePythonAuto(context: Context, timeoutMs: Long = 10000L) = compileGeneric(context, timeoutMs)
    suspend fun compileCAuto(context: Context, timeoutMs: Long = 10000L) = compileGeneric(context, timeoutMs)

    suspend fun compileKotlinAuto(context: Context, timeoutMs: Long = 10000L): CompileResponse =
        withContext(Dispatchers.IO) {
            try {
                val outputFile = File(context.getExternalFilesDir(null), "compile_output.txt")
                if (outputFile.exists()) outputFile.delete()

                val pollInterval = 100L
                var waited = 0L
                while (!outputFile.exists() && waited < timeoutMs) {
                    Thread.sleep(pollInterval)
                    waited += pollInterval
                }

                if (!outputFile.exists()) {
                    return@withContext CompileResponse(false,
                        listOf(CompileError(0,0,"Compilation output not found. Ensure PC compiler is running.")), "")
                }

                val lines = outputFile.readLines()
                val flag = lines.firstOrNull()
                val isCompileError = flag == "COMPILE_ERROR"
                val outputText = lines.drop(1).joinToString("\n").removeAnsiColors()
                val errors = mutableListOf<CompileError>()

                if (isCompileError) {
                    lines.drop(1).forEach { line ->
                        val clean = line.removeAnsiColors()
                        if (clean.startsWith("WARNING:")) return@forEach
                        errorRegex.matchEntire(clean)?.destructured?.let { (_, l, c, msg) ->
                            errors.add(CompileError(l.toInt(), c.toInt(), msg))
                        }
                    }
                }

                CompileResponse(success = !isCompileError, errors = errors, output = if (!isCompileError) outputText else "")
            } catch (e: Exception) {
                CompileResponse(false, listOf(CompileError(0,0,e.localizedMessage ?: "Unknown error")), "")
            }
        }
}
