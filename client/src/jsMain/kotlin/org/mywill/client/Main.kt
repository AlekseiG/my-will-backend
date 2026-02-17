package org.mywill.client

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun main() {
    val apiClient = ApiClient()
    
    println("Client JS started")
    
    GlobalScope.launch {
        println("Calling backend from JS...")
        val hello = apiClient.getHello()
        println("JS Response: $hello")
    }
}
