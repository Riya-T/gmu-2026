package com.example.studymate.data.util

import com.example.studymate.data.model.User

object SampleData {
    val sampleUsers = listOf(
        User(uid = "test_id1", name = "Aarav", major = "Computer Science", year = "Junior", bio = "Love coding and coffee. Looking for a study buddy for CS 310."),
        User(uid = "test_id2", name = "Maya", major = "Biology", year = "Sophomore", bio = "Pre-med. Currently struggling with Organic Chemistry. Help!"),
        User(uid = "test_id3", name = "Ethan", major = "Mathematics", year = "Senior", bio = "Math tutor. Happy to help with Calculus or Linear Algebra."),
        User(uid = "test_id4", name = "Zara", major = "Engineering", year = "Freshman", bio = "First year student. Interested in Robotics and building cool stuff.")
    )
}
