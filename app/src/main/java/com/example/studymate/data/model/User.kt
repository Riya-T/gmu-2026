package com.example.studymate.data.model

import com.example.studymate.Calendar.data.model.Task
import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val major: String = "",
    val year: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val createdAt: Timestamp? = null,
    val calendar: UserCalendar = UserCalendar()
)

data class UserCalendar(
    val settings: Map<String, String> = emptyMap(),
    val categories: List<String> = listOf("Study", "Leisure", "Work")
)
