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

    /**
     * Выполняет запрос к "приветственному" эндпоинту.
     * @return Ответ от сервера.
     */
    suspend fun getHello(): String = safeCall {
        api.getHello().also { msg ->
            state.setMessage(msg)
        }
    }

    /**
     * Регистрирует нового пользователя.
     * @param email Email пользователя.
     * @param password Пароль.
     */
    suspend fun register(email: String, password: String): AuthResponse = safeCall {
        val resp = api.register(AuthRequest(email, password))
        state.setMessage(resp.message)
        resp
    }

    /**
     * Выполняет вход в систему. При успехе обновляет состояние авторизации.
     */
    suspend fun login(email: String, password: String): AuthResponse = safeCall {
        val resp = api.login(AuthRequest(email, password))
        if (resp.success && resp.token != null) {
            state.setAuthorized(resp.token)
        }
        state.setMessage(resp.message)
        resp
    }

    /**
     * Верифицирует аккаунт с помощью кода.
     */
    suspend fun verify(email: String, code: String): AuthResponse = safeCall {
        val resp = api.verify(VerifyRequest(email, code))
        state.setMessage(resp.message)
        resp
    }

    /**
     * Загружает список собственных завещаний и обновляет [AppState].
     */
    suspend fun loadMyWills(): List<WillDto> = safeCall {
        val list = api.getMyWills()
        state.updateMyWills(list)
        list
    }

    /**
     * Загружает список чужих завещаний, к которым есть доступ, и обновляет [AppState].
     */
    suspend fun loadSharedWills(): List<WillDto> = safeCall {
        val list = api.getSharedWills()
        state.updateSharedWills(list)
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

    suspend fun loadProfile(): ProfileDto? = safeNullableCall {
        api.getProfile()
    }

    suspend fun updateProfile(avatarUrl: String?, deathTimeoutSeconds: Long?): ProfileDto? = safeNullableCall {
        api.updateProfile(UpdateProfileRequest(avatarUrl, deathTimeoutSeconds))
    }

    suspend fun changePassword(old: String, new: String): Boolean = safeCall {
        api.changePassword(ChangePasswordRequest(old, new))
    }

    suspend fun deleteAccount(): Boolean = safeCall {
        api.deleteAccount()
    }

    suspend fun cancelDeath(): Boolean = safeCall {
        api.cancelDeath()
    }

    suspend fun loadMyTrustedPeople(): List<TrustedPersonDto> = safeCall {
        api.getMyTrustedPeople()
    }

    suspend fun addTrustedPerson(email: String): TrustedPersonDto? = safeNullableCall {
        api.addTrustedPerson(AddTrustedPersonRequest(email))
    }

    suspend fun removeTrustedPerson(id: Long): Boolean = safeCall {
        api.removeTrustedPerson(id)
    }

    suspend fun confirmDeath(ownerEmail: String): Boolean = safeCall {
        api.confirmDeath(DeathConfirmationRequest(ownerEmail))
    }

    suspend fun loadWhoseTrustedIAm(): List<String> = safeCall {
        api.getWhoseTrustedIAm()
    }

    /**
     * Безопасная обёртка для сетевых вызовов.
     * Переключает контекст на Dispatchers.Default и ловит исключения, записывая их в состояние.
     */
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
