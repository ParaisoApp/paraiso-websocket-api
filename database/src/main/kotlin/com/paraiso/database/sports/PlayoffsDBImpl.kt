package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.data.AllStandings
import com.paraiso.domain.sport.data.Playoff
import com.paraiso.domain.sport.data.ScheduleEntity
import com.paraiso.domain.sport.interfaces.PlayoffsDB
import com.paraiso.domain.sport.interfaces.StandingsDB
import com.paraiso.domain.util.Constants
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class PlayoffsDBImpl(database: MongoDatabase) : PlayoffsDB, Klogging {
    private val collection = database.getCollection("playoffs", Playoff::class.java)

    override suspend fun findById(id: String) =
        withContext(Dispatchers.IO) {
            collection.find(Filters.eq(Constants.ID, id)).limit(1).firstOrNull()
        }

    override suspend fun findBySportAndYear(
        sport: String,
        year: Int,
    ): Playoff? =
        withContext(Dispatchers.IO) {
            collection.find(
                Filters.and(
                    Filters.eq(Playoff::sport.name, sport),
                    Filters.eq(Playoff::year.name, year),
                )
            ).limit(1).firstOrNull()
        }

    override suspend fun save(playoffs: List<Playoff>) =
        withContext(Dispatchers.IO) {
            val bulkOps = playoffs.map { playoff ->
                ReplaceOneModel(
                    Filters.eq(Constants.ID, playoff.id),
                    playoff,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }
}
