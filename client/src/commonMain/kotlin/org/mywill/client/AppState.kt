package org.mywill.client

import androidx.compose.runtime.*

/**
 * Общее состояние клиентского приложения, доступное на всех платформах.
 * Хранит данные об авторизации, сообщениях и списках завещаний.
 */
class AppState {
    /**
     * Флаг авторизации. True, если у пользователя есть валидный токен.
     */
    var isAuthorized: Boolean by mutableStateOf(false)
        private set

    /**
     * Текущий JWT токен авторизации.
     */
    var token: String? by mutableStateOf(null)
        private set

    /**
     * Последнее сообщение от системы (текст ошибки или уведомление об успехе).
     */
    var lastMessage: String? by mutableStateOf(null)
        private set

    /**
     * Список завещаний, принадлежащих текущему пользователю.
     */
    var myWills: List<WillDto> by mutableStateOf(emptyList())
        private set

    /**
     * Список завещаний, к которым предоставлен доступ текущему пользователю.
     */
    var sharedWills: List<WillDto> by mutableStateOf(emptyList())
        private set

    /**
     * Устанавливает состояние авторизации и токен.
     */
    fun setAuthorized(token: String?) {
        this.token = token
        this.isAuthorized = !token.isNullOrBlank()
    }

    fun setMessage(message: String?) {
        this.lastMessage = message
    }

    fun updateMyWills(list: List<WillDto>) {
        this.myWills = list
    }

    fun updateSharedWills(list: List<WillDto>) {
        this.sharedWills = list
    }
}
