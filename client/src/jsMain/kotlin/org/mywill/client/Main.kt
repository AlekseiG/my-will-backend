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
    
    try {
        val apiClient = ApiClient()
        println("[DEBUG_LOG] ApiClient initialized")

        // Состояние приложения
        val appState = AppState()

        // Создание элементов интерфейса
        val ui = createUI()
        document.body?.appendChild(ui.container)

        // Инициализация обработчиков событий
        setupEventHandlers(ui, apiClient, appState)

    } catch (e: Throwable) {
        println("[ERROR_LOG] Error during Main initialization: ${e.message}")
    }
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
    val myWillsBtn: HTMLButtonElement,
    val sharedWillsBtn: HTMLButtonElement,
    val createWillNavBtn: HTMLButtonElement,
    val authDiv: HTMLDivElement,
    val emailInput: HTMLInputElement,
    val passwordInput: HTMLInputElement,
    val loginButton: HTMLButtonElement,
    val registerButton: HTMLButtonElement,
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
    container.style.apply {
        maxWidth = "800px"
        margin = "0 auto"
        fontFamily = "sans-serif"
    }

    // --- Навигация ---
    val navDiv = document.createElement("div") as HTMLDivElement
    navDiv.style.apply {
        marginBottom = "20px"
        borderBottom = "1px solid #ccc"
        padding = "10px 0"
        display = "none"
    }
    container.appendChild(navDiv)

    val myWillsBtn = createButton("Мои завещания", navDiv)
    val sharedWillsBtn = createButton("Доступные мне", navDiv)
    val createWillNavBtn = createButton("Создать новое", navDiv)

    // --- Секция авторизации ---
    val authDiv = document.createElement("div") as HTMLDivElement
    container.appendChild(authDiv)

    val emailInput = createInput("Email", authDiv)
    val passwordInput = createInput("Password", authDiv, "password")
    val loginButton = createButton("Login", authDiv)
    val registerButton = createButton("Register", authDiv)

    // --- Секция верификации ---
    val verifyDiv = document.createElement("div") as HTMLDivElement
    verifyDiv.style.display = "none"
    container.appendChild(verifyDiv)
    
    val codeInput = createInput("Code", verifyDiv)
    val verifyBtn = createButton("Verify", verifyDiv)

    // --- Секция списка ---
    val listDiv = document.createElement("div") as HTMLDivElement
    listDiv.style.display = "none"
    container.appendChild(listDiv)

    // --- Секция редактора ---
    val editorDiv = document.createElement("div") as HTMLDivElement
    editorDiv.style.display = "none"
    container.appendChild(editorDiv)

    val titleInput = createInput("Заголовок", editorDiv)
    titleInput.style.width = "100%"
    titleInput.style.marginBottom = "10px"

    val contentArea = document.createElement("textarea") as HTMLTextAreaElement
    contentArea.style.apply {
        width = "100%"
        height = "300px"
    }
    editorDiv.appendChild(contentArea)

    val saveBtn = createButton("Сохранить", editorDiv)

    // --- Управление доступом ---
    val accessSection = document.createElement("div") as HTMLDivElement
    accessSection.style.marginTop = "20px"
    editorDiv.appendChild(accessSection)

    val accessInput = createInput("Email для доступа", accessSection)
    val addAccessBtn = createButton("Дать доступ", accessSection)
    val allowedList = document.createElement("div") as HTMLDivElement
    accessSection.appendChild(allowedList)

    // --- Статус-бар ---
    val statusDiv = document.createElement("div") as HTMLDivElement
    statusDiv.style.apply {
        marginTop = "20px"
        padding = "10px"
        backgroundColor = "#f0f0f0"
    }
    container.appendChild(statusDiv)

    return AppUI(
        container, navDiv, myWillsBtn, sharedWillsBtn, createWillNavBtn,
        authDiv, emailInput, passwordInput, loginButton, registerButton,
        verifyDiv, codeInput, verifyBtn, listDiv, editorDiv,
        titleInput, contentArea, saveBtn, accessSection, accessInput,
        addAccessBtn, allowedList, statusDiv
    )
}

// Вспомогательные функции для создания UI элементов
private fun createButton(text: String, parent: HTMLDivElement): HTMLButtonElement {
    val btn = document.createElement("button") as HTMLButtonElement
    btn.textContent = text
    parent.appendChild(btn)
    return btn
}

private fun createInput(placeholderText: String, parent: HTMLDivElement, inputType: String = "text"): HTMLInputElement {
    val input = document.createElement("input") as HTMLInputElement
    input.placeholder = placeholderText
    input.type = inputType
    parent.appendChild(input)
    return input
}

/**
 * Настраивает логику переключения экранов и обработчики кликов.
 */
