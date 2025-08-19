package com.paraiso.domain.routes

class RoutesApi(private val routesDBAdapter: RoutesDBAdapter) {
    suspend fun getById(id: String) = routesDBAdapter.findById(id)?.toResponse()

    suspend fun saveRoutes(routeDetails: List<RouteDetails>) = routesDBAdapter.save(routeDetails)

    suspend fun toggleFavoriteRoute(favorite: Favorite) {
        if (favorite.userId != null) {
            // toggle favorite from Route
            if (!favorite.favorite) {
                routesDBAdapter.removeUserFavorites(favorite.route, favorite.userId)
            } else {
                routesDBAdapter.addUserFavorites(favorite.route, favorite.userId)
            }
        }
    }
}
