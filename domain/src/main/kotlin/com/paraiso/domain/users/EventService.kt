package com.paraiso.domain.users

interface EventService {
    fun publish(key: String, message: String)
    suspend fun addChannels(keys: List<String>)
    suspend fun subscribe(onMessage: suspend (Pair<String, String>) -> Unit)
    fun saveUserSession(userSession: UserSession)
    suspend fun getUserSession(userId: String): UserSession?
    fun deleteUserSession(userId: String)
    suspend fun getAllActiveUsers(): List<UserSession>
    fun close()
}
