package com.paraiso.client.util

data class ClientConfig(
    var statsBaseUrl: String = "https://site.api.espn.com/apis/site/v2/sports",
    var coreApiBaseUrl: String = "https://sports.core.api.espn.com/v2/sports",
    var cdnApiBaseUrl: String = "https://cdn.espn.com/core",
    var bballStatsUri: String = "/basketball/nba",
    var bballCoreUri: String = "/basketball/leagues/nba",
    var bballCdnUri: String = "/nba",
)
