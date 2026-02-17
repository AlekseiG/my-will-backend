package org.mywill.client

import kotlinx.coroutines.runBlocking

fun main() {
    val apiClient = ApiClient()
    
    runBlocking {
        println("Calling backend root...")
        val hello = apiClient.getHello()
        println("Response: $hello")
    }
    
    apiClient.close()
}
