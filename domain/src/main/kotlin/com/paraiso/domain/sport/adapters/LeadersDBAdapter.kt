package com.paraiso.domain.sport.adapters

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.StatLeaders

interface LeadersDBAdapter {
    suspend fun findBySport(sport: SiteRoute): StatLeaders?
    suspend fun save(statLeaders: List<StatLeaders>): Int
}
