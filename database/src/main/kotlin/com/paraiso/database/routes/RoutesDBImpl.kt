package com.paraiso.database.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates
import com.mongodb.client.model.Updates.addToSet
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.pull
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.posts.Post
import com.paraiso.domain.routes.RouteDetails
import com.paraiso.domain.routes.RoutesDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.util.Date

class RoutesDBImpl(database: MongoDatabase) : RoutesDB {
    private val collection = database.getCollection("routes", RouteDetails::class.java)

    override suspend fun findById(id: String) =
        collection.find(Filters.eq(ID, id)).firstOrNull()

    //username stored in title for profile routes
    override suspend fun findByUserName(userName: String): RouteDetails? =
        collection.find(Filters.eq(RouteDetails::title.name, userName)).firstOrNull()

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
    override suspend fun setFavorites(
        route: String,
        favorite: Int
    ) =
        collection.updateOne(
            Filters.eq(ID, route),
            combine(
                Updates.inc(RouteDetails::userFavorites.name, favorite),
                set(RouteDetails::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
            )
        ).modifiedCount
}
