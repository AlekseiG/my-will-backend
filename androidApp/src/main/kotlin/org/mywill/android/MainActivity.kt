package org.mywill.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.mywill.client.ApiClient
import org.mywill.client.AppController
import org.mywill.client.ui.App

/**
 * Главная Activity Android-приложения.
 * Инициализирует общий [AppController] и запускает Compose UI.
 */
class MainActivity : ComponentActivity() {
    /**
     * Контроллер бизнес-логики.
     * Базовый URL берём из ресурсов (R.string.backend_base_url).
     */
    private val controller by lazy {
        val baseUrl = getString(R.string.backend_base_url)
        AppController(ApiClient(baseUrl))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Обработка токена из Intent (deep link)
        intent?.data?.let { uri ->
            val token = uri.getQueryParameter("token")
            if (token != null) {
                controller.setToken(token)
            }
        }

        setContent {
            App(controller, onGoogleLogin = {
                val baseUrl = getString(R.string.backend_base_url)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$baseUrl/oauth2/authorization/google"))
                startActivity(intent)
            })
        }
    }
}
