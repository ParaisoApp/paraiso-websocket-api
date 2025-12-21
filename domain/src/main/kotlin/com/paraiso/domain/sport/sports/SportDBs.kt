package com.paraiso.domain.sport.sports

import com.paraiso.domain.sport.interfaces.AthletesDB
import com.paraiso.domain.sport.interfaces.BoxscoresDB
import com.paraiso.domain.sport.interfaces.CoachesDB
import com.paraiso.domain.sport.interfaces.CompetitionsDB
import com.paraiso.domain.sport.interfaces.LeadersDB
import com.paraiso.domain.sport.interfaces.LeaguesDB
import com.paraiso.domain.sport.interfaces.RostersDB
import com.paraiso.domain.sport.interfaces.SchedulesDB
import com.paraiso.domain.sport.interfaces.ScoreboardsDB
import com.paraiso.domain.sport.interfaces.StandingsDB
import com.paraiso.domain.sport.interfaces.TeamsDB

data class SportDBs(
    val leaguesDB: LeaguesDB,
    val standingsDB: StandingsDB,
    val teamsDB: TeamsDB,
    val rostersDB: RostersDB,
    val athletesDB: AthletesDB,
    val coachesDB: CoachesDB,
    val schedulesDB: SchedulesDB,
    val scoreboardsDB: ScoreboardsDB,
    val boxscoresDB: BoxscoresDB,
    val competitionsDB: CompetitionsDB,
    val leadersDB: LeadersDB
)
