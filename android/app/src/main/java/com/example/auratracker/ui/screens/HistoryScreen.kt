package com.example.auratracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auratracker.data.local.LogEntryEntity
import com.example.auratracker.data.repository.LogRepository
import com.example.auratracker.theme.DarkBackground
import com.example.auratracker.theme.DarkCard
import com.example.auratracker.theme.DarkCardBorder
import com.example.auratracker.theme.LimeGreen
import com.example.auratracker.theme.NeonCyan
import com.example.auratracker.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    repository: LogRepository,
    modifier: Modifier = Modifier
) {
    val logs by repository.logsFlow.collectAsState(initial = emptyList())
    var selectedFilter by remember { mutableStateOf("All") }

    val isRu = remember { repository.getAppLanguage() == "RU" }

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "Fitness", "Фитнес" -> logs.filter { it.category == "FITNESS" }
            "Finance", "Финансы" -> logs.filter { it.category == "FINANCE" }
            else -> logs
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = if (isRu) "История активности" else "Activity History",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Pills (All, Fitness, Finance)
        val filterOptions = if (isRu) listOf("Все", "Фитнес", "Финансы") else listOf("All", "Fitness", "Finance")

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            filterOptions.forEach { filter ->
                val isSelected = selectedFilter == filter || (selectedFilter == "All" && filter == "Все")
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) DarkCardBorder else DarkCard)
                        .border(
                            1.dp,
                            if (isSelected) NeonCyan else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) NeonCyan else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Timeline Section
        Text(
            text = if (isRu) "Сегодня" else "Today",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("История пуста. Напишите новую запись!", color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    HistoryCardItem(log = log)
                }
            }
        }
    }
}

@Composable
fun HistoryCardItem(log: LogEntryEntity) {
    val structured = log.getStructuredLog()
    val timeFormatted = remember(log.createdAt) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(log.createdAt))
    }

    val (badgeColor, iconVector, titleText, categoryTag) = when (log.category) {
        "FINANCE" -> {
            val amount = structured?.finance_data?.amount?.toInt() ?: 0
            val item = structured?.finance_data?.item ?: log.rawText
            val subCat = structured?.finance_data?.category ?: "finance"
            Quadruple(
                NeonCyan,
                Icons.Default.ShoppingBag,
                "$item ($amount ₽)",
                "[category: $subCat]"
            )
        }
        "FITNESS" -> {
            val dist = structured?.fitness_data?.distance_km
            val distText = if (dist != null) " ($dist km)" else ""
            val actType = structured?.fitness_data?.activity_type ?: "running"
            Quadruple(
                LimeGreen,
                Icons.Default.DirectionsRun,
                "${actType.replaceFirstChar { it.uppercase() }}$distText",
                "[category: $actType]"
            )
        }
        else -> {
            Quadruple(
                NeonCyan,
                Icons.Default.TwoWheeler,
                log.rawText,
                "[category: ${log.category.lowercase()}]"
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkCardBorder))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Icon Box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$timeFormatted - ${log.category}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = titleText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = categoryTag,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // More Options Icon
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
