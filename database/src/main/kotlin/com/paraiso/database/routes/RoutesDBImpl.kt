package com.paraiso.database.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.sports.data.toDomain
import com.paraiso.database.sports.data.toEntity
import com.paraiso.domain.routes.RoutesDB
import com.paraiso.domain.util.Constants.ID
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.util.Date
import com.paraiso.domain.routes.RouteDetails as RouteDetailsDomain

class RoutesDBImpl(database: MongoDatabase) : RoutesDB, Klogging {
    private val collection = database.getCollection("routes", RouteDetails::class.java)

    override suspend fun findByIdIn(ids: List<String>) =
        withContext(Dispatchers.IO) {
            try {
                if (ids.size == 1) {
                    collection.find(
                        Filters.and(
                            Filters.eq(ID, ids.firstOrNull())
                        )
                    ).map { it.toDomain() }.toList()
                } else {
                    collection.find(
                        Filters.and(
                            Filters.`in`(ID, ids)
                        )
                    ).map { it.toDomain() }.toList()
                }
            } catch (ex: Exception) {
                logger.error { "Error finding athletes by ids: $ex" }
                emptyList()
            }
        }

    override suspend fun save(routes: List<RouteDetailsDomain>) =
        withContext(Dispatchers.IO) {
            val allExisting = findByIdIn(routes.map { it.id }).associateBy { it.id }
            val now = Clock.System.now()
            val bulkOps = routes.map { route ->
                val existing = allExisting[route.id]
                val entity = route.copy(
                    userFavorites = existing?.userFavorites ?: route.userFavorites,
                    pinnedPostIds = existing?.pinnedPostIds ?: route.pinnedPostIds,
                    pinnedPosts = existing?.pinnedPosts ?: route.pinnedPosts,
                    createdOn = existing?.createdOn ?: now,
                    updatedOn = now
                ).toEntity()
                ReplaceOneModel(
                    Filters.eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext if (bulkOps.isNotEmpty()) collection.bulkWrite(bulkOps).modifiedCount else 0
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
