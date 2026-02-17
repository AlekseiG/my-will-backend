package org.mywill.client

import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    val apiClient = ApiClient()
    
    val container = document.createElement("div") as HTMLDivElement
    document.body?.appendChild(container)

    val button = document.createElement("button") as HTMLButtonElement
    button.textContent = "Fetch Admin UI Content"
    container.appendChild(button)

    val resultDiv = document.createElement("div") as HTMLDivElement
    resultDiv.style.marginTop = "20px"
    resultDiv.style.border = "1px solid #ccc"
    resultDiv.style.padding = "10px"
    resultDiv.textContent = "Click the button to load data..."
    container.appendChild(resultDiv)

    button.addEventListener("click", {
        resultDiv.textContent = "Loading..."
        GlobalScope.launch {
            try {
                val response = apiClient.getAdminUi()
                resultDiv.textContent = "Response: $response"
            } catch (e: Exception) {
                resultDiv.textContent = "Error: ${e.message}"
            }
        }
    })

    println("Client JS UI initialized")
}
