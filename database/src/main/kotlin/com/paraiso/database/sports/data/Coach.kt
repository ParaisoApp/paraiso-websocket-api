package com.paraiso.database.sports.data

import com.paraiso.domain.util.Constants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.paraiso.domain.sport.data.Coach as CoachDomain

@Serializable
data class Coach(
    @SerialName(Constants.ID) val id: String,
    val firstName: String,
    val lastName: String,
    val experience: Int
)
fun CoachDomain.toEntity() =
    Coach(
        id = id,
        firstName = firstName,
        lastName = lastName,
        experience = experience
    )
fun Coach.toDomain() =
    CoachDomain(
        id = id,
        firstName = firstName,
        lastName = lastName,
        experience = experience
    )
