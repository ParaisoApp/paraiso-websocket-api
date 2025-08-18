package com.paraiso.database.sports

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.sport.adapters.AthletesDBAdapter
import com.paraiso.domain.sport.adapters.CoachesDBAdapter
import com.paraiso.domain.sport.adapters.CompetitionsDBAdapter
import com.paraiso.domain.sport.data.Athlete
import com.paraiso.domain.sport.data.Coach
import com.paraiso.domain.sport.data.Competition
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull

class CoachesDBAdapterImpl(database: MongoDatabase) : CoachesDBAdapter {
    private val collection = database.getCollection("coaches", Coach::class.java)

    suspend fun findById(id: String) =
        collection.find(Filters.eq(ID, id)).firstOrNull()

    suspend fun save(coaches: List<Coach>) =
        collection.insertMany(coaches)
}
