package com.paraiso.com.paraiso.server.plugins

import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.RoutesApi
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.users.UserStatus
import com.paraiso.domain.util.ServerConfig.autoBuild
import com.paraiso.domain.util.ServerState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class ServerHandler(
    private val routesApi: RoutesApi
) {
    suspend fun bootJobs() = coroutineScope {
        //TODO launch { cleanUserList() }
        launch { if (autoBuild) buildRoutes() }
    }
//    private suspend fun cleanUserList() = coroutineScope {
//        while (isActive) {
//            ServerState.userList.entries
//                .removeIf {
//                    it.value.lastSeen != 0L && // clear user from user list if disconnected for more than 10 min
//                        it.value.lastSeen < System.currentTimeMillis() - (10 * 60 * 1000) &&
//                        it.value.status == UserStatus.DISCONNECTED
//                }
//            delay(10 * 60 * 1000) // delay for ten minutes
//        }
//    }

    private suspend fun buildRoutes() {
        val now = Clock.System.now()
        routesApi.saveRoutes(
            listOf(
                RouteDetails(
                    id = "/",
                    route = SiteRoute.HOME,
                    modifier = null,
                    title = "${SiteRoute.HOME}",
                    userFavorites = emptySet(),
                    about = null,
                    createdOn = now,
                    updatedOn = now
                ),
                RouteDetails(
                    id = "/s/football",
                    route = SiteRoute.FOOTBALL,
                    modifier = null,
                    title = "${SiteRoute.FOOTBALL}",
                    userFavorites = emptySet(),
                    about = null,
                    createdOn = now,
                    updatedOn = now
                ),
                RouteDetails(
                    id = "/s/basketball",
                    route = SiteRoute.BASKETBALL,
                    modifier = null,
                    title = "${SiteRoute.BASKETBALL}",
                    userFavorites = emptySet(),
                    about = null,
                    createdOn = now,
                    updatedOn = now
                )
            )
        )
    }
}
