package org.mywill.client

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement

/**
 * Основная точка входа клиентского JavaScript приложения.
 * Управляет жизненным циклом UI, навигацией и взаимодействием с API.
 */
@OptIn(DelicateCoroutinesApi::class)
fun main() {
    // Глобальный обработчик ошибок для логирования в консоль браузера
    window.asDynamic().onerror = { message: dynamic, source: dynamic, lineno: dynamic, colno: dynamic, error: dynamic ->
        println("[ERROR_LOG] Uncaught error: $message at $source:$lineno:$colno. Stack: ${error?.stack}")
        false
    }
    
    println("[DEBUG_LOG] Client JS starting at ${window.location.href}")
    
    val apiClient: ApiClient? = try {
        println("[DEBUG_LOG] Initializing ApiClient...")
        val client = ApiClient()
        println("[DEBUG_LOG] ApiClient initialized successfully")
        client
    } catch (e: Throwable) {
        println("[ERROR_LOG] ApiClient initialization FATAL ERROR: ${e.message}")
        null
    }
    
    if (apiClient == null) {
        println("[ERROR_LOG] Cannot continue without ApiClient")
        return
    }

    try {
        // Состояние приложения
        val appState = AppState()

        // Создание элементов интерфейса
        val ui = createUI()
        document.getElementById("root")?.appendChild(ui.container)

        // Инициализация обработчиков событий
        setupEventHandlers(ui, apiClient, appState)

        // Проверка наличия токена в URL (после редиректа OAuth2)
        val hash = window.location.hash
        if (hash.startsWith("#token=")) {
            val token = hash.substringAfter("#token=")
            if (token.isNotEmpty()) {
                println("[DEBUG_LOG] Token found in URL hash, logging in...")
                apiClient.setToken(token)
                window.location.hash = "" // Очищаем hash
                GlobalScope.launch {
                    // Переключаемся на список завещаний
                    ui.authDiv.style.display = "none"
                    ui.navDiv.style.display = "flex"
                    showStatus(ui, "Logged in via Google")
                    
                    ui.myWillsBtn.click()
                }
            }
        }

    } catch (e: Throwable) {
        println("[ERROR_LOG] Error during Main initialization: ${e.message}")
    }
}

/**
 * Показывает статусное сообщение с анимацией.
 */
fun showStatus(ui: AppUI, message: String, isError: Boolean = false) {
    ui.statusDiv.textContent = message
    ui.statusDiv.className = "status-msg show" + if (isError) " error" else ""
    window.setTimeout({
        ui.statusDiv.className = "status-msg"
    }, 3000)
}

/**
 * Хранит текущее состояние интерфейса и выбранные данные.
 */
class AppState {
    var currentWillId: Long? = null
    var isSharedMode = false
}

/**
 * Содержит ссылки на все основные DOM-элементы для удобного доступа.
 */
class AppUI(
    val container: HTMLDivElement,
    val navDiv: HTMLDivElement,
    val myWillsBtn: HTMLDivElement,
    val sharedWillsBtn: HTMLDivElement,
    val createWillNavBtn: HTMLDivElement,
    val authDiv: HTMLDivElement,
    val emailInput: HTMLInputElement,
    val passwordInput: HTMLInputElement,
    val loginButton: HTMLButtonElement,
    val registerButton: HTMLButtonElement,
    val googleLoginBtn: HTMLButtonElement,
    val verifyDiv: HTMLDivElement,
    val codeInput: HTMLInputElement,
    val verifyBtn: HTMLButtonElement,
    val listDiv: HTMLDivElement,
    val editorDiv: HTMLDivElement,
    val titleInput: HTMLInputElement,
    val contentArea: HTMLTextAreaElement,
    val saveBtn: HTMLButtonElement,
    val accessSection: HTMLDivElement,
    val accessInput: HTMLInputElement,
    val addAccessBtn: HTMLButtonElement,
    val allowedList: HTMLDivElement,
    val statusDiv: HTMLDivElement
)

/**
 * Создает и настраивает DOM-структуру приложения.
 */
