package com.paraiso.database.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.RoutesDBAdapter
import com.paraiso.domain.users.User
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock

class RoutesDBAdapterImpl(database: MongoDatabase): RoutesDBAdapter {
    private val collection = database.getCollection("routes", RouteDetails::class.java)

    suspend fun findById(id: String) =
        collection.find(Filters.eq(RouteDetails::id.name, id)).firstOrNull()

    suspend fun save(routes: List<RouteDetails>) =
        collection.insertMany(routes)

    suspend fun setUserFavorites(
        route: String,
        userFavoriteId: String
    ) =
        collection.updateOne(
            Filters.eq(RouteDetails::id.name, route),
            combine(
                addToSet(RouteDetails::userFavorites.name, userFavoriteId),
                set(RouteDetails::updatedOn.name, Clock.System.now())
            )
        )
}