package org.mywill.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.mywill.client.AppController
import org.mywill.client.ProfileDto
import org.mywill.client.TrustedPersonDto
import org.mywill.client.WillDto

enum class Screen {
    Auth, List, Editor, Profile, Trusted
}

/**
 * Основной Composable-компонент приложения.
 * Управляет навигацией между экранами и общим макетом (Scaffold).
 * 
 * @param controller Контроллер бизнес-логики.
 * @param onGoogleLogin Callback для запуска Google OAuth2 (зависит от платформы).
 */
@Composable
fun App(controller: AppController, onGoogleLogin: (() -> Unit)? = null) {
    var currentScreen by remember { mutableStateOf(Screen.Auth) }
    
    // Переходим на основной экран, если уже авторизованы (например, пришел токен из URL)
    LaunchedEffect(controller.state.isAuthorized) {
        if (controller.state.isAuthorized && currentScreen == Screen.Auth) {
            currentScreen = Screen.List
        }
    }

    var isSharedMode by remember { mutableStateOf(false) }
    var currentWill by remember { mutableStateOf<WillDto?>(null) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val state = controller.state

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentScreen != Screen.Auth) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    NavigationBar(
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == Screen.List && !isSharedMode,
                            onClick = { 
                                isSharedMode = false
                                currentScreen = Screen.List 
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("Мои") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.List && isSharedMode,
                            onClick = { 
                                isSharedMode = true
                                currentScreen = Screen.List 
                            },
                            icon = { Icon(Icons.Default.Handshake, contentDescription = null) },
                            label = { Text("Чужие") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Editor && currentWill == null,
                            onClick = { 
                                currentWill = null
                                currentScreen = Screen.Editor 
                            },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            label = { Text("Новое") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Profile,
                            onClick = { currentScreen = Screen.Profile },
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            label = { Text("Профиль") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Trusted,
                            onClick = { currentScreen = Screen.Trusted },
                            icon = { Icon(Icons.Default.Security, contentDescription = null) },
                            label = { Text("Доверенные") }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                when (currentScreen) {
                    Screen.Auth -> AuthScreen(
                        controller = controller,
                        onLoginSuccess = { currentScreen = Screen.List },
                        onGoogleLogin = onGoogleLogin,
                        showSnackbar = { scope.launch { snackbarHostState.showSnackbar(it) } }
                    )
                    Screen.List -> ListScreen(
                        controller = controller,
                        isSharedMode = isSharedMode,
                        onWillClick = { will ->
                            currentWill = will
                            currentScreen = Screen.Editor
                        }
                    )
                    Screen.Editor -> EditorScreen(
                        controller = controller,
                        will = currentWill,
                        isReadOnly = isSharedMode && currentWill != null,
                        onBack = { currentScreen = Screen.List },
                        showSnackbar = { scope.launch { snackbarHostState.showSnackbar(it) } }
                    )
                    Screen.Profile -> ProfileScreen(
                        controller = controller,
                        showSnackbar = { scope.launch { snackbarHostState.showSnackbar(it) } }
                    )
                    Screen.Trusted -> TrustedPeopleScreen(
                        controller = controller,
                        showSnackbar = { scope.launch { snackbarHostState.showSnackbar(it) } }
                    )
                }
            }
        }
    }
}

/**
 * Экран авторизации.
 * Поддерживает вход, регистрацию и верификацию через email.
 */
@Composable
fun AuthScreen(
    controller: AppController,
    onLoginSuccess: () -> Unit,
    onGoogleLogin: (() -> Unit)? = null,
    showSnackbar: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("MyWill", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))

        if (!isVerifying) {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        // Выбираем метод контроллера в зависимости от текущего режима (логин/регистрация)
                        val res = if (isRegistering) {
                            controller.register(email, password)
                        } else {
                            controller.login(email, password)
                        }
                        isLoading = false
                        showSnackbar(res.message)
                        if (res.success) {
                            if (isRegistering) {
                                // Если регистрация прошла успешно, переходим к вводу кода верификации
                                isVerifying = true
                            } else {
                                // Если вход успешен, переходим на экран списка
                                onLoginSuccess()
                            }
                        } else if (res.message.contains("verify")) {
                            // Если сервер сообщил, что нужна верификация (аккаунт еще не подтвержден)
                            isVerifying = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isRegistering) "Register" else "Login")
            }

            TextButton(onClick = { isRegistering = !isRegistering }) {
                Text(if (isRegistering) "Back to Login" else "Create Account")
            }
            
            if (onGoogleLogin != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onGoogleLogin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Login with Google")
                }
            }
        } else {
            Text("Verification required")
            TextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Code") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val res = controller.verify(email, code)
                        isLoading = false
                        showSnackbar(res.message)
                        if (res.success) {
                            isVerifying = false
                            isRegistering = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verify")
            }
        }
        
        if (isLoading) {
            CircularProgressIndicator()
        }
    }
}

