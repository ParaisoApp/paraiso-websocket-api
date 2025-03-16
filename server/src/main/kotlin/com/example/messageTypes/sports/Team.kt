package com.example.messageTypes.sports

import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val id: String,
    val location: String,
    val name: String,
    val abbreviation: String,
    val displayName: String,
    val shortDisplayName: String
)
