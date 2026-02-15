package com.example.studymate.Calendar.ui.week

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studymate.Calendar.data.model.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun WeekView(
    tasks: List<Task>,
    onDateClick: (LocalDate) -> Unit,
    onTaskClick: (Task) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    var startOfWeek by remember { 
        mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))) 
    }
    val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                startOfWeek = startOfWeek.minusWeeks(1)
                onPrevious()
            }) {
                Text("<")
            }
            Text(
                text = "Week of ${startOfWeek}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { 
                startOfWeek = startOfWeek.plusWeeks(1)
                onNext()
            }) {
                Text(">")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(weekDays) { date ->
                val dayTasks = tasks.filter { it.start.startsWith(date.toString()) }
                WeekDayRow(
                    date = date,
                    tasks = dayTasks,
                    onDateClick = { onDateClick(date) },
                    onTaskClick = onTaskClick
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun WeekDayRow(
    date: LocalDate,
    tasks: List<Task>,
    onDateClick: () -> Unit,
    onTaskClick: (Task) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .width(60.dp)
                .clickable { onDateClick() }
        ) {
            Text(
                text = date.dayOfWeek.name.take(3),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            if (tasks.isEmpty()) {
                Text(
                    text = "No tasks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 4.dp).clickable { onDateClick() }.fillMaxWidth()
                )
            } else {
                tasks.forEach { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable { onTaskClick(task) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = task.title,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
