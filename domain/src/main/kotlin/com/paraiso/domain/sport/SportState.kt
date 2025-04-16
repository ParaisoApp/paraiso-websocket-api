package com.paraiso.domain.sport

import com.paraiso.domain.sport.sports.AllStandings
import com.paraiso.domain.sport.sports.FullTeam
import com.paraiso.domain.sport.sports.Roster
import com.paraiso.domain.sport.sports.Schedule
import com.paraiso.domain.sport.sports.Scoreboard
import com.paraiso.domain.sport.sports.StatLeaders
import com.paraiso.domain.sport.sports.Team

object SportState {
    var scoreboard: Scoreboard? = null
    var teams: List<Team> = emptyList()
    var standings: AllStandings? = null
    var boxScores: List<FullTeam> = emptyList()
    var rosters: List<Roster> = emptyList()
    var leaders: StatLeaders? = null
    var schedules: List<Schedule> = emptyList()
}