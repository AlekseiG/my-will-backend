package org.mywill.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.mywill.client.ApiClient
import org.mywill.client.AppController
import org.mywill.client.ui.App

class MainActivity : ComponentActivity() {
    private val controller = AppController(ApiClient("http://10.0.2.2:8080"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(controller)
        }
    }
}
