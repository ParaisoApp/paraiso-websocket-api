package com.paraiso.domain.sport.sports

import com.paraiso.domain.sport.data.BoxScore
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.data.init
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SportState {
    private val scoreboards = mutableMapOf<String, MutableStateFlow<Scoreboard>>()
    private val boxscores = mutableMapOf<String, MutableStateFlow<List<BoxScore>>>()

    fun getScoreboardFlow(id: String): StateFlow<Scoreboard> =
        scoreboards.getOrPut(id) { MutableStateFlow(Scoreboard.init()) }

    fun updateScoreboard(id: String, newState: Scoreboard) {
        scoreboards.getOrPut(id) { MutableStateFlow(Scoreboard.init()) }.value = newState
    }

    fun getBoxscoreFlow(id: String): StateFlow<List<BoxScore>> =
        boxscores.getOrPut(id) { MutableStateFlow(emptyList()) }

    fun updateBoxscore(id: String, newState: List<BoxScore>) {
        boxscores.getOrPut(id) { MutableStateFlow(emptyList()) }.value = newState
    }
}
