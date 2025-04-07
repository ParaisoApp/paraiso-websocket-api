package com.paraiso.websocket.api.testRestClient.sport

import com.paraiso.websocket.api.testRestClient.util.ApiConfig
import com.paraiso.websocket.api.testRestClient.util.BaseAdapter
import io.klogging.Klogging
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.paraiso.websocket.api.messageTypes.sports.AllStandings as AllStandingsDomain
import com.paraiso.websocket.api.messageTypes.sports.BoxScore as BoxScoreDomain
import com.paraiso.websocket.api.messageTypes.sports.Scoreboard as ScoreboardDomain
import com.paraiso.websocket.api.messageTypes.sports.Team as TeamDomain

class SportOperationAdapter(
    private val apiConfig: ApiConfig
) : BaseAdapter, Klogging {

    companion object {
        private const val SEASON = 2025
        private const val REGULAR = 2

        // private const val PLAYOFFS = 1
        private const val EAST = 5
        private const val WEST = 6
        private const val OVERALL = 0
        private val dispatcher = Dispatchers.IO
    }

    suspend fun getScoreboard(): ScoreboardDomain? = withContext(dispatcher) {
        try {
            val url = "${apiConfig.statsBaseUrl}/scoreboard"
            val response: BBallScoreboard = getHttpClient().use { httpClient ->
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
    suspend fun getGameStats(gameId: String): BoxScoreDomain? = withContext(dispatcher) {
        try {
            val url = "${apiConfig.statsBaseUrl}/summary?event=$gameId"
            val response: BBallGameStats = getHttpClient().use { httpClient ->
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
    suspend fun getStandings(): AllStandingsDomain? = withContext(dispatcher) {
        try {
            val westUrl = "${apiConfig.cdnApiBaseUrl}/standings?xhr=1"
            val standingsResponse: BBallStandingsContainer = getHttpClient().use { httpClient ->
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
    suspend fun getTeams(): List<TeamDomain> = withContext(dispatcher) {
        try {
            val url = "${apiConfig.statsBaseUrl}/teams"
            val response: BBallTeams = getHttpClient().use { httpClient ->
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
}
