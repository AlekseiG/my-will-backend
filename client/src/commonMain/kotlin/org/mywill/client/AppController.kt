package org.mywill.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Общий контроллер, инкапсулирующий работу с ApiClient и ведение состояния.
 * Используется и Web, и Android слоями UI.
 */
class AppController(
    private val api: ApiClient,
    val state: AppState = AppState(),
) {
    /**
     * Позволяет задать токен (например, из OAuth редиректа в Web или сохранённый в Android).
     */
    fun setToken(token: String) {
        api.setToken(token)
        state.setAuthorized(token)
    }

    suspend fun getHello(): String = safeCall {
        api.getHello().also { msg ->
            state.setMessage(msg)
        }
    }

    suspend fun register(email: String, password: String): AuthResponse = safeCall {
        val resp = api.register(AuthRequest(email, password))
        state.setMessage(resp.message)
        resp
    }

    suspend fun login(email: String, password: String): AuthResponse = safeCall {
        val resp = api.login(AuthRequest(email, password))
        if (resp.success && resp.token != null) {
            state.setAuthorized(resp.token)
        }
        state.setMessage(resp.message)
        resp
    }

    suspend fun verify(email: String, code: String): AuthResponse = safeCall {
        val resp = api.verify(VerifyRequest(email, code))
        state.setMessage(resp.message)
        resp
    }

    suspend fun loadMyWills(): List<WillDto> = safeCall {
        val list = api.getMyWills()
        state.setMyWills(list)
        list
    }

    suspend fun loadSharedWills(): List<WillDto> = safeCall {
        val list = api.getSharedWills()
        state.setSharedWills(list)
        list
    }

    suspend fun getWill(id: Long): WillDto? = safeNullableCall {
        api.getWill(id)
    }

    suspend fun createWill(title: String, content: String): WillDto? = safeNullableCall {
        api.createWill(CreateWillRequest(title, content))
    }

    suspend fun updateWill(id: Long, title: String, content: String): WillDto? = safeNullableCall {
        api.updateWill(id, UpdateWillRequest(title, content))
    }

    suspend fun addAccess(id: Long, email: String): WillDto? = safeNullableCall {
        api.addAccess(id, AddAccessRequest(email))
    }

    private suspend fun <T> safeCall(block: suspend () -> T): T =
        withContext(Dispatchers.Default) { // нейтральный диспетчер для общей логики
            try {
                block()
            } catch (e: Throwable) {
                state.setMessage("Error: ${e.message}")
                throw e
            }
        }

    private suspend fun <T> safeNullableCall(block: suspend () -> T?): T? =
        withContext(Dispatchers.Default) {
            try {
                block()
            } catch (e: Throwable) {
                state.setMessage("Error: ${e.message}")
                null
            }
        }
}
