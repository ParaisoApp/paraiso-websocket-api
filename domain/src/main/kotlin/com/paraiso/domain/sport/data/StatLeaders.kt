package com.paraiso.domain.sport.data

import kotlinx.serialization.Serializable

@Serializable
data class StatLeaders(
    val categories: List<Category>
)

@Serializable
data class Category(
    val name: String,
    val displayName: String,
    val shortDisplayName: String,
    val leaders: List<CategoryLeader>
)

@Serializable
data class CategoryLeader(
    val athleteId: Int,
    val value: Double,
    val displayValue: String
)

@Serializable
data class LeaderReturn(
    val athleteName: String?,
    val leaderStat: Double,
    val teamAbbr: String?
)
