package com.paraiso.domain.sport.data

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants.ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Roster(
    val id: String,
    val sport: SiteRoute,
    val athletes: List<Athlete>,
    val coach: Coach?,
    val teamId: String
)
