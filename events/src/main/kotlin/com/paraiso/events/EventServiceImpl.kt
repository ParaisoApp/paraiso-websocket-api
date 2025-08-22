package com.paraiso.events

import com.paraiso.domain.users.EventService
import com.paraiso.domain.users.UserSession
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanIterator.scan
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class EventServiceImpl(
    private val serverId: String,
    private val client: RedisClient
): EventService {
    private val pubSubConnection: StatefulRedisPubSubConnection<String, String> = client.connectPubSub()
    private val pubConnection = client.connect()
    private val pub = pubConnection.async()

    override fun publishToServer(targetServerId: String, message: String) {
        pub.publish("server:$targetServerId", message)
    }

    override suspend fun subscribe(onMessage: suspend (String) -> Unit) = coroutineScope {
        val reactive: RedisPubSubReactiveCommands<String, String> = pubSubConnection.reactive()

        reactive.subscribe("server:$serverId").subscribe()

        reactive.observeChannels().subscribe { msg ->
            launch {
                onMessage(msg.message)
            }
        }
    }

    override fun saveUserSession(userSession: UserSession) {
        val sync = pubConnection.sync()
        sync.hset("user:session:${userSession.userId}", mapOf(
            "serverId" to userSession.serverId,
            "sessionId" to userSession.id
        ))
    }
    override fun getUserSession(userId: String): UserSession? {
        val sync = pubConnection.sync()
        val data = sync.hgetall("user:session:$userId")
        val sessionId = data["sessionId"]
        val serverId = data["serverId"]
        return if (
            data.isNotEmpty() &&
            sessionId != null &&
            serverId != null
        ) {
            UserSession(
                id = sessionId,
                userId = userId,
                serverId = serverId
            )
        } else null
    }

    override fun deleteUserSession(userId: String) {
        val sync = pubConnection.sync()
        sync.del("user:session:$userId")
    }

    override fun getAllActiveUsers(): List<UserSession> {
        val activeSessions = mutableListOf<UserSession>()
        val sync = pubConnection.sync()
        var cursor = KeyScanCursor.INITIAL
        val scanArgs = ScanArgs.Builder.matches("user:session:*")

        do {
            val scanResult = sync.scan(cursor, scanArgs)
            cursor = scanResult

            for (key in scanResult.keys) {
                val data = sync.hgetall(key)
                if (data.isNotEmpty()) {
                    val userId = key.removePrefix("user:session:")
                    val serverId = data["serverId"] ?: ""
                    val sessionId = data["sessionId"] ?: ""
                    activeSessions.add(UserSession(userId, serverId, sessionId))
                }
            }
        } while (!cursor.isFinished)

        return activeSessions
    }

    override fun close() {
        pubSubConnection.close()
        pubConnection.close()
        client.shutdown()
    }
}