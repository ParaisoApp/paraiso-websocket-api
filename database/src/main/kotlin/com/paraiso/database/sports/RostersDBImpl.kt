package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.data.RosterEntity
import com.paraiso.domain.sport.interfaces.RostersDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class RostersDBImpl(database: MongoDatabase) : RostersDB {
    private val collection = database.getCollection("rosters", RosterEntity::class.java)

    override suspend fun findById(id: String) =
        withContext(Dispatchers.IO) {
            collection.find(Filters.eq(ID, id)).limit(1).firstOrNull()
        }

    override suspend fun findBySportAndTeamId(sport: String, teamId: String) =
        withContext(Dispatchers.IO) {
            collection.find(
                Filters.and(
                    Filters.eq(RosterEntity::sport.name, sport),
                    Filters.eq(RosterEntity::teamId.name, teamId)
                )
            ).limit(1).firstOrNull()
        }

    override suspend fun save(rosters: List<RosterEntity>) =
        withContext(Dispatchers.IO) {
            val bulkOps = rosters.map { roster ->
                ReplaceOneModel(
                    Filters.eq(ID, roster.id),
                    roster,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}
