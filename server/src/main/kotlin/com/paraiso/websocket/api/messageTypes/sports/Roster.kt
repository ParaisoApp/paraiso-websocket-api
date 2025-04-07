package com.paraiso.websocket.api.messageTypes.sports

data class Roster(
    val athletes: List<Athlete>,
    val coach: Coach,
    val team: Team
)
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