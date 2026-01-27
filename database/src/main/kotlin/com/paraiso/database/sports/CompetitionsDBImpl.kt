package com.paraiso.database.sports

import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gt
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.InsertOneModel
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts.ascending
import com.mongodb.client.model.Sorts.descending
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.setOnInsert
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.Competition
import com.paraiso.domain.sport.interfaces.CompetitionsDB
import com.paraiso.domain.util.Constants.ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import java.time.ZoneId
import java.util.Date

class CompetitionsDBImpl(database: MongoDatabase) : CompetitionsDB {
    private val collection = database.getCollection("competitions", Competition::class.java)

    override suspend fun findById(id: String) =
        withContext(Dispatchers.IO) {
            collection.find(eq(ID, id)).limit(1).firstOrNull()
        }

    override suspend fun findByIdIn(ids: Set<String>): List<Competition> =
        withContext(Dispatchers.IO) {
            collection.find(Filters.`in`(ID, ids)).toList()
        }

    override suspend fun save(competitions: List<Competition>) =
        withContext(Dispatchers.IO) {
            val bulkOps = competitions.map { competition ->
                ReplaceOneModel(
                    eq(ID, competition.id),
                    competition,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }

    override suspend fun saveIfNew(competitions: List<Competition>) =
        withContext(Dispatchers.IO) {
            val bulkOps = competitions.map { competition ->
                val doc = Document.parse(Json.encodeToString(competition))
                UpdateOneModel<Competition>(
                    eq("_id", competition.id),
                    setOnInsert(doc),
                    UpdateOptions().upsert(true)
                )
            }
            val result = collection.bulkWrite(bulkOps, BulkWriteOptions().ordered(false))
            result.insertedCount
        }

    override suspend fun findScoreboard(
        sport: String,
        year: Int,
        type: Int,
        modifier: String,
        past: Boolean
    ) =
        withContext(Dispatchers.IO) {
            when (sport) {
                SiteRoute.FOOTBALL.name -> {
                    val week = modifier.toIntOrNull()
                        ?.plus(if (past) -1 else 1) ?: 1
                    collection.find(
                        and(
                            eq(Competition::sport.name, sport),
                            eq("${Competition::season.name}.year", year),
                            eq("${Competition::season.name}.type", type),
                            eq(Competition::week.name, week)
                        )
                    ).sort(ascending(Competition::date.name)).toList()
                }

                else -> {
                    getNextDay(
                        sport,
                        year,
                        type,
                        modifier,
                        past
                    )
                }
            }
        }

    override suspend fun findPlayoffsByYear(sport: String, year: Int): List<Competition> =
    withContext(Dispatchers.IO) {
        collection.find(
            and(
                eq(Competition::sport.name, sport),
                eq("${Competition::season.name}.year", year),
                eq("${Competition::season.name}.type", 3),
            )
        ).toList()
    }

    private suspend fun getNextDay(
        sport: String,
        year: Int,
        type: Int,
        modifier: String,
        past: Boolean
    ): List<Competition> =
        withContext(Dispatchers.IO) {
            val estZone = TimeZone.of("America/New_York") // for kotlinx.datetime
            val estJavaZone = ZoneId.of("America/New_York") // for java.time conversions

            // Convert input instant to UTC local date (input is 00:00 so use utc)
            val estLocalDate = Instant.parse(modifier).toLocalDateTime(TimeZone.UTC).date

            // Determine boundary instant depending on past/future suing eastern time zone
            val boundaryInstant = if (past) {
                estLocalDate.atStartOfDayIn(estZone)
            } else {
                estLocalDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(estZone)
            }

            val boundaryDate = Date.from(boundaryInstant.toJavaInstant())

            // Build direction filter
            val direction = if (past) {
                lt(Competition::date.name, boundaryDate)
            } else {
                gt(Competition::date.name, boundaryDate)
            }

            val directionSort = if (past) {
                descending(Competition::date.name)
            } else {
                ascending(Competition::date.name)
            }

            // Find nearest event in that direction
            val nextEvent = collection.find(
                and(
                    eq(Competition::sport.name, sport),
                    eq("${Competition::season.name}.year", year),
                    eq("${Competition::season.name}.type", type),
                    direction
                )
            )
                .sort(directionSort)
                .limit(1)
                .firstOrNull()

            // Compute start/end of that event's day in EST
            return@withContext nextEvent?.date?.toJavaInstant()?.atZone(estJavaZone)?.toLocalDate()?.let { nextEventLocalDate ->
                val startOfDay = nextEventLocalDate.atStartOfDay(estJavaZone).toInstant()
                val endOfDay = nextEventLocalDate.plusDays(1).atStartOfDay(estJavaZone).toInstant()

                // Fetch all events on that day
                collection.find(
                    and(
                        eq(Competition::sport.name, sport),
                        eq("${Competition::season.name}.year", year),
                        eq("${Competition::season.name}.type", type),
                        gte(Competition::date.name, Date.from(startOfDay)),
                        lt(Competition::date.name, Date.from(endOfDay))
                    )
                ).sort(ascending(Competition::date.name)).toList()
            } ?: emptyList()
        }
}
