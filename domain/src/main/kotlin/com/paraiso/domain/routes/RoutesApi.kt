package com.paraiso.domain.routes

class RoutesApi(private val routesDB: RoutesDB) {
    suspend fun getById(id: String) = routesDB.findById(id)?.toResponse()
    suspend fun getByUserName(userName: String) = routesDB.findByUserName(userName)?.toResponse()

    suspend fun saveRoutes(routeDetails: List<RouteDetails>) = routesDB.save(routeDetails)

    suspend fun toggleFavoriteRoute(favorite: Favorite) {
        if (favorite.userId != null) {
            // toggle favorite from Route
            if (!favorite.favorite) {
                routesDB.removeUserFavorites(favorite.route, favorite.userId)
            } else {
                routesDB.addUserFavorites(favorite.route, favorite.userId)
            }
        }
    }
}
