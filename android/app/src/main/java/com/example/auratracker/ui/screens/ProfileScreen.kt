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
    
    // Счётчик записей для информации
    val logs by repository.logsFlow.collectAsState(initial = emptyList())

    // Настройка Credential Manager
    val credentialManager = CredentialManager.create(context)

    fun handleGoogleSignIn() {
        scope.launch {
            // Наш Web Client ID из консоли Google Cloud (плейсхолдер)
            val serverClientId = "1234567890-mock.apps.googleusercontent.com"
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                // Пытаемся вызвать реальный Credential Manager
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                
                // Получаем ID Token из Google Credential
                val googleIdToken = credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                
                if (googleIdToken != null) {
                    repository.authenticateWithGoogle(googleIdToken).fold(
                        onSuccess = {
                            isLoggedIn = true
                            userEmail = repository.getUserEmail()
                            Toast.makeText(context, "Вход успешен!", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = {
                            Toast.makeText(context, "Ошибка бэкенда: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "Google ID Token не найден в ответе", Toast.LENGTH_SHORT).show()
                }
            } catch (e: GetCredentialException) {
                // Если не настроены сервисы Google Play или падает ошибка,
                // запускаем удобный мок-режим, чтобы можно было проверить вход без Google Cloud
                showMockLoginDialog = true
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка входа: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Профиль AuraTracker", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(24.dp),
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
                        "НАСТРОЙКИ И ДИАГНОСТИКА",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    // Адрес Сервера
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Сервер:", fontSize = 14.sp)
                        Text("45.194.66.113:8000 (PL)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

    // 1. Модальное окно мока авторизации (удобно для разработчика без play services)
    if (showMockLoginDialog) {
        AlertDialog(
            onDismissRequest = { showMockLoginDialog = false },
            title = { Text("Вход разработчика / Mock Mode") },
            text = {
                Column {
                    Text("Credential Manager недоступен в эмуляторе без Play Services. Введите email для входа:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mockEmailInput,
                        onValueChange = { mockEmailInput = it },
                        placeholder = { Text("developer@auratracker.ru") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = if (mockEmailInput.isBlank()) "developer@auratracker.ru" else mockEmailInput.trim()
                        scope.launch {
                            // Имитируем успешный логин через Google ID Token
                            repository.authenticateWithGoogle("mock_token_for_$email").fold(
                                onSuccess = {
                                    isLoggedIn = true
                                    userEmail = repository.getUserEmail()
                                    showMockLoginDialog = false
                                },
                                onFailure = {
                                    Toast.makeText(context, "Ошибка бэкенда: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("Войти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMockLoginDialog = false }) {
                    Text("Отмена")
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
}
