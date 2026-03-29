package com.paraiso.events

import com.paraiso.domain.users.EventService
import com.paraiso.domain.users.UserSession
import io.klogging.Klogging
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.RedisClient
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

    override suspend fun publish(key: String, message: String): Long = withContext(Dispatchers.IO) {
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

    override fun close() {
        pubSubConnection.close()
        pubConnection.close()
        client.shutdown()
    }
}
