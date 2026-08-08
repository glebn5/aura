package com.example.auratracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.auratracker.data.local.LogEntryEntity
import com.example.auratracker.data.repository.LogRepository
import com.example.auratracker.theme.DarkBackground
import com.example.auratracker.theme.DarkCard
import com.example.auratracker.theme.DarkCardBorder
import com.example.auratracker.theme.NeonCyan
import com.example.auratracker.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryMonthlyDetailDialog(
    categoryKey: String, // FINANCE, FITNESS, CAR_MAINTENANCE, ROUTINE, OTHER
    accentColor: Color,
    categoryTitleRu: String,
    categoryTitleEn: String,
    logs: List<LogEntryEntity>,
    repository: LogRepository,
    isRu: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Current selected calendar month (0 = Jan, 11 = Dec)
    val calendar = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonthIndex by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }

    // Calculate month range timestamps
    val (startTs, endTs, monthNameStr, dateRangeStr) = remember(selectedYear, selectedMonthIndex, isRu) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, selectedYear)
        cal.set(Calendar.MONTH, selectedMonthIndex)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDay)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        val monthFormat = SimpleDateFormat("LLLL yyyy", if (isRu) Locale("ru") else Locale.ENGLISH)
        val mName = monthFormat.format(Date(start)).replaceFirstChar { it.uppercase() }
        val dRange = String.format(Locale.getDefault(), "01.%02d - %02d.%02d.%d", selectedMonthIndex + 1, maxDay, selectedMonthIndex + 1, selectedYear)

        Tuple4(start, end, mName, dRange)
    }

    // Filter logs for this category and selected month
    val monthLogs = remember(logs, categoryKey, startTs, endTs) {
        logs.filter { log ->
            log.category == categoryKey && log.createdAt in startTs..endTs
        }.sortedByDescending { it.createdAt }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkCard)
                            .border(1.dp, DarkCardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isRu) categoryTitleRu else categoryTitleEn,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Text(
                            text = dateRangeStr,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkCard)
                            .border(1.dp, DarkCardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Month Selector Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard)
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (selectedMonthIndex == 0) {
                                selectedMonthIndex = 11
                                selectedYear--
                            } else {
                                selectedMonthIndex--
                            }
                        }
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", tint = Color.White)
                    }

                    Text(
                        text = monthNameStr,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = {
                            if (selectedMonthIndex == 11) {
                                selectedMonthIndex = 0
                                selectedYear++
                            } else {
                                selectedMonthIndex++
                            }
                        }
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Statistics Hero Card for this month
                MonthlyCategorySummaryHero(
                    categoryKey = categoryKey,
                    monthLogs = monthLogs,
                    accentColor = accentColor,
                    isRu = isRu
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section Title: Log Entries List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Записи за месяц (${monthLogs.size})" else "Month Entries (${monthLogs.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (monthLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRu) "Нет записей в этой категории за выбранный месяц." else "No entries in this category for selected month.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(monthLogs, key = { it.id }) { log ->
                            CategoryLogItemCard(
                                log = log,
                                accentColor = accentColor,
                                isRu = isRu,
                                onDelete = {
                                    scope.launch {
                                        repository.deleteLog(log.id)
                                        Toast.makeText(context, if (isRu) "Запись удалена" else "Entry deleted", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyCategorySummaryHero(
    categoryKey: String,
    monthLogs: List<LogEntryEntity>,
    accentColor: Color,
    isRu: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = accentColor, ambientColor = accentColor),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            when (categoryKey) {
                "FINANCE" -> {
                    val totalAmount = monthLogs.sumOf { it.getStructuredLog()?.finance_data?.amount ?: 0.0 }
                    val itemsCount = monthLogs.size
                    val topItems = monthLogs.mapNotNull { it.getStructuredLog()?.finance_data?.item }.take(3).joinToString(", ")

                    Text(if (isRu) "Итого трат за месяц" else "Total Expenses This Month", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%,.0f ₽", totalAmount),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isRu) "Транзакций: $itemsCount" else "Transactions: $itemsCount", fontSize = 12.sp, color = Color.White)
                        if (topItems.isNotBlank()) {
                            Text(if (isRu) "Траты: $topItems" else "Items: $topItems", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
                "FITNESS" -> {
                    val totalReps = monthLogs.sumOf { 
                        it.getStructuredLog()?.fitness_data?.reps
                            ?: it.rawText.filter { c -> c.isDigit() }.toIntOrNull()
                            ?: 0
                    }
                    val totalKm = monthLogs.sumOf { it.getStructuredLog()?.fitness_data?.distance_km ?: 0.0 }
                    val totalMinutes = monthLogs.sumOf { it.getStructuredLog()?.fitness_data?.duration_minutes ?: 0.0 }

                    Text(if (isRu) "Статистика тренировок за месяц" else "Monthly Fitness Summary", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(if (isRu) "Повторения (reps)" else "Total Reps", fontSize = 11.sp, color = TextSecondary)
                            Text("$totalReps", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                        Column {
                            Text(if (isRu) "Дистанция (км)" else "Distance (km)", fontSize = 11.sp, color = TextSecondary)
                            Text(String.format(Locale.getDefault(), "%.1f км", totalKm), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text(if (isRu) "Время (мин)" else "Duration (min)", fontSize = 11.sp, color = TextSecondary)
                            Text("${totalMinutes.toInt()} мин", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                "CAR_MAINTENANCE" -> {
                    val totalCost = monthLogs.sumOf { it.getStructuredLog()?.car_data?.cost ?: 0.0 }
                    val servicesCount = monthLogs.size

                    Text(if (isRu) "Расходы на авто за месяц" else "Monthly Car Expenses", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%,.0f ₽", totalCost),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (isRu) "Работы / Запчасти: $servicesCount записей" else "Services / Parts: $servicesCount entries", fontSize = 12.sp, color = Color.White)
                }
                "ROUTINE" -> {
                    val totalHours = monthLogs.sumOf { it.getStructuredLog()?.routine_data?.duration_hours ?: 0.0 }
                    Text(if (isRu) "Рутина и сон за месяц" else "Monthly Routine & Sleep", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f ч.", totalHours),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (isRu) "Зафиксировано событий: ${monthLogs.size}" else "Logged events: ${monthLogs.size}", fontSize = 12.sp, color = Color.White)
                }
                else -> {
                    Text(if (isRu) "Записи в категории Разное" else "Other Category Summary", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${monthLogs.size} ${if (isRu) "записей" else "entries"}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = accentColor)
                }
            }
        }
    }
}

@Composable
fun CategoryLogItemCard(
    log: LogEntryEntity,
    accentColor: Color,
    isRu: Boolean,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", if (isRu) Locale("ru") else Locale.ENGLISH) }
    val formattedDate = remember(log.createdAt) { dateFormat.format(Date(log.createdAt)) }
    val structured = log.getStructuredLog()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.rawText,
                fontSize = 15.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Structured detail badge tags
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                structured?.finance_data?.let { f ->
                    LogTagBadge(text = "${f.amount} ${f.currency}", color = accentColor)
                    LogTagBadge(text = f.item, color = TextSecondary)
                }
                structured?.fitness_data?.let { fit ->
                    fit.reps?.let { r -> LogTagBadge(text = "$r reps", color = accentColor) }
                    fit.distance_km?.let { d -> LogTagBadge(text = "$d km", color = accentColor) }
                    fit.duration_minutes?.let { min -> LogTagBadge(text = "${min.toInt()} min", color = TextSecondary) }
                }
                structured?.car_data?.let { c ->
                    c.cost?.let { cost -> LogTagBadge(text = "$cost ${c.currency}", color = accentColor) }
                    LogTagBadge(text = c.part_or_service, color = TextSecondary)
                }
                structured?.routine_data?.let { r ->
                    r.duration_hours?.let { h -> LogTagBadge(text = "$h hrs", color = accentColor) }
                    LogTagBadge(text = r.activity, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun LogTagBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
