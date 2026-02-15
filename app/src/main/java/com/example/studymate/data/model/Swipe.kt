package com.example.studymate.data.model

import com.google.firebase.Timestamp

data class Swipe(
    val fromUser: String = "",
    val toUser: String = "",
    val liked: Boolean = false,
    val timestamp: Timestamp? = null
)
