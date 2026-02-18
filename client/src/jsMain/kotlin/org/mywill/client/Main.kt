package org.mywill.client

import androidx.compose.ui.ExperimentalComposeUiApi
import org.jetbrains.skiko.wasm.onWasmReady
import androidx.compose.ui.window.CanvasBasedWindow
import org.mywill.client.ui.App
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        val controller = AppController(ApiClient())

        // Handle OAuth2 token from hash if present
        val hash = window.location.hash
        if (hash.startsWith("#token=")) {
            val token = hash.substringAfter("#token=")
            if (token.isNotEmpty()) {
                controller.setToken(token)
                window.location.hash = ""
            }
        }

        CanvasBasedWindow("MyWill", canvasElementId = "ComposeTarget") {
            App(controller, onGoogleLogin = {
                window.location.href = "http://localhost:8080/oauth2/authorization/google"
            })
        }
    }
}
