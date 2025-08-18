package com.paraiso.client.sport.adapters

import com.paraiso.client.sport.RestGameStats
import com.paraiso.client.sport.RestLeaders
import com.paraiso.client.sport.RestRoster
import com.paraiso.client.sport.RestRosterNested
import com.paraiso.client.sport.RestSchedule
import com.paraiso.client.sport.RestScoreboard
import com.paraiso.client.sport.RestStandingsContainer
import com.paraiso.client.sport.RestTeams
import com.paraiso.client.sport.toDomain
import com.paraiso.client.util.BaseAdapter
import com.paraiso.client.util.ClientConfig
import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.sports.fball.FBallOperation
import io.klogging.Klogging
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.paraiso.domain.sport.data.AllStandings as AllStandingsDomain
import com.paraiso.domain.sport.data.BoxScore as BoxScoreDomain
import com.paraiso.domain.sport.data.Roster as RosterDomain
import com.paraiso.domain.sport.data.Schedule as ScheduleDomain
import com.paraiso.domain.sport.data.Scoreboard as ScoreboardDomain
import com.paraiso.domain.sport.data.StatLeaders as StatLeadersDomain
import com.paraiso.domain.sport.data.Team as TeamDomain

class FBallOperationAdapter() : FBallOperation, BaseAdapter, Klogging {

    companion object {
        private const val SEASON = 2025
        private const val REGULAR = 2
        private const val LIMIT = 10
        private val dispatcher = Dispatchers.IO
        private val clientConfig = ClientConfig()

        // private const val PLAYOFFS = 1
        // private const val EAST = 5
        // private const val WEST = 6
        // private const val OVERALL = 0
    }

    override suspend fun getScoreboard(): ScoreboardDomain? = withContext(dispatcher) {
        try {
            val url = "${clientConfig.statsBaseUrl}${clientConfig.fballStatsUri}/scoreboard"
            val response: RestScoreboard = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain()
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }
    override suspend fun getGameStats(gameId: String): BoxScoreDomain? = withContext(dispatcher) {
        try {
            val url = "${clientConfig.siteBaseUrl}${clientConfig.fballStatsUri}/summary?event=$gameId"
            val response: RestGameStats = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain(SiteRoute.FOOTBALL)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }
    override suspend fun getStandings(): AllStandingsDomain? = withContext(dispatcher) {
        try {
            val westUrl = "${clientConfig.cdnApiBaseUrl}${clientConfig.fballCdnUri}/standings?xhr=1"
            val standingsResponse: RestStandingsContainer = getHttpClient().use { httpClient ->
                httpClient.get(westUrl).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            standingsResponse.toDomain(SiteRoute.FOOTBALL)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }
    override suspend fun getTeams(): List<TeamDomain> = withContext(dispatcher) {
        try {
            val url = "${clientConfig.statsBaseUrl}${clientConfig.fballStatsUri}/teams"
            val response: RestTeams = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain(SiteRoute.FOOTBALL)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            emptyList()
        }
    }
    override suspend fun getRoster(teamId: String): RosterDomain? = withContext(dispatcher) {
        try {
            val url = "${clientConfig.statsBaseUrl}${clientConfig.fballStatsUri}/teams/$teamId/roster"
            val response: RestRosterNested = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain(SiteRoute.FOOTBALL)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }
    override suspend fun getLeaders(): StatLeadersDomain? = withContext(dispatcher) {
        try {
            val url = "${clientConfig.coreApiBaseUrl}${clientConfig.fballCoreUri}/leaders?limit=$LIMIT"
            val response: RestLeaders = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain(SiteRoute.FOOTBALL)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }
    override suspend fun getSchedule(teamId: String): ScheduleDomain? = withContext(dispatcher) {
        try {
            val url = "${clientConfig.statsBaseUrl}${clientConfig.fballStatsUri}/teams/$teamId/schedule?season=$SEASON"
            val response: RestSchedule = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            response.toDomain()
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }
}
