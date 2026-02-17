package org.mywill.client

import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    kotlinx.browser.window.asDynamic().onerror = { message: dynamic, source: dynamic, lineno: dynamic, colno: dynamic, error: dynamic ->
        println("[ERROR_LOG] Uncaught error: $message at $source:$lineno:$colno. Stack: ${error?.stack}")
        false
    }
    
    println("[DEBUG_LOG] Client JS starting at ${kotlinx.browser.window.location.href}")
    try {
        val apiClient = ApiClient()
        println("[DEBUG_LOG] ApiClient initialized")

        val container = document.createElement("div") as HTMLDivElement
        container.style.maxWidth = "800px"
        container.style.margin = "0 auto"
        container.style.fontFamily = "sans-serif"
        document.body?.appendChild(container)

        // Navigation
        val navDiv = document.createElement("div") as HTMLDivElement
        navDiv.style.marginBottom = "20px"
        navDiv.style.borderBottom = "1px solid #ccc"
        navDiv.style.padding = "10px 0"
        navDiv.style.display = "none"
        container.appendChild(navDiv)

        val myWillsBtn = document.createElement("button") as HTMLButtonElement
        myWillsBtn.textContent = "Мои завещания"
        navDiv.appendChild(myWillsBtn)

        val sharedWillsBtn = document.createElement("button") as HTMLButtonElement
        sharedWillsBtn.textContent = "Доступные мне"
        navDiv.appendChild(sharedWillsBtn)

        val createWillNavBtn = document.createElement("button") as HTMLButtonElement
        createWillNavBtn.textContent = "Создать новое"
        navDiv.appendChild(createWillNavBtn)

        // Auth Section
        val authDiv = document.createElement("div") as HTMLDivElement
        container.appendChild(authDiv)

        val emailInput = document.createElement("input") as HTMLInputElement
        emailInput.placeholder = "Email"
        authDiv.appendChild(emailInput)

        val passwordInput = document.createElement("input") as HTMLInputElement
        passwordInput.placeholder = "Password"
        passwordInput.type = "password"
        authDiv.appendChild(passwordInput)

        val loginButton = document.createElement("button") as HTMLButtonElement
        loginButton.textContent = "Login"
        authDiv.appendChild(loginButton)

        val registerButton = document.createElement("button") as HTMLButtonElement
        registerButton.textContent = "Register"
        authDiv.appendChild(registerButton)

        // Verification Section
        val verifyDiv = document.createElement("div") as HTMLDivElement
        verifyDiv.style.display = "none"
        container.appendChild(verifyDiv)
        val codeInput = document.createElement("input") as HTMLInputElement
        codeInput.placeholder = "Code"
        verifyDiv.appendChild(codeInput)
        val verifyBtn = document.createElement("button") as HTMLButtonElement
        verifyBtn.textContent = "Verify"
        verifyDiv.appendChild(verifyBtn)

        // List Section
        val listDiv = document.createElement("div") as HTMLDivElement
        listDiv.style.display = "none"
        container.appendChild(listDiv)

        // Editor Section
        val editorDiv = document.createElement("div") as HTMLDivElement
        editorDiv.style.display = "none"
        container.appendChild(editorDiv)

        val titleInput = document.createElement("input") as HTMLInputElement
        titleInput.placeholder = "Заголовок"
        titleInput.style.width = "100%"
        titleInput.style.marginBottom = "10px"
        editorDiv.appendChild(titleInput)

        val contentArea = document.createElement("textarea") as HTMLTextAreaElement
        contentArea.style.width = "100%"
        contentArea.style.height = "300px"
        editorDiv.appendChild(contentArea)

        val saveBtn = document.createElement("button") as HTMLButtonElement
        saveBtn.textContent = "Сохранить"
        editorDiv.appendChild(saveBtn)

        val accessSection = document.createElement("div") as HTMLDivElement
        accessSection.style.marginTop = "20px"
        editorDiv.appendChild(accessSection)

        val accessInput = document.createElement("input") as HTMLInputElement
        accessInput.placeholder = "Email для доступа"
        accessSection.appendChild(accessInput)

        val addAccessBtn = document.createElement("button") as HTMLButtonElement
        addAccessBtn.textContent = "Дать доступ"
        accessSection.appendChild(addAccessBtn)

        val allowedList = document.createElement("div") as HTMLDivElement
        accessSection.appendChild(allowedList)

        // Status
        val statusDiv = document.createElement("div") as HTMLDivElement
        statusDiv.style.marginTop = "20px"
        statusDiv.style.padding = "10px"
        statusDiv.style.backgroundColor = "#f0f0f0"
        container.appendChild(statusDiv)

        var currentWillId: Long? = null
        var isSharedMode = false

        fun showSection(section: String) {
            authDiv.style.display = if (section == "auth") "block" else "none"
            verifyDiv.style.display = if (section == "verify") "block" else "none"
            listDiv.style.display = if (section == "list") "block" else "none"
            editorDiv.style.display = if (section == "editor") "block" else "none"
            navDiv.style.display = if (section != "auth" && section != "verify") "block" else "none"
            
            // Clear status message when switching sections to avoid "Saved" hanging around
            statusDiv.textContent = ""
            statusDiv.style.color = "black"
        }

        suspend fun loadMyWills() {
            showSection("list")
            isSharedMode = false
            listDiv.innerHTML = "<h3>Мои завещания</h3>"
            val wills = apiClient.getMyWills()
            if (wills.isEmpty()) {
                listDiv.innerHTML += "<p>У вас еще нет завещаний.</p>"
            } else {
                wills.forEach { will ->
                    val item = document.createElement("div") as HTMLDivElement
                    item.style.padding = "10px"
                    item.style.border = "1px solid #eee"
                    item.style.marginBottom = "5px"
                    item.style.cursor = "pointer"
                    item.innerHTML = "<b>${will.title}</b>"
                    item.onclick = {
                        GlobalScope.launch {
                            currentWillId = will.id
                            titleInput.value = will.title
                            contentArea.value = will.content
                            titleInput.disabled = false
                            contentArea.disabled = false
                            saveBtn.style.display = "inline-block"
                            accessSection.style.display = "block"
                            allowedList.textContent = "Доступ: ${will.allowedEmails.joinToString()}"
                            showSection("editor")
                        }
                    }
                    listDiv.appendChild(item)
                }
            }
        }

        suspend fun loadSharedWills() {
            showSection("list")
            isSharedMode = true
            listDiv.innerHTML = "<h3>Доступные мне завещания</h3>"
            val wills = apiClient.getSharedWills()
            if (wills.isEmpty()) {
                listDiv.innerHTML += "<p>Вам пока ничего не открыли.</p>"
            } else {
                wills.forEach { will ->
                    val item = document.createElement("div") as HTMLDivElement
                    item.style.padding = "10px"
                    item.style.border = "1px solid #eee"
                    item.style.marginBottom = "5px"
                    item.style.cursor = "pointer"
                    item.innerHTML = "<b>${will.title}</b> (от ${will.ownerEmail})"
                    item.onclick = {
                        GlobalScope.launch {
                            currentWillId = will.id
                            titleInput.value = will.title
                            contentArea.value = will.content
                            titleInput.disabled = true
                            contentArea.disabled = true
                            saveBtn.style.display = "none"
                            accessSection.style.display = "none"
                            showSection("editor")
                        }
                    }
                    listDiv.appendChild(item)
                }
            }
        }

        loginButton.onclick = {
            GlobalScope.launch {
                statusDiv.textContent = "Logging in..."
                val res = apiClient.login(AuthRequest(emailInput.value, passwordInput.value))
                if (res.success) {
                    statusDiv.textContent = "Logged in"
                    loadMyWills()
                } else {
                    statusDiv.textContent = res.message
                    if (res.message.contains("verify")) showSection("verify")
                }
            }
        }

        registerButton.onclick = {
            GlobalScope.launch {
                statusDiv.textContent = "Registering..."
                val res = apiClient.register(AuthRequest(emailInput.value, passwordInput.value))
                statusDiv.textContent = res.message
                if (res.success) showSection("verify")
            }
        }

        verifyBtn.onclick = {
            GlobalScope.launch {
                val res = apiClient.verify(VerifyRequest(emailInput.value, codeInput.value))
                statusDiv.textContent = res.message
                if (res.success) showSection("auth")
            }
        }

        myWillsBtn.onclick = { GlobalScope.launch { loadMyWills() } }
        sharedWillsBtn.onclick = { GlobalScope.launch { loadSharedWills() } }
        createWillNavBtn.onclick = {
            currentWillId = null
            titleInput.value = ""
            contentArea.value = ""
            titleInput.disabled = false
            contentArea.disabled = false
            saveBtn.style.display = "inline-block"
            accessSection.style.display = "none"
            showSection("editor")
        }

        saveBtn.onclick = {
            GlobalScope.launch {
                val title = titleInput.value.trim()
                if (title.isEmpty()) {
                    statusDiv.textContent = "Ошибка: Заголовок обязателен!"
                    statusDiv.style.color = "red"
                    return@launch
                }
                statusDiv.style.color = "black"
                statusDiv.textContent = "Saving..."
                val id = currentWillId
                val res = if (id == null) {
                    apiClient.createWill(CreateWillRequest(title, contentArea.value))
                } else {
                    apiClient.updateWill(id, UpdateWillRequest(title, contentArea.value))
                }
                if (res != null) {
                    statusDiv.textContent = "Saved"
                    currentWillId = res.id
                    accessSection.style.display = "block"
                    allowedList.textContent = "Доступ: ${res.allowedEmails.joinToString()}"
                } else {
                    statusDiv.textContent = "Error saving"
                }
            }
        }

        addAccessBtn.onclick = {
            val id = currentWillId
            if (id != null) {
                GlobalScope.launch {
                    val res = apiClient.addAccess(id, AddAccessRequest(accessInput.value))
                    if (res != null) {
                        allowedList.textContent = "Доступ: ${res.allowedEmails.joinToString()}"
                        accessInput.value = ""
                    }
                }
            }
        }

    } catch (e: Throwable) {
        println("[ERROR_LOG] Error during Main initialization: ${e.message}")
    }
}
