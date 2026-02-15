package com.example.studymate.Calendar.domain

import com.example.studymate.Calendar.data.model.Task
import com.example.studymate.Calendar.data.repository.CalendarRepository
import java.time.DayOfWeek
import java.time.LocalDate

class CalendarManager(
    private val repository: CalendarRepository
) {

    suspend fun loadMonth(year: Int, month: Int): List<Task> {
        val start = LocalDate.of(year, month, 1)
        val end = start.withDayOfMonth(start.lengthOfMonth())
        return repository.getRange(start.toString(), end.toString())
    }

    suspend fun loadWeek(date: LocalDate): List<Task> {
        val start = date.with(DayOfWeek.MONDAY)
        val end = start.plusDays(6)
        return repository.getRange(start.toString(), end.toString())
    }

    suspend fun loadRange(start: String, end: String): List<Task> {
        return repository.getRange(start, end)
    }

    suspend fun addTask(task: Task) {
        repository.addTask(task)
    }

    suspend fun updateTask(task: Task) {
        repository.updateTask(task)
    }

    suspend fun deleteTask(taskId: String) {
        repository.deleteTask(taskId)
    }
}
