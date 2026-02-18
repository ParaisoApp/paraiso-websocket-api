package com.paraiso.server.plugins

import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.RoutesApi
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.util.Constants.FAVORITES_PREFIX
import com.paraiso.domain.util.Constants.HOME_PREFIX
import com.paraiso.domain.util.Constants.SPORT_PREFIX
import com.paraiso.domain.util.ServerConfig.autoBuild
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class ServerHandler(
    private val routesApi: RoutesApi
) {
    suspend fun bootJobs() = coroutineScope {
        launch { buildRoutes(manual = false) }
    }

    suspend fun buildRoutes(manual: Boolean) {
        if (autoBuild || manual) {
            val now = Clock.System.now()
            routesApi.saveRoutes(
                listOf(
                    RouteDetails(
                        id = HOME_PREFIX,
                        route = SiteRoute.HOME,
                        modifier = null,
                        title = "${SiteRoute.HOME}",
                        userFavorites = 0,
                        about = null,
                        createdOn = now,
                        updatedOn = now
                    ),
                    RouteDetails(
                        id = FAVORITES_PREFIX,
                        route = SiteRoute.FAVORITES,
                        modifier = null,
                        title = "${SiteRoute.FAVORITES}",
                        userFavorites = 0,
                        about = null,
                        createdOn = now,
                        updatedOn = now
                    ),
                    RouteDetails(
                        id = "$SPORT_PREFIX/football",
                        route = SiteRoute.FOOTBALL,
                        modifier = null,
                        title = "${SiteRoute.FOOTBALL}",
                        userFavorites = 0,
                        about = null,
                        createdOn = now,
                        updatedOn = now
                    ),
                    RouteDetails(
                        id = "$SPORT_PREFIX/basketball",
                        route = SiteRoute.BASKETBALL,
                        modifier = null,
                        title = "${SiteRoute.BASKETBALL}",
                        userFavorites = 0,
                        about = null,
                        createdOn = now,
                        updatedOn = now
                    ),
                    RouteDetails(
                        id = "$SPORT_PREFIX/hockey",
                        route = SiteRoute.HOCKEY,
                        modifier = null,
                        title = "${SiteRoute.HOCKEY}",
                        userFavorites = 0,
                        about = null,
                        createdOn = now,
                        updatedOn = now
                    )
                )
            )
        }
    }
}
