package org.mywill.android

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
        setContent {
            App(controller)
        }
    }
}
