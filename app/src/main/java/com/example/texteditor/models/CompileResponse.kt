package com.example.texteditor.models

data class CompileError(
    val line: Int,
    val column: Int,
    val message: String
)

data class CompileResponse(
    val success: Boolean,
    val errors: List<CompileError>,
    val output: String = ""
)
