package com.example.studymate.data.util

import android.content.Context
import com.example.studymate.data.model.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

object SampleData {
    var cachedUsers: List<User>? = null
        private set

    fun getSampleUsers(context: Context): List<User> {
        if (cachedUsers != null) return cachedUsers!!

        val jsonString: String = try {
            context.assets.open("sample_users.json").bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return emptyList()
        }

        val listUserType = object : TypeToken<List<User>>() {}.type
        cachedUsers = Gson().fromJson(jsonString, listUserType)
        return cachedUsers ?: emptySet<User>().toList()
    }

    // Keep this for backward compatibility or as a starting point
    val sampleUsers = listOf(
        User(uid = "test_id1", name = "Aarav", major = "Computer Science", year = "Junior", bio = "Love coding and coffee. Looking for a study buddy for CS 310."),
        User(uid = "test_id2", name = "Maya", major = "Biology", year = "Sophomore", bio = "Pre-med. Currently struggling with Organic Chemistry. Help!"),
        User(uid = "test_id3", name = "Ethan", major = "Mathematics", year = "Senior", bio = "Math tutor. Happy to help with Calculus or Linear Algebra."),
        User(uid = "test_id4", name = "Zara", major = "Engineering", year = "Freshman", bio = "First year student. Interested in Robotics and building cool stuff.")
    )
}
