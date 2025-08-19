package com.paraiso.domain.sport.sports.bball

import com.paraiso.domain.routes.SiteRoute
import com.paraiso.domain.sport.adapters.AthletesDBAdapter
import com.paraiso.domain.sport.adapters.CoachesDBAdapter
import com.paraiso.domain.sport.adapters.CompetitionsDBAdapter
import com.paraiso.domain.sport.adapters.LeadersDBAdapter
import com.paraiso.domain.sport.adapters.RostersDBAdapter
import com.paraiso.domain.sport.adapters.SchedulesDBAdapter
import com.paraiso.domain.sport.adapters.StandingsDBAdapter
import com.paraiso.domain.sport.adapters.TeamsDBAdapter
import com.paraiso.domain.sport.data.LeaderResponse
import com.paraiso.domain.sport.data.RosterResponse
import com.paraiso.domain.sport.data.toDomain
import com.paraiso.domain.sport.data.toResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class BBallApi(
    private val teamsDBAdapter: TeamsDBAdapter,
    private val rostersDBAdapter: RostersDBAdapter,
    private val athletesDBAdapter: AthletesDBAdapter,
    private val coachesDBAdapter: CoachesDBAdapter,
    private val standingsDBAdapter: StandingsDBAdapter,
    private val schedulesDBAdapter: SchedulesDBAdapter,
    private val competitionsDBAdapter: CompetitionsDBAdapter,
    private val leadersDBAdapter: LeadersDBAdapter
) {
    suspend fun getTeamByAbbr(teamAbbr: String) = teamsDBAdapter.findById("${SiteRoute.BASKETBALL}-$teamAbbr")?.toResponse()
    suspend fun getTeams() = teamsDBAdapter.findBySport(SiteRoute.BASKETBALL).map { it.toResponse() }.associateBy { it.id }
    suspend fun getStandings() = standingsDBAdapter.findById(SiteRoute.BASKETBALL.toString())?.standingsGroups?.associate { standingsGroup ->
        standingsGroup.confAbbr to standingsGroup.standings.map { it.toResponse() }
    }
    suspend fun getLeaders() = leadersDBAdapter.findBySport(SiteRoute.BASKETBALL)?.categories?.let { categories ->
        // grab athletes from DB and associate with their id
        val athletes = categories.flatMap { category -> category.leaders.map { it.athleteId } }.let { athleteIds ->
            athletesDBAdapter.findByIdsIn(athleteIds.map { it.toString() })
        }.associateBy { it.id }
        // associate each category with athlete name and stats
        categories.associate {
            it.displayName to it.leaders.mapNotNull { leader ->
                athletes[leader.athleteId.toString()]?.let { athlete ->
                    LeaderResponse(
                        athleteName = athlete.shortName,
                        leaderStat = leader.value,
                        teamAbbr = athlete.teamAbbr
                    )
                }
            }
        }
    }

    suspend fun getLeaderCategories() = leadersDBAdapter.findBySport(SiteRoute.BASKETBALL)?.categories?.map { it.displayName }
    suspend fun getTeamRoster(teamId: String) = coroutineScope {
        rostersDBAdapter.findById("${SiteRoute.BASKETBALL}-$teamId")?.let { rosterEntity ->
            val athletes = async {
                athletesDBAdapter.findByIdsIn(rosterEntity.athletes)
            }
            val coach = async {
                rosterEntity.coach?.let { coachesDBAdapter.findById(it) }
            }
            RosterResponse(
                id = rosterEntity.id,
                athletes = athletes.await().map { it.toResponse() },
                coach = coach.await()?.toResponse(),
                teamId = rosterEntity.teamId
            )
        }
    }
    suspend fun getTeamSchedule(teamId: String, seasonYear: String, seasonType: String) =
        schedulesDBAdapter.findById("${SiteRoute.BASKETBALL}-$teamId-$seasonYear-$seasonType")?.let { schedule ->
            val competitions = competitionsDBAdapter.findByIdIn(schedule.events)
            schedule.toDomain(competitions)
        }?.toResponse()
}
