package com.paraiso.domain.routes

import com.paraiso.domain.util.ServerState
import kotlinx.datetime.Instant

class RoutesApi {
    fun getById(id: String) = ServerState.routes[id]?.toReturn()
    fun toggleFavoriteRoute(favorite: Favorite, now: Instant) {
        if(favorite.userId != null){
            // toggle favorite from Route
            ServerState.routes[favorite.route]?.let { routeDetails ->
                routeDetails.userFavorites.toMutableSet().let { mutableFavorites ->
                    if (!favorite.favorite) {
                        mutableFavorites.remove(favorite.userId)
                    } else {
                        mutableFavorites.add(favorite.userId)
                    }
                    ServerState.routes[favorite.route] = routeDetails.copy(
                        userFavorites = mutableFavorites,
                        updatedOn = now
                    )
                }
            }
        }
    }
}
