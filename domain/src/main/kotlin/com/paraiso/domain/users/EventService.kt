package com.paraiso.domain.users

interface EventService {
    suspend fun publish(key: String, message: String): Long
    suspend fun addChannels(keys: List<String>)
    suspend fun subscribe(onMessage: suspend (Pair<String, String>) -> Unit)
    fun close()
}
