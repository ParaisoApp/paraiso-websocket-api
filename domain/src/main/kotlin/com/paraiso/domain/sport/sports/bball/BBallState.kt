package com.paraiso.domain.sport.sports.bball

import com.paraiso.domain.sport.data.FullTeam
import com.paraiso.domain.sport.data.Roster
import com.paraiso.domain.sport.data.Schedule
import com.paraiso.domain.sport.data.Scoreboard

object BBallState {
    var scoreboard: Scoreboard? = null
    var boxScores: List<FullTeam> = emptyList()
}
