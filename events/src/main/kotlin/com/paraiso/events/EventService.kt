package com.paraiso.events

import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class EventService(
    private val serverId: String,
    private val client: RedisClient
) {
    private val pubSubConnection: StatefulRedisPubSubConnection<String, String> = client.connectPubSub()
    private val pubConnection = client.connect()
    private val pub = pubConnection.async()

    fun publish(targetServerId: String, message: String) {
        pub.publish("server:$targetServerId", message)
    }

    suspend fun subscribe(onMessage: suspend (String) -> Unit) = coroutineScope {
        val reactive: RedisPubSubReactiveCommands<String, String> = pubSubConnection.reactive()

        reactive.subscribe("server:$serverId").subscribe()

        reactive.observeChannels().subscribe { msg ->
            launch {
                onMessage(msg.message)
            }
        }
    }

    fun close() {
        pubSubConnection.close()
        pubConnection.close()
        client.shutdown()
    }
}