package org.mywill.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.mywill.client.*

enum class Screen {
    Auth, List, Editor
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
                            icon = { Text("M") },
                            label = { Text("Мои") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.List && isSharedMode,
                            onClick = { 
                                isSharedMode = true
                                currentScreen = Screen.List 
                            },
                            icon = { Text("S") },
                            label = { Text("Чужие") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Editor && currentWill == null,
                            onClick = { 
                                currentWill = null
                                currentScreen = Screen.Editor 
                            },
                            icon = { Text("+") },
                            label = { Text("Новое") }
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
    var accessEmail by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()

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
        
        if (!isReadOnly) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        val res = if (currentWillId == null) {
                            controller.createWill(title, content)
                        } else {
                            controller.updateWill(currentWillId!!, title, content)
                        }
                        if (res != null) {
                            showSnackbar("Saved")
                            currentWillId = res.id
                            allowedEmails = res.allowedEmails
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
