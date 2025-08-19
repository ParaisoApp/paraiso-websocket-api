package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.AthletesDBAdapter
import com.paraiso.domain.sport.adapters.CompetitionsDBAdapter
import com.paraiso.domain.sport.data.Athlete
import com.paraiso.domain.sport.data.Competition
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

class AthletesDBAdapterImpl(database: MongoDatabase) : AthletesDBAdapter {
    private val collection = database.getCollection("athletes", Athlete::class.java)

    override suspend fun findByIdsIn(ids: List<String>) =
        collection.find(`in`(ID, ids)).toList()

    override suspend fun save(athletes: List<Athlete>): Int {
        val bulkOps = athletes.map { athlete ->
            ReplaceOneModel(
                Filters.eq(ID, athlete.id),
                athlete,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}
