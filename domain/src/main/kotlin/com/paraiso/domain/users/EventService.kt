package com.paraiso.domain.users

import reactor.core.Disposable

interface EventService {
    fun publishToServer(targetServerId: String, message: String)
    suspend fun subscribe(onMessage: suspend (String) -> Unit): Disposable
    fun saveUserSession(userSession: UserSession)
    suspend fun getUserSession(userId: String): UserSession?
    fun deleteUserSession(userId: String)
    suspend fun getAllActiveUsers(): List<UserSession>
    fun close()
}