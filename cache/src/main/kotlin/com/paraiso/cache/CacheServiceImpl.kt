package com.paraiso.cache

import com.paraiso.domain.users.CacheService
import io.klogging.Klogging
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScriptOutputType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes
import com.paraiso.domain.users.UserSession as UserSessionDomain

class CacheServiceImpl(client: RedisClient) : CacheService, Klogging {
    private val connection = client.connect()
    private val commands = connection.async()
    // Define your time constants clearly
    private val THIRTY_MINUTES = 30.minutes.inWholeSeconds
    // Only rewrite if the session has lost more than 30 minutes of its life
    private val BUMP_THRESHOLD_SECONDS = 10.minutes.inWholeSeconds
    private val THRESHOLD_LIMIT = THIRTY_MINUTES - BUMP_THRESHOLD_SECONDS

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

    // get and delete ticket in one transaction
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

    override suspend fun saveUserSession(userSession: UserSessionDomain): String =
        withContext(Dispatchers.IO) {
            commands.setex(
                "user:session:${userSession.userId}",
                THIRTY_MINUTES,
                Json.encodeToString(userSession.toEntity())
            ).await()
        }

    override suspend fun bumpUserSession(userId: String): Boolean =
        withContext(Dispatchers.IO) {val key = "user:session:$userId"

            // Check remaining TTL
            val remainingTtl = commands.ttl(key).await() ?: return@withContext false

            // Key doesn't exist (-2) or has no TTL (-1)
            if (remainingTtl < 0) return@withContext false

            // only bump if the remaining time has dipped below the threshold
            if (remainingTtl <= THRESHOLD_LIMIT) {
                commands.expire(key, THIRTY_MINUTES).await()
                return@withContext true
            }

            // Skip the write - the session is still fresh enough
            true
        }
    override suspend fun getUserSession(userId: String): UserSessionDomain? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                commands.get("user:session:$userId")?.await()?.let {
                    Json.decodeFromString<UserSession>(it).toDomain()
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

    override suspend fun getAllActiveUsers(): List<UserSessionDomain> {
        val activeSessions = mutableListOf<UserSessionDomain>()
        var cursor = KeyScanCursor.INITIAL
        val scanArgs = ScanArgs.Builder.matches("user:session:*")

        do {
            cursor = commands.scan(cursor, scanArgs).await()
            val sessions = cursor.keys.mapNotNull { key ->
                try {
                    Json.decodeFromString<UserSession>(
                        commands.get(key).await()
                    ).toDomain()
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
