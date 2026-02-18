package org.mywill.client

import androidx.compose.ui.ExperimentalComposeUiApi
import org.jetbrains.skiko.wasm.onWasmReady
import androidx.compose.ui.window.CanvasBasedWindow
import org.mywill.client.ui.App
import kotlinx.browser.window

/**
 * Точка входа в Web-версию приложения (Kotlin/JS).
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        // Инициализируем контроллер с базовым URL по умолчанию
        val controller = AppController(ApiClient())

        // Обработка токена OAuth2 из URL (hash), если он есть
        // Например: http://localhost:8081/#token=abc...
        val hash = window.location.hash
        if (hash.startsWith("#token=")) {
            val token = hash.substringAfter("#token=")
            if (token.isNotEmpty()) {
                controller.setToken(token)
                window.location.hash = "" // Очищаем URL после сохранения токена
            }
        }

        // Запуск Compose в Canvas-элементе с ID "ComposeTarget"
        CanvasBasedWindow("MyWill", canvasElementId = "ComposeTarget") {
            App(controller, onGoogleLogin = {
                // Редирект на бэкенд для начала авторизации Google
                window.location.href = "http://localhost:8080/oauth2/authorization/google"
            })
        }
    }
}
