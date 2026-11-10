@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package io.margy.aichat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import io.margy.aichat.data.Attachment
import io.margy.aichat.data.GROQ_PRESETS
import io.margy.aichat.data.MODEL_PRESETS
import io.margy.aichat.data.Message
import io.margy.aichat.data.Role
import io.margy.aichat.util.Media
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun LotiChatRoot(vm: ChatViewModel) {
    val s = vm.settings
    val dark = when (s.themeMode) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    LotiChatTheme(darkTheme = dark, dynamic = s.dynamicColor, accent = s.accent) {
        val context = LocalContext.current
        LaunchedEffect(vm.toastMessage) {
            vm.toastMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                vm.consumeToast()
            }
        }
        when (vm.screen) {
            "login" -> LoginScreen(vm = vm, onLogin = { vm.closeLogin() })
            "settings" -> SettingsScreen(vm = vm, onBack = { vm.closeSettings() })
            else -> ChatScreen(vm = vm, onOpenSettings = { vm.openSettings() })
        }
        if (vm.showModelPicker) {
            ModelPickerDialog(vm)
        }
    }
}

@Composable
private fun LoginScreen(vm: ChatViewModel, onLogin: () -> Unit) {
    val s = vm.settings
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(40.dp))
        Icon(
            Icons.Rounded.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "LotiChat",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            "Вход в аккаунт — переписка сохраняется",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("Логин") },
            placeholder = { Text("ваш логин") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Пароль") },
            placeholder = { Text("ваш пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        if (!mode) {
            Button(
                onClick = {
                    if (vm.loginAccount(user, pass)) onLogin()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("Войти") }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { mode = true }
            ) { Text("Нет аккаунта? Создать") }
        } else {
            Button(
                onClick = {
                    if (vm.registerAccount(user, pass)) onLogin()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("Зарегистрироваться") }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { mode = false }
            ) { Text("Уже есть аккаунт? Войти") }
        }

        if (s.accountUser.isNotBlank() && !s.accountLoggedIn) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Аккаунт: ${s.accountUser}. Введите пароль.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = {
            vm.closeLogin()
            vm.toChatScreen()
        }) { Text("Продолжить без аккаунта") }
    }
}

private fun modelLabel(vm: ChatViewModel): String {
    val model = vm.settings.model.ifBlank { "Loki" }
    val provider = when (vm.settings.provider) {
        "groq" -> "Groq"
        "openrouter" -> "OpenRouter"
        "custom" -> "Свой сервер"
        else -> "Loki"
    }
    val extras = ArrayList<String>()
    if (vm.thinking) extras.add("мышление")
    if (vm.searchEnabled) extras.add("поиск")
    return buildString {
        append(provider).append(" · ").append(model)
        if (extras.isNotEmpty()) append(" · ").append(extras.joinToString(", "))
    }
}

@Composable
private fun ChatScreen(vm: ChatViewModel, onOpenSettings: () -> Unit) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val chat = vm.currentChat
    val messages = chat?.messages.orEmpty()
    val listState = rememberLazyListState()
    var previewPath by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val last = messages.lastOrNull()
    LaunchedEffect(messages.size, last?.text?.length, last?.reasoning?.length, last?.status) {
        if (messages.isEmpty()) return@LaunchedEffect
        val info = listState.layoutInfo
        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (lastVisible >= messages.lastIndex - 1) {
            listState.scrollToItem(messages.lastIndex)
        }
    }
    LaunchedEffect(chat?.id) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawer(vm) { scope.launch { drawerState.close() } }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("LotiChat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (vm.isLoggedIn) "Аккаунт: ${vm.settings.accountUser}" else "Без аккаунта",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Меню")
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.openModelPicker() }) {
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Модель")
                        }
                        IconButton(onClick = { vm.newChat() }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Новый чат")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Настройки")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = { Composer(vm) }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (messages.isEmpty()) {
                    EmptyState(vm)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageRow(
                                msg = msg,
                                isLast = msg.id == last?.id,
                                onImage = { previewPath = it },
                                onCopy = { m ->
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("LotiChat", m.text))
                                    vm.showToast("Скопировано")
                                },
                                onShare = { m ->
                                    val intent = Intent(Intent.ACTION_SEND)
                                        .setType("text/plain")
                                        .putExtra(Intent.EXTRA_TEXT, m.text)
                                    runCatching {
                                        context.startActivity(Intent.createChooser(intent, "Поделиться"))
                                    }
                                },
                                onDelete = { m -> vm.deleteMessage(m.id) },
                                onRetry = { vm.retry() }
                            )
                        }
                    }
                }
            }
        }
    }

    previewPath?.let { path ->
        ImagePreviewDialog(path) { previewPath = null }
    }
}

