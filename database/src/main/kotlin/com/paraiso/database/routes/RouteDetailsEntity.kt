package com.paraiso.database.routes

import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.SiteRoute
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId


@Serializable
data class RouteDetailsEntity(
    @SerialName("_id") val id: String,
    val route: SiteRoute,
    val modifier: String?,
    val title: String,
    val userFavorites: Set<String>,
    val about: String?,
    val createdOn: Instant?,
    val updatedOn: Instant?
)

fun RouteDetailsEntity.toDomain() =
    RouteDetails(
        id = id,
        route = route,
        modifier = modifier,
        title = title,
        userFavorites = userFavorites,
        about = about,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )

fun RouteDetails.toEntity() =
    RouteDetailsEntity(
        id = id,
        route = route,
        modifier = modifier,
        title = title,
        userFavorites = userFavorites,
        about = about,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
