package com.example.messageTypes

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TypeMapping<T>(
    val typeMapping: Map<MessageType, @Contextual T>
)

@Serializable
enum class MessageType() {
    @SerialName("msg")
    MSG,

    @SerialName("vote")
    VOTE,

    @SerialName("delete")
    DELETE,

    @SerialName("guest")
    GUEST,

    @SerialName("userLeave")
    USER_LEAVE,

    @SerialName("userList")
    USER_LIST,

    @SerialName("scoreboard")
    SCOREBOARD,

    @SerialName("boxScores")
    BOX_SCORES,

    @SerialName("basic")
    BASIC,

    @SerialName("ping")
    PING,

    @SerialName("pong")
    PONG
}
