package com.paraiso.domain.sport.sports

import com.paraiso.domain.sport.adapters.AthletesDBAdapter
import com.paraiso.domain.sport.adapters.BoxscoresDBAdapter
import com.paraiso.domain.sport.adapters.CoachesDBAdapter
import com.paraiso.domain.sport.adapters.CompetitionsDBAdapter
import com.paraiso.domain.sport.adapters.LeadersDBAdapter
import com.paraiso.domain.sport.adapters.LeaguesDBAdapter
import com.paraiso.domain.sport.adapters.RostersDBAdapter
import com.paraiso.domain.sport.adapters.SchedulesDBAdapter
import com.paraiso.domain.sport.adapters.ScoreboardsDBAdapter
import com.paraiso.domain.sport.adapters.StandingsDBAdapter
import com.paraiso.domain.sport.adapters.TeamsDBAdapter

data class SportDBs(
    val leaguesDBAdapter: LeaguesDBAdapter,
    val standingsDBAdapter: StandingsDBAdapter,
    val teamsDBAdapter: TeamsDBAdapter,
    val rostersDBAdapter: RostersDBAdapter,
    val athletesDBAdapter: AthletesDBAdapter,
    val coachesDBAdapter: CoachesDBAdapter,
    val schedulesDBAdapter: SchedulesDBAdapter,
    val scoreboardsDBAdapter: ScoreboardsDBAdapter,
    val boxscoresDBAdapter: BoxscoresDBAdapter,
    val competitionsDBAdapter: CompetitionsDBAdapter,
    val leadersDBAdapter: LeadersDBAdapter
)