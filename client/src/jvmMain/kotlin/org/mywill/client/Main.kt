package org.mywill.client

import kotlinx.coroutines.runBlocking

fun main() {
    val apiClient = ApiClient()
    
    runBlocking {
        println("Calling backend root...")
        val hello = apiClient.getHello()
        println("Response: $hello")
        
        println("\nCalling admin ui...")
        val adminUi = apiClient.getAdminUi()
        println("Response: $adminUi")
    }
    
    apiClient.close()
}
