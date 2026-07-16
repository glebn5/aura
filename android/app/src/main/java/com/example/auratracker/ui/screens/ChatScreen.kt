package com.example.auratracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auratracker.data.local.GeminiStructuredLog
import com.example.auratracker.data.local.LogEntryEntity
import com.example.auratracker.data.repository.LogRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    repository: LogRepository,
    modifier: Modifier = Modifier
) {
    val logs by repository.logsFlow.collectAsState(initial = emptyList())
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Прокрутка чата вниз при добавлении новых логов
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Шапка чата
        TopAppBar(
            title = {
                Column {
                    Text("Aura Чат", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Ваш личный ИИ-логгер", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            actions = {
                IconButton(onClick = {
                    scope.launch {
                        repository.syncPendingLogs()
                        repository.refreshHistory()
                    }
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Синхронизация")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        )

        // Список сообщений
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            reverseLayout = true, // Новые сообщения внизу
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(logs, key = { it.id }) { log ->
                LogChatBlock(log = log)
            }
        }

        // Поле ввода
        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Например: купил кофе за 150р") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val textToSend = inputText.trim()
                            inputText = ""
                            scope.launch {
                                repository.sendLog(textToSend)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(24.dp)),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Отправить",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LogChatBlock(log: LogEntryEntity) {
    val timeString = remember(log.createdAt) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(log.createdAt))
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. Сообщение пользователя (выравнивание по правому краю)
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .fillMaxWidth(0.85f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = log.rawText,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = timeString,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (log.syncStatus == "SYNCED") {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Синхронизировано",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(12.dp)
                        )
                    } else {
                        // Иконка часиков для PENDING
                        Text(
                            text = "⏳",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Карточка распознавания ИИ (выравнивание по левому краю)
        val structured = log.getStructuredLog()
        if (structured != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .fillMaxWidth(0.85f)
            ) {
                AIStructuredCard(structured = structured)
            }
        }
    }
}

@Composable
fun AIStructuredCard(structured: GeminiStructuredLog) {
    val (emoji, title, color) = when (structured.category) {
        "FINANCE" -> Triple("💰", "ФИНАНСЫ", MaterialTheme.colorScheme.primary)
        "FITNESS" -> Triple("🏃", "СПОРТ", Color(0xFFE91E63))
        "CAR_MAINTENANCE" -> Triple("🔧", "АВТОСЕРВИС", Color(0xFFFF9800))
        "ROUTINE" -> Triple("🗓️", "РУТИНА", Color(0xFF00BCD4))
        else -> Triple("📝", "ЗАМЕТКА", MaterialTheme.colorScheme.secondary)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.4f)))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = color,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            when (structured.category) {
                "FINANCE" -> {
                    structured.finance_data?.let {
                        Text("💵 Сумма: ${it.amount} ${it.currency}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("🛍️ Предмет: ${it.item}", fontSize = 13.sp)
                        Text("🏷️ Категория: ${it.category}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                "FITNESS" -> {
                    structured.fitness_data?.let {
                        val type = when (it.activity_type.lowercase()) {
                            "running" -> "Бег 🏃"
                            "swimming" -> "Плавание 🏊"
                            "gym" -> "Тренажерный зал 🏋️"
                            else -> it.activity_type
                        }
                        Text("🎯 Активность: $type", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (it.distance_km != null) {
                            Text("📍 Дистанция: ${it.distance_km} км", fontSize = 13.sp)
                        }
                        if (it.duration_minutes != null) {
                            Text("⏱️ Время: ${it.duration_minutes} мин", fontSize = 13.sp)
                        }
                        it.intensity_level?.let { intensity ->
                            Text("⚡ Интенсивность: $intensity", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                "CAR_MAINTENANCE" -> {
                    structured.car_data?.let {
                        Text("⚙️ Деталь/Работа: ${it.part_or_service}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (it.cost != null) {
                            Text("💸 Стоимость: ${it.cost} ${it.currency}", fontSize = 13.sp)
                        }
                    }
                }
                "ROUTINE" -> {
                    structured.routine_data?.let {
                        Text("⏳ Рутина: ${it.activity}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (it.duration_hours != null) {
                            Text("🕒 Время: ${it.duration_hours} ч", fontSize = 13.sp)
                        }
                    }
                }
                else -> {
                    Text(structured.other_summary ?: "Запись распознана", fontSize = 13.sp)
                }
            }
        }
    }
}
