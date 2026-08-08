package com.example.auratracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
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
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.example.auratracker.data.repository.LogRepository
import com.example.auratracker.theme.DarkBackground
import com.example.auratracker.theme.DarkCard
import com.example.auratracker.theme.DarkCardBorder
import com.example.auratracker.theme.NeonCyan
import com.example.auratracker.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: LogRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isLoggedIn by remember { mutableStateOf(repository.isUserLoggedIn()) }
    var userEmail by remember { mutableStateOf(repository.getUserEmail()) }
    var showMockLoginDialog by remember { mutableStateOf(false) }
    var showTelegramLoginDialog by remember { mutableStateOf(false) }
    var mockEmailInput by remember { mutableStateOf("") }

    var serverUrl by remember { mutableStateOf(repository.getServerUrl()) }
    var showServerDialog by remember { mutableStateOf(false) }
    var customServerInput by remember { mutableStateOf(repository.getServerUrl()) }

    // Счётчик записей для информации
    val logs by repository.logsFlow.collectAsState(initial = emptyList())

    // Настройка Credential Manager
    val credentialManager = CredentialManager.create(context)

    fun handleGoogleSignIn() {
        showMockLoginDialog = true
    }

    var currentLang by remember { mutableStateOf(repository.getAppLanguage()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = { Text(if (currentLang == "RU") "Профиль AuraTracker" else "AuraTracker Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Карточка состояния профиля
            if (isLoggedIn) {
                // Профиль авторизованного пользователя
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Аватар",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Text(
                    text = userEmail,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Пользователь AuraTracker",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        repository.logout()
                        isLoggedIn = false
                        userEmail = "guest@auratracker.ru"
                        Toast.makeText(context, "Вышли из системы", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Выйти")
                }
            } else {
                // Профиль Гостя
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Замок",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Text(
                    text = "Вы не вошли в систему",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Войдите через Google или Telegram, чтобы синхронизировать ваши данные с сервером.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Google Sign In
                    Button(
                        onClick = { handleGoogleSignIn() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Войти через Google Account")
                    }

                    // Telegram Sign In placeholder
                    OutlinedButton(
                        onClick = { showTelegramLoginDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Войти через Telegram")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Раздел Настроек и инфы
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        if (currentLang == "RU") "НАСТРОЙКИ И ДИАГНОСТИКА" else "SETTINGS & DIAGNOSTICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    // Язык интерфейса / Language Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (currentLang == "RU") "Язык приложения:" else "App Language:", fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = currentLang == "RU",
                                onClick = {
                                    repository.setAppLanguage("RU")
                                    currentLang = "RU"
                                    Toast.makeText(context, "Язык переключен на Русский", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("RU 🇷🇺", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = currentLang == "EN",
                                onClick = {
                                    repository.setAppLanguage("EN")
                                    currentLang = "EN"
                                    Toast.makeText(context, "Language switched to English", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("EN 🇬🇧", fontSize = 12.sp) }
                            )
                        }
                    }

                    // Адрес Сервера
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (currentLang == "RU") "Адрес Сервера:" else "Server URL:", fontSize = 14.sp)
                            Text(serverUrl, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(
                            onClick = {
                                customServerInput = serverUrl
                                showServerDialog = true
                            }
                        ) {
                            Text(if (currentLang == "RU") "Изменить" else "Change", fontSize = 13.sp)
                        }
                    }

                    // Размер локального кэша
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Локальная база данных:", fontSize = 14.sp)
                        Text("${logs.size} записей", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Кнопки управления кэшем
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                repository.syncPendingLogs()
                                repository.refreshHistory()
                                Toast.makeText(context, "Данные синхронизированы!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Синхронизировать сейчас")
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                repository.logout()
                                isLoggedIn = false
                                userEmail = "guest@auratracker.ru"
                                Toast.makeText(context, "Локальный кэш очищен!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Очистить локальный кэш")
                    }
                }
            }
        }
    }

    // 1. Модальное окно авторизации Google OAuth / Email
    if (showMockLoginDialog) {
        AlertDialog(
            onDismissRequest = { showMockLoginDialog = false },
            containerColor = DarkCard,
            title = {
                Text(
                    text = if (currentLang == "RU") "Вход в аккаунт AuraTracker" else "AuraTracker Account Login",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = if (currentLang == "RU") "Выберите способ входа для синхронизации с сервером:" else "Choose login method to sync with server:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    // Кнопка быстрого входа через Google
                    Button(
                        onClick = {
                            val email = if (mockEmailInput.isNotBlank()) mockEmailInput.trim() else "user@gmail.com"
                            scope.launch {
                                repository.authenticateWithEmail(email).fold(
                                    onSuccess = {
                                        isLoggedIn = true
                                        userEmail = repository.getUserEmail()
                                        showMockLoginDialog = false
                                        Toast.makeText(context, if (currentLang == "RU") "Вход успешен!" else "Login successful!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = {
                                        Toast.makeText(context, "Ошибка сервера: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text(
                            text = if (currentLang == "RU") "🌐 Войти через Google (OAuth)" else "🌐 Sign in with Google (OAuth)",
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = DarkCardBorder)

                    // Ввод любого Email
                    Text(
                        text = if (currentLang == "RU") "Или введите ваш Email:" else "Or enter your Email:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = mockEmailInput,
                        onValueChange = { mockEmailInput = it },
                        placeholder = { Text("user@gmail.com", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = if (mockEmailInput.isBlank()) "user@gmail.com" else mockEmailInput.trim()
                        scope.launch {
                            repository.authenticateWithEmail(email).fold(
                                onSuccess = {
                                    isLoggedIn = true
                                    userEmail = repository.getUserEmail()
                                    showMockLoginDialog = false
                                    Toast.makeText(context, if (currentLang == "RU") "Вход выполнен!" else "Signed in!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = {
                                    Toast.makeText(context, "Ошибка: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(if (currentLang == "RU") "Войти по Email" else "Sign in with Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMockLoginDialog = false }) {
                    Text(if (currentLang == "RU") "Отмена" else "Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 2. Модальное окно плейсхолдера Telegram глубокой ссылки
    if (showTelegramLoginDialog) {
        AlertDialog(
            onDismissRequest = { showTelegramLoginDialog = false },
            title = { Text("Авторизация Telegram") },
            text = {
                Text(
                    "Для глубоких ссылок (deep-linking) вы перенаправляетесь в Telegram-бот @AuraTrackerBot, откуда возвращается хэш авторизации. В данном прототипе этот шаг имитируется автоматически при согласии.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            // Симулируем ответ от Telegram auth
                            repository.authenticateWithGoogle("mock_token_for_tg_user_123456").fold(
                                onSuccess = {
                                    isLoggedIn = true
                                    userEmail = "tg_123456@telegram.auratracker.ru"
                                    showTelegramLoginDialog = false
                                    Toast.makeText(context, "Успешный вход через Telegram!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = {
                                    Toast.makeText(context, "Ошибка бэкенда: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("Симулировать вход")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTelegramLoginDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // 3. Модальное окно выбора/ввода адреса бэкенд сервера
    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            containerColor = DarkCard,
            title = {
                Text(
                    text = if (currentLang == "RU") "Настройка подключения к серверу" else "Server Connection Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (currentLang == "RU") "Выберите пресет или укажите свой URL бэкенда:" else "Choose preset or enter custom backend URL:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    // Пресет 1: Эмулятор Android (10.0.2.2)
                    OutlinedButton(
                        onClick = { customServerInput = "http://10.0.2.2:8000" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("💻 Локальный Эмулятор (http://10.0.2.2:8000)", fontSize = 11.sp)
                    }

                    // Пресет 2: Локальная Wi-Fi сеть (192.168.0.106:8000)
                    OutlinedButton(
                        onClick = { customServerInput = "http://192.168.0.106:8000" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📱 Локальная Wi-Fi сеть (http://192.168.0.106:8000)", fontSize = 11.sp)
                    }

                    // Пресет 3: Удаленный VPS (45.194.66.113)
                    OutlinedButton(
                        onClick = { customServerInput = "http://45.194.66.113" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🌐 Удаленный Сервер (http://45.194.66.113)", fontSize = 11.sp)
                    }

                    // Пресет 4: Localhost (127.0.0.1:8000)
                    OutlinedButton(
                        onClick = { customServerInput = "http://127.0.0.1:8000" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🏠 Localhost (http://127.0.0.1:8000)", fontSize = 11.sp)
                    }

                    OutlinedTextField(
                        value = customServerInput,
                        onValueChange = { customServerInput = it },
                        label = { Text(if (currentLang == "RU") "Пользовательский URL" else "Custom Server URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanUrl = customServerInput.trim().trimEnd('/')
                        if (cleanUrl.isNotBlank()) {
                            repository.setServerUrl(cleanUrl)
                            serverUrl = cleanUrl
                            showServerDialog = false
                            Toast.makeText(context, if (currentLang == "RU") "Адрес сервера сохранен!" else "Server URL saved!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text(if (currentLang == "RU") "Сохранить" else "Save", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text(if (currentLang == "RU") "Отмена" else "Cancel")
                }
            }
        )
    }
}