@Composable
private fun EmptyState(vm: ChatViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Напишите первое сообщение", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Loki · Бесплатно · Без ключа",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MessageRow(
    msg: Message,
    isLast: Boolean,
    onImage: (String) -> Unit,
    onCopy: (Message) -> Unit,
    onShare: (Message) -> Unit,
    onDelete: (Message) -> Unit,
    onRetry: () -> Unit
) {
    val isUser = msg.role == Role.USER
    var menu by remember { mutableStateOf(false) }

    val bg = when {
        msg.error -> MaterialTheme.colorScheme.errorContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val fg = when {
        msg.error -> MaterialTheme.colorScheme.onErrorContainer
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box {
            Surface(
                shape = if (isUser) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
                else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp),
                color = bg,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .combinedClickable(onClick = {}, onLongClick = { menu = true })
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (msg.attachments.isNotEmpty()) {
                        AttachmentsRow(msg.attachments, onImage)
                        Spacer(Modifier.height(6.dp))
                    }
                    if (msg.reasoning.isNotBlank()) {
                        ReasoningBlock(msg.id, msg.reasoning, fg)
                        Spacer(Modifier.height(6.dp))
                    }
                    when {
                        msg.text.isNotBlank() -> {
                            if (msg.streaming) {
                                Text(
                                    msg.text + "▍",
                                    color = fg,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            } else {
                                MarkdownText(msg.text, color = fg)
                            }
                        }
                        msg.streaming -> TypingDots(msg.status)
                        else -> {}
                    }
                    if (msg.text.isNotBlank() && !msg.status.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(msg.status!!, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.7f))
                    }
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Копировать") },
                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                    onClick = { menu = false; onCopy(msg) }
                )
                DropdownMenuItem(
                    text = { Text("Поделиться") },
                    leadingIcon = { Icon(Icons.Rounded.Share, null) },
                    onClick = { menu = false; onShare(msg) }
                )
                if (isLast && !isUser && !msg.streaming) {
                    DropdownMenuItem(
                        text = { Text("Повторить") },
                        leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                        onClick = { menu = false; onRetry() }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Удалить") },
                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) },
                    onClick = { menu = false; onDelete(msg) }
                )
            }
        }
    }
}

