package com.example.studymate.Calendar.ui.month

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studymate.Calendar.data.model.Task
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthView(
    tasks: List<Task>,
    onDateSelected: (LocalDate) -> Unit,
    onTaskClick: (Task) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7 // 0 for Sunday
    val days = (1..daysInMonth).toList()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                currentMonth = currentMonth.minusMonths(1)
                onPrevious()
            }) {
                Text("<")
            }
            Text(
                text = "${currentMonth.month} ${currentMonth.year}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { 
                currentMonth = currentMonth.plusMonths(1)
                onNext()
            }) {
                Text(">")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Day Headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize()
        ) {
            // Empty spaces for the first week
            items(firstDayOfMonth) {
                Box(modifier = Modifier.aspectRatio(1f))
            }

            items(days) { day ->
                val date = currentMonth.atDay(day)
                val dayTasks = tasks.filter { it.start.startsWith(date.toString()) }
                
                MonthDayItem(
                    day = day,
                    tasks = dayTasks,
                    onClick = { onDateSelected(date) },
                    onTaskClick = onTaskClick
                )
            }
        }
    }
}

@Composable
fun MonthDayItem(
    day: Int,
    tasks: List<Task>,
    onClick: () -> Unit,
    onTaskClick: (Task) -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(text = day.toString(), style = MaterialTheme.typography.bodySmall)
            tasks.take(2).forEach { task ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 1.dp)
                        .clickable { onTaskClick(task) }
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color = Color.Blue
                    )
                }
            }
            if (tasks.size > 2) {
                Text(text = "+${tasks.size - 2}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
