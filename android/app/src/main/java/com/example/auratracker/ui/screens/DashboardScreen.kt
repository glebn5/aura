package com.example.auratracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auratracker.data.local.LogEntryEntity
import com.example.auratracker.data.repository.LogRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: LogRepository,
    modifier: Modifier = Modifier
) {
    val logs by repository.logsFlow.collectAsState(initial = emptyList())
    val scrollState = rememberScrollState()

    // Рассчитываем метрики на основе логов
    val (totalExpenses, expensesByCategory) = remember(logs) {
        val financeLogs = logs.filter { it.category == "FINANCE" }
        val total = financeLogs.sumOf { it.getStructuredLog()?.finance_data?.amount ?: 0.0 }
        
        val byCat = mutableMapOf<String, Double>()
        financeLogs.forEach { log ->
            val data = log.getStructuredLog()?.finance_data
            if (data != null) {
                val cat = data.category.lowercase().capitalize(Locale.ROOT)
                byCat[cat] = (byCat[cat] ?: 0.0) + data.amount
            }
        }
        total to byCat
    }

    val (totalDistance, totalMinutes, workoutCount) = remember(logs) {
        val fitnessLogs = logs.filter { it.category == "FITNESS" }
        val distance = fitnessLogs.sumOf { it.getStructuredLog()?.fitness_data?.distance_km ?: 0.0 }
        val mins = fitnessLogs.sumOf { it.getStructuredLog()?.fitness_data?.duration_minutes ?: 0.0 }
        Triple(distance, mins, fitnessLogs.size)
    }

    // Данные для графика расходов за последние 7 дней
    val chartData = remember(logs) {
        val calendar = Calendar.getInstance()
        val days = List(7) { index ->
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -index)
            calendar.time
        }.reversed()

        val sdf = SimpleDateFormat("dd.MM", Locale.getDefault())
        val dayLabels = days.map { sdf.format(it) }

        // Суммируем расходы по каждому из 7 дней
        val dailySums = DoubleArray(7)
        logs.filter { it.category == "FINANCE" }.forEach { log ->
            val dateStr = sdf.format(Date(log.createdAt))
            val index = dayLabels.indexOf(dateStr)
            if (index != -1) {
                dailySums[index] += log.getStructuredLog()?.finance_data?.amount ?: 0.0
            }
        }

        // Если данных вообще нет, подставляем моки для красивого превью
        val finalSums = if (dailySums.all { it == 0.0 }) {
            doubleArrayOf(250.0, 1200.0, 0.0, 450.0, 980.0, 150.0, 500.0)
        } else {
            dailySums
        }

        dayLabels to finalSums
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Аналитика Aura", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Раздел 1. График расходов за неделю
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "РАСХОДЫ ЗА НЕДЕЛЮ (RUB)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Отрисовка графика Canvas
                    ExpensesBarChart(labels = chartData.first, values = chartData.second)
                }
            }

            // Раздел 2. Сводка расходов по категориям
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ОБЩИЕ ТРАТЫ В ЭТОМ МЕСЯЦЕ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${totalExpenses} ₽",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (expensesByCategory.isEmpty()) {
                        Text("Нет трат в этом месяце. Напишите в чат, например: 'купил продукты на 800 рублей'.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        expensesByCategory.forEach { (cat, sum) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cat, fontSize = 14.sp)
                                Text("${sum} ₽", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }

            // Раздел 3. Карточка тренировок
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "АКТИВНОСТЬ И СПОРТ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE91E63),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Всего тренировок", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$workoutCount", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Дистанция", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.getDefault(), "%.1f км", totalDistance), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Время в зале/беге", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.getDefault(), "%.0f мин", totalMinutes), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpensesBarChart(labels: List<String>, values: DoubleArray) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val width = size.width
        val height = size.height

        val maxVal = values.maxOrNull()?.coerceAtLeast(100.0) ?: 100.0
        val chartHeight = height - 40f
        val chartWidth = width - 40f

        // 1. Отрисовка горизонтальных линий сетки
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = chartHeight * (i.toFloat() / gridLines)
            drawLine(
                color = gridColor,
                start = Offset(40f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            // Текст значений сетки
            val gridValue = (maxVal * (1 - i.toFloat() / gridLines)).toInt()
            drawContext.canvas.nativeCanvas.drawText(
                gridValue.toString(),
                5f,
                y + 10f,
                android.graphics.Paint().apply {
                    color = textColor.value.toInt()
                    textSize = 24f
                }
            )
        }

        // 2. Отрисовка столбцов
        val barCount = values.size
        val gap = 16f
        val totalGaps = (barCount - 1) * gap
        val barWidth = (chartWidth - totalGaps) / barCount

        for (i in 0 until barCount) {
            val valPercent = (values[i] / maxVal).toFloat()
            val barHeight = chartHeight * valPercent
            
            val x = 40f + i * (barWidth + gap)
            val y = chartHeight - barHeight

            // Рисуем закругленный столбик с градиентом
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(barColor.copy(alpha = 0.8f), barColor)
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Подпись даты под столбиком
            drawContext.canvas.nativeCanvas.drawText(
                labels[i],
                x + (barWidth / 2) - 25f,
                height - 5f,
                android.graphics.Paint().apply {
                    color = textColor.value.toInt()
                    textSize = 24f
                }
            )
        }
    }
}
