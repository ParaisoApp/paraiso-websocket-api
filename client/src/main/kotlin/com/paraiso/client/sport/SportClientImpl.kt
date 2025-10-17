package com.paraiso.client.sport

import com.paraiso.client.sport.data.RestGameStats
import com.paraiso.client.sport.data.RestLeaders
import com.paraiso.client.sport.data.RestLeague
import com.paraiso.client.sport.data.RestRoster
import com.paraiso.client.sport.data.RestRosterNested
import com.paraiso.client.sport.data.RestSchedule
import com.paraiso.client.sport.data.RestScoreboard
import com.paraiso.client.sport.data.RestStandingsContainer
import com.paraiso.client.sport.data.RestTeams
import com.paraiso.client.sport.data.toDomain
import com.paraiso.client.util.BaseAdapter
import com.paraiso.client.util.ClientConfig
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.League
import com.paraiso.domain.sport.sports.SportClient
import io.klogging.Klogging
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.paraiso.domain.sport.data.AllStandings as AllStandingsDomain
import com.paraiso.domain.sport.data.BoxScore as BoxScoreDomain
import com.paraiso.domain.sport.data.Roster as RosterDomain
import com.paraiso.domain.sport.data.Schedule as ScheduleDomain
import com.paraiso.domain.sport.data.Scoreboard as ScoreboardDomain
import com.paraiso.domain.sport.data.StatLeaders as StatLeadersDomain
import com.paraiso.domain.sport.data.Team as TeamDomain

class SportClientImpl : SportClient, BaseAdapter, Klogging {

    companion object {
        private const val LIMIT = 10
        private val dispatcher = Dispatchers.IO
        private val clientConfig = ClientConfig()
    }

    override suspend fun getLeague(sport: SiteRoute): League? = withContext(dispatcher) {
        try {
            var url = clientConfig.coreApiBaseUrl
            when (sport) {
                SiteRoute.BASKETBALL -> url += clientConfig.bballCoreUri
                SiteRoute.FOOTBALL -> url += clientConfig.fballCoreUri
                else -> { logger.info { "Unrecognized sport $sport" } }
            }
            val response: RestLeague = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain(sport)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }

    override suspend fun getScoreboard(sport: SiteRoute): ScoreboardDomain? = withContext(dispatcher) {
        try {
            var url = clientConfig.statsBaseUrl
            when (sport) {
                SiteRoute.BASKETBALL -> url += clientConfig.bballStatsUri
                SiteRoute.FOOTBALL -> url += clientConfig.fballStatsUri
                else -> { logger.info { "Unrecognized sport $sport" } }
            }
            url += "/scoreboard"
            val response: RestScoreboard = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain(sport)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }

    override suspend fun getScoreboardWithDate(
        sport: SiteRoute,
        date: String
    ): ScoreboardDomain? = withContext(dispatcher) {
        try {
            var url = clientConfig.statsBaseUrl
            when (sport) {
                SiteRoute.BASKETBALL -> url += clientConfig.bballStatsUri
                SiteRoute.FOOTBALL -> url += clientConfig.fballStatsUri
                else -> { logger.info { "Unrecognized sport $sport" } }
            }
            url += "/scoreboard?$date"
            val response: RestScoreboard = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain(sport)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }
    override suspend fun getGameStats(sport: SiteRoute, competitionId: String): BoxScoreDomain? = withContext(dispatcher) {
        try {
            var url = clientConfig.statsBaseUrl
            when (sport) {
                SiteRoute.BASKETBALL -> url += clientConfig.bballStatsUri
                SiteRoute.FOOTBALL -> url += clientConfig.fballStatsUri
                else -> { logger.info { "Unrecognized sport $sport" } }
            }
            url += "/summary?event=$competitionId"
            val response: RestGameStats = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain(competitionId)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }
    override suspend fun getStandings(sport: SiteRoute): AllStandingsDomain? = withContext(dispatcher) {
        try {
            var url = clientConfig.cdnApiBaseUrl
            when (sport) {
                SiteRoute.BASKETBALL -> url += clientConfig.bballCdnUri
                SiteRoute.FOOTBALL -> url += clientConfig.fballCdnUri
                else -> { logger.info { "Unrecognized sport $sport" } }
            }
            url += "/standings?xhr=1"
            val standingsResponse: RestStandingsContainer = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            standingsResponse.toDomain(sport)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }
    override suspend fun getTeams(sport: SiteRoute): List<TeamDomain> = withContext(dispatcher) {
        try {
            var url = clientConfig.statsBaseUrl
            when (sport) {
                SiteRoute.BASKETBALL -> url += clientConfig.bballStatsUri
                SiteRoute.FOOTBALL -> url += clientConfig.fballStatsUri
                else -> { logger.info { "Unrecognized sport $sport" } }
            }
            url += "/teams"
            val response: RestTeams = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain(sport)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            emptyList()
        }
    }
    override suspend fun getRoster(sport: SiteRoute, teamId: String): RosterDomain? = withContext(dispatcher) {
        try {
            var url = clientConfig.statsBaseUrl
            when (sport) {
                SiteRoute.BASKETBALL -> url += clientConfig.bballStatsUri
                SiteRoute.FOOTBALL -> url += clientConfig.fballStatsUri
                else -> { logger.info { "Unrecognized sport $sport" } }
            }
            url += "/teams/$teamId/roster"
            val response: String = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            val json = Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
            val parsedResponse = when (sport) {
                SiteRoute.BASKETBALL -> json.decodeFromString<RestRoster>(response).toDomain(sport)
                SiteRoute.FOOTBALL -> json.decodeFromString<RestRosterNested>(response).toDomain(sport)
                else -> { null }
            }
            parsedResponse
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }
    override suspend fun getLeaders(
        sport: SiteRoute,
        season: String,
        type: String
    ): StatLeadersDomain? = withContext(dispatcher) {
        try {
            var url = clientConfig.coreApiBaseUrl
            when (sport) {
                SiteRoute.BASKETBALL -> url += clientConfig.bballCoreUri
                SiteRoute.FOOTBALL -> url += clientConfig.fballCoreUri
                else -> { logger.info { "Unrecognized sport $sport" } }
            }
            url += "/seasons/$season/types/$type/leaders"
            val response: RestLeaders = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain(sport, season, type)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }
    override suspend fun getTeamLeaders(
        sport: SiteRoute,
        season: String,
        type: String,
        teamId: String
    ): StatLeadersDomain? = withContext(dispatcher) {
        try {
            var url = clientConfig.coreApiBaseUrl
            when (sport) {
                SiteRoute.BASKETBALL -> url += clientConfig.bballCoreUri
                SiteRoute.FOOTBALL -> url += clientConfig.fballCoreUri
                else -> { logger.info { "Unrecognized sport $sport" } }
            }
            url += "/seasons/$season/types/$type/teams/$teamId/leaders"
            val response: RestLeaders = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain(sport, season, type, teamId)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }
    override suspend fun getSchedule(sport: SiteRoute, teamId: String): ScheduleDomain? = withContext(dispatcher) {
        try {
            var url = clientConfig.statsBaseUrl
            when (sport) {
                SiteRoute.BASKETBALL -> url += clientConfig.bballStatsUri
                SiteRoute.FOOTBALL -> url += clientConfig.fballStatsUri
                else -> { logger.info { "Unrecognized sport $sport" } }
            }
            url += "/teams/$teamId/schedule"
            val response: RestSchedule = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain(sport)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }
}
