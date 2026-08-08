package com.example.auratracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auratracker.data.local.LogEntryEntity
import com.example.auratracker.data.repository.LogRepository
import com.example.auratracker.theme.DarkBackground
import com.example.auratracker.theme.DarkCard
import com.example.auratracker.theme.DarkCardBorder
import com.example.auratracker.theme.NeonCyan
import com.example.auratracker.theme.TextSecondary
import java.util.Locale

data class CategoryDetailInfo(
    val categoryKey: String,
    val accentColor: Color,
    val titleRu: String,
    val titleEn: String
)

@Composable
fun DashboardScreen(
    repository: LogRepository,
    modifier: Modifier = Modifier
) {
    val logs by repository.logsFlow.collectAsState(initial = emptyList())
    val isRu = remember { repository.getAppLanguage() == "RU" }

    // State for opening monthly detail dialog
    var selectedCategoryForDetail by remember { mutableStateOf<CategoryDetailInfo?>(null) }

    // Current month timestamp cutoff (from 1st day of current month)
    val currentMonthStartTs = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val currentMonthLogs = remember(logs, currentMonthStartTs) {
        logs.filter { it.createdAt >= currentMonthStartTs }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Header: Title + Subtitle + Profile Avatar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Aura Tracker",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isRu) "Дашборд категорий" else "Category Dashboard",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkCard)
                    .border(1.dp, DarkCardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Аватар",
                    tint = NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5 Core Categories Grid Layout
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isRu) "Категории за текущий месяц" else "Current Month Categories",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            val mainCategories = listOf(
                CategoryDetailInfo("FINANCE", Color(0xFF00E5FF), "Финансы", "Finance"),
                CategoryDetailInfo("FITNESS", Color(0xFFAEEA00), "Фитнес и Спорт", "Fitness & Sport"),
                CategoryDetailInfo("CAR_MAINTENANCE", Color(0xFFFF9100), "Авто и Обслуживание", "Car Maintenance"),
                CategoryDetailInfo("ROUTINE", Color(0xFFE040FB), "Рутина и Сон", "Routine & Sleep"),
                CategoryDetailInfo("OTHER", Color(0xFF448AFF), "Разное и Выжимки", "Other & Summary")
            )

            // Render 5 main categories in 2-column bento grid
            val pairs = mainCategories.chunked(2)
            for (pair in pairs) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    for (cat in pair) {
                        Box(modifier = Modifier.weight(1f)) {
                            MainCategoryBentoCard(
                                category = cat,
                                logs = currentMonthLogs,
                                isRu = isRu,
                                onClick = { selectedCategoryForDetail = cat }
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    // Full Monthly Detail Dialog when any category widget is tapped
    selectedCategoryForDetail?.let { detailInfo ->
        CategoryMonthlyDetailDialog(
            categoryKey = detailInfo.categoryKey,
            accentColor = detailInfo.accentColor,
            categoryTitleRu = detailInfo.titleRu,
            categoryTitleEn = detailInfo.titleEn,
            logs = logs,
            repository = repository,
            isRu = isRu,
            onDismiss = { selectedCategoryForDetail = null }
        )
    }
}

@Composable
fun MainCategoryBentoCard(
    category: CategoryDetailInfo,
    logs: List<LogEntryEntity>,
    isRu: Boolean,
    onClick: () -> Unit
) {
    val categoryLogs = remember(logs, category.categoryKey) {
        logs.filter { it.category == category.categoryKey }
    }

    val title = if (isRu) category.titleRu else category.titleEn

    val summaryText = remember(categoryLogs, category.categoryKey, isRu) {
        when (category.categoryKey) {
            "FINANCE" -> {
                val sum = categoryLogs.sumOf { log ->
                    log.getStructuredLog()?.finance_data?.amount
                        ?: log.rawText.filter { it.isDigit() }.toDoubleOrNull()
                        ?: 0.0
                }
                String.format(Locale.getDefault(), "%,.0f ₽", sum)
            }
            "FITNESS" -> {
                val sports = categoryLogs.mapNotNull { log ->
                    log.getStructuredLog()?.fitness_data?.activity_type
                        ?: log.rawText.split(" ").firstOrNull { w -> w.length > 3 && !w.any { c -> c.isDigit() } }
                }.distinct().take(3)

                if (sports.isNotEmpty()) {
                    sports.joinToString(", ").replaceFirstChar { it.uppercase() }
                } else {
                    if (isRu) "Тренировки" else "Workouts"
                }
            }
            "CAR_MAINTENANCE" -> {
                val carParts = categoryLogs.mapNotNull { log ->
                    log.getStructuredLog()?.car_data?.part_or_service
                        ?: log.rawText
                }.distinct().take(2)

                if (carParts.isNotEmpty()) {
                    carParts.joinToString(", ")
                } else {
                    if (isRu) "Авто / Запчасти" else "Car & Parts"
                }
            }
            "ROUTINE" -> {
                val hours = categoryLogs.sumOf { log ->
                    log.getStructuredLog()?.routine_data?.duration_hours ?: 0.0
                }
                if (hours > 0) String.format(Locale.getDefault(), "%.1f ч.", hours) else if (isRu) "${categoryLogs.size} дел" else "${categoryLogs.size} events"
            }
            else -> {
                if (isRu) "${categoryLogs.size} записей" else "${categoryLogs.size} entries"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DarkCard)
            .border(1.dp, category.accentColor.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = category.accentColor,
                    maxLines = 1
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(category.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = category.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = summaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isRu) "Статистика за месяц →" else "Monthly stats →",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}
