package com.paraiso.database.sports

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.interfaces.LeadersDB
import com.paraiso.domain.sport.data.StatLeaders
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull

class LeadersDBImpl(database: MongoDatabase) : LeadersDB {
    private val collection = database.getCollection("leaders", StatLeaders::class.java)

    override suspend fun findBySport(sport: String) =
        collection.find(eq(ID, sport)).firstOrNull()

    override suspend fun findBySportAndSeasonAndType(
        sport: String,
        season: Int,
        type: Int
    ) =
        collection.find(
            and(
                eq(StatLeaders::sport.name, sport),
                eq(StatLeaders::teamId.name, null),
                eq(StatLeaders::season.name, season),
                eq(StatLeaders::type.name, type),
            )
        ).firstOrNull()

    override suspend fun findBySportAndSeasonAndTypeAndTeam(
        sport: String,
        teamId: String,
        season: Int,
        type: Int
    ): StatLeaders?=
    collection.find(
        and(
            eq(StatLeaders::sport.name, sport),
            eq(StatLeaders::teamId.name, teamId),
            eq(StatLeaders::season.name, season),
            eq(StatLeaders::type.name, type),
        )
    ).firstOrNull()

    override suspend fun save(statLeaders: List<StatLeaders>): Int {
        val bulkOps = statLeaders.map { statLeader ->
            ReplaceOneModel(
                eq(ID, statLeader.id),
                statLeader,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}