@Composable
private fun AttachmentsRow(list: List<Attachment>, onImage: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        list.forEach { a ->
            val path = a.path
            if (a.isImage && path != null) {
                val bmp = remember(path) { Media.decodeThumbnail(path, 320) }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = a.name,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onImage(path) },
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Description, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(a.name.take(20), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReasoningBlock(id: String, text: String, fg: Color) {
    var open by rememberSaveable(id) { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lightbulb, null, Modifier.size(16.dp), tint = fg.copy(alpha = 0.85f))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Мышление модели",
                    style = MaterialTheme.typography.labelMedium,
                    color = fg.copy(alpha = 0.85f)
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = fg.copy(alpha = 0.85f)
                )
            }
            AnimatedVisibility(visible = open) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    color = fg.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun TypingDots(status: String?) {
    val transition = rememberInfiniteTransition(label = "dots")
    val a1 by transition.animateFloat(
        0.25f, 1f,
        infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "a1"
    )
    val a2 by transition.animateFloat(
        0.25f, 1f,
        infiniteRepeatable(tween(520, delayMillis = 130), RepeatMode.Reverse),
        label = "a2"
    )
    val a3 by transition.animateFloat(
        0.25f, 1f,
        infiniteRepeatable(tween(520, delayMillis = 260), RepeatMode.Reverse),
        label = "a3"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf(a1, a2, a3).forEach { a ->
            Box(
                Modifier
                    .padding(end = 4.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = a))
            )
        }
        if (!status.isNullOrBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Composer(vm: ChatViewModel) {
    val context = LocalContext.current
    var attachMenu by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) vm.addAttachments(uris)
    }
    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.addAttachments(uris)
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        cameraUri = null
        if (ok && uri != null) vm.addAttachments(listOf(uri))
    }

    fun openCamera() {
        runCatching {
            val file = File(context.cacheDir, "cam_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            cameraUri = uri
            takePicture.launch(uri)
        }.onFailure {
            Toast.makeText(context, "Камера недоступна", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(top = 6.dp)
        ) {
            if (vm.pending.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vm.pending, key = { it.path ?: it.name }) { att ->
                        PendingChip(att) { vm.removeAttachment(att) }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box {
                    IconButton(onClick = { attachMenu = true }, modifier = Modifier.size(44.dp)) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Прикрепить",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = attachMenu, onDismissRequest = { attachMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Камера") },
                            leadingIcon = { Icon(Icons.Rounded.PhotoCamera, null) },
                            onClick = { attachMenu = false; openCamera() }
                        )
                        DropdownMenuItem(
                            text = { Text("Галерея") },
                            leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null) },
                            onClick = { attachMenu = false; pickImages.launch("image/*") }
                        )
                        DropdownMenuItem(
                            text = { Text("Файл") },
                            leadingIcon = { Icon(Icons.Rounded.Description, null) },
                            onClick = { attachMenu = false; pickFiles.launch(arrayOf("*/*")) }
                        )
                    }
                }

                BasicTextField(
                    value = vm.draft,
                    onValueChange = { vm.draft = it },
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp, vertical = 10.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 5,
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth()) {
                            if (vm.draft.isEmpty()) {
                                Text(
                                    "Сообщение…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            inner()
                        }
                    }
                )

                MiniToggle(Icons.Rounded.Lightbulb, vm.thinking, "Мышление") { vm.thinking = !vm.thinking }
                MiniToggle(Icons.Rounded.Public, vm.searchEnabled, "Поиск") { vm.searchEnabled = !vm.searchEnabled }
                Spacer(Modifier.width(2.dp))

                FilledIconButton(
                    onClick = { vm.send() },
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (vm.busy) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = if (vm.busy) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        if (vm.busy) Icons.Rounded.Stop else Icons.Rounded.ArrowUpward,
                        contentDescription = if (vm.busy) "Стоп" else "Отправить",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    on: Boolean,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (on) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            contentColor = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PendingChip(att: Attachment, onRemove: () -> Unit) {
    Box {
        if (att.isImage && att.path != null) {
            val bmp = remember(att.path) { Media.decodeThumbnail(att.path, 220) }
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                if (bmp != null) {
                    Image(
                        bmp.asImageBitmap(),
                        contentDescription = att.name,
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.size(64.dp))
                }
            }
        } else {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    Modifier.height(64.dp).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Description, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(att.name.take(18), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(20.dp)
                .clickable { onRemove() }
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Убрать",
                modifier = Modifier.padding(4.dp),
                tint = MaterialTheme.colorScheme.onError
            )
        }
    }
}

@Composable
private fun ChatDrawer(vm: ChatViewModel, close: () -> Unit) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxHeight()) {
            Spacer(Modifier.height(20.dp))
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Chat,
                            null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("LotiChat", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (vm.isLoggedIn) "Аккаунт: ${vm.settings.accountUser}" else "Без аккаунта",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            NavigationDrawerItem(
                label = { Text("Новый чат") },
                icon = { Icon(Icons.Rounded.Add, null) },
                selected = false,
                onClick = { vm.newChat(); close() },
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(vm.chats, key = { it.id }) { chat ->
                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(
                                    chat.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    chat.preview(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        selected = chat.id == vm.currentId,
                        onClick = { vm.selectChat(chat.id); close() },
                        badge = {
                            IconButton(onClick = { vm.deleteChat(chat.id) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Rounded.DeleteOutline, "Удалить", Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            NavigationDrawerItem(
                label = { Text("Настройки") },
                icon = { Icon(Icons.Rounded.Settings, null) },
                selected = false,
                onClick = { close(); vm.openSettings() },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
            )
            if (vm.isLoggedIn) {
                NavigationDrawerItem(
                    label = { Text("Выйти из аккаунта") },
                    icon = { Icon(Icons.Rounded.ExitToApp, null) },
                    selected = false,
                    onClick = { vm.logoutAccount(); close() },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
            NavigationDrawerItem(
                label = { Text("О приложении") },
                icon = { Icon(Icons.Rounded.Info, null) },
                selected = false,
                onClick = {
                    close()
                    vm.showToast("LotiChat 2.7 · Loki + Groq")
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ImagePreviewDialog(path: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        val bmp = remember(path) { Media.decodeThumbnail(path, 2000) }
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                    Text("Не удалось открыть изображение")
                }
            }
        }
    }
}

@Composable
private fun ModelPickerDialog(vm: ChatViewModel) {
    val presets = when (vm.settings.provider) {
        "groq" -> GROQ_PRESETS
        else -> MODEL_PRESETS
    }
    val all = vm.models.ifEmpty { presets }
    var query by remember { mutableStateOf("") }
    val filtered = remember(all, query) {
        if (query.isBlank()) all else all.filter { it.contains(query.trim(), ignoreCase = true) }
    }

    Dialog(onDismissRequest = { vm.closeModelPicker() }) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                Modifier
                    .padding(20.dp)
                    .heightIn(max = 560.dp)
            ) {
                Text("Выбор модели", style = MaterialTheme.typography.titleLarge)
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
                                if (id == "demo:claude" || id == "demo:gpt" || id == "demo:deepseek") {
                                    Text(
                                        "бесплатно, без ключа",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (id.endsWith(":free")) {
                                    Text(
                                        "бесплатно (:free)",
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