/**
 * Экран со списком завещаний.
 * Переключается между "Моими" и "Чужими" завещаниями.
 */
@Composable
fun ListScreen(
    controller: AppController,
    isSharedMode: Boolean,
    onWillClick: (WillDto) -> Unit
) {
    var wills by remember { mutableStateOf(emptyList<WillDto>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isSharedMode) {
        wills = if (isSharedMode) controller.loadSharedWills() else controller.loadMyWills()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(if (isSharedMode) "Доступные мне" else "Мои завещания", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        
        LazyColumn {
            items(wills) { will ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { onWillClick(will) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(will.title, style = MaterialTheme.typography.titleMedium)
                        if (isSharedMode) {
                            Text("От: ${will.ownerEmail}", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(will.content.take(50) + if (will.content.length > 50) "..." else "", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Экран редактора завещания.
 * Позволяет просматривать, создавать и редактировать завещания, а также управлять доступом.
 */
@Composable
fun EditorScreen(
    controller: AppController,
    will: WillDto?,
    isReadOnly: Boolean,
    onBack: () -> Unit,
    showSnackbar: (String) -> Unit
) {
    var title by remember { mutableStateOf(will?.title ?: "") }
    var content by remember { mutableStateOf(will?.content ?: "") }
    var currentWillId by remember { mutableStateOf(will?.id) }
    var allowedEmails by remember { mutableStateOf(will?.allowedEmails ?: emptyList()) }
    var attachments by remember { mutableStateOf(will?.attachments ?: emptyList()) }
    var selectedFiles by remember { mutableStateOf(emptyList<SelectedFile>()) }
    var accessEmail by remember { mutableStateOf("") }
    var isLoadingProfile by remember { mutableStateOf(false) }

    val profile = controller.state.profile
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (profile == null) {
            isLoadingProfile = true
            try {
                println("[DEBUG_LOG] Loading profile in EditorScreen...")
                controller.loadProfile()
                println("[DEBUG_LOG] Profile loaded in EditorScreen: ${controller.state.profile}")
            } catch (e: Exception) {
                println("[ERROR_LOG] Failed to load profile in EditorScreen: ${e.message}")
            } finally {
                isLoadingProfile = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Заголовок") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = isReadOnly
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Содержание") },
            modifier = Modifier.fillMaxWidth().weight(1f),
            readOnly = isReadOnly
        )

        if (attachments.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Вложения", style = MaterialTheme.typography.titleMedium)
            attachments.forEach { attachment ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(attachment.name, modifier = Modifier.weight(1f), maxLines = 1)
                    Button(onClick = {
                        scope.launch {
                            val url = controller.getDownloadUrl(currentWillId!!, attachment.key)
                            val bytes = if (currentWillId != null) controller.downloadFile(url) else null
                            downloadFile(url, attachment.name, bytes)
                        }
                    }) {
                        Text("Скачать")
                    }
                    if (!isReadOnly) {
                        IconButton(onClick = { attachments = attachments - attachment }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
        
        if (!isReadOnly) {
            Spacer(Modifier.height(16.dp))

            val profileLoaded = profile != null
            if (profileLoaded && profile.isSubscribed) {
                // Новые выбранные файлы
                selectedFiles.forEach { file ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            file.name + " (новое)",
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { selectedFiles = selectedFiles - file }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }

                // Секция добавления вложений
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            openFilePicker { newFiles ->
                                selectedFiles = (selectedFiles + newFiles).distinctBy { it.name }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Выбрать файлы для вложения")
                    }
                }
                Spacer(Modifier.height(8.dp))
            } else if (profileLoaded) {
                // Неактивная область для вложений
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(vertical = 8.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Вложения доступны только по подписке", color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Spacer(Modifier.height(8.dp))
            } else if (isLoadingProfile) {
                Text("Загрузка данных профиля...", color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
            } else {
                // Если профиль не загрузился, пытаемся загрузить автоматически в LaunchedEffect,
                // но если совсем беда - показываем сообщение.
                Text("Не удалось загрузить данные профиля", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    scope.launch {
                        val res = if (currentWillId == null) {
                            controller.createWill(title, content, attachments, selectedFiles)
                        } else {
                            controller.updateWill(currentWillId!!, title, content, attachments, selectedFiles)
                        }
                        if (res != null) {
                            showSnackbar("Saved")
                            currentWillId = res.id
                            allowedEmails = res.allowedEmails
                            attachments = res.attachments
                            selectedFiles = emptyList() // Очищаем список новых файлов после успешного сохранения
                        } else {
                            showSnackbar("Error saving")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
            
            if (currentWillId != null) {
                Spacer(Modifier.height(24.dp))
                Text("Доступ", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = accessEmail,
                        onValueChange = { accessEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        scope.launch {
                            val res = controller.addAccess(currentWillId!!, accessEmail)
                            if (res != null) {
                                showSnackbar("Access granted")
                                allowedEmails = res.allowedEmails
                                accessEmail = ""
                            } else {
                                showSnackbar("Error")
                            }
                        }
                    }) {
                        Text("Дать")
                    }
                }
                Text("Разрешено: ${allowedEmails.joinToString()}")
            }
        }
    }
}

@Composable
fun ProfileScreen(
    controller: AppController,
    showSnackbar: (String) -> Unit
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var avatarUrl by remember { mutableStateOf("") }
    var deathTimeout by remember { mutableStateOf("") }

    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        profile = controller.loadProfile()
        profile?.let {
            avatarUrl = it.avatarUrl ?: ""
            deathTimeout = it.deathTimeoutSeconds.toString()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Профиль", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        TextField(
            value = avatarUrl,
            onValueChange = { avatarUrl = it },
            label = { Text("URL аватарки") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            value = deathTimeout,
            onValueChange = { deathTimeout = it.filter { ch -> ch.isDigit() } },
            label = { Text("Таймаут смерти (сек)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                val seconds = deathTimeout.toLongOrNull()
                val updated = controller.updateProfile(
                    avatarUrl.ifBlank { null },
                    seconds
                )
                if (updated != null) {
                    profile = updated
                    showSnackbar("Профиль обновлён")
                } else showSnackbar("Ошибка обновления профиля")
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Сохранить профиль")
        }

        Spacer(Modifier.height(24.dp))
        Text("Смена пароля", style = MaterialTheme.typography.titleMedium)
        TextField(
            value = oldPass,
            onValueChange = { oldPass = it },
            label = { Text("Старый пароль") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            value = newPass,
            onValueChange = { newPass = it },
            label = { Text("Новый пароль") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                val ok = controller.changePassword(oldPass, newPass)
                showSnackbar(if (ok) "Пароль обновлён" else "Ошибка смены пароля")
                if (ok) { oldPass = ""; newPass = "" }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Сменить пароль")
        }

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                scope.launch {
                    val ok = controller.cancelDeath()
                    showSnackbar(if (ok) "Подтверждение смерти отменено" else "Ошибка отмены")
                }
            }) { Text("Я жив") }
            Spacer(Modifier.width(16.dp))
            OutlinedButton(onClick = {
                scope.launch {
                    val ok = controller.deleteAccount()
                    showSnackbar(if (ok) "Аккаунт удалён" else "Ошибка удаления")
                }
            }) { Text("Удалить аккаунт") }
        }
    }
}

@Composable
fun TrustedPeopleScreen(
    controller: AppController,
    showSnackbar: (String) -> Unit
) {
    var trusted by remember { mutableStateOf(listOf<TrustedPersonDto>()) }
    var newEmail by remember { mutableStateOf("") }
    var owners by remember { mutableStateOf(listOf<String>()) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        trusted = controller.loadMyTrustedPeople()
        owners = controller.loadWhoseTrustedIAm()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Доверенные лица", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("Email доверенного") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                scope.launch {
                    val added = controller.addTrustedPerson(newEmail)
                    if (added != null) {
                        trusted = controller.loadMyTrustedPeople()
                        newEmail = ""
                        showSnackbar("Доверенное лицо добавлено")
                    } else showSnackbar("Ошибка добавления")
                }
            }) { Text("Добавить") }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
            items(trusted) { tp ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tp.email, style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (tp.confirmedDeath) "Статус: подтвердил смерть" else "Статус: ожидается",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OutlinedButton(onClick = {
                            scope.launch {
                                val ok = controller.removeTrustedPerson(tp.id!!)
                                if (ok) {
                                    trusted = controller.loadMyTrustedPeople()
                                    showSnackbar("Удалено")
                                } else showSnackbar("Ошибка")
                            }
                        }) { Text("Удалить") }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Я доверенное лицо для:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (owners.isEmpty()) {
            Text("Вы не являетесь доверенным ни для кого")
        } else {
            LazyColumn {
                items(owners) { owner ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(owner, modifier = Modifier.weight(1f))
                            Button(onClick = {
                                scope.launch {
                                    val ok = controller.confirmDeath(owner)
                                    showSnackbar(if (ok) "Подтверждение отправлено" else "Ошибка подтверждения")
                                }
                            }) { Text("Подтвердить смерть") }
                        }
                    }
                }
            }
        }
    }
}
