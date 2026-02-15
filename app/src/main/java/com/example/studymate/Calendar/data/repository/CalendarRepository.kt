package com.example.studymate.Calendar.data.repository

import com.example.studymate.Calendar.data.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CalendarRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private fun getTasksCollection() = auth.currentUser?.let {
        db.collection("users").document(it.uid).collection("tasks")
    }

    suspend fun getRange(start: String, end: String): List<Task> {
        val collection = getTasksCollection() ?: return emptyList()
        return try {
            // Include all tasks where the start date matches the range
            // For a single day view, start and end strings are the same (e.g., "2026-02-15")
            // Firestore string comparison works correctly for YYYY-MM-DD format.
            val snapshot = collection
                .whereGreaterThanOrEqualTo("start", start)
                .whereLessThanOrEqualTo("start", end + "\uf8ff")
                .get()
                .await()
            snapshot.toObjects(Task::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addTask(task: Task) {
        getTasksCollection()?.document(task.id)?.set(task)?.await()
    }

    suspend fun updateTask(updatedTask: Task) {
        getTasksCollection()?.document(updatedTask.id)?.set(updatedTask)?.await()
    }

    suspend fun deleteTask(taskId: String) {
        getTasksCollection()?.document(taskId)?.delete()?.await()
    }
}
