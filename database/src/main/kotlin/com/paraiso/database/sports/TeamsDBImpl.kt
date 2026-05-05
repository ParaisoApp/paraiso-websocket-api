package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.sports.data.Team
import com.paraiso.database.sports.data.toDomain
import com.paraiso.database.sports.data.toEntity
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.Team as TeamDomain
import com.paraiso.domain.sport.interfaces.TeamsDB
import com.paraiso.domain.util.Constants.ID
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class TeamsDBImpl(database: MongoDatabase) : TeamsDB, Klogging {
    private val collection = database.getCollection("teams", Team::class.java)

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
                            `in`(ID, ids)
                        )
                    ).map { it.toDomain() }.toList()
                }
            } catch (ex: Exception){
                logger.error { "Error finding team by id: $ex" }
                emptyList()
            }
        }

    override suspend fun findBySportAndTeamId(sport: String, teamId: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(
                    and(
                        eq(Team::sport.name, sport),
                        eq(Team::teamId.name, teamId)
                    )
                ).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception){
                logger.error { "Error finding team by sport and team id: $ex" }
                null
            }
        }

    override suspend fun findByIds(ids: Set<String>) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(
                    `in`(ID, ids)
                ).map { it.toDomain() }.toList()
            } catch (ex: Exception){
                logger.error { "Error finding teams by ids: $ex" }
                emptyList()
            }
        }

    override suspend fun findBySportAndAbbr(sport: String, abbr: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(
                    and(
                        eq(Team::sport.name, sport),
                        eq(Team::abbreviation.name, abbr)
                    )
                ).limit(1).firstOrNull()?.toDomain()
            } catch (ex: Exception){
                logger.error { "Error finding team by sport and abbr: $ex" }
                null
            }
        }

    override suspend fun findBySport(sport: String) =
        withContext(Dispatchers.IO) {
            try{
                collection.find(eq(Team::sport.name, sport)).map { it.toDomain() }.toList()
            } catch (ex: Exception){
                logger.error { "Error finding teams by sport: $ex" }
                emptyList()
            }
        }

    override suspend fun save(teams: List<TeamDomain>) =
        withContext(Dispatchers.IO) {
            val allExisting = findByIdIn(teams.map { it.id }).associateBy { it.id }
            val now = Clock.System.now()
            val bulkOps = teams.map { team ->
                val existing = allExisting[team.id]
                val entity = team.copy(
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
