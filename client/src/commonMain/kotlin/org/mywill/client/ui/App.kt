package org.mywill.client.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mywill.client.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.mywill.client.AppController
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
        if (controller.state.isAuthorized) {
            if (currentScreen == Screen.Auth) {
                currentScreen = Screen.List
            }
            // Подгружаем профиль один раз при авторизации
            if (controller.state.profile == null) {
                controller.loadProfile()
            }
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
                            icon = { Icon(painterResource(Res.drawable.home), contentDescription = null) },
                            label = { Text("Мои") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.List && isSharedMode,
                            onClick = { 
                                isSharedMode = true
                                currentScreen = Screen.List 
                            },
                            icon = { Icon(painterResource(Res.drawable.handshake), contentDescription = null) },
                            label = { Text("Чужие") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Editor && currentWill == null,
                            onClick = { 
                                currentWill = null
                                currentScreen = Screen.Editor 
                            },
                            icon = { Icon(painterResource(Res.drawable.add), contentDescription = null) },
                            label = { Text("Новое") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Profile,
                            onClick = { currentScreen = Screen.Profile },
                            icon = { Icon(painterResource(Res.drawable.person), contentDescription = null) },
                            label = { Text("Профиль") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Trusted,
                            onClick = { currentScreen = Screen.Trusted },
                            icon = { Icon(painterResource(Res.drawable.security), contentDescription = null) },
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
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    }
                ) { targetScreen ->
                    when (targetScreen) {
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

    val onLoginClick = {
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
    }


    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
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
                placeholder = { Text("example@gmail.com") },
                modifier = Modifier.fillMaxWidth().semantics {
                    contentType = ContentType.EmailAddress 
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )
            Spacer(Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("Введите пароль") },
                modifier = Modifier.fillMaxWidth().semantics {
                    contentType = ContentType.Password
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onLoginClick() })
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onLoginClick() },
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
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(isSharedMode) {
        isLoading = true
        try {
            wills = if (isSharedMode) controller.loadSharedWills() else controller.loadMyWills()
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(if (isSharedMode) "Доступные мне" else "Мои завещания", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            repeat(5) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            Modifier.fillMaxWidth(0.6f).height(20.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.fillMaxWidth(0.9f).height(16.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                        )
                    }
                }
            }
        } else {
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
                                Text(
                                    will.content.take(50) + if (will.content.length > 50) "..." else "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
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
    var title by remember(will) { mutableStateOf(will?.title ?: "") }
    var content by remember(will) { mutableStateOf(will?.content ?: "") }
    var currentWillId by remember(will) { mutableStateOf(will?.id) }
    var allowedEmails by remember(will) { mutableStateOf(will?.allowedEmails ?: emptyList()) }
    var attachments by remember(will) { mutableStateOf(will?.attachments ?: emptyList()) }
    var selectedFiles by remember(will) { mutableStateOf(emptyList<SelectedFile>()) }
    var isRecording by remember { mutableStateOf(false) }
    var isVideoRecording by remember { mutableStateOf(false) }
    var accessEmail by remember(will) { mutableStateOf("") }
    var isLoadingProfile by remember { mutableStateOf(false) }
    var profileLoadError by remember { mutableStateOf(false) }
    var initialLoadDone by remember { mutableStateOf(false) }
    var activeVideoUrl by remember { mutableStateOf<String?>(null) }
    var activeImageUrl by remember { mutableStateOf<String?>(null) }

    val currentProfile = controller.state.profile
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (currentProfile == null) {
            isLoadingProfile = true
            profileLoadError = false
            try {
                println("[DEBUG_LOG] Loading profile in EditorScreen...")
                val res = controller.loadProfile()
                if (res == null) {
                    profileLoadError = true
                }
                println("[DEBUG_LOG] Profile loaded in EditorScreen: $res")
            } catch (e: Exception) {
                println("[ERROR_LOG] Failed to load profile in EditorScreen: ${e.message}")
                profileLoadError = true
            } finally {
                isLoadingProfile = false
                initialLoadDone = true
            }
        } else {
            initialLoadDone = true
        }
    }

    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState)) {
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
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
            readOnly = isReadOnly
        )

        if (attachments.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Вложения", style = MaterialTheme.typography.titleMedium)
            attachments.forEach { attachment ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(attachment.name, modifier = Modifier.weight(1f), maxLines = 1)

                    if (attachment.name.endsWith(".webm", ignoreCase = true)) {
                        if (attachment.name.contains("video", ignoreCase = true)) {
                            val videoUrl = controller.getDownloadUrl(currentWillId ?: 0, attachment.key)
                            IconButton(onClick = { activeVideoUrl = videoUrl }) {
                                Icon(painterResource(Res.drawable.play_arrow), contentDescription = "Смотреть")
                            }
                        } else {
                            AudioPlayer(
                                url = controller.getDownloadUrl(currentWillId ?: 0, attachment.key),
                                authToken = controller.state.token
                            )
                        }
                    }

                    if (attachment.name.let {
                            it.endsWith(".jpg", true) || it.endsWith(
                                ".jpeg",
                                true
                            ) || it.endsWith(".png", true) || it.endsWith(".gif", true)
                        }) {
                        val imageUrl = controller.getDownloadUrl(currentWillId ?: 0, attachment.key)
                        IconButton(onClick = { activeImageUrl = imageUrl }) {
                            Icon(painterResource(Res.drawable.image), contentDescription = "Посмотреть")
                        }
                    }

                    IconButton(onClick = {
                        scope.launch {
                            val url = controller.getDownloadUrl(currentWillId!!, attachment.key)
                            val bytes = if (currentWillId != null) controller.downloadFile(url) else null
                            downloadFile(url, attachment.name, bytes)
                        }
                    }) {
                        Icon(painterResource(Res.drawable.download), contentDescription = "Скачать")
                    }

                    if (!isReadOnly) {
                        IconButton(onClick = { attachments = attachments - attachment }) {
                            Icon(painterResource(Res.drawable.delete), contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
        
        if (!isReadOnly) {
            Spacer(Modifier.height(16.dp))

            // Секция вложений (доступна по подписке)
            val profileLoaded = currentProfile != null
            if (profileLoaded && currentProfile.isSubscribed) {
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
                            Icon(painterResource(Res.drawable.delete), contentDescription = "Удалить")
                        }
                    }
                }

                // Секция добавления вложений
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            openFilePicker { newFiles ->
                                selectedFiles = (selectedFiles + newFiles).distinctBy { it.name }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(painterResource(Res.drawable.attach_file), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Файлы")
                    }

                    if (isAudioRecordingSupported) {
                        if (!isRecording) {
                            Button(
                                onClick = {
                                    startAudioRecording()
                                    isRecording = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(painterResource(Res.drawable.mic), contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Голос")
                            }
                        } else {
                            Button(
                                onClick = {
                                    stopAudioRecording { voiceFile ->
                                        if (voiceFile != null) {
                                            selectedFiles = (selectedFiles + voiceFile).distinctBy { it.name }
                                        }
                                        isRecording = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(painterResource(Res.drawable.stop), contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Стоп")
                            }
                        }
                    }

                    if (isVideoRecordingSupported) {
                        if (!isVideoRecording) {
                            Button(
                                onClick = {
                                    startVideoRecording()
                                    isVideoRecording = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(painterResource(Res.drawable.play_arrow), contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Видео")
                            }
                        } else {
                            Button(
                                onClick = {
                                    stopVideoRecording { videoFile ->
                                        if (videoFile != null) {
                                            selectedFiles = (selectedFiles + videoFile).distinctBy { it.name }
                                        }
                                        isVideoRecording = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(painterResource(Res.drawable.stop), contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Стоп")
                            }
                        }
                    }
                }
                if (isRecording) {
                    Text(
                        "Идёт запись голосового сообщения...",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (isVideoRecording) {
                    Text(
                        "Идёт запись видео сообщения...",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            } else if (isLoadingProfile) {
                Text("Загрузка данных профиля...", color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
            } else if (profileLoadError && !profileLoaded) {
                // Если профиль не загрузился, показываем кнопку повтора
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Не удалось загрузить данные профиля для проверки подписки",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = {
                        scope.launch {
                            isLoadingProfile = true
                            profileLoadError = false
                            try {
                                val res = controller.loadProfile()
                                if (res == null) {
                                    profileLoadError = true
                                }
                            } catch (e: Exception) {
                                profileLoadError = true
                            } finally {
                                isLoadingProfile = false
                                initialLoadDone = true
                            }
                        }
                    }, modifier = Modifier.height(32.dp)) {
                        Text("Попробовать снова", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
            } else if (!initialLoadDone) {
                // Если профиль ещё не загружен и нет ошибки (в процессе первого запроса), показываем текст ожидания
                Text("Проверка подписки...", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            } else if (!profileLoaded) {
                // Случай, когда загрузка завершена, но профиль все еще null (и нет ошибки?)
                // Такое возможно если loadProfile вернул null без исключения.
                Text(
                    "Данные профиля отсутствуют",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
            } else if (!currentProfile.isSubscribed) {
                Text("Вложения доступны только по подписке", color = MaterialTheme.colorScheme.secondary)
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

        activeVideoUrl?.let { url ->
            VideoPlayer(
                url = url,
                authToken = controller.state.token,
                onClose = { activeVideoUrl = null }
            )
        }

        activeImageUrl?.let { url ->
            ImageViewer(
                url = url,
                authToken = controller.state.token,
                onClose = { activeImageUrl = null }
            )
        }
    }
}

@Composable
fun ProfileScreen(
    controller: AppController,
    showSnackbar: (String) -> Unit
) {
    var avatarUrl by remember { mutableStateOf("") }
    var deathTimeout by remember { mutableStateOf("") }

    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var profileLoadError by remember { mutableStateOf(false) }

    val currentProfile = controller.state.profile

    LaunchedEffect(Unit) {
        if (currentProfile == null) {
            isLoading = true
            profileLoadError = false
            try {
                val res = controller.loadProfile()
                if (res == null) {
                    profileLoadError = true
                }
            } catch (e: Exception) {
                profileLoadError = true
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentProfile) {
        currentProfile?.let {
            avatarUrl = it.avatarUrl ?: ""
            deathTimeout = it.deathTimeoutSeconds.toString()
        }
    }

    if (isLoading && currentProfile == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Box(
                Modifier.fillMaxWidth(0.4f).height(32.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            )
            Spacer(Modifier.height(24.dp))
            repeat(4) {
                Box(
                    Modifier.fillMaxWidth().height(56.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f))
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        return
    }

    if (currentProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (profileLoadError) {
                    Text("Не удалось загрузить данные профиля", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            isLoading = true
                            profileLoadError = false
                            try {
                                val res = controller.loadProfile()
                                if (res == null) {
                                    profileLoadError = true
                                }
                            } catch (e: Exception) {
                                profileLoadError = true
                            } finally {
                                isLoading = false
                            }
                        }
                    }) {
                        Text("Попробовать снова")
                    }
                } else {
                    Text("Загрузка...", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        return
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
            modifier = Modifier.fillMaxWidth().semantics {
                contentType = ContentType.Password
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            value = newPass,
            onValueChange = { newPass = it },
            label = { Text("Новый пароль") },
            modifier = Modifier.fillMaxWidth().semantics {
                contentType = ContentType.Password
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
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
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            trusted = controller.loadMyTrustedPeople()
            owners = controller.loadWhoseTrustedIAm()
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Доверенные лица", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("Email доверенного") },
                modifier = Modifier.weight(1f).semantics { contentType = ContentType.EmailAddress },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
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

        if (isLoading) {
            repeat(3) {
                Box(
                    Modifier.fillMaxWidth().height(72.dp).padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f))
                )
            }
        } else {
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
        }

        Spacer(Modifier.height(16.dp))
        Text("Я доверенное лицо для:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (isLoading) {
            repeat(2) {
                Box(
                    Modifier.fillMaxWidth().height(64.dp).padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f))
                )
            }
        } else if (owners.isEmpty()) {
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
