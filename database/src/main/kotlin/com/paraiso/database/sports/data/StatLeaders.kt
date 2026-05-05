package com.paraiso.database.sports.data

import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.StatLeaders as StatLeadersDomain
import com.paraiso.domain.sport.data.Category as CategoryDomain
import com.paraiso.domain.sport.data.CategoryLeader as CategoryLeaderDomain

@Serializable
data class StatLeaders(
    @SerialName(ID) val id: String,
    val sport: String,
    val season: Int,
    val type: Int,
    val teamId: String? = null,
    val categories: List<Category>,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant?
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

fun StatLeadersDomain.toEntity() = StatLeaders(
    id = id,
    sport = sport,
    season = season,
    type = type,
    teamId = teamId,
    categories = categories.map { it.toEntity() },
    // fields are set in DB layer during save
    createdOn = createdOn,
    updatedOn = updatedOn
)

fun CategoryDomain.toEntity() = Category(
    name = name,
    displayName = displayName,
    shortDisplayName = shortDisplayName,
    leaders = leaders.map { it.toEntity() },
)

fun CategoryLeaderDomain.toEntity() = CategoryLeader(
    athleteId = athleteId,
    value = value,
    displayValue = displayValue
)

fun StatLeaders.toDomain() = StatLeadersDomain(
    id = id,
    sport = sport,
    season = season,
    type = type,
    teamId = teamId,
    categories = categories.map { it.toDomain() },
    createdOn = createdOn,
    updatedOn = updatedOn
)

fun Category.toDomain() = CategoryDomain(
    name = name,
    displayName = displayName,
    shortDisplayName = shortDisplayName,
    leaders = leaders.map { it.toDomain() },
)

fun CategoryLeader.toDomain() = CategoryLeaderDomain(
    athleteId = athleteId,
    value = value,
    displayValue = displayValue
)
