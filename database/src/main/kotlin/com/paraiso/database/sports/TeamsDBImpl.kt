package com.paraiso.database.sports

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.data.Team
import com.paraiso.domain.sport.interfaces.TeamsDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

class TeamsDBImpl(database: MongoDatabase) : TeamsDB {
    private val collection = database.getCollection("teams", Team::class.java)

    override suspend fun findById(id: String) =
        collection.find(eq(ID, id)).limit(1).firstOrNull()

    override suspend fun findBySportAndTeamId(sport: String, teamId: String) =
        collection.find(
            and(
                eq(Team::sport.name, sport),
                eq(Team::teamId.name, teamId)
            )
        ).limit(1).firstOrNull()

    override suspend fun findBySportAndAbbr(sport: String, abbr: String) =
        collection.find(
            and(
                eq(Team::sport.name, sport),
                eq(Team::abbreviation.name, abbr)
            )
        ).limit(1).firstOrNull()

    override suspend fun findBySport(sport: String) =
        collection.find(eq(Team::sport.name, sport)).toList()

    override suspend fun save(teams: List<Team>): Int {
        val bulkOps = teams.map { team ->
            ReplaceOneModel(
                eq(ID, team.id),
                team,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}
