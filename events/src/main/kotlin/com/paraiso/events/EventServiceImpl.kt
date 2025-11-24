package com.paraiso.events

import com.paraiso.domain.users.EventService
import com.paraiso.domain.users.UserSession
import io.klogging.Klogging
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisFuture
import io.lettuce.core.ScanArgs
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EventServiceImpl(
    private val client: RedisClient
) : EventService, Klogging {
    private val pubSubConnection: StatefulRedisPubSubConnection<String, String> = client.connectPubSub()
    private val pubConnection = client.connect()
    private val pub = pubConnection.async()
    private val reactive: RedisPubSubReactiveCommands<String, String> = pubSubConnection.reactive()

    override suspend fun publish(key: String, message: String): Long = withContext(Dispatchers.IO){
        pub.publish(key, message).get()
    }

    override suspend fun addChannels(keys: List<String>) =
        withContext(Dispatchers.IO) {
            keys.forEach { key ->
                reactive.subscribe(key).awaitFirstOrNull()
            }
        }

    override suspend fun subscribe(
        onMessage: suspend (Pair<String, String>) -> Unit
    ) = coroutineScope {
        reactive.observeChannels().asFlow().collect { msg ->
            launch {
                onMessage(Pair(msg.channel, msg.message))
            }
        }
    }

    override suspend fun saveUserSession(userSession: UserSession): String =
        withContext(Dispatchers.IO) {
            pubConnection.sync().set(
                "user:session:${userSession.userId}",
                Json.encodeToString(userSession)
            )
        }
    override suspend fun getUserSession(userId: String): UserSession? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                pubConnection.sync().get("user:session:$userId")?.let {
                    Json.decodeFromString<UserSession>(it)
                }
            } catch (e: SerializationException) {
                logger.error { e }
                null
            }
        }

    override suspend fun deleteUserSession(userId: String): Long =
        withContext(Dispatchers.IO) {
            pubConnection.sync().del("user:session:$userId")
        }

    override suspend fun getAllActiveUsers(): List<UserSession> {
        val activeSessions = mutableListOf<UserSession>()
        val sync = pubConnection.sync()
        var cursor = KeyScanCursor.INITIAL
        val scanArgs = ScanArgs.Builder.matches("user:session:*")

        do {
            cursor = sync.scan(cursor, scanArgs)
            val sessions = cursor.keys.mapNotNull { key ->
                try {
                    Json.decodeFromString<UserSession>(
                        sync.get(key)
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

    override fun close() {
        pubSubConnection.close()
        pubConnection.close()
        client.shutdown()
    }
}
