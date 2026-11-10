@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.margy.aichat.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.margy.aichat.data.*
import java.util.Locale

@Composable
fun SettingsScreen(vm: ChatViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var s by remember { mutableStateOf(vm.settings) }
    var clearDialog by remember { mutableStateOf(false) }
    var keyVisible by remember { mutableStateOf(false) }

    fun set(new: AppSettings) {
        s = new
        vm.updateSettings(new)
    }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Section("Подключение") {
                Text(
                    "Сервис",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        PROVIDER_OPENROUTER to "OpenRouter",
                        PROVIDER_DEEPSEEK to "DeepSeek",
                        PROVIDER_CUSTOM to "Свой сервер"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = s.provider == value,
                            onClick = {
                                if (s.provider != value) {
                                    val next = s.copy(provider = value)
                                    set(next.copy(model = next.defaultModel()))
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = s.apiKey,
                    onValueChange = { set(s.copy(apiKey = it.trim())) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API-ключ") },
                    singleLine = true,
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                if (keyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                if (keyVisible) "Скрыть" else "Показать"
                            )
                        }
                    }
                )

                Text(
                    when (s.provider) {
                        PROVIDER_DEEPSEEK -> "Официальный API DeepSeek — очень дешёвый. Ключ: platform.deepseek.com"
                        PROVIDER_CUSTOM -> "Свой сервер: LM Studio, Ollama, vLLM. Адрес вида http://192.168.1.10:1234/v1"
                        else -> "Ключ бесплатный: openrouter.ai → Keys → Create Key. Бесплатные модели помечены :free"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.checkKey() }, enabled = !vm.checkingKey) {
                        if (vm.checkingKey) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Проверить ключ")
                    }
                    TextButton(onClick = {
                        val url = when (s.provider) {
                            PROVIDER_DEEPSEEK -> "https://platform.deepseek.com/api_keys"
                            PROVIDER_CUSTOM -> "https://lmstudio.ai"
                            else -> "https://openrouter.ai/keys"
                        }
                        openUrl(context, url)
                    }) {
                        Text("Получить ключ")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.OpenInNew, null, Modifier.size(16.dp))
                    }
                }

                if (s.provider == PROVIDER_CUSTOM) {
                    OutlinedTextField(
                        value = s.baseUrl,
                        onValueChange = { set(s.copy(baseUrl = it.trim())) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Адрес сервера") },
                        placeholder = { Text("http://192.168.1.10:1234/v1") },
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = s.model,
                    onValueChange = { set(s.copy(model = it.trim())) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Модель") },
                    singleLine = true
                )

                val presets = when (s.provider) {
                    PROVIDER_DEEPSEEK -> DEEPSEEK_PRESETS
                    PROVIDER_CUSTOM -> emptyList()
                    else -> MODEL_PRESETS
                }
                if (presets.isNotEmpty()) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { id ->
                            AssistChip(
                                onClick = { set(s.copy(model = id)) },
                                label = { Text(id.substringAfterLast('/'), maxLines = 1) }
                            )
                        }
                    }
                }

                TextButton(onClick = { vm.fetchModels() }, enabled = !vm.checkingKey) {
                    Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Выбрать из списка сервера")
                }
            }

            Section("Характер") {
                OutlinedTextField(
                    value = s.system,
                    onValueChange = { set(s.copy(system = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Системный промпт") },
                    minLines = 3,
                    maxLines = 8
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Температура", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.weight(1f))
                    Text(
                        String.format(Locale.US, "%.1f", s.temperature),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = s.temperature,
                    onValueChange = { set(s.copy(temperature = it)) },
                    valueRange = 0f..1.5f
                )
                Text(
                    "0 — строго и предсказуемо, 1.5 — смело и креативно",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Section("Оформление") {
                SettingSwitch(
                    title = "Цвета системы (Material You)",
                    subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "Палитра подхватывается с обоев и темы телефона"
                    } else {
                        "Доступно на Android 12 и новее"
                    },
                    checked = s.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ) { set(s.copy(dynamicColor = it)) }

                Text("Тема", style = MaterialTheme.typography.labelLarge)
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
                            onClick = { set(s.copy(themeMode = value)) },
                            label = { Text(label) }
                        )
                    }
                }

                Text("Акцент", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ACCENTS.forEachIndexed { index, color ->
                        Box(
                            Modifier
                                .size(42.dp)
                                .background(color, CircleShape)
                                .clickable { set(s.copy(accent = index, dynamicColor = false)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (s.accent == index) {
                                Icon(Icons.Rounded.Check, ACCENT_NAMES.getOrElse(index) { "" }, tint = Color.White)
                            }
                        }
                    }
                }
                Text(
                    "Акцент работает, когда цвета системы выключены или телефон младше Android 12",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Section("Вложения") {
                SettingSwitch(
                    title = "Отправлять картинки модели",
                    subtitle = "Если модель без зрения — автоматически отправлю без картинок",
                    checked = s.sendImages
                ) { set(s.copy(sendImages = it)) }
            }

            Section("Данные") {
                TextButton(
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
                    Text("Экспортировать чаты (JSON)", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                }
                TextButton(
                    onClick = { clearDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Очистить все чаты", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                }
            }

            Section("О приложении") {
                Text("LotiChat 2.0", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Нативное приложение: Kotlin + Jetpack Compose, Material 3 (Material You). " +
                        "Без WebView и без внешних CDN — интерфейс не может «не загрузиться».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = {
                    openUrl(context, "https://github.com/kirilltitov777z-dotcom/ai-chat")
                }) {
                    Icon(Icons.Rounded.OpenInNew, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("GitHub репозиторий")
                }
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
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
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
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
fun ModelPickerDialog(vm: ChatViewModel) {
    val all = vm.models
    var query by remember { mutableStateOf("") }
    val filtered = remember(all, query) {
        if (query.isBlank()) all else all.filter { it.contains(query.trim(), ignoreCase = true) }
    }

    Dialog(onDismissRequest = { vm.closeModelPicker() }) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(Modifier.padding(20.dp).heightIn(max = 560.dp)) {
                Text("Модели · ${all.size}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Поиск") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(filtered, key = { it }) { id ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.chooseModel(id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(id, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                if (id.endsWith(":free")) {
                                    Text(
                                        "бесплатно",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (id == vm.settings.model) {
                                Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
