package com.example.studymate.Calendar.data.repository

import com.example.studymate.Calendar.data.model.Task
import java.util.UUID

class CalendarRepository {
    private val tasks = mutableListOf<Task>()

    suspend fun getRange(start: String, end: String): List<Task> {
        // Simple mock filter
        return tasks.filter { it.start >= start && it.start <= end }
    }

    suspend fun addTask(task: Task) {
        tasks.add(task)
    }

    suspend fun updateTask(updatedTask: Task) {
        val index = tasks.indexOfFirst { it.id == updatedTask.id }
        if (index != -1) {
            tasks[index] = updatedTask
        }
    }

    suspend fun deleteTask(taskId: String) {
        tasks.removeIf { it.id == taskId }
    }
}
