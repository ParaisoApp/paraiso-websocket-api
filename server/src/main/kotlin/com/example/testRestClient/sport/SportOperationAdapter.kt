package com.example.testRestClient.sport

import com.example.testRestClient.util.ApiConfig
import com.example.testRestClient.util.BaseAdapter
import io.klogging.Klogging
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.messageTypes.sports.BoxScore as BoxScoreDomain
import com.example.messageTypes.sports.Scoreboard as ScoreboardDomain
import com.example.messageTypes.sports.AllStandings as AllStandingsDomain
import com.example.messageTypes.sports.Team as TeamDomain

class SportOperationAdapter(
    private val apiConfig: ApiConfig
) : BaseAdapter, Klogging {

    companion object {
        private const val SEASON = 2025
        private const val REGULAR = 2
        //private const val PLAYOFFS = 1
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
            val eastUrl = "${apiConfig.coreApiBaseUrl}/seasons/$SEASON/types/$REGULAR/groups/$EAST/standings/$OVERALL"
            val eastResponse: BBallStandings = getHttpClient().use { httpClient ->
                httpClient.get(eastUrl).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            val westUrl = "${apiConfig.coreApiBaseUrl}/seasons/$SEASON/types/$REGULAR/groups/$WEST/standings/$OVERALL"
            val westResponse: BBallStandings = getHttpClient().use { httpClient ->
                httpClient.get(westUrl).let {
                    if (it.status != HttpStatusCode.OK) {
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            AllStandingsDomain(eastResponse.toDomain().standings + westResponse.toDomain().standings)
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
