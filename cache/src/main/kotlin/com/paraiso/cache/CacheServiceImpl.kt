package com.paraiso.cache

import com.paraiso.domain.users.CacheService
import io.klogging.Klogging
import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.future.await

class CacheServiceImpl(private val client: RedisClient): CacheService, Klogging {
    private val connection = client.connect()
    private val commands = connection.async()
    override suspend fun set(key: String, value: String, expirySeconds: Long?): String = withContext(Dispatchers.IO) {
        if (expirySeconds != null) {
            commands.setex(key, expirySeconds, value).await()
        } else {
            commands.set(key, value).await()
        }
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        commands.get(key).await()
    }

    override suspend fun delete(key: String): Long =
        withContext(Dispatchers.IO) {
            commands.del(key).await()
        }

    //get and delete ticket in one transaction
    override suspend fun redeemTicket(ticket: String): String? = withContext(Dispatchers.IO) {
        val luaScript = """
            local val = redis.call('GET', KEYS[1])
            if val then
                redis.call('DEL', KEYS[1])
            end
            return val
        """.trimIndent()

        // We use eval to run the script atomically
        commands.eval<String>(
            luaScript,
            ScriptOutputType.VALUE,
            arrayOf("ws_ticket:$ticket")
        ).await()
    }

}