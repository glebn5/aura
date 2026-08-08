package com.example.auratracker.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
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
import androidx.navigation3.runtime.NavKey
import com.example.auratracker.AuraTrackerApp
import com.example.auratracker.theme.DarkBackground
import com.example.auratracker.theme.DarkCard
import com.example.auratracker.theme.DarkCardBorder
import com.example.auratracker.theme.NeonCyan
import com.example.auratracker.theme.TextSecondary
import com.example.auratracker.ui.screens.DashboardScreen
import com.example.auratracker.ui.screens.HistoryScreen
import com.example.auratracker.ui.screens.LogEntryScreen
import com.example.auratracker.ui.screens.ProfileScreen

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AuraTrackerApp
    val repository = app.repository

    var selectedTab by remember { mutableStateOf(0) }

    val isRu = repository.getAppLanguage() == "RU"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            if (selectedTab != 1) {
                Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = DarkCard,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item 0: Dashboard
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Dashboard",
                                tint = if (selectedTab == 0) NeonCyan else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isRu) "Дашборд" else "Dashboard",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) NeonCyan else TextSecondary
                            )
                        }
                    }

                    // Item 1: Center Glowing Plus FAB
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .shadow(10.dp, CircleShape, spotColor = NeonCyan, ambientColor = NeonCyan)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NeonCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Log",
                                tint = DarkBackground,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Item 2: History
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 2 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = if (selectedTab == 2) NeonCyan else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isRu) "История" else "History",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 2) NeonCyan else TextSecondary
                            )
                        }
                    }

                    // Item 3: Profile
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 3 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = if (selectedTab == 3) NeonCyan else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isRu) "Профиль" else "Profile",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 3) NeonCyan else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(repository = repository)
                1 -> LogEntryScreen(
                    repository = repository,
                    onBack = { selectedTab = 0 },
                    onLogSent = { selectedTab = 2 }
                )
                2 -> HistoryScreen(repository = repository)
                3 -> ProfileScreen(repository = repository)
            }
        }
    }
}
