package com.example.testRestClient.sport

import com.example.testRestClient.util.ApiConfig
import com.example.testRestClient.util.BaseAdapter
import io.klogging.Klogging
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.messageTypes.BoxScore as BoxScoreDomain
import com.example.messageTypes.Scoreboard as ScoreboardDomain

class SportOperationAdapter(
    private val apiConfig: ApiConfig
) : BaseAdapter, Klogging {

    companion object {
        private val dispatcher = Dispatchers.IO
    }

    suspend fun getSchedule(): ScoreboardDomain? = withContext(dispatcher) {
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
}
