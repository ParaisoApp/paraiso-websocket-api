package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.sports.data.StatLeaders
import com.paraiso.database.sports.data.toDomain
import com.paraiso.database.sports.data.toEntity
import com.paraiso.domain.sport.data.StatLeaders as StatLeadersDomain
import com.paraiso.domain.sport.interfaces.LeadersDB
import com.paraiso.domain.util.Constants.ID
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class LeadersDBImpl(database: MongoDatabase) : LeadersDB, Klogging {
    private val collection = database.getCollection("leaders", StatLeaders::class.java)
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
                logger.error { "Error finding athletes by ids: $ex" }
                emptyList()
            }
        }
    override suspend fun findBySport(sport: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(eq(ID, sport)).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception){
                logger.error { "Error finding leaders by sport: $ex" }
                null
            }
        }

    override suspend fun findBySportAndSeasonAndType(
        sport: String,
        season: Int,
        type: Int
    ) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(
                    and(
                        eq(StatLeaders::sport.name, sport),
                        eq(StatLeaders::teamId.name, null),
                        eq(StatLeaders::season.name, season),
                        eq(StatLeaders::type.name, type)
                    )
                ).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception){
                logger.error { "Error finding leaders by sport, season, and type: $ex" }
                null
            }
        }

    override suspend fun findBySportAndSeasonAndTypeAndTeam(
        sport: String,
        teamId: String,
        season: Int,
        type: Int
    ) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(
                    and(
                        eq(StatLeaders::sport.name, sport),
                        eq(StatLeaders::teamId.name, teamId),
                        eq(StatLeaders::season.name, season),
                        eq(StatLeaders::type.name, type)
                    )
                ).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception){
                logger.error { "Error finding leaders by sport, teamId, season, and type: $ex" }
                null
            }
        }

    override suspend fun save(statLeaders: List<StatLeadersDomain>) =
        withContext(Dispatchers.IO) {
            val allExisting = findByIdIn(statLeaders.map { it.id }).associateBy { it.id }
            val now = Clock.System.now()
            val bulkOps = statLeaders.map { statLeader ->
                val existing = allExisting[statLeader.id]
                val entity = statLeader.copy(
                    createdOn = existing?.createdOn ?: now,
                    updatedOn = now
                ).toEntity()
                ReplaceOneModel(
                    eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}
