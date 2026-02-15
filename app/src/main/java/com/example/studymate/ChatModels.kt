package com.example.studymate

data class ChatRequest(
    val message: String,
    val context: String? = null
)

data class ChatResponse(
    val reply: String
)