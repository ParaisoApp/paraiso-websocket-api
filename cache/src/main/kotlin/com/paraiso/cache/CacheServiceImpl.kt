package com.paraiso.cache

import com.paraiso.domain.users.CacheService
import com.paraiso.domain.users.UserSession
import io.klogging.Klogging
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScriptOutputType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.future.await
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    override suspend fun saveUserSession(userSession: UserSession): String =
        withContext(Dispatchers.IO) {
            commands.set(
                "user:session:${userSession.userId}",
                Json.encodeToString(userSession)
            ).await()
        }
    override suspend fun getUserSession(userId: String): UserSession? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                commands.get("user:session:$userId")?.await()?.let {
                    Json.decodeFromString<UserSession>(it)
                }
            } catch (e: SerializationException) {
                logger.error { e }
                null
            }
        }

    override suspend fun deleteUserSession(userId: String): Long =
        withContext(Dispatchers.IO) {
            commands.del("user:session:$userId").await()
        }

    override suspend fun getAllActiveUsers(): List<UserSession> {
        val activeSessions = mutableListOf<UserSession>()
        var cursor = KeyScanCursor.INITIAL
        val scanArgs = ScanArgs.Builder.matches("user:session:*")

        do {
            cursor = commands.scan(cursor, scanArgs).await()
            val sessions = cursor.keys.mapNotNull { key ->
                try {
                    Json.decodeFromString<UserSession>(
                        commands.get(key).await()
                    )
                } catch (e: SerializationException) {
                    logger.error { e }
                    null
                }
            }
            activeSessions.addAll(sessions)
        } while (!cursor.isFinished)

        return activeSessions
    }

}