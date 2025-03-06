package com.example.testRestClient.sport

import com.example.testRestClient.util.BaseAdapter
import io.klogging.Klogging
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

class SportOperationAdapter(
    //private val apiConfig: ApiConfig
): BaseAdapter, Klogging {
    suspend fun getSchedule(): BBallScoreboard? {
        try{
            val url = "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard"
            val response: BBallScoreboard = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if(it.status != HttpStatusCode.OK){
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            getGameStats(response.events[0].competitions[0].id)
            return response
        }catch (ex: Exception){
            logger.error("ex: $ex")
            return null
        }
    }
    suspend fun getGameStats(gameId: String): BBallGameStats? {
        try{
            val url = "http://site.api.espn.com/apis/site/v2/sports/basketball/nba/summary?event=$gameId"
            val response: BBallGameStats = getHttpClient().use { httpClient ->
                httpClient.get(url).let {
                    if(it.status != HttpStatusCode.OK){
                        logger.error { "Error fetching data status ${it.status} body: ${it.body<String>()}" }
                    }
                    it.body()
                }
            }
            return response
        }catch (ex: Exception){
            logger.error("ex: $ex")
            return null
        }
    }
}