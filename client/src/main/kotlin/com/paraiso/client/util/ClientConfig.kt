package com.paraiso.client.util

data class ClientConfig(
    var statsBaseUrl: String = "https://site.api.espn.com/apis/site/v2/sports",
    var coreApiBaseUrl: String = "https://sports.core.api.espn.com/v2/sports",
    var cdnApiBaseUrl: String = "https://cdn.espn.com/core",
    var siteBaseUrl: String = "https://site.web.api.espn.com/apis/site/v2/sports",
    var bballStatsUri: String = "/basketball/nba",
    var bballCoreUri: String = "/basketball/leagues/nba",
    var bballCdnUri: String = "/nba",
    var fballStatsUri: String = "/football/nfl",
    var fballCoreUri: String = "/football/leagues/nfl",
    var fballCdnUri: String = "/nfl",
)
