package com.paraiso.com.paraiso.server.plugins

import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.users.UserStatus
import com.paraiso.domain.util.ServerState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ServerHandler {
    suspend fun bootJobs() = coroutineScope {
        launch { cleanUserList() }
        launch { buildRoutes() }
    }
    private suspend fun cleanUserList() = coroutineScope {
        while (isActive) {
            ServerState.userList.entries
                .removeIf {
                    it.value.lastSeen != 0L && // clear user from user list if disconnected for more than 10 min
                        it.value.lastSeen < System.currentTimeMillis() - (10 * 60 * 1000) &&
                        it.value.status == UserStatus.DISCONNECTED
                }
            delay(10 * 60 * 1000) // delay for ten minutes
        }
    }

    private suspend fun buildRoutes() = coroutineScope {
        ServerState.routes["${SiteRoute.HOME}"] = RouteDetails(
            id = "${SiteRoute.HOME}",
            route = SiteRoute.HOME,
            modifier = null,
            title = "${SiteRoute.HOME}",
            userFavorites = emptySet(),
            about = null,
        )
        ServerState.routes["${SiteRoute.FOOTBALL}"] = RouteDetails(
            id = "${SiteRoute.FOOTBALL}",
            route = SiteRoute.FOOTBALL,
            modifier = null,
            title = "${SiteRoute.FOOTBALL}",
            userFavorites = emptySet(),
            about = null,
        )
        ServerState.routes["${SiteRoute.BASKETBALL}"] = RouteDetails(
            id = "${SiteRoute.BASKETBALL}",
            route = SiteRoute.BASKETBALL,
            modifier = null,
            title = "${SiteRoute.BASKETBALL}",
            userFavorites = emptySet(),
            about = null,
        )
    }
}
