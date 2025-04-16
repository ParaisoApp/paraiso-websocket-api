package com.paraiso.domain.messageTypes

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TypeMapping<T>(
    val typeMapping: Map<MessageType, @Contextual T>
)

@Serializable
enum class MessageType {
    @SerialName("MSG")
    MSG,

    @SerialName("DM")
    DM,

    @SerialName("VOTE")
    VOTE,

    @SerialName("DELETE")
    DELETE,

    @SerialName("USER")
    USER,

    @SerialName("LOGIN")
    LOGIN,

    @SerialName("USER_LOGIN")
    USER_LOGIN,

    @SerialName("BAN")
    BAN,

    @SerialName("BLOCK")
    BLOCK,

    @SerialName("USER_LEAVE")
    USER_LEAVE,

    @SerialName("SCOREBOARD")
    SCOREBOARD,

    @SerialName("BOX_SCORES")
    BOX_SCORES,

    @SerialName("ROSTERS")
    ROSTERS,

    @SerialName("SCHEDULE")
    SCHEDULE,

    @SerialName("BASIC")
    BASIC,

    @SerialName("ROUTE")
    ROUTE,

    @SerialName("ping")
    PING,

    @SerialName("pong")
    PONG
}