fun createUI(): AppUI {
    val container = document.createElement("div") as HTMLDivElement
    container.className = "app-container"

    // --- Навигация (Bottom Nav) ---
    val navDiv = document.createElement("div") as HTMLDivElement
    navDiv.className = "bottom-nav"
    navDiv.style.display = "none"
    container.appendChild(navDiv)

    val myWillsBtn = createNavItem("Мои", "M", navDiv)
    val sharedWillsBtn = createNavItem("Чужие", "S", navDiv)
    val createWillNavBtn = createNavItem("Новое", "+", navDiv)

    // --- Секция авторизации ---
    val authDiv = document.createElement("div") as HTMLDivElement
    authDiv.className = "card"
    authDiv.innerHTML = "<h1>MyWill</h1><p>Сохраните ваше наследие</p>"
    container.appendChild(authDiv)

    val emailInput = createInput("Email", authDiv)
    val passwordInput = createInput("Password", authDiv, "password")
    
    val btnRow = document.createElement("div") as HTMLDivElement
    btnRow.style.display = "flex"
    btnRow.style.asDynamic().gap = "8px"
    authDiv.appendChild(btnRow)
    
    val loginButton = createButton("Login", btnRow, "btn-primary")
    val registerButton = createButton("Register", btnRow, "btn-text")

    val googleLoginBtn = createButton("Login with Google", authDiv, "btn-google")
    googleLoginBtn.style.width = "100%"
    googleLoginBtn.style.marginTop = "16px"

    // --- Секция верификации ---
    val verifyDiv = document.createElement("div") as HTMLDivElement
    verifyDiv.className = "card"
    verifyDiv.style.display = "none"
    verifyDiv.innerHTML = "<h2>Верификация</h2><p>Введите код из письма</p>"
    container.appendChild(verifyDiv)
    
    val codeInput = createInput("Code", verifyDiv)
    val verifyBtn = createButton("Verify", verifyDiv, "btn-primary")

    // --- Секция списка ---
    val listDiv = document.createElement("div") as HTMLDivElement
    listDiv.style.padding = "16px"
    listDiv.style.display = "none"
    container.appendChild(listDiv)

    // --- Секция редактора ---
    val editorDiv = document.createElement("div") as HTMLDivElement
    editorDiv.className = "editor-container"
    editorDiv.style.display = "none"
    container.appendChild(editorDiv)

    val titleInput = createInput("Заголовок", editorDiv)
    titleInput.className = "input-field"
    titleInput.style.fontWeight = "bold"
    titleInput.style.fontSize = "20px"
    titleInput.style.border = "none"
    titleInput.style.borderBottom = "1px solid var(--outline)"
    titleInput.style.borderRadius = "0"

    val contentArea = document.createElement("textarea") as HTMLTextAreaElement
    contentArea.className = "content-area"
    contentArea.placeholder = "Ваше послание..."
    editorDiv.appendChild(contentArea)

    val saveBtn = createButton("Сохранить", editorDiv, "btn-primary")
    saveBtn.style.width = "100%"

    // --- Управление доступом ---
    val accessSection = document.createElement("div") as HTMLDivElement
    accessSection.className = "card"
    accessSection.style.marginTop = "24px"
    accessSection.innerHTML = "<h3>Доступ</h3>"
    editorDiv.appendChild(accessSection)

    val accessInput = createInput("Email для доступа", accessSection)
    val addAccessBtn = createButton("Дать доступ", accessSection, "btn-secondary")
    addAccessBtn.style.width = "100%"
    
    val allowedList = document.createElement("div") as HTMLDivElement
    allowedList.style.marginTop = "12px"
    allowedList.style.fontSize = "14px"
    allowedList.style.color = "var(--on-surface-variant)"
    accessSection.appendChild(allowedList)

    // --- Статус-бар ---
    val statusDiv = document.createElement("div") as HTMLDivElement
    statusDiv.className = "status-msg"
    document.body?.appendChild(statusDiv)

    return AppUI(
        container, navDiv, myWillsBtn, sharedWillsBtn, createWillNavBtn,
        authDiv, emailInput, passwordInput, loginButton, registerButton, googleLoginBtn,
        verifyDiv, codeInput, verifyBtn, listDiv, editorDiv,
        titleInput, contentArea, saveBtn, accessSection, accessInput,
        addAccessBtn, allowedList, statusDiv
    )
}

