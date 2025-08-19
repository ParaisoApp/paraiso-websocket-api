package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatLeaders(
    @SerialName(ID) val id: String,
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
data class LeaderResponse(
    val athleteName: String?,
    val leaderStat: Double,
    val teamAbbr: String?
)
