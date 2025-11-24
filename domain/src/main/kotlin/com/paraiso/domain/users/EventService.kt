package com.paraiso.domain.users

interface EventService {
    suspend fun publish(key: String, message: String): Long
    suspend fun addChannels(keys: List<String>)
    suspend fun subscribe(onMessage: suspend (Pair<String, String>) -> Unit)
    suspend fun saveUserSession(userSession: UserSession): String
    suspend fun getUserSession(userId: String): UserSession?
    suspend fun deleteUserSession(userId: String): Long
    suspend fun getAllActiveUsers(): List<UserSession>
    fun close()
}
