package org.mywill.client

/**
 * Общее состояние клиентского приложения, доступное на всех платформах.
 */
class AppState {
    var isAuthorized: Boolean = false
        private set

    var token: String? = null
        private set

    var lastMessage: String? = null
        private set

    var myWills: List<WillDto> = emptyList()
        private set

    var sharedWills: List<WillDto> = emptyList()
        private set

    fun setAuthorized(token: String?) {
        this.token = token
        this.isAuthorized = !token.isNullOrBlank()
    }

    fun setMessage(message: String?) {
        this.lastMessage = message
    }

    fun setMyWills(list: List<WillDto>) {
        this.myWills = list
    }

    fun setSharedWills(list: List<WillDto>) {
        this.sharedWills = list
    }
}
