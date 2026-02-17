package org.mywill.client

import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    val apiClient = ApiClient()
    
    val container = document.createElement("div") as HTMLDivElement
    document.body?.appendChild(container)

    // Form
    val formDiv = document.createElement("div") as HTMLDivElement
    container.appendChild(formDiv)

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

    val resultDiv = document.createElement("div") as HTMLDivElement
    resultDiv.style.marginTop = "20px"
    resultDiv.style.border = "1px solid #ccc"
    resultDiv.style.padding = "10px"
    resultDiv.textContent = "Waiting for action..."
    container.appendChild(resultDiv)

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
        resultDiv.textContent = "Logging in..."
        GlobalScope.launch {
            try {
                val response = apiClient.login(AuthRequest(email, password))
                resultDiv.textContent = "Login Response: ${response.success} - ${response.message}"
                if (!response.success && response.message.contains("verify")) {
                    verifyDiv.style.display = "block"
                }
            } catch (e: Exception) {
                resultDiv.textContent = "Error: ${e.message}"
            }
        }
    })

    println("Client JS UI initialized")
}
