package com.paraiso.domain.sport.adapters

import com.paraiso.domain.sport.data.RosterEntity

interface RostersDBAdapter{
    suspend fun findById(id: String): RosterEntity?
    suspend fun save(rosters: List<RosterEntity>): Int
}
