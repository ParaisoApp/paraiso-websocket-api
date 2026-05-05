package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.sports.data.Roster
import com.paraiso.database.sports.data.toDomain
import com.paraiso.database.sports.data.toEntity
import com.paraiso.domain.sport.data.Roster as RosterDomain
import com.paraiso.domain.sport.interfaces.RostersDB
import com.paraiso.domain.util.Constants.ID
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class RostersDBImpl(database: MongoDatabase) : RostersDB, Klogging {
    private val collection = database.getCollection("rosters", Roster::class.java)

    override suspend fun findByIdIn(ids: List<String>) =
        withContext(Dispatchers.IO) {
            try{
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
            } catch (ex: Exception){
                logger.error { "Error finding roster by id: $ex" }
                emptyList()
            }
        }

    override suspend fun findBySportAndTeamId(sport: String, teamId: String) =
        withContext(Dispatchers.IO) {
            try{
                val roster = collection.find(
                    Filters.and(
                        Filters.eq(Roster::sport.name, sport),
                        Filters.eq(Roster::teamId.name, teamId)
                    )
                ).limit(1).firstOrNull()
                Triple(roster?.toDomain(), roster?.athletes ?: emptyList(), roster?.coach)
            } catch (ex: Exception){
                logger.error { "Error finding roster by sport and team id: $ex" }
                Triple(null, emptyList(), null)
            }
        }

    override suspend fun save(rosters: List<RosterDomain>) =
        withContext(Dispatchers.IO) {
            val bulkOps = rosters.map { roster ->
                val entity = roster.toEntity()
                ReplaceOneModel(
                    Filters.eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}
