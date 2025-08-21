package com.paraiso.domain.users

interface UserSessionsDBAdapter{
    suspend fun get(): List<UserSession>
    suspend fun findById(id: String): UserSession?
    suspend fun findByUserId(id: String): UserSession?
    suspend fun findByUserIdsIn(ids: List<String>): List<UserSession>
    suspend fun save(userSessions: List<UserSession>): Int
    suspend fun setConnectedByUserId(
        userId: String,
        status: UserStatus
    ): Long
}
