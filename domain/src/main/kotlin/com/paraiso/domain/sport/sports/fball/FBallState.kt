package com.paraiso.domain.sport.sports.fball

import com.paraiso.domain.sport.data.FullTeam
import com.paraiso.domain.sport.data.Roster
import com.paraiso.domain.sport.data.Schedule
import com.paraiso.domain.sport.data.Scoreboard
import com.paraiso.domain.sport.data.StatLeaders
import com.paraiso.domain.sport.data.Team

object FBallState {
    var scoreboard: Scoreboard? = null
    var teams: List<Team> = emptyList()
    var boxScores: List<FullTeam> = emptyList()
    var rosters: List<Roster> = emptyList()
    var leaders: StatLeaders? = null
    var schedules: List<Schedule> = emptyList()
}
