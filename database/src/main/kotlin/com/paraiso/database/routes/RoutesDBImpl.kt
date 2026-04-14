package com.paraiso.database.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.routes.RouteDetails as RouteDetailsDomain
import com.paraiso.domain.routes.RoutesDB
import com.paraiso.domain.util.Constants.ID
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.util.Date

class RoutesDBImpl(database: MongoDatabase) : RoutesDB, Klogging {
    private val collection = database.getCollection("routes", RouteDetails::class.java)

    override suspend fun findById(id: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(Filters.eq(ID, id)).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception){
                logger.error { "Error finding route by id: $ex" }
                null
            }
        }

    override suspend fun save(routes: List<RouteDetailsDomain>) =
        withContext(Dispatchers.IO) {
            val bulkOps = routes.map { route ->
                val entity = route.toEntity()
                ReplaceOneModel(
                    Filters.eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
    override suspend fun setTitle(
        id: String,
        title: String
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                Filters.eq(ID, id),
                combine(
                    set(RouteDetails::title.name, title),
                    set(RouteDetails::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }
    override suspend fun setFavorites(
        id: String,
        favorite: Int
    ) =
        withContext(Dispatchers.IO) {
            collection.updateOne(
                Filters.eq(ID, id),
                combine(
                    Updates.inc(RouteDetails::userFavorites.name, favorite),
                    set(RouteDetails::updatedOn.name, Date.from(Clock.System.now().toJavaInstant()))
                )
            ).modifiedCount
        }
}
