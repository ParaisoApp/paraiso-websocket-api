package com.paraiso.domain.sport.sports

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.data.BoxScore
import com.paraiso.domain.sport.data.Competition
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.data.ScoreboardEntity
import com.paraiso.domain.sport.data.init
import com.paraiso.domain.sport.data.toEntity
import io.klogging.Klogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

object SportState: Klogging {
    private val scoreboards = ConcurrentHashMap<String, MutableStateFlow<ScoreboardEntity>>()
    private val competitions = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableStateFlow<Competition>>>()
    private val boxScores = ConcurrentHashMap<String, MutableStateFlow<BoxScore>>()
    private val triggerRestart = ConcurrentHashMap(
        listOf(
            SiteRoute.FOOTBALL.name to MutableStateFlow(false),
            SiteRoute.BASKETBALL.name to MutableStateFlow(false),
            SiteRoute.HOCKEY.name to MutableStateFlow(false)
        ).toMap()
    )
    fun getAllTriggers(): Map<String, StateFlow<Boolean>> = triggerRestart
    fun getCompetitionsIn(ids: Set<String>): List<StateFlow<Competition>> =
        competitions.values.flatMap { it.entries }.filter { ids.contains(it.key) }.map { it.value }
    private fun getCompetition(id: String): StateFlow<Competition>? =
        competitions.values.flatMap { it.entries }.firstOrNull { it.key == id }?.value
    fun getCompetitionsBySport(sport: String): Map<String, StateFlow<Competition>>? = competitions[sport]
    fun getAllBoxScores(): Map<String, StateFlow<BoxScore>> = boxScores
    fun getScoreboardFlow(id: String): StateFlow<ScoreboardEntity> =
        scoreboards.getOrPut(id) { MutableStateFlow(Scoreboard.init().toEntity()) }

    suspend fun updateScoreboard(id: String, newSb: ScoreboardEntity) {
        scoreboards.getOrPut(id) { MutableStateFlow(newSb) }.value = newSb
    }
    fun updateCompetitions(newComps: List<Competition>) {
        var restart = false
        newComps.firstOrNull()?.sport?.name?.let {sport ->
            newComps.forEach { comp ->
                val sportComps = competitions.getOrPut(sport){ ConcurrentHashMap() }
                sportComps.getOrPut(comp.id) { MutableStateFlow(comp) }.value = comp
                if (comp.status.completed) {
                    sportComps.remove(comp.id)
                    restart = true
                }
            }
            val sportRestart = triggerRestart[sport]
            if (restart && sportRestart != null) {
                // when a comp ends we want to restart consumers (flip trigger state - picked up by consumer)
                sportRestart.value = !sportRestart.value
            }
        }
    }

    fun updateBoxScores(newBoxScores: List<BoxScore>) {
        newBoxScores.forEach { boxScore ->
            boxScores.getOrPut(boxScore.id) { MutableStateFlow(boxScore) }.value = boxScore
            val currentComp = getCompetition(boxScore.id)
            if (currentComp == null || currentComp.value.status.completed) {
                boxScores[boxScore.id]?.value = boxScore.copy(completed = true)
                boxScores.remove(boxScore.id)
            }
        }
    }
}
