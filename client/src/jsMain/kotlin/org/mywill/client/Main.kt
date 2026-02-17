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
        document.body?.appendChild(container)
        println("[DEBUG_LOG] Container added")

        // Form
        val formDiv = document.createElement("div") as HTMLDivElement
        container.appendChild(formDiv)
        println("[DEBUG_LOG] Form div added")

        val emailInput = document.createElement("input") as HTMLInputElement
        emailInput.placeholder = "Email"
        emailInput.type = "email"
        formDiv.appendChild(emailInput)

        val passwordInput = document.createElement("input") as HTMLInputElement
        passwordInput.placeholder = "Password"
        passwordInput.type = "password"
        formDiv.appendChild(passwordInput)

        val loginButton = document.createElement("button") as HTMLButtonElement
        loginButton.textContent = "Login"
        formDiv.appendChild(loginButton)

        val registerButton = document.createElement("button") as HTMLButtonElement
        registerButton.textContent = "Register"
        formDiv.appendChild(registerButton)
        println("[DEBUG_LOG] Form buttons added")

        // Verification Section
        val verifyDiv = document.createElement("div") as HTMLDivElement
        verifyDiv.style.marginTop = "10px"
        verifyDiv.style.display = "none" // Hidden initially
        container.appendChild(verifyDiv)

        val codeInput = document.createElement("input") as HTMLInputElement
        codeInput.placeholder = "Verification Code"
        verifyDiv.appendChild(codeInput)

        val verifyButton = document.createElement("button") as HTMLButtonElement
        verifyButton.textContent = "Verify Email"
        verifyDiv.appendChild(verifyButton)
        println("[DEBUG_LOG] Verification section added")

        // Will Section
        val willDiv = document.createElement("div") as HTMLDivElement
        willDiv.style.marginTop = "20px"
        willDiv.style.display = "none"
        container.appendChild(willDiv)

        val willTitle = document.createElement("h3")
        willTitle.textContent = "Мое завещание"
        willDiv.appendChild(willTitle)

        val willTextArea = document.createElement("textarea") as HTMLTextAreaElement
        willTextArea.style.width = "400px"
        willTextArea.style.height = "200px"
        willTextArea.placeholder = "Напишите ваше послание здесь..."
        willDiv.appendChild(willTextArea)

        val saveWillButton = document.createElement("button") as HTMLButtonElement
        saveWillButton.textContent = "Сохранить завещание"
        willDiv.appendChild(saveWillButton)

        val accessDiv = document.createElement("div") as HTMLDivElement
        accessDiv.style.marginTop = "10px"
        willDiv.appendChild(accessDiv)

        val accessInput = document.createElement("input") as HTMLInputElement
        accessInput.placeholder = "Email для доступа"
        accessDiv.appendChild(accessInput)

        val addAccessButton = document.createElement("button") as HTMLButtonElement
        addAccessButton.textContent = "Дать доступ"
        accessDiv.appendChild(addAccessButton)

        val allowedEmailsDiv = document.createElement("div") as HTMLDivElement
        willDiv.appendChild(allowedEmailsDiv)
        println("[DEBUG_LOG] Will section added")

        val resultDiv = document.createElement("div") as HTMLDivElement
        resultDiv.style.marginTop = "20px"
        resultDiv.style.border = "1px solid #ccc"
        resultDiv.style.padding = "10px"
        resultDiv.textContent = "Waiting for action..."
        container.appendChild(resultDiv)
        println("[DEBUG_LOG] Result div added")

        fun updateWillUI(will: WillDto) {
            willTextArea.value = will.content ?: ""
            allowedEmailsDiv.textContent = "Доступ разрешен для: ${will.allowedEmails.joinToString(", ")}"
            willDiv.style.display = "block"
            formDiv.style.display = "none"
            verifyDiv.style.display = "none"
            resultDiv.style.display = "none"
        }

        registerButton.addEventListener("click", {
            val email = emailInput.value
            val password = passwordInput.value
            resultDiv.textContent = "Registering..."
            GlobalScope.launch {
                try {
                    val response = apiClient.register(AuthRequest(email, password))
                    resultDiv.textContent = "Register Response: ${response.success} - ${response.message}"
                    if (response.success) {
                        verifyDiv.style.display = "block"
                    }
                } catch (e: Exception) {
                    resultDiv.textContent = "Error: ${e.message}"
                }
            }
        })

        verifyButton.addEventListener("click", {
            val email = emailInput.value
            val code = codeInput.value
            resultDiv.textContent = "Verifying..."
            GlobalScope.launch {
                try {
                    val response = apiClient.verify(VerifyRequest(email, code))
                    resultDiv.textContent = "Verify Response: ${response.success} - ${response.message}"
                    if (response.success) {
                        verifyDiv.style.display = "none"
                    }
                } catch (e: Exception) {
                    resultDiv.textContent = "Error: ${e.message}"
                }
            }
        })

        loginButton.addEventListener("click", {
            val email = emailInput.value
            val password = passwordInput.value
            resultDiv.style.display = "block"
            resultDiv.textContent = "Logging in..."
            GlobalScope.launch {
                try {
                    val response = apiClient.login(AuthRequest(email, password))
                    println("[DEBUG_LOG] Login response: $response")
                    if (response.success) {
                        // Immediately transition to Will screen
                        formDiv.style.display = "none"
                        verifyDiv.style.display = "none"
                        resultDiv.style.display = "none"
                        willDiv.style.display = "block"
                        
                        println("[DEBUG_LOG] Fetching will...")
                        try {
                            val will = apiClient.getWill()
                            if (will != null) {
                                updateWillUI(will)
                            } else {
                                println("[DEBUG_LOG] No will found for user, showing empty editor")
                            }
                        } catch (e: Exception) {
                            println("[ERROR_LOG] Error fetching will after login: ${e.message}")
                            resultDiv.style.display = "block"
                            resultDiv.textContent = "Login successful, but failed to fetch will: ${e.message}"
                        }
                    } else {
                        resultDiv.textContent = "Login Response: ${response.success} - ${response.message}"
                        if (response.message.contains("verify")) {
                            verifyDiv.style.display = "block"
                        }
                    }
                } catch (e: Exception) {
                    println("[ERROR_LOG] Login error: ${e.message}")
                    resultDiv.textContent = "Error: ${e.message}"
                }
            }
        })

        saveWillButton.addEventListener("click", {
            val content = willTextArea.value
            GlobalScope.launch {
                val updatedWill = apiClient.updateWill(UpdateWillRequest(content))
                if (updatedWill != null) {
                    updateWillUI(updatedWill)
                    resultDiv.textContent = "Завещание сохранено"
                }
            }
        })

        addAccessButton.addEventListener("click", {
            val email = accessInput.value
            GlobalScope.launch {
                val updatedWill = apiClient.addAccess(AddAccessRequest(email))
                if (updatedWill != null) {
                    updateWillUI(updatedWill)
                    accessInput.value = ""
                    resultDiv.textContent = "Доступ добавлен"
                }
            }
        })

        println("[DEBUG_LOG] Client JS UI initialized")
    } catch (e: Throwable) {
        println("[ERROR_LOG] Error during Main initialization: ${e.message}")
        val stack = e.asDynamic().stack
        if (stack != null) {
            println("[ERROR_LOG] Stack: $stack")
        }
    }
}
