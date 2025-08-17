package com.paraiso.database.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.pull
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.RoutesDBAdapter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock

class RoutesDBAdapterImpl(database: MongoDatabase) : RoutesDBAdapter {
    private val collection = database.getCollection("routes", RouteDetailsEntity::class.java)

    suspend fun findById(id: String) =
        collection.find(Filters.eq(RouteDetailsEntity::id.name, id)).firstOrNull()

    suspend fun save(routes: List<RouteDetailsEntity>) =
        collection.insertMany(routes)

    suspend fun addUserFavorites(
        route: String,
        userFavoriteId: String
    ) =
        collection.updateOne(
            Filters.eq(RouteDetailsEntity::id.name, route),
            combine(
                addToSet(RouteDetailsEntity::userFavorites.name, userFavoriteId),
                set(RouteDetailsEntity::updatedOn.name, Clock.System.now())
            )
        )

    suspend fun removeUserFavorites(
        route: String,
        userFavoriteId: String
    ) =
        collection.updateOne(
            Filters.eq(RouteDetailsEntity::id.name, route),
            combine(
                pull(RouteDetailsEntity::userFavorites.name, userFavoriteId),
                set(RouteDetailsEntity::updatedOn.name, Clock.System.now())
            )
        )
}
