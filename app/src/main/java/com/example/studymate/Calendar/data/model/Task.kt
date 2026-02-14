package com.example.studymate.Calendar.data.model

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val start: String,  // ISO string
    val end: String,
    val priority: Int
)