// Вспомогательные функции для создания UI элементов
private fun createButton(text: String, parent: HTMLDivElement, className: String = ""): HTMLButtonElement {
    val btn = document.createElement("button") as HTMLButtonElement
    btn.textContent = text
    btn.className = "btn $className"
    parent.appendChild(btn)
    return btn
}

private fun createInput(placeholderText: String, parent: HTMLDivElement, inputType: String = "text"): HTMLInputElement {
    val input = document.createElement("input") as HTMLInputElement
    input.placeholder = placeholderText
    input.type = inputType
    input.className = "input-field"
    parent.appendChild(input)
    return input
}

private fun createNavItem(text: String, icon: String, parent: HTMLDivElement): HTMLDivElement {
    val item = document.createElement("div") as HTMLDivElement
    item.className = "nav-item"
    
    val iconDiv = document.createElement("div") as HTMLDivElement
    iconDiv.className = "nav-icon"
    iconDiv.textContent = icon
    
    val textDiv = document.createElement("div") as HTMLDivElement
    textDiv.textContent = text
    
    item.appendChild(iconDiv)
    item.appendChild(textDiv)
    parent.appendChild(item)
    return item
}

/**
 * Настраивает логику переключения экранов и обработчики кликов.
 */
@OptIn(DelicateCoroutinesApi::class)
fun setupEventHandlers(ui: AppUI, apiClient: ApiClient, state: AppState) {
    
    fun updateNavSelection(selected: HTMLDivElement) {
        listOf(ui.myWillsBtn, ui.sharedWillsBtn, ui.createWillNavBtn).forEach {
            it.className = "nav-item"
        }
        selected.className = "nav-item active"
    }

    fun showSection(section: String) {
        ui.authDiv.style.display = if (section == "auth") "block" else "none"
        ui.verifyDiv.style.display = if (section == "verify") "block" else "none"
        ui.listDiv.style.display = if (section == "list") "block" else "none"
        ui.editorDiv.style.display = if (section == "editor") "flex" else "none"
        ui.navDiv.style.display = if (section != "auth" && section != "verify") "flex" else "none"
    }

    suspend fun loadMyWills() {
        showSection("list")
        updateNavSelection(ui.myWillsBtn)
        state.isSharedMode = false
        ui.listDiv.innerHTML = "<h2>Мои завещания</h2>"
        val wills = apiClient.getMyWills()
        if (wills.isEmpty()) {
            ui.listDiv.innerHTML += "<p style='color: var(--on-surface-variant)'>У вас еще нет завещаний.</p>"
        } else {
            wills.forEach { will ->
                val item = document.createElement("div") as HTMLDivElement
                item.className = "will-card"
                item.innerHTML = "<h3>${will.title}</h3><p>${will.content.take(50)}${if(will.content.length > 50) "..." else ""}</p>"
                item.onclick = {
                    GlobalScope.launch {
                        state.currentWillId = will.id
                        ui.titleInput.value = will.title
                        ui.contentArea.value = will.content
                        ui.titleInput.disabled = false
                        ui.contentArea.disabled = false
                        ui.saveBtn.style.display = "inline-flex"
                        ui.accessSection.style.display = "block"
                        ui.allowedList.textContent = "Доступ: ${will.allowedEmails.joinToString()}"
                        showSection("editor")
                    }
                }
                ui.listDiv.appendChild(item)
            }
        }
    }

    suspend fun loadSharedWills() {
        showSection("list")
        updateNavSelection(ui.sharedWillsBtn)
        state.isSharedMode = true
        ui.listDiv.innerHTML = "<h2>Доступные мне</h2>"
        val wills = apiClient.getSharedWills()
        if (wills.isEmpty()) {
            ui.listDiv.innerHTML += "<p style='color: var(--on-surface-variant)'>Вам пока ничего не открыли.</p>"
        } else {
            wills.forEach { will ->
                val item = document.createElement("div") as HTMLDivElement
                item.className = "will-card"
                item.innerHTML = "<h3>${will.title}</h3><p>От: ${will.ownerEmail}</p>"
                item.onclick = {
                    GlobalScope.launch {
                        state.currentWillId = will.id
                        ui.titleInput.value = will.title
                        ui.contentArea.value = will.content
                        ui.titleInput.disabled = true
                        ui.contentArea.disabled = true
                        ui.saveBtn.style.display = "none"
                        ui.accessSection.style.display = "none"
                        showSection("editor")
                    }
                }
                ui.listDiv.appendChild(item)
            }
        }
    }

    // Обработчики кнопок авторизации
    ui.loginButton.onclick = {
        GlobalScope.launch {
            showStatus(ui, "Logging in...")
            val res = apiClient.login(AuthRequest(ui.emailInput.value, ui.passwordInput.value))
            if (res.success) {
                showStatus(ui, "Welcome back")
                loadMyWills()
            } else {
                showStatus(ui, res.message, true)
                if (res.message.contains("verify")) showSection("verify")
            }
        }
    }

    ui.registerButton.onclick = {
        GlobalScope.launch {
            showStatus(ui, "Registering...")
            val res = apiClient.register(AuthRequest(ui.emailInput.value, ui.passwordInput.value))
            showStatus(ui, res.message, !res.success)
            if (res.success) showSection("verify")
        }
    }

    ui.googleLoginBtn.onclick = {
        // Редирект на бэкенд для начала OAuth2 флоу
        window.location.href = "http://localhost:8080/oauth2/authorization/google"
    }

    ui.verifyBtn.onclick = {
        GlobalScope.launch {
            val res = apiClient.verify(VerifyRequest(ui.emailInput.value, ui.codeInput.value))
            showStatus(ui, res.message, !res.success)
            if (res.success) showSection("auth")
        }
    }

    // Навигация
    ui.myWillsBtn.onclick = { GlobalScope.launch { loadMyWills() } }
    ui.sharedWillsBtn.onclick = { GlobalScope.launch { loadSharedWills() } }
    ui.createWillNavBtn.onclick = {
        updateNavSelection(ui.createWillNavBtn)
        state.currentWillId = null
        ui.titleInput.value = ""
        ui.contentArea.value = ""
        ui.titleInput.disabled = false
        ui.contentArea.disabled = false
        ui.saveBtn.style.display = "inline-flex"
        ui.accessSection.style.display = "none"
        showSection("editor")
    }

    // Сохранение и доступ
    ui.saveBtn.onclick = {
        GlobalScope.launch {
            val title = ui.titleInput.value.trim()
            if (title.isEmpty()) {
                showStatus(ui, "Заголовок обязателен!", true)
                return@launch
            }
            showStatus(ui, "Saving...")
            val id = state.currentWillId
            val res = if (id == null) {
                apiClient.createWill(CreateWillRequest(title, ui.contentArea.value))
            } else {
                apiClient.updateWill(id, UpdateWillRequest(title, ui.contentArea.value))
            }
            if (res != null) {
                showStatus(ui, "Saved")
                state.currentWillId = res.id
                ui.accessSection.style.display = "block"
                ui.allowedList.textContent = "Доступ: ${res.allowedEmails.joinToString()}"
            } else {
                showStatus(ui, "Error saving", true)
            }
        }
    }

    ui.addAccessBtn.onclick = {
        val id = state.currentWillId
        if (id != null) {
            GlobalScope.launch {
                val res = apiClient.addAccess(id, AddAccessRequest(ui.accessInput.value))
                if (res != null) {
                    showStatus(ui, "Access granted")
                    ui.allowedList.textContent = "Доступ: ${res.allowedEmails.joinToString()}"
                    ui.accessInput.value = ""
                } else {
                    showStatus(ui, "Error adding access", true)
                }
            }
        }
    }
}
