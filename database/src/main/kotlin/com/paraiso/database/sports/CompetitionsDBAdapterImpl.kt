package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.CompetitionsDBAdapter
import com.paraiso.domain.sport.data.Competition
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull

class CompetitionsDBAdapterImpl(database: MongoDatabase) : CompetitionsDBAdapter {
    private val collection = database.getCollection("competitions", Competition::class.java)

    suspend fun findById(id: String) =
        collection.find(Filters.eq(ID, id)).firstOrNull()

    suspend fun save(competitions: List<Competition>): Int {
        val bulkOps = competitions.map { competition ->
            ReplaceOneModel(
                Filters.eq(ID, competition.id),
                competition,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}
