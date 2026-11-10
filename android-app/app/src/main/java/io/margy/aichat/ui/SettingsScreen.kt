@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.margy.aichat.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.margy.aichat.data.AppSettings
import io.margy.aichat.data.PROVIDER_LOCAL
import io.margy.aichat.data.PROVIDER_POLLINATIONS
import java.util.Locale

@Composable
fun SettingsScreen(vm: ChatViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var s by remember { mutableStateOf(vm.settings) }
    var clearDialog by remember { mutableStateOf(false) }
    var keyVisible by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    fun saveSettings(new: AppSettings) {
        s = new
        vm.updateSettings(new)
    }

    val tabs = listOf("Основное", "Модель", "Аккаунт", "О приложении")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Назад") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Вкладки как чипсы
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        Modifier
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable { selectedTab = index }
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            when (selectedTab) {
                0 -> TabMain(s, ::saveSettings, keyVisible, context, vm)
                1 -> TabModel(s, ::saveSettings, keyVisible, context, vm)
                2 -> TabAccount(vm, ::saveSettings)
                3 -> TabAbout(vm, context)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (clearDialog) {
        AlertDialog(
            onDismissRequest = { clearDialog = false },
            title = { Text("Очистить все чаты?") },
            text = { Text("История переписки будет удалена с телефона. Отменить это нельзя.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAll()
                    clearDialog = false
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { clearDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun TabMain(
    s: AppSettings,
    onUpdate: (AppSettings) -> Unit,
    keyVisible: Boolean,
    context: Context,
    vm: ChatViewModel
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Тема", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "system" to "Системная",
                "light" to "Светлая",
                "dark" to "Тёмная"
            ).forEach { (value, label) ->
                FilterChip(
                    selected = s.themeMode == value,
                    onClick = { onUpdate(s.copy(themeMode = value)) },
                    label = { Text(label) }
                )
            }
        }

        Text("Акцент", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ACCENTS.forEachIndexed { index, color ->
                Box(
                    Modifier
                        .size(36.dp)
                        .background(color, CircleShape)
                        .clickable { onUpdate(s.copy(accent = index, dynamicColor = false)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (s.accent == index) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White)
                    }
                }
            }
        }

        SettingSwitch(
            title = "Цвета системы",
            subtitle = "Подхватить палитру с обоев телефона",
            checked = s.dynamicColor,
            onChange = { onUpdate(s.copy(dynamicColor = it)) }
        )
    }
}

@Composable
private fun TabModel(
    s: AppSettings,
    onUpdate: (AppSettings) -> Unit,
    keyVisible: Boolean,
    context: Context,
    vm: ChatViewModel
) {
    var keyVisibleLocal by remember { mutableStateOf(keyVisible) }
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Провайдер", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                PROVIDER_POLLINATIONS to "Бесплатно (без ключа)",
                PROVIDER_GROQ to "Groq",
                PROVIDER_OPENROUTER to "OpenRouter",
                PROVIDER_CUSTOM to "Свой сервер",
                PROVIDER_LOCAL to "Локальный (оффлайн)"
            ).forEach { (value, label) ->
                FilterChip(
                    selected = s.provider == value,
                    onClick = {
                        if (s.provider != value) {
                            val next = s.copy(provider = value)
                            onUpdate(next.copy(model = next.defaultModel()))
                        }
                    },
                    label = { Text(label) }
                )
            }
        }

        if (s.provider != PROVIDER_POLLINATIONS && s.provider != PROVIDER_LOCAL) {
            OutlinedTextField(
                value = s.apiKey,
                onValueChange = { onUpdate(s.copy(apiKey = it.trim())) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API-ключ") },
                singleLine = true,
                visualTransformation = if (keyVisibleLocal) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { keyVisibleLocal = !keyVisibleLocal }) {
                        Icon(
                            if (keyVisibleLocal) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            if (keyVisibleLocal) "Скрыть" else "Показать"
                        )
                    }
                }
            )
        } else if (s.provider == PROVIDER_POLLINATIONS) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Работает сразу без ключа и без регистрации. Лимит — 1 ответ в 45 секунд на устройство.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (s.provider == PROVIDER_CUSTOM) {
            OutlinedTextField(
                value = s.baseUrl,
                onValueChange = { onUpdate(s.copy(baseUrl = it.trim())) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Адрес сервера") },
                placeholder = { Text("http://192.168.1.10:1234/v1") },
                singleLine = true
            )
        }

        OutlinedTextField(
            value = s.model,
            onValueChange = { onUpdate(s.copy(model = it.trim())) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Модель") },
            singleLine = true
        )

        val presets = when (s.provider) {
            PROVIDER_GROQ -> GROQ_PRESETS
            PROVIDER_POLLINATIONS -> POLLINATIONS_PRESETS
            PROVIDER_CUSTOM -> emptyList()
            else -> emptyList()
        }
        if (presets.isNotEmpty()) {
            Text("Готовые модели", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                presets.forEach { id ->
                    val isActive = s.model == id
                    val desc = modelDescription(id)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onUpdate(s.copy(model = id)) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                id,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                            if (desc.isNotBlank()) {
                                Text(
                                    desc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (isActive) {
                            Icon(
                                Icons.Rounded.Check,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (s.provider == PROVIDER_POLLINATIONS) {
                Button(
                    onClick = { vm.checkConnection() },
                    enabled = !vm.checkingKey,
                    modifier = Modifier.weight(1f)
                ) {
                    if (vm.checkingKey) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Проверить связь")
                }
            } else {
                Button(
                    onClick = { vm.checkKey() },
                    enabled = !vm.checkingKey,
                    modifier = Modifier.weight(1f)
                ) {
                    if (vm.checkingKey) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Проверить ключ")
                }
            }
            TextButton(
                onClick = {
                    val url = when (s.provider) {
                        PROVIDER_GROQ -> "https://console.groq.com/keys"
                        PROVIDER_CUSTOM -> "https://lmstudio.ai"
                        else -> "https://openrouter.ai/keys"
                    }
                    openUrl(context, url)
                }
            ) {
                Text("Получить ключ")
            }
        }
    }
}

@Composable
private fun TabAccount(vm: ChatViewModel, onUpdate: (AppSettings) -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (vm.isLoggedIn) {
            Text("Аккаунт: ${vm.settings.accountUser}", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { vm.logoutAccount() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Выйти из аккаунта")
            }
        } else {
            Text("Войдите в аккаунт, чтобы сохранять переписку", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            var user by remember { mutableStateOf("") }
            var pass by remember { mutableStateOf("") }
            var mode by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Логин") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    if (mode) {
                        vm.registerAccount(user, pass)
                    } else {
                        vm.loginAccount(user, pass)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (mode) "Зарегистрироваться" else "Войти")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { mode = !mode }
            ) {
                Text(if (mode) "Уже есть аккаунт? Войти" else "Нет аккаунта? Создать")
            }
        }
    }
}

@Composable
private fun TabAbout(vm: ChatViewModel, context: Context) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("LokiChat", style = MaterialTheme.typography.titleLarge)
        Text(
            "Версия 1.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Нативное приложение на Kotlin. " +
                "Ваша переписка сохраняется на телефоне в аккаунте. " +
                "Поддерживает Pollinations, Groq, OpenRouter и локальный режим без интернета.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, vm.exportJson())
                runCatching {
                    context.startActivity(Intent.createChooser(intent, "Экспорт чатов"))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.FileDownload, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Экспорт чатов (JSON)")
        }
        TextButton(
            onClick = { vm.clearAll() }
        ) {
            Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Очистить все чаты", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Section("Ресурсы") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val url = "https://github.com/kirilltitov777z-dotcom/ai-chat"
                        openUrl(context, url)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.OpenInNew, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Репозиторий на GitHub")
                }
                OutlinedButton(
                    onClick = {
                        val url = "https://github.com/kirilltitov777z-dotcom/ai-chat/actions/workflows/build.yml"
                        openUrl(context, url)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Build, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Сборка на GitHub")
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    val url = "https://github.com/kirilltitov777z-dotcom/ai-chat/releases"
                    openUrl(context, url)
                }
            ) {
                Text("Все версии в Releases")
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
