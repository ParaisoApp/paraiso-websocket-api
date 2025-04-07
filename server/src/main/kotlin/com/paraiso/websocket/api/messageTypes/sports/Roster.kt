package com.paraiso.websocket.api.messageTypes.sports

import kotlinx.serialization.Serializable

@Serializable
data class Roster(
    val athletes: List<Athlete>,
    val coach: Coach,
    val team: Team
)

@Serializable
data class Coach(
    val id: String,
    val firstName: String,
    val lastName: String,
    val experience: Int
) {companion object}

fun Coach.Companion.createUnknown() = Coach(
    id = "",
    firstName = "",
    lastName = "",
    experience = 0
)