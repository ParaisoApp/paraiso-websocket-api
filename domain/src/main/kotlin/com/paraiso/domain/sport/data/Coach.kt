package com.paraiso.domain.sport.data

import com.paraiso.domain.util.Constants
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Coach(
    @SerialName(Constants.ID) val id: String,
    val firstName: String,
    val lastName: String,
    val experience: Int,
    val createdOn: Instant?,
    val updatedOn: Instant?
)
