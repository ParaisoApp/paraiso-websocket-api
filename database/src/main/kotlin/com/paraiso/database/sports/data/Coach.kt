package com.paraiso.database.sports.data

import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Coach as CoachDomain

@Serializable
data class Coach(
    @SerialName(Constants.ID) val id: String,
    val firstName: String,
    val lastName: String,
    val experience: Int,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant?
)
fun CoachDomain.toEntity() =
    Coach(
        id = id,
        firstName = firstName,
        lastName = lastName,
        experience = experience,
        // fields are set in DB layer during save
        createdOn = createdOn,
        updatedOn = updatedOn
    )
fun Coach.toDomain() =
    CoachDomain(
        id = id,
        firstName = firstName,
        lastName = lastName,
        experience = experience,
        createdOn = createdOn,
        updatedOn = updatedOn
    )
