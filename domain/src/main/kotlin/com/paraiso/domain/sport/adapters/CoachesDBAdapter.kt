package com.paraiso.domain.sport.adapters

import com.paraiso.domain.sport.data.Coach

interface CoachesDBAdapter{
    suspend fun findById(id: String): Coach?
    suspend fun save(coaches: List<Coach>): Int
}
