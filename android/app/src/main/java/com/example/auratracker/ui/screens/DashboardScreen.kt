package com.example.auratracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auratracker.data.local.CustomDashboardEntity
import com.example.auratracker.data.local.LogEntryEntity
import com.example.auratracker.data.repository.LogRepository
import com.example.auratracker.theme.DarkBackground
import com.example.auratracker.theme.DarkCard
import com.example.auratracker.theme.DarkCardBorder
import com.example.auratracker.theme.NeonCyan
import com.example.auratracker.theme.TextSecondary
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs by repository.logsFlow.collectAsState(initial = emptyList())
    val dashboards by repository.dashboardsFlow.collectAsState(initial = emptyList())
    val isRu = remember { repository.getAppLanguage() == "RU" }
    var showAddDialog by remember { mutableStateOf(false) }

    // State for opening monthly detail dialog
    var selectedCategoryForDetail by remember { mutableStateOf<CategoryDetailInfo?>(null) }

    LaunchedEffect(Unit) {
        repository.seedDefaultDashboardsIfEmpty()
    }

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

    // Calculate dynamic totals for Month in Numbers hero card
    val totalExpenses = remember(currentMonthLogs) {
        currentMonthLogs.filter { it.category == "FINANCE" }
            .sumOf { it.getStructuredLog()?.finance_data?.amount ?: 0.0 }
    }

    val runningDistance = remember(currentMonthLogs) {
        currentMonthLogs.filter { it.category == "FITNESS" }
            .sumOf { it.getStructuredLog()?.fitness_data?.distance_km ?: 0.0 }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Header: "Omnis Hub" + "+ Add Dashboard" Button + Profile Avatar
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
                    text = if (isRu) "Статистика за текущий месяц" else "Current month statistics",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // "+ Add Dashboard" Button
                Box(
                    modifier = Modifier
                        .shadow(8.dp, CircleShape, spotColor = NeonCyan, ambientColor = NeonCyan)
                        .clip(CircleShape)
                        .background(DarkCard)
                        .border(1.dp, NeonCyan, CircleShape)
                        .clickable { showAddDialog = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Widget",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRu) "+ Виджет" else "+ Widget",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Profile Avatar Box
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hero glowing card: "Month in Numbers"
        HeroMonthInNumbersCard(
            totalExpenses = totalExpenses,
            runningDistance = runningDistance,
            isRu = isRu
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 5 Core Categories & Dynamic Custom Cards List
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isRu) "Категории и виджеты" else "Categories & Widgets",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // 5 Main Core Category Bento Cards
            val mainCategories = listOf(
                CategoryDetailInfo("FINANCE", Color(0xFF00E5FF), "Финансы", "Finance"),
                CategoryDetailInfo("FITNESS", Color(0xFFAEEA00), "Фитнес и Спорт", "Fitness & Sport"),
                CategoryDetailInfo("CAR_MAINTENANCE", Color(0xFFFF9100), "Авто и Обслуживание", "Car Maintenance"),
                CategoryDetailInfo("ROUTINE", Color(0xFFE040FB), "Рутина и Сон", "Routine & Sleep"),
                CategoryDetailInfo("OTHER", Color(0xFF448AFF), "Разное и Выжимки", "Other & Summary")
            )

            // Render 5 main categories in 2-column bento grid layout
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

            // Custom AI/User Generated Bento Cards
            if (dashboards.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isRu) "Пользовательские виджеты" else "Custom AI Widgets",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                val customPairs = dashboards.chunked(2)
                for (pair in customPairs) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        for (dash in pair) {
                            Box(modifier = Modifier.weight(1f)) {
                                DynamicBentoCard(
                                    dashboard = dash,
                                    logs = logs,
                                    isRu = isRu,
                                    onDelete = {
                                        scope.launch {
                                            repository.deleteDashboard(dash.id)
                                            Toast.makeText(context, if (isRu) "Виджет удален" else "Widget deleted", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onClick = {
                                        val catInfo = CategoryDetailInfo(
                                            categoryKey = dash.categoryFilter,
                                            accentColor = try { Color(android.graphics.Color.parseColor(dash.accentColorHex)) } catch (e: Exception) { NeonCyan },
                                            titleRu = dash.titleRu,
                                            titleEn = dash.title
                                        )
                                        selectedCategoryForDetail = catInfo
                                    }
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
    }

    // Modal Dialog to add dynamic dashboard from prompt
    if (showAddDialog) {
        AddDashboardDialog(
            isRu = isRu,
            onDismiss = { showAddDialog = false },
            onAddPrompt = { prompt ->
                scope.launch {
                    repository.addDashboardFromPrompt(prompt)
                    Toast.makeText(context, if (isRu) "Виджет создан!" else "Widget created!", Toast.LENGTH_SHORT).show()
                    showAddDialog = false
                }
            }
        )
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
                val totalReps = categoryLogs.sumOf { log ->
                    log.getStructuredLog()?.fitness_data?.reps
                        ?: log.rawText.filter { it.isDigit() }.toIntOrNull()
                        ?: 0
                }
                val totalKm = categoryLogs.sumOf { log ->
                    log.getStructuredLog()?.fitness_data?.distance_km ?: 0.0
                }
                if (totalReps > 0) {
                    "$totalReps reps"
                } else if (totalKm > 0) {
                    String.format(Locale.getDefault(), "%.1f км", totalKm)
                } else {
                    if (isRu) "${categoryLogs.size} тренировок" else "${categoryLogs.size} workouts"
                }
            }
            "CAR_MAINTENANCE" -> {
                val sum = categoryLogs.sumOf { log ->
                    log.getStructuredLog()?.car_data?.cost
                        ?: log.rawText.filter { it.isDigit() }.toDoubleOrNull()
                        ?: 0.0
                }
                String.format(Locale.getDefault(), "%,.0f ₽", sum)
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
            .padding(16.dp)
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

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = summaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isRu) "Нажмите для статистики →" else "Tap for stats →",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun DynamicBentoCard(
    dashboard: CustomDashboardEntity,
    logs: List<LogEntryEntity>,
    isRu: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val accentColor = remember(dashboard.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(dashboard.accentColorHex))
        } catch (e: Exception) {
            NeonCyan
        }
    }

    val title = if (isRu) dashboard.titleRu else dashboard.title

    // Filter logs for this widget
    val filteredLogs = remember(logs, dashboard) {
        val now = System.currentTimeMillis()
        val cutoff = now - (dashboard.timeRangeDays * 24 * 60 * 60 * 1000L)
        logs.filter { log ->
            val logTime = log.createdAt
            val categoryMatches = when (dashboard.categoryFilter) {
                "ALL" -> true
                else -> log.category == dashboard.categoryFilter
            }
            val subCatMatches = dashboard.subCategoryFilter == null || 
                log.getStructuredLog()?.let { s ->
                    s.finance_data?.category?.lowercase()?.contains(dashboard.subCategoryFilter) == true ||
                    s.fitness_data?.activity_type?.lowercase()?.contains(dashboard.subCategoryFilter) == true
                } == true

            categoryMatches && subCatMatches && (logTime >= cutoff || logs.size < 5)
        }
    }

    val valueDisplay = remember(filteredLogs, dashboard) {
        when (dashboard.categoryFilter) {
            "FITNESS" -> {
                if (dashboard.subCategoryFilter == "pushups" || dashboard.subCategoryFilter == "pullups" || dashboard.subCategoryFilter == "squats") {
                    val count = filteredLogs.sumOf { log ->
                        log.getStructuredLog()?.fitness_data?.reps
                            ?: log.rawText.filter { it.isDigit() }.toIntOrNull()
                            ?: 0
                    }
                    val unitName = when (dashboard.subCategoryFilter) {
                        "pushups" -> if (isRu) "отжиманий" else "pushups"
                        "pullups" -> if (isRu) "подтягиваний" else "pullups"
                        else -> if (isRu) "приседаний" else "squats"
                    }
                    "$count $unitName"
                } else if (dashboard.subCategoryFilter == "running") {
                    val dist = filteredLogs.sumOf { log ->
                        log.getStructuredLog()?.fitness_data?.distance_km
                            ?: log.rawText.split(" ").firstNotNullOfOrNull { word ->
                                word.replace(",", ".").replace("км", "").toDoubleOrNull()
                            }
                            ?: 0.0
                    }
                    if (isRu) "${dist.toInt()} км за месяц" else "${dist.toInt()} km this month"
                } else {
                    if (isRu) "Тренировок: ${filteredLogs.size}" else "Workouts: ${filteredLogs.size}"
                }
            }
            "FINANCE" -> {
                val sum = filteredLogs.sumOf { log ->
                    log.getStructuredLog()?.finance_data?.amount
                        ?: log.rawText.filter { it.isDigit() }.toDoubleOrNull()
                        ?: 0.0
                }
                String.format(Locale.getDefault(), "%,.0f ₽", sum)
            }
            "CAR_MAINTENANCE" -> {
                val sum = filteredLogs.sumOf { log ->
                    log.getStructuredLog()?.car_data?.cost
                        ?: log.rawText.filter { it.isDigit() }.toDoubleOrNull()
                        ?: 0.0
                }
                String.format(Locale.getDefault(), "%,.0f ₽", sum)
            }
            else -> {
                if (isRu) "Записей: ${filteredLogs.size}" else "Entries: ${filteredLogs.size}"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkCard)
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
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
                    color = accentColor,
                    maxLines = 1
                )
                
                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = valueDisplay,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${dashboard.timeRangeDays} ${if (isRu) "дн." else "days"}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun AddDashboardDialog(
    isRu: Boolean,
    onDismiss: () -> Unit,
    onAddPrompt: (String) -> Unit
) {
    var promptText by remember { mutableStateOf("") }

    val presetPrompts = if (isRu) listOf(
        "расходы за неделю",
        "отжимания за месяц",
        "траты на бензин",
        "расходы на кофе"
    ) else listOf(
        "weekly expenses",
        "monthly pushups",
        "gas spending",
        "coffee expenses"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Text(
                text = if (isRu) "Добавить дашборд" else "Add Custom Widget",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp
            )
        },
        text = {
            Column {
                Text(
                    text = if (isRu) "Введите текстовый промпт для ИИ (например: 'расходы за неделю')" else "Type a prompt for AI (e.g. 'weekly expenses')",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = { Text(if (isRu) "Промпт для дашборда..." else "Dashboard prompt...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Suggestion chips
                Text(if (isRu) "Примеры:" else "Examples:", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetPrompts.take(2).forEach { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBackground)
                                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable { promptText = chip }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(chip, color = NeonCyan, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (promptText.isNotBlank()) {
                        onAddPrompt(promptText.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text(if (isRu) "Создать" else "Create", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isRu) "Отмена" else "Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun HeroMonthInNumbersCard(totalExpenses: Double, runningDistance: Double, isRu: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = NeonCyan, ambientColor = NeonCyan)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkCard)
            .border(1.5.dp, NeonCyan.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRu) "Месяц в цифрах" else "Month in Numbers",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Total Expenses
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRu) "Всего трат" else "Total expenses",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%,.0f ₽", totalExpenses),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MiniBarChartCanvas()
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right side: Running Distance
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRu) "Дистанция бега" else "Running distance",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), if (isRu) "%.0f км" else "%.0f km", runningDistance),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MiniLineChartCanvas()
                }
            }
        }
    }
}

@Composable
fun MiniBarChartCanvas() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val width = size.width
        val height = size.height
        val values = listOf(0.4f, 0.9f, 0.3f, 0.7f, 0.5f)
        val barWidth = width / (values.size * 2)

        values.forEachIndexed { index, valPct ->
            val barHeight = height * valPct
            val x = index * barWidth * 2f + 4f
            val y = height - barHeight
            drawRoundRect(
                color = if (index == 1) NeonCyan else Color.Gray.copy(alpha = 0.3f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

@Composable
fun MiniLineChartCanvas() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val width = size.width
        val height = size.height
        val points = listOf(
            Offset(0f, height * 0.7f),
            Offset(width * 0.3f, height * 0.4f),
            Offset(width * 0.6f, height * 0.8f),
            Offset(width, height * 0.2f)
        )

        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        drawPath(
            path = path,
            color = NeonCyan,
            style = Stroke(width = 3f)
        )
    }
}
