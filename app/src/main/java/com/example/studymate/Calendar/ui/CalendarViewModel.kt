package com.example.studymate.Calendar.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studymate.Calendar.data.model.Task
import com.example.studymate.Calendar.domain.CalendarManager
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class CalendarViewType {
    MONTHLY, WEEKLY, DAILY
}

class CalendarViewModel(
    private val manager: CalendarManager
) : ViewModel() {

    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    private val _viewType = MutableLiveData(CalendarViewType.MONTHLY)
    val viewType: LiveData<CalendarViewType> = _viewType

    private val _selectedDate = MutableLiveData(LocalDate.now())
    val selectedDate: LiveData<LocalDate> = _selectedDate

    private var currentMonth = YearMonth.now()
    private var currentWeekDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    init {
        loadData()
    }

    fun setViewType(type: CalendarViewType) {
        _viewType.value = type
        loadData()
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        currentWeekDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        currentMonth = YearMonth.from(date)
        loadData()
    }

    fun loadData() {
        val type = _viewType.value ?: CalendarViewType.MONTHLY
        when (type) {
            CalendarViewType.MONTHLY -> loadMonth()
            CalendarViewType.WEEKLY -> loadWeek()
            CalendarViewType.DAILY -> loadDay()
        }
    }

    private fun loadMonth() {
        viewModelScope.launch {
            _tasks.value = manager.loadMonth(
                currentMonth.year,
                currentMonth.monthValue
            )
        }
    }

    private fun loadWeek() {
        viewModelScope.launch {
            // Load tasks for the full week
            val start = currentWeekDate
            val end = start.plusDays(6)
            _tasks.value = manager.loadRange(start.toString(), end.toString())
        }
    }

    private fun loadDay() {
        viewModelScope.launch {
            _selectedDate.value?.let { date ->
                _tasks.value = manager.loadRange(date.toString(), date.toString())
            }
        }
    }

    fun next() {
        val type = _viewType.value ?: CalendarViewType.MONTHLY
        when (type) {
            CalendarViewType.MONTHLY -> currentMonth = currentMonth.plusMonths(1)
            CalendarViewType.WEEKLY -> currentWeekDate = currentWeekDate.plusWeeks(1)
            CalendarViewType.DAILY -> _selectedDate.value = _selectedDate.value?.plusDays(1)
        }
        loadData()
    }

    fun previous() {
        val type = _viewType.value ?: CalendarViewType.MONTHLY
        when (type) {
            CalendarViewType.MONTHLY -> currentMonth = currentMonth.minusMonths(1)
            CalendarViewType.WEEKLY -> currentWeekDate = currentWeekDate.minusWeeks(1)
            CalendarViewType.DAILY -> _selectedDate.value = _selectedDate.value?.minusDays(1)
        }
        loadData()
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            manager.addTask(task)
            loadData()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            manager.updateTask(task)
            loadData()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            manager.deleteTask(taskId)
            loadData()
        }
    }
}
