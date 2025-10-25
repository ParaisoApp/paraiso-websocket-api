package com.paraiso.database.sports

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gt
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts.ascending
import com.mongodb.client.model.Sorts.descending
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.ScoreboardEntity
import com.paraiso.domain.sport.interfaces.ScoreboardsDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.util.Date

class ScoreboardsDBImpl(database: MongoDatabase) : ScoreboardsDB {
    private val collection = database.getCollection("scoreboards", ScoreboardEntity::class.java)

    override suspend fun findById(id: String) =
        collection.find(eq(ID, id)).limit(1).firstOrNull()

    override suspend fun findScoreboard(
        sport: String,
        year: Int,
        type: Int,
        modifier: String,
        past: Boolean
    ) = when(sport){
        SiteRoute.FOOTBALL.name -> {
            val week = modifier.toIntOrNull()
                ?.plus(if (past) -1 else 1) ?: 1
            collection.find(
                and(
                    eq(ScoreboardEntity::sport.name, sport),
                    eq("${ScoreboardEntity::season.name}.year", year),
                    eq("${ScoreboardEntity::season.name}.type", type),
                    eq(ScoreboardEntity::week.name, week),
                )
            ).limit(1).firstOrNull()
        }
        SiteRoute.BASKETBALL.name -> {
            val targetDay = Date.from(Instant.parse(modifier).toJavaInstant())
            collection.find(
                and(
                    eq(ScoreboardEntity::sport.name, sport),
                    eq("${ScoreboardEntity::season.name}.year", year),
                    eq("${ScoreboardEntity::season.name}.type", type),
                    if (past)
                        lt(ScoreboardEntity::day.name, targetDay)
                    else
                        gt(ScoreboardEntity::day.name, targetDay)
                )
            ).sort(
                if (past)
                    descending(ScoreboardEntity::day.name)
                else
                    ascending(ScoreboardEntity::day.name)
            ).limit(1).firstOrNull()
        }
        else-> null
    }

    override suspend fun save(scoreboards: List<ScoreboardEntity>): Int {
        val bulkOps = scoreboards.map { scoreboard ->
            ReplaceOneModel(
                eq(ID, scoreboard.id),
                scoreboard,
                ReplaceOptions().upsert(true) // insert if not exists, replace if exists
            )
        }
        return collection.bulkWrite(bulkOps).modifiedCount
    }
}
