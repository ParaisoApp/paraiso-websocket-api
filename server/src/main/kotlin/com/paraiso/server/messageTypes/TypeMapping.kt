package com.paraiso.server.messageTypes

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

    @SerialName("USER_LEAVE")
    USER_LEAVE,

    @SerialName("USER_LIST")
    USER_LIST,

    @SerialName("SCOREBOARD")
    SCOREBOARD,

    @SerialName("TEAMS")
    TEAMS,

    @SerialName("STANDINGS")
    STANDINGS,

    @SerialName("LEADERS")
    LEADERS,

    @SerialName("BOX_SCORES")
    BOX_SCORES,

    @SerialName("ROSTERS")
    ROSTERS,

    @SerialName("BASIC")
    BASIC,

    @SerialName("ROUTE")
    ROUTE,

    @SerialName("ping")
    PING,

    @SerialName("pong")
    PONG
}
