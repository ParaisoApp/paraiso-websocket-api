package com.paraiso.database.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.pull
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.RoutesDBAdapter
import com.paraiso.domain.util.Constants
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock

class RoutesDBAdapterImpl(database: MongoDatabase) : RoutesDBAdapter {
    private val collection = database.getCollection("routes", RouteDetails::class.java)

    override suspend fun findById(id: String) =
        collection.find(Filters.eq(ID, id)).firstOrNull()

    override suspend fun save(routes: List<RouteDetails>): Int {
        val bulkOps = routes.map { route ->
            ReplaceOneModel(
                Filters.eq(ID, route.id),
                route,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }

    override suspend fun addUserFavorites(
        route: String,
        userFavoriteId: String
    ) =
        collection.updateOne(
            Filters.eq(ID, route),
            combine(
                addToSet(RouteDetails::userFavorites.name, userFavoriteId),
                set(RouteDetails::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount

    override suspend fun removeUserFavorites(
        route: String,
        userFavoriteId: String
    ) =
        collection.updateOne(
            Filters.eq(ID, route),
            combine(
                pull(RouteDetails::userFavorites.name, userFavoriteId),
                set(RouteDetails::updatedOn.name, Clock.System.now())
            )
        ).modifiedCount
}
