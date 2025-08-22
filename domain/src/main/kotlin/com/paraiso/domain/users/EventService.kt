package com.paraiso.domain.users

import kotlinx.coroutines.CoroutineScope
import reactor.core.Disposable

interface EventService {
    fun publish(key: String, message: String)
    suspend fun subscribe(
        key: String,
        onMessage: suspend (String) -> Unit
    ): Disposable
    fun saveUserSession(userSession: UserSession)
    suspend fun getUserSession(userId: String): UserSession?
    fun deleteUserSession(userId: String)
    suspend fun getAllActiveUsers(): List<UserSession>
    fun close()
}