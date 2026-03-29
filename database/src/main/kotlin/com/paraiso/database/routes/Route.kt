package com.paraiso.database.routes

import com.paraiso.domain.posts.PostResponse
import com.paraiso.domain.routes.RouteDetails as RouteDetailsDomain
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants.ID
import com.paraiso.domain.util.InstantBsonSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class RouteDetails(
    @SerialName(ID) val id: String,
    val route: SiteRoute,
    val modifier: String?,
    val title: String,
    val userFavorites: Int,
    val about: String?,
    @Serializable(with = InstantBsonSerializer::class)
    val createdOn: Instant?,
    @Serializable(with = InstantBsonSerializer::class)
    val updatedOn: Instant?
)
fun RouteDetailsDomain.toEntity() =
    RouteDetails(
        id = id,
        route = route,
        modifier = modifier,
        title = title,
        userFavorites = userFavorites,
        about = about,
        createdOn = createdOn,
        updatedOn = updatedOn
    )
fun RouteDetails.toDomain() =
    RouteDetailsDomain(
        id = id,
        route = route,
        modifier = modifier,
        title = title,
        userFavorites = userFavorites,
        about = about,
        pinnedPostIds = emptyList(),
        pinnedPosts = emptyMap(),
        createdOn = createdOn,
        updatedOn = updatedOn
    )