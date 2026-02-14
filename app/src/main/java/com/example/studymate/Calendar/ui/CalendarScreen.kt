package com.example.studymate.Calendar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studymate.Calendar.data.model.Task
import com.example.studymate.Calendar.ui.day.DayView
import com.example.studymate.Calendar.ui.month.MonthView
import com.example.studymate.Calendar.ui.week.WeekView
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onBack: () -> Unit
) {
    val tasks by viewModel.tasks.observeAsState(initial = emptyList())
    val viewType by viewModel.viewType.observeAsState(initial = CalendarViewType.MONTHLY)
    val selectedDate by viewModel.selectedDate.observeAsState(initial = LocalDate.now())
    
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(when(viewType) {
                        CalendarViewType.MONTHLY -> "Monthly View"
                        CalendarViewType.WEEKLY -> "Weekly View"
                        CalendarViewType.DAILY -> "Daily View"
                    }) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val nextType = when(viewType) {
                            CalendarViewType.MONTHLY -> CalendarViewType.WEEKLY
                            CalendarViewType.WEEKLY -> CalendarViewType.DAILY
                            CalendarViewType.DAILY -> CalendarViewType.MONTHLY
                        }
                        viewModel.setViewType(nextType)
                    }) {
                        Text(
                            text = when(viewType) {
                                CalendarViewType.MONTHLY -> "Week"
                                CalendarViewType.WEEKLY -> "Day"
                                CalendarViewType.DAILY -> "Month"
                            },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedTask = null
                showTaskDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (viewType) {
                CalendarViewType.MONTHLY -> {
                    MonthView(
                        tasks = tasks,
                        onDateSelected = { date ->
                            viewModel.setSelectedDate(date)
                            viewModel.setViewType(CalendarViewType.DAILY)
                        },
                        onTaskClick = { 
                            selectedTask = it
                            showTaskDialog = true
                        },
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() }
                    )
                }
                CalendarViewType.WEEKLY -> {
                    WeekView(
                        tasks = tasks,
                        onDateClick = { date ->
                            viewModel.setSelectedDate(date)
                            viewModel.setViewType(CalendarViewType.DAILY)
                        },
                        onTaskClick = { 
                            selectedTask = it
                            showTaskDialog = true
                        },
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() }
                    )
                }
                CalendarViewType.DAILY -> {
                    DayView(
                        date = selectedDate,
                        tasks = tasks,
                        onTaskClick = {
                            selectedTask = it
                            showTaskDialog = true
                        },
                        onTaskMove = { task, newStart, newEnd ->
                            viewModel.updateTask(task.copy(start = newStart, end = newEnd))
                        },
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() }
                    )
                }
            }
        }
    }

    if (showTaskDialog) {
        TaskDialog(
            task = selectedTask,
            defaultDate = selectedDate,
            onDismiss = { showTaskDialog = false },
            onSave = { title, desc, start, end ->
                if (selectedTask == null) {
                    viewModel.addTask(
                        Task(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            description = desc,
                            start = start,
                            end = end,
                            priority = 1
                        )
                    )
                } else {
                    viewModel.updateTask(
                        selectedTask!!.copy(
                            title = title,
                            description = desc,
                            start = start,
                            end = end
                        )
                    )
                }
                showTaskDialog = false
            },
            onDelete = {
                selectedTask?.let { viewModel.deleteTask(it.id) }
                showTaskDialog = false
            }
        )
    }
}

@Composable
fun TaskDialog(
    task: Task?,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    
    // Split start/end into date and time
    val initialStartDate = task?.start?.split("T")?.getOrNull(0) ?: defaultDate.toString()
    val initialStartTime = task?.start?.split("T")?.getOrNull(1)?.take(5) ?: "09:00"
    val initialEndDate = task?.end?.split("T")?.getOrNull(0) ?: defaultDate.toString()
    val initialEndTime = task?.end?.split("T")?.getOrNull(1)?.take(5) ?: "10:00"

    var startDate by remember { mutableStateOf(initialStartDate) }
    var startTime by remember { mutableStateOf(initialStartTime) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var endTime by remember { mutableStateOf(initialEndTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task == null) "Add Task" else "Edit Task") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Start", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Time (HH:MM)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("End", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Time (HH:MM)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                val finalStart = "${startDate}T${startTime}"
                val finalEnd = "${endDate}T${endTime}"
                onSave(title, description, finalStart, finalEnd) 
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (task != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
