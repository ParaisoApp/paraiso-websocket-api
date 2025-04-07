package com.paraiso.websocket.api.messageTypes

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TypeMapping<T>(
    val typeMapping: Map<MessageType, @Contextual T>
)

@Serializable
enum class MessageType {
    @SerialName("msg")
    MSG,

    @SerialName("dm")
    DM,

    @SerialName("vote")
    VOTE,

    @SerialName("delete")
    DELETE,

    @SerialName("user")
    USER,

    @SerialName("login")
    LOGIN,

    @SerialName("userLogin")
    USER_LOGIN,

    @SerialName("ban")
    BAN,

    @SerialName("userLeave")
    USER_LEAVE,

    @SerialName("userList")
    USER_LIST,

    @SerialName("scoreboard")
    SCOREBOARD,

    @SerialName("teams")
    TEAMS,

    @SerialName("standings")
    STANDINGS,

    @SerialName("boxScores")
    BOX_SCORES,

    @SerialName("rosters")
    ROSTERS,

    @SerialName("basic")
    BASIC,

    @SerialName("route")
    ROUTE,

    @SerialName("ping")
    PING,

    @SerialName("pong")
    PONG
}
