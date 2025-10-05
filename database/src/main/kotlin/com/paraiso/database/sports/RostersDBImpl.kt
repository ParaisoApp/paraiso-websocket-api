package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.data.RosterEntity
import com.paraiso.domain.sport.interfaces.RostersDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull

class RostersDBImpl(database: MongoDatabase) : RostersDB {
    private val collection = database.getCollection("rosters", RosterEntity::class.java)

    override suspend fun findById(id: String) =
        collection.find(Filters.eq(ID, id)).firstOrNull()

    override suspend fun findBySportAndTeamId(sport: String, teamId: String) =
        collection.find(
            Filters.and(
                Filters.eq(RosterEntity::sport.name, sport),
                Filters.eq(RosterEntity::teamId.name, teamId)
            )
        ).firstOrNull()

    override suspend fun save(rosters: List<RosterEntity>): Int {
        val bulkOps = rosters.map { roster ->
            ReplaceOneModel(
                Filters.eq(ID, roster.id),
                roster,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}
