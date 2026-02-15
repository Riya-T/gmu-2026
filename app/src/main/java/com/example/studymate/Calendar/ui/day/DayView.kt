package com.example.studymate.Calendar.ui.day

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studymate.Calendar.data.model.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DayView(
    date: LocalDate,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onTaskMove: (Task, String, String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val hourHeight = 64.dp
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Text("<", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onNext) {
                Text(">", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

        // Time Grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Hour Labels and Lines
            Column {
                for (hour in 0..23) {
                    Row(
                        modifier = Modifier
                            .height(hourHeight)
                            .fillMaxWidth()
                    ) {
                        // Hour Label
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .fillMaxHeight()
                                .padding(end = 8.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            if (hour != 0) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:00", hour),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        // Horizontal Line
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    drawLine(
                                        color = Color.LightGray,
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, 0f),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                        )
                    }
                }
            }

            // Tasks layer
            tasks.forEach { task ->
                val taskPosition = calculateTaskPosition(task, date, hourHeight)
                if (taskPosition != null) {
                    var offsetY by remember { mutableStateOf(0f) }
                    val isDragging = remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .padding(start = 64.dp, end = 16.dp)
                            .offset { IntOffset(0, (offsetY).roundToInt()) }
                            .offset(y = taskPosition.top)
                            .height(taskPosition.height)
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { isDragging.value = true },
                                    onDragEnd = {
                                        isDragging.value = false
                                        // Calculate new time
                                        val totalMinutesOffset = (offsetY / hourHeightPx) * 60
                                        val newTimes = calculateNewTimes(task, totalMinutesOffset.toLong())
                                        onTaskMove(task, newTimes.first, newTimes.second)
                                        offsetY = 0f
                                    },
                                    onDragCancel = {
                                        isDragging.value = false
                                        offsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetY += dragAmount.y
                                    }
                                )
                            }
                    ) {
                        TaskCard(
                            task = task, 
                            onClick = { onTaskClick(task) },
                            isDragging = isDragging.value
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(task: Task, onClick: () -> Unit, isDragging: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            else 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        elevation = if (isDragging) CardDefaults.cardElevation(defaultElevation = 8.dp) else CardDefaults.cardElevation(),
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (!task.description.isNullOrEmpty()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

data class TaskPosition(val top: androidx.compose.ui.unit.Dp, val height: androidx.compose.ui.unit.Dp)

private fun calculateTaskPosition(task: Task, date: LocalDate, hourHeight: androidx.compose.ui.unit.Dp): TaskPosition? {
    return try {
        val startDateTime = if (task.start.length <= 10) {
            LocalDateTime.of(LocalDate.parse(task.start), LocalTime.of(9, 0))
        } else {
            LocalDateTime.parse(task.start)
        }
        
        val endDateTime = if (task.end.length <= 10) {
            startDateTime.plusHours(1)
        } else {
            LocalDateTime.parse(task.end)
        }

        if (startDateTime.toLocalDate() != date) return null

        val startMinutes = startDateTime.hour * 60 + startDateTime.minute
        val durationMinutes = ChronoUnit.MINUTES.between(startDateTime, endDateTime).coerceAtLeast(15)

        val topOffset = (startMinutes / 60f) * hourHeight.value
        val taskHeight = (durationMinutes / 60f) * hourHeight.value

        TaskPosition(topOffset.dp, taskHeight.dp)
    } catch (e: Exception) {
        null
    }
}

private fun calculateNewTimes(task: Task, minutesOffset: Long): Pair<String, String> {
    return try {
        val startDateTime = if (task.start.length <= 10) {
            LocalDateTime.of(LocalDate.parse(task.start), LocalTime.of(9, 0))
        } else {
            LocalDateTime.parse(task.start)
        }
        
        val endDateTime = if (task.end.length <= 10) {
            startDateTime.plusHours(1)
        } else {
            LocalDateTime.parse(task.end)
        }

        // Snap to 15 minute increments
        val snappedMinutes = ((minutesOffset.toDouble() / 15).roundToInt() * 15).toLong()
        
        val newStart = startDateTime.plusMinutes(snappedMinutes)
        val newEnd = endDateTime.plusMinutes(snappedMinutes)

        Pair(newStart.toString(), newEnd.toString())
    } catch (e: Exception) {
        Pair(task.start, task.end)
    }
}
