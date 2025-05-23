package com.paraiso.client.util

data class ClientConfig(
    var statsBaseUrl: String = "https://site.api.espn.com/apis/site/v2/sports/basketball/nba",
    var coreApiBaseUrl: String = "https://sports.core.api.espn.com/v2/sports/basketball/leagues/nba",
    var cdnApiBaseUrl: String = "https://cdn.espn.com/core/nba"
)
