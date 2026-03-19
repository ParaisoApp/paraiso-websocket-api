package com.paraiso.domain.users

interface CacheService {
    suspend fun set(key: String, value: String, expirySeconds: Long? = null): String
    suspend fun get(key: String): String?
    suspend fun delete(key: String): Long
    suspend fun redeemTicket(ticket: String): String?
}