@OptIn(DelicateCoroutinesApi::class)
fun setupEventHandlers(ui: AppUI, apiClient: ApiClient, state: AppState) {
    
    fun showSection(section: String) {
        ui.authDiv.style.display = if (section == "auth") "block" else "none"
        ui.verifyDiv.style.display = if (section == "verify") "block" else "none"
        ui.listDiv.style.display = if (section == "list") "block" else "none"
        ui.editorDiv.style.display = if (section == "editor") "block" else "none"
        ui.navDiv.style.display = if (section != "auth" && section != "verify") "block" else "none"
        
        ui.statusDiv.textContent = ""
        ui.statusDiv.style.color = "black"
    }

    suspend fun loadMyWills() {
        showSection("list")
        state.isSharedMode = false
        ui.listDiv.innerHTML = "<h3>Мои завещания</h3>"
        val wills = apiClient.getMyWills()
        if (wills.isEmpty()) {
            ui.listDiv.innerHTML += "<p>У вас еще нет завещаний.</p>"
        } else {
            wills.forEach { will ->
                val item = document.createElement("div") as HTMLDivElement
                item.style.apply {
                    padding = "10px"
                    border = "1px solid #eee"
                    marginBottom = "5px"
                    cursor = "pointer"
                }
                item.innerHTML = "<b>${will.title}</b>"
                item.onclick = {
                    GlobalScope.launch {
                        state.currentWillId = will.id
                        ui.titleInput.value = will.title
                        ui.contentArea.value = will.content
                        ui.titleInput.disabled = false
                        ui.contentArea.disabled = false
                        ui.saveBtn.style.display = "inline-block"
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
        state.isSharedMode = true
        ui.listDiv.innerHTML = "<h3>Доступные мне завещания</h3>"
        val wills = apiClient.getSharedWills()
        if (wills.isEmpty()) {
            ui.listDiv.innerHTML += "<p>Вам пока ничего не открыли.</p>"
        } else {
            wills.forEach { will ->
                val item = document.createElement("div") as HTMLDivElement
                item.style.apply {
                    padding = "10px"
                    border = "1px solid #eee"
                    marginBottom = "5px"
                    cursor = "pointer"
                }
                item.innerHTML = "<b>${will.title}</b> (от ${will.ownerEmail})"
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
            ui.statusDiv.textContent = "Logging in..."
            val res = apiClient.login(AuthRequest(ui.emailInput.value, ui.passwordInput.value))
            if (res.success) {
                ui.statusDiv.textContent = "Logged in"
                loadMyWills()
            } else {
                ui.statusDiv.textContent = res.message
                if (res.message.contains("verify")) showSection("verify")
            }
        }
    }

    ui.registerButton.onclick = {
        GlobalScope.launch {
            ui.statusDiv.textContent = "Registering..."
            val res = apiClient.register(AuthRequest(ui.emailInput.value, ui.passwordInput.value))
            ui.statusDiv.textContent = res.message
            if (res.success) showSection("verify")
        }
    }

    ui.verifyBtn.onclick = {
        GlobalScope.launch {
            val res = apiClient.verify(VerifyRequest(ui.emailInput.value, ui.codeInput.value))
            ui.statusDiv.textContent = res.message
            if (res.success) showSection("auth")
        }
    }

    // Навигация
    ui.myWillsBtn.onclick = { GlobalScope.launch { loadMyWills() } }
    ui.sharedWillsBtn.onclick = { GlobalScope.launch { loadSharedWills() } }
    ui.createWillNavBtn.onclick = {
        state.currentWillId = null
        ui.titleInput.value = ""
        ui.contentArea.value = ""
        ui.titleInput.disabled = false
        ui.contentArea.disabled = false
        ui.saveBtn.style.display = "inline-block"
        ui.accessSection.style.display = "none"
        showSection("editor")
    }

    // Сохранение и доступ
    ui.saveBtn.onclick = {
        GlobalScope.launch {
            val title = ui.titleInput.value.trim()
            if (title.isEmpty()) {
                ui.statusDiv.textContent = "Ошибка: Заголовок обязателен!"
                ui.statusDiv.style.color = "red"
                return@launch
            }
            ui.statusDiv.style.color = "black"
            ui.statusDiv.textContent = "Saving..."
            val id = state.currentWillId
            val res = if (id == null) {
                apiClient.createWill(CreateWillRequest(title, ui.contentArea.value))
            } else {
                apiClient.updateWill(id, UpdateWillRequest(title, ui.contentArea.value))
            }
            if (res != null) {
                ui.statusDiv.textContent = "Saved"
                state.currentWillId = res.id
                ui.accessSection.style.display = "block"
                ui.allowedList.textContent = "Доступ: ${res.allowedEmails.joinToString()}"
            } else {
                ui.statusDiv.textContent = "Error saving"
            }
        }
    }

    ui.addAccessBtn.onclick = {
        val id = state.currentWillId
        if (id != null) {
            GlobalScope.launch {
                val res = apiClient.addAccess(id, AddAccessRequest(ui.accessInput.value))
                if (res != null) {
                    ui.allowedList.textContent = "Доступ: ${res.allowedEmails.joinToString()}"
                    ui.accessInput.value = ""
                }
            }
        }
    }
}
