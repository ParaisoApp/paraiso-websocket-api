package com.paraiso.client.sport

import com.paraiso.client.util.BaseAdapter
import com.paraiso.client.util.ClientConfig
import com.paraiso.domain.sport.SportOperation
import io.klogging.Klogging
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.paraiso.domain.sport.sports.AllStandings as AllStandingsDomain
import com.paraiso.domain.sport.sports.BoxScore as BoxScoreDomain
import com.paraiso.domain.sport.sports.Roster as RosterDomain
import com.paraiso.domain.sport.sports.Scoreboard as ScoreboardDomain
import com.paraiso.domain.sport.sports.StatLeaders as StatLeadersDomain
import com.paraiso.domain.sport.sports.Team as TeamDomain

class SportOperationAdapter() : SportOperation, BaseAdapter, Klogging {

    companion object {
        private const val SEASON = 2025
        private const val REGULAR = 2
        private const val LIMIT = 10

        // private const val PLAYOFFS = 1
        private const val EAST = 5
        private const val WEST = 6
        private const val OVERALL = 0
        private val dispatcher = Dispatchers.IO
        private val clientConfig = ClientConfig()
    }

    override suspend fun getScoreboard(): ScoreboardDomain? = withContext(dispatcher) {
        try {
            val url = "${clientConfig.statsBaseUrl}/scoreboard"
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
            val url = "${clientConfig.statsBaseUrl}/summary?event=$gameId"
            val response: RestGameStats = getHttpClient().use { httpClient ->
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
    override suspend fun getStandings(): AllStandingsDomain? = withContext(dispatcher) {
        try {
            val westUrl = "${clientConfig.cdnApiBaseUrl}/standings?xhr=1"
            val standingsResponse: RestStandingsContainer = getHttpClient().use { httpClient ->
                httpClient.get(westUrl).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            standingsResponse.toDomain()
        } catch (ex: Exception) {
            logger.error("ex: $ex")
            null
        }
    }
    override suspend fun getTeams(): List<TeamDomain> = withContext(dispatcher) {
        try {
            val url = "${clientConfig.statsBaseUrl}/teams"
            val response: RestTeams = getHttpClient().use { httpClient ->
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
            emptyList()
        }
    }
    override suspend fun getRoster(teamId: String): RosterDomain? = withContext(dispatcher) {
        try {
            val url = "${clientConfig.statsBaseUrl}/teams/$teamId/roster"
            val response: RestRoster = getHttpClient().use { httpClient ->
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
    override suspend fun getLeaders(): StatLeadersDomain? = withContext(dispatcher) {
        try {
            val url = "${clientConfig.coreApiBaseUrl}/seasons/$SEASON/types/$REGULAR/leaders?limit=$LIMIT"
            val response: RestLeaders = getHttpClient().use { httpClient ->
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
    override suspend fun getSchedule(teamId: String) = withContext(dispatcher) {
        try {
            val url = "${clientConfig.statsBaseUrl}/teams/$teamId/schedule?season=$SEASON"
            val response: RestSchedule = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            println(response)
        } catch (ex: Exception) {
            logger.error("ex: $ex")
        }
    }
}
