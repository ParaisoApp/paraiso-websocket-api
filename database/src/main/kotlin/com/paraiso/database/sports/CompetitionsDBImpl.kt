package com.paraiso.database.sports

import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gt
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts.ascending
import com.mongodb.client.model.Sorts.descending
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.setOnInsert
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.database.sports.data.Competition
import com.paraiso.database.sports.data.toDomain
import com.paraiso.database.sports.data.toEntity
import com.paraiso.database.userchats.toDomain
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.Competition as CompetitionDomain
import com.paraiso.domain.sport.interfaces.CompetitionsDB
import com.paraiso.domain.util.Constants.ID
import io.klogging.Klogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
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
import org.bson.conversions.Bson
import java.time.ZoneId
import java.util.Date

class CompetitionsDBImpl(database: MongoDatabase) : CompetitionsDB, Klogging {
    private val collection = database.getCollection("competitions", Competition::class.java)

    override suspend fun findByIdIn(ids: Set<String>) =
        withContext(Dispatchers.IO) {
            try{
                if (ids.size == 1) {
                    collection.find(
                        and(
                            eq(ID, ids.firstOrNull())
                        )
                    ).map { it.toDomain() }.toList()
                } else {
                    collection.find(
                        and(
                            Filters.`in`(ID, ids)
                        )
                    ).map { it.toDomain() }.toList()
                }
            } catch (ex: Exception){
                logger.error { "Error finding dms by ids: $ex" }
                emptyList()
            }
        }

    override suspend fun save(competitions: List<CompetitionDomain>) =
        withContext(Dispatchers.IO) {
            val bulkOps = competitions.map { competition ->
                val entity = competition.toEntity()
                ReplaceOneModel(
                    eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
        }

    override suspend fun saveWithExisting(competitions: List<CompetitionDomain>) =
        withContext(Dispatchers.IO) {
            val existingComps = collection.find(
                `in`(ID, competitions.map { it.id })
            ).toList().associateBy { it.id }
            val bulkOps = competitions.map { competition ->
                val entity = competition.copy(
                    status = competition.status.copy(
                        completedTime = existingComps[competition.id]?.status?.completedTime
                    )
                ).toEntity()
                ReplaceOneModel(
                    eq(ID, entity.id),
                    entity,
                    ReplaceOptions().upsert(true) // insert if not exists, replace if exists
                )
            }
            return@withContext collection.bulkWrite(bulkOps).modifiedCount
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
                    ).sort(ascending(Competition::date.name)).map { it.toDomain() }.toList()
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

    override suspend fun findPlayoffsByYear(sport: String, year: Int): List<CompetitionDomain> =
        withContext(Dispatchers.IO) {
            collection.find(
                and(
                    eq(Competition::sport.name, sport),
                    eq("${Competition::season.name}.year", year),
                    or(
                        eq("${Competition::season.name}.type", 3),
                        eq("${Competition::season.name}.type", 5)
                    )
                )
            ).map { it.toDomain() }.toList()
        }

    private suspend fun getNextDay(
        sport: String,
        year: Int,
        type: Int,
        modifier: String,
        past: Boolean
    ): List<CompetitionDomain> =
        withContext(Dispatchers.IO) {
            val estZone = TimeZone.of("America/New_York") // for kotlinx.datetime
            val estJavaZone = ZoneId.of("America/New_York") // for java.time conversions

            // Convert input instant to UTC local date (input is 00:00 so use utc)
            val estLocalDate = Instant.parse(modifier).toLocalDateTime(TimeZone.UTC).date

            // Determine boundary instant depending on past/future using eastern time zone
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
                ).sort(ascending(Competition::date.name)).map { it.toDomain() }.toList()
            } ?: emptyList()
        }
}
