package com.example.studymate.data.model

import com.google.firebase.Timestamp

data class Match(
    val users: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null
)
