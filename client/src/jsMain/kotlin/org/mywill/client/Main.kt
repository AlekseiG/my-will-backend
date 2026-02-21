package org.mywill.client

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.skiko.wasm.onWasmReady
import org.mywill.client.ui.App
import org.w3c.dom.HTMLCanvasElement

/**
 * Точка входа в Web-версию приложения (Kotlin/JS).
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        // Определяем адрес бэкенда динамически по текущему хосту (порт 8080)
        val backendOrigin = "${window.location.protocol}//${window.location.hostname}:8080"
        val controller = AppController(ApiClient(backendOrigin))

        // Обработка токена OAuth2 из URL (hash), если он есть
        // Например: http://<host>:8081/#token=abc...
        val hash = window.location.hash
        if (hash.startsWith("#token=")) {
            val token = hash.substringAfter("#token=")
            if (token.isNotEmpty()) {
                controller.setToken(token)
                window.location.hash = "" // Очищаем URL после сохранения токена
            }
        }

        // Запуск Compose в Canvas-элементе с ID "ComposeTarget"
        val canvas = document.getElementById("ComposeTarget") as HTMLCanvasElement
        ComposeViewport(canvas) {
            App(controller, onGoogleLogin = {
                // Редирект на бэкенд для начала авторизации Google
                window.location.href = "$backendOrigin/oauth2/authorization/google"
            })
        }
    }
}
