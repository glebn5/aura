package com.example.auratracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
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
import com.example.auratracker.data.repository.LogRepository
import com.example.auratracker.theme.DarkBackground
import com.example.auratracker.theme.DarkCard
import com.example.auratracker.theme.DarkCardBorder
import com.example.auratracker.theme.NeonCyan
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

@Composable
fun LogEntryScreen(
    repository: LogRepository,
    onBack: () -> Unit,
    onLogSent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val currentLang = remember { repository.getAppLanguage() }

    val suggestionChips = if (currentLang == "RU") listOf(
        "+ Продукты" to "купил продукты на 1200 руб",
        "+ Бег" to "пробежал 5 км за 30 мин",
        "+ Бензин" to "заправил бензин на 2000 руб",
        "+ Кофе" to "купил кофе за 250 руб"
    ) else listOf(
        "+ Groceries" to "купил продукты на 1200 руб",
        "+ Run" to "пробежал 5 км за 30 мин",
        "+ Gas" to "заправил бензин на 2000 руб",
        "+ Coffee" to "купил кофе за 250 руб"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Header: Back arrow + "New Log"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkCard)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (currentLang == "RU") "Новая запись" else "New Log",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Suggestion Chips (Horizontal Scrollable LazyRow)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(suggestionChips) { (label, presetText) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkCard)
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable {
                            inputText = presetText
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = label,
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Text Input Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        "Type your day... AI will parse it.",
                        color = Color.Gray,
                        fontSize = 18.sp
                    )
                },
                modifier = Modifier.fillMaxSize(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, lineHeight = 26.sp)
            )
        }

        // Bottom Input Action Bar with Microphone and Neon Send Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Microphone Button
            IconButton(
                onClick = {
                    Toast.makeText(context, "Голосовой ввод активен...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(DarkCard)
                    .border(1.dp, DarkCardBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Голос",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Glowing Neon Send Button
            Box(
                modifier = Modifier
                    .shadow(12.dp, CircleShape, spotColor = NeonCyan, ambientColor = NeonCyan)
                    .clip(CircleShape)
                    .background(NeonCyan)
                    .clickable {
                        if (inputText.isNotBlank()) {
                            val text = inputText.trim()
                            inputText = ""
                            scope.launch {
                                repository.sendLog(text)
                                Toast.makeText(context, "Запись отправлена в ИИ!", Toast.LENGTH_SHORT).show()
                                onLogSent()
                            }
                        }
                    }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Отправить",
                        tint = DarkBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